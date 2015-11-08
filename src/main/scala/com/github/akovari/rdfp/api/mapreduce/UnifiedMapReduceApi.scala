package com.github.akovari.rdfp.api.mapreduce

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor._
//import com.github.akovari.rdfp.data.cases.impl.{CaseResource, CaseResourceImpl}
//import com.github.akovari.rdfp.data.{ResourceProjection, ResourceReliability, ResourceType}
import com.github.akovari.rdfp.api.mapreduce.MapReduceModels.CallbackType

import scala.collection.JavaConverters._
import scala.concurrent.duration._

case class ApiQuery(query: AnyRef, sender: ActorRef)

case class CaseQuery[T, R](query: String, callback: CallbackType[T, R])

/**
 * Created by akovari on 4.3.2015.
 */
class UnifiedMapReduceApi extends Actor with ActorLogging {
  implicit val executionContext = context.dispatcher

  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
    case e: Exception =>
      log.error(e, e.getMessage)
      Escalate
  }

  override def receive = {
    case ApiQuery(CaseQuery(query, cb), senderRef) =>
//      caseResource.casesByFilter( s"""entity="${ResourceType.SupportCase}" and $query""")(ResourceReliability.Fresh, ResourceProjection.Minimal, None, None, None).map(_.getOrElse(Set.empty)).map(_.toList).map(s => senderRef ! DataWithCallback(s, cb))
  }

//  private def caseResource: CaseResource = TypedActor(context).typedActorOf(TypedProps[CaseResourceImpl])
}
