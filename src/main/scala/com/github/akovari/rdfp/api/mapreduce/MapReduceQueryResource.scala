package com.github.akovari.rdfp.api.mapreduce

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{OneForOneStrategy, Props, TypedActor}
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import com.github.akovari.rdfp.api.mapreduce.MapReduceModels._
import com.github.akovari.rdfp.api.mapreduce.impl.MapReducer

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by akovari on 3.3.2015.
  */
trait MapReduceQueryResource {
  def evaluate(script: String, params: Seq[String] = Seq.empty): Future[EmitableScala]
}

class MapReduceQueryResourceImpl extends MapReduceQueryResource with TypedActor.Supervisor {
  implicit val timeout = Timeout(5 seconds)
  implicit val executionContext = TypedActor.context.dispatcher
  private val log = Logging(TypedActor.context.system, TypedActor.context.self)

  override def supervisorStrategy() = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
    case e: Exception =>
      log.error(e, e.getMessage)
      Escalate
  }

  override def evaluate(script: String, params: Seq[String] = Seq.empty): Future[EmitableScala] =
    (mapReducer ? EvaluateScript(script, params)).map {
      case FinalizedData(data) =>
        val asScala: EmitableScala = data
        asScala

      case error: MapReducerFailure =>
        throw error
    }

  private def mapReducer = TypedActor.context.actorOf(Props[MapReducer])
}
