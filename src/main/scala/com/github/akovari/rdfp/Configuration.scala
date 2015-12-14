package com.github.akovari.rdfp

import scala.concurrent.ExecutionContext.Implicits.global

import com.github.akovari.typesafeSalesforce.util.{SalesForceConnectionImpl, SalesForceConnection}
import com.typesafe.config.ConfigFactory

/**
  * Created by akovari on 13.6.2015.
  */
object Configuration {
  val cfg = ConfigFactory.load

  @inline def apply() = cfg

  implicit val sfdcConn: SalesForceConnection = new SalesForceConnectionImpl()
}
