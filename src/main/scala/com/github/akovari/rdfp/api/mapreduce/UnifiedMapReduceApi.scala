package com.github.akovari.rdfp.api.mapreduce

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import com.github.akovari.rdfp.Configuration
import com.github.akovari.rdfp.api.ql.UQLContext.UnifiedResult.{UnifiedQueryLimit, UnifiedQueryOffset, UnifiedResultFromResourceType}
import com.github.akovari.rdfp.api.ql.{UQLParser, UQLContext}
import com.github.akovari.rdfp.data.cases.{CasesResourceImpl, CasesResource}

import com.github.akovari.rdfp.data.ResourceType
import com.github.akovari.rdfp.api.mapreduce.MapReduceModels.CallbackType

import scala.collection.JavaConverters._
import scala.concurrent.duration._

case class ApiQuery(query: AnyRef, sender: ActorRef)

case class CaseQuery[T, R](query: String, callback: CallbackType[T, R])

case class CaseLinksQuery[T, R](query: String, callback: CallbackType[T, R])

/**
  * Created by akovari on 4.3.2015.
  */
class UnifiedMapReduceApi extends Actor with ActorLogging {

  import Configuration._

  implicit val executionContext = context.dispatcher

  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
    case e: Exception =>
      log.error(e, e.getMessage)
      Escalate
  }

  override def receive = {
    case ApiQuery(CaseQuery(query, cb), senderRef) =>
      caseResource.getCasesByFilter(
        UQLContext.conditionToSOQLConditionWithoutLimit(
          UQLParser.parseUQL( s"""entity = "${ResourceType.SupportCase}" and $query""").condition
        )(UnifiedResultFromResourceType(ResourceType.SupportCase))).map(_.toList).map(s => senderRef ! DataWithCallback(s, cb))

    case ApiQuery(CaseLinksQuery(query, cb), senderRef) =>
      implicit val offset: Option[UnifiedQueryOffset] = None
      implicit val limit: Option[UnifiedQueryLimit] = None
      caseResource.getCaseLinksByFilter(UQLParser.parseUQL( s"""entity = "${ResourceType.SupportCase}" and $query""")).map(_.toList).map(s => senderRef ! DataWithCallback(s, cb))
  }

  private def caseResource: CasesResource = TypedActor(context).typedActorOf(TypedProps[CasesResourceImpl])
}
