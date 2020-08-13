package com.example.classic.stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}

case class Item(id: Int)
case class NItem(id: Int, name: String)

class GraphDSLSample {
  val graph = RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder =>
      import GraphDSL.Implicits._
      val source = Source.single((0 to 1000).map(i => Item(i))).mapConcat(identity)

      val broadcast = builder.add(Broadcast[Item](3))
      val merge = builder.add(Merge[String](2))

      val printStringSink = Sink.foreach[String](println)
      val printNItemSink = Sink.foreach[NItem](println)

      val toStringFlow = Flow[Item].map(i => s"item.id=${i.id}")
      val eventFilterFlow = Flow[Item].filter(_.id % 2 == 0)
      val transformFlow = Flow[Item].map(i => NItem(i.id, i.id.toString))

      source ~> broadcast
                broadcast   ~> toStringFlow                       ~> merge  ~> printStringSink
                broadcast   ~> eventFilterFlow  ~> toStringFlow   ~> merge
                broadcast   ~> eventFilterFlow  ~> transformFlow  ~> printNItemSink

      ClosedShape
    }
  )
}

object GraphDSLApp extends App {
  implicit val system = ActorSystem("ActorSystem")
  implicit val materializer = ActorMaterializer()

  val g = new GraphDSLSample
  g.graph.run()
}