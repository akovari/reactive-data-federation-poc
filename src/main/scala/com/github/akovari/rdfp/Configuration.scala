package com.github.akovari.rdfp

import com.typesafe.config.ConfigFactory

/**
 * Created by akovari on 13.6.2015.
 */
object Configuration {
  val cfg = ConfigFactory.load

  @inline def apply() = cfg
}
