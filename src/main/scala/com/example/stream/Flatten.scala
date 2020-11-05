package com.example.stream

import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.example.stream.StreamBasicActor.{flow, intSource, sink}
import com.example.utils.AkkaApp

import scala.concurrent.Future


object Flatten extends AkkaApp {
  /**
    * identity
    */
  val strSource: Source[String, NotUsed] = Source(Seq("123", "456"))

  // Sink
  val sink: Sink[Any, Future[Done]] = Sink.foreach(v => print(s"${v} "))

  // Flow
  val flow: Flow[Int, String, NotUsed] = Flow[Int].map(i => (i * 2).toString)

  // flatten
  strSource.map(a => a.toCharArray.toSeq) // Source[Seq[Char], NotUsed]
    .mapConcat(identity) // Source[Char, NotUsed]
    .map(_.toString.toInt) // Source[Int, NotUsed]
    .via(flow)
    .runWith(sink)

  val optSource = Source(Seq(Some(1), None, Some(2), None, Some(3), Some(4)))

  optSource
    .grouped(10)
    .mapConcat(_.flatten)
    //.runForeach(println)

  optSource
    .grouped(10)
    .map(_.flatten)
    .runForeach(println)
}
