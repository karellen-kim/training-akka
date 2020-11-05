package com.example.stream

import akka.stream.scaladsl.{Sink, Source}
import com.example.utils.AkkaApp
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object AsyncBoundary extends AkkaApp {
  val blockingDispatcher: ExecutionContext = system.dispatchers.lookup("blocking-thread-pool-dispatcher")

  def spin(value: Int): Int = {
    val start = System.currentTimeMillis()
    while ((System.currentTimeMillis() - start) < 1000) {}
    log.info(value.toString)
    value
  }

  /**
    * 동일한 actor에서 실행된다
    * dispatcher-5 : 1
    * dispatcher-5 : 1
    * dispatcher-5 : 2
    * dispatcher-5 : 2
    * dispatcher-5 : 3
    * dispatcher-5 : 3
    */
  Source(1 to 3)
    .map(spin)
    .map(spin)
    .runWith(Sink.ignore)

  /**
    * Async를 기준으로 다른 actor에서 실행
    * dispatcher-7 : 1
    * dispatcher-7 : 2
    * dispatcher-6 : 1
    * dispatcher-7 : 3
    * dispatcher-6 : 2
    * dispatcher-6 : 3
    */
  Source(1 to 3)
    .map(spin)
    .async
    .map(spin)
    .runWith(Sink.ignore)
}
