package com.example.stream

import akka.stream.scaladsl.Source
import com.example.utils.AkkaApp
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

object PaginationAsync extends AkkaApp {
  Source
    .unfoldAsync(Some(0): Option[Int] /* startPage */) { pageOpt: Option[Int] =>
      pageOpt match {
        case Some(page) =>
          fetchAsync(page).map {
            result =>
              result.nextPage match {
                case Some(n) =>
                  Some(Some(page + 1), result)
                case None =>
                  Some(None: Option[Int], result)
              }
          }
        case None =>
          Future.successful(None)
      }
    }
    .mapAsync(100){ r =>
      Future {
        //Thread.sleep(1000 * 3)
        log.info(r.toString)
        r
      }
    }
    .run()
    .onComplete {
      case Success(v) =>
        log.info("END")
      case Failure(e) =>
        log.error(s"ERROR ${e.getMessage}")
    }

  case class Result(nextPage: Option[Int],
                    list: Seq[String])

  def fetchAsync(page: Int): Future[Result] = {
    log.info(s"fetch ${page}")

    // 100 페이지까지 있다고 가정하자
    val nextPage = if (page < 100) {
      Some(page + 1)
    } else {
      None
    }
    Future {
      //Thread.sleep(1000 * 1)
      Result(nextPage, (1 to 10).map(i => s"item_${page}_${i}"))
    }
  }
}
