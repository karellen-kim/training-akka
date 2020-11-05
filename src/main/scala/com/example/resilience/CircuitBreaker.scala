package com.example.resilience

import akka.pattern.CircuitBreaker
import akka.stream.scaladsl.Source
import com.example.utils.AkkaApp

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.language.postfixOps

object CircuitBreaker extends AkkaApp {
  val breaker =
    new CircuitBreaker(scheduler = system.scheduler,
      maxFailures = 3,
      callTimeout = 10 seconds,
      resetTimeout = 3 seconds)

  var count = 0
  def func(count: Int): Future[String] = {
    Future {
      log.info("Future Execute!!")
      if (count < 5) {
        throw new RuntimeException("ERROR")
      } else {
        "성공"
      }
    }
  }


  // 오류가 3번 반복되면 3초간 fail fast
  (1 to 10).foreach { _ =>
    Thread.sleep((1 seconds).toMillis)
    count += 1

    breaker.withCircuitBreaker {
      func(count)
    }.onComplete {
      case Success(value) =>
        log.info(s"count : ${count}, ${value}")
      case Failure(e) =>
        log.error(s"count: ${count}, ${e.getMessage}")
    }
  }

}
