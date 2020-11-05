package com.example.stream

import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.example.utils.AkkaApp
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object StreamBasicActor extends AkkaApp {
  val intSource: Source[Int, NotUsed] = Source(1 to 10)

  // runForeach
  intSource.runForeach(i => print(i))

  // Sink
  val sink: Sink[Any, Future[Done]] = Sink.foreach(v => print(s"${v} "))
  intSource.runWith(sink)

  // Flow
  val flow: Flow[Int, String, NotUsed] = Flow[Int].map(i => (i * 2).toString)
  intSource.via(flow).runWith(sink)

}
