package com.example.stream

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.example.utils.AkkaApp
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object FutureSource extends AkkaApp {
  val users: Future[Source[Int, NotUsed]] = Future {
    Source((1 to 100).toList)
  }

  Source
    .futureSource(users)
    .runForeach { id: Int =>
      log.info(id.toString)
    }
}
