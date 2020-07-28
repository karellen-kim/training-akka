package com.example.classic.stream

import java.util.concurrent.atomic.AtomicInteger

import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import com.example.utils.AkkaApp

import scala.concurrent.Future
import scala.language.postfixOps

// 참고 : https://gist.github.com/rrodseth/ed3e5edf90ce36e3cf6b
object Repeat extends AkkaApp{
  val sink: Sink[Any, Future[Done]] = Sink.foreach(println)
  var start = new AtomicInteger(0)

  val repeatSource: Source[List[Int], NotUsed] = Source.repeat {
      println("# Execute repeatSource")
      (start.get() to start.addAndGet(10)).toList
  }
  //repeatSource.runWith(sink)

  def callFunc(): Future[List[Int]] = Future {
    println("# Execute future")
    if (start.get() < 100) {
      (start.get() to start.addAndGet(10)).toList
    } else {
      Nil : List[Int]
      (start.get() to start.addAndGet(10)).toList
    }
  }

  val startPage = 1
  val source = Source.unfoldAsync(startPage) { curPage =>
    val next = callFunc().map { r =>
      r match {
        case Nil => None // 값이 없을 때까지 full-scan
        case _ if curPage > 1000 => None // 최대 1000 page
        case _ => Some(curPage + 1, r)
      }
    }
    next
  }
  source.runWith(sink)
}
