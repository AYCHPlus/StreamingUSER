package com.crystal
package processors

// akka
import akka.actor._

// Spark
import org.apache.spark.streaming.kinesis._
import org.apache.spark.streaming.{ Duration, StreamingContext }
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.storage.StorageLevel
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream

// JSON Parsing
import scala.util.parsing.json.JSON

// Messages
import Overseer.ProcessorReady

class CommandStreamProcessor(appConfig: AppConfig, streamingCtx: StreamingContext) extends Actor {
  import CommandStreamProcessor._

  override def preStart() {
    super.preStart()
    val cmdStream = getCommandStream()

    cmdStream.foreachRDD { rdd =>
      rdd.collect().foreach{ cmd =>
        println("--- Command Received ---")
      }
    }

    context.parent ! ProcessorReady(self)
  }

  def receive = {
    case _ => ()
  }

  private def getCommandStream(): DStream[Map[String, Any]] = {
    val stream = KinesisUtils.createStream(
      streamingCtx,
      appConfig.commandAppName,
      appConfig.commandStreamName,
      s"kinesis.${appConfig.regionName}.amazonaws.com",
      appConfig.regionName,
      InitialPositionInStream.LATEST,
      Duration(appConfig.checkpointInterval),
      StorageLevel.MEMORY_AND_DISK_2
    )

    stream
      .map { byteArray => new String(byteArray) }
      .map { jsonStr => JSON.parseFull(jsonStr).get.asInstanceOf[Map[String, Any]] }
  }
}

object CommandStreamProcessor {
  def props(appConfig: AppConfig, streamingCtx: StreamingContext): Props = {
    Props(new CommandStreamProcessor(appConfig, streamingCtx))
  }
}
