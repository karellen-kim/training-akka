package com.example.classic.stream

import akka.stream.scaladsl.Source
import com.example.utils.AkkaApp
import org.slf4j.LoggerFactory

object PaginationSync extends AkkaApp {
  val log = LoggerFactory.getLogger(this.getClass)

  Source
    .unfold(Some(0): Option[Int] /* startPage */) { pageOpt: Option[Int] =>
      pageOpt match {
        case Some(page) =>
          val result = fetchSync(page)
          result.nextPage match {
            case Some(n) =>
              Some(Some(page + 1), result)
            case None =>
              Some(None: Option[Int], result)
          }
        case None =>
          None
      }
    }
    .runForeach { r =>
      log.info(r.toString)
    }

  case class Result(nextPage: Option[Int],
                    list: Seq[String])

  // dummy
  def fetchSync(page: Int): Result = {
    log.info(s"fetch ${page}")

    // 100 페이지까지 있다고 가정하자
    val nextPage = if (page < 100) {
      Some(page + 1)
    } else {
      None
    }

    Result(nextPage, (1 to 10).map(i => s"item_${page}_${i}"))
  }

}
