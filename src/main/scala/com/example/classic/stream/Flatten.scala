package com.example.classic.stream

import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.example.classic.stream.StreamBasicActor.{flow, intSource, sink}
import com.example.utils.AkkaApp

import scala.concurrent.Future

object Flatten extends AkkaApp {
  val strSource: Source[String, NotUsed] = Source(Seq("123", "456"))

  // Sink
  val sink: Sink[Any, Future[Done]] = Sink.foreach(v => print(s"${v} "))
  intSource.runWith(sink)

  // Flow
  val flow: Flow[Int, String, NotUsed] = Flow[Int].map(i => (i * 2).toString)
  intSource.via(flow).runWith(sink)

  // flatten
  strSource.map(a => a.toCharArray.toSeq) // Source[Seq[Char], NotUsed]
    .mapConcat(identity) // Source[Char, NotUsed]
    .map(_.toString.toInt) // Source[Int, NotUsed]
    .via(flow)
    .runWith(sink)
}
