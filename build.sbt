assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly) { (old) => {
  case PathList(ps@_*) if ps.last contains ".class" => MergeStrategy.first
  case x => old(x)
}}

name := "kafka_customer_logins"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val sparkVersion = "1.5.2"

  Seq(
    "org.apache.spark"            %% "spark-core"                     % sparkVersion % "provided",
    "org.apache.spark"            %% "spark-streaming"                % sparkVersion % "provided",
    "org.apache.spark"            %% "spark-streaming-kafka"          % sparkVersion,
    "org.scalatest"               % "scalatest_2.11"                  % "3.0.0"      % "test",
    "io.spray"                    %% "spray-json"                     % "1.3.2",
    "com.github.benfradet"        % "spark-kafka-writer_2.11"         % "0.1.0"
  )
}
