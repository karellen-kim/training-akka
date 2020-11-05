package com.example.stream

import akka.stream.scaladsl.Source
import com.example.utils.AkkaApp

object Grouped extends AkkaApp {

  Source(1 to 65)
    .grouped(10)
    .runForeach(println)
}
