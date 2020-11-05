package com.example.stream

import akka.stream.scaladsl.Source
import com.example.utils.AkkaApp

object Unfold extends AkkaApp {

  Source.unfold(0 -> 1) {
    case (preSum, _) if preSum > 10000000 =>
      None
    case (preSum, b) =>
      val s = (b -> (preSum + b))
      println(s"preSum=${preSum}, b=${b}, s=${s}")
      Some(s -> preSum)
  }.runForeach { n =>
    println(n)
  }

}
