package com.github.akovari

import akka.actor.{TypedActor, ActorSystem}
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by akovari on 14.12.15.
  */
package object rdfp {
  implicit val json4sJacksonFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

  def withResource[Resource <: AnyRef, Result](res: Resource)(fun: Resource => Future[Result])(implicit system: ActorSystem, executionContext: ExecutionContext) = {
    val ret = fun(res)
    ret.onComplete { _ => TypedActor(system).stop(res) }
    ret
  }
}
