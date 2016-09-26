package com.betsson.access.control

import org.apache.spark.{SparkContext, Accumulator}

object FailedLoginsCounter {

  @volatile private var instance: Accumulator[Long] = null

  def getInstance(ssc: SparkContext): Accumulator[Long] = {
    if (instance == null) {
      synchronized {
        if (instance == null) {
          instance = ssc.accumulator(0L, "FailedLoginsCounter")
        }
      }
    }
    instance
  }
}