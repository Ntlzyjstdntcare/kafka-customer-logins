package com.betsson.access.control

import _root_.kafka.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming._

import scala.util.Try

import java.util.Properties

import _root_.kafka.serializer.StringDecoder
import com.github.benfradet.spark.kafka.writer.KafkaWriter._
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.spark.streaming.dstream._
import org.apache.spark.streaming.kafka._
import spray.json._

import scala.collection.immutable.Map


case class LoginAttemptMessage(CustomerId: String, BrandName: String, Timestamp: String, IsSuccessful: Boolean)
case class LoginFailuresMessage(CustomerId: String, EventName: String, TimeStamp: String)


object LoginsJsonProtocol extends DefaultJsonProtocol {
  implicit val loginAttemptFormat = jsonFormat4(LoginAttemptMessage)
  implicit val loginFailuresFormat = jsonFormat3(LoginFailuresMessage)
}

object FailedLoginsHandler {
  def main(args: Array[String]): Unit = {
    FailedLoginsHandler()
  }

  def apply(): Unit = {
    new FailedLoginsHandler().start()
  }

  def kafkaParams(): Map[String, String] = {
    Map[String, String](
      "group.id" -> "mygroup",
      "metadata.broker.list" -> "localhost:9092",
      "zookeeper.connection.timeout.ms" -> "10000",
      "auto.offset.reset" -> "smallest"
    )
  }

  val producerConfig = {
    val p = new Properties()
    p.setProperty("bootstrap.servers", "127.0.0.1:9092")
    p.setProperty("key.serializer", classOf[StringSerializer].getName)
    p.setProperty("value.serializer", classOf[StringSerializer].getName)
    p
  }
}

class FailedLoginsHandler {
  val conf = new SparkConf().setMaster("local[2]").setAppName("FailedLoginsHandler")

  val loginsTopic = "CustomerLogins"
  val failuresTopic = "ConsumerTopic"
  val consecutiveFailuresEventName = "Failed3ConsecutiveLogins"

  val s3Bucket = "s3n://<myS3Credentials>@<bucketName>"

  var lastValueFailure = false

  import LoginsJsonProtocol._
  import spray.json._
  import FailedLoginsHandler._

  println("Starting application\n\n\n\n\n\n\n\n\n")

  def start(termination: Int = 0) {

    val ssc = createContext()

    ssc.start()

    if (termination == 0)
      ssc.awaitTermination()
    else
      ssc.awaitTerminationOrTimeout(termination)
  }

  def extractMessage(messageValue: String): Try[LoginAttemptMessage] = {
    Try(messageValue.parseJson.convertTo[LoginAttemptMessage])
  }

  def loginFailed(message: LoginAttemptMessage): Boolean = {
    message match {
      case loginMessage: LoginAttemptMessage => !(loginMessage.IsSuccessful)
      case _ => false
    }
  }

  def checkFailures(ssc: StreamingContext, message: LoginAttemptMessage, accumulator: Accumulator[Long]): Unit = {
    val consecutiveFailures = accumulator.value
    if (consecutiveFailures != 0 && consecutiveFailures % 2 == 0) {
      sendFailuresMessage(ssc, message)
    }
  }

  def sendFailuresMessage(ssc: StreamingContext, message: LoginAttemptMessage): Unit = {
    val failureMessageList: List[JsValue] = List(LoginFailuresMessage(message.CustomerId, consecutiveFailuresEventName, message.Timestamp).toJson)
    val failureMessageRDD: RDD[JsValue] = ssc.sparkContext.parallelize(failureMessageList)
    failureMessageRDD.writeToKafka(producerConfig, message => new ProducerRecord(failuresTopic, message))
  }

  def createContext(): StreamingContext = {
    val ssc = new StreamingContext(conf, Seconds(3))
    val failedLoginsCounter = FailedLoginsCounter.getInstance(ssc.sparkContext)

    val kafkaInputStream: InputDStream[(String, String)] = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc,
      kafkaParams(),
      Set[String](loginsTopic))

    val loginAttemptMessageStream: DStream[LoginAttemptMessage] = kafkaInputStream.map {
      case (_, messageValue: String) =>
        extractMessage(messageValue).getOrElse(LoginAttemptMessage("", "", "", true))
    }

    loginAttemptMessageStream.foreachRDD(rdd => {
      rdd.foreach {
        case (message: LoginAttemptMessage) =>
          loginFailed(message) match {
            case true => if (lastValueFailure == false) lastValueFailure = true else failedLoginsCounter += 1
            case false => lastValueFailure = false
          }
          checkFailures(ssc, message, failedLoginsCounter)
      }})

    ssc.checkpoint(s3Bucket)
    ssc
  }
}

