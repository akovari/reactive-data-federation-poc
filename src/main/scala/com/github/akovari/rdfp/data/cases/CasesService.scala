package com.github.akovari.rdfp.data.cases

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import com.github.akovari.rdfp.api.ql.UQLContext.UnifiedResult.{UnifiedQueryLimit, UnifiedQueryOffset, UnifiedResultFromResourceType}
import com.github.akovari.rdfp.api.ql.{UQLContext, UQLParser}
import com.github.akovari.rdfp.data.{ResourceType, JsonProtocols}
import com.github.akovari.typesafeSalesforce.util.SalesForceConnection

import scala.concurrent.ExecutionContext

/**
  * Created by akovari on 07.11.15.
  */
class CasesService(casesResource: CasesResource)(implicit executionContext: ExecutionContext, sfdcConn: SalesForceConnection)
  extends Directives with SprayJsonSupport with JsonProtocols {

  val route =
    path("case" / Rest) {
      caseNumber =>
        get {
          complete {
            casesResource.getCase(caseNumber)
          }
        }
    } ~ path("cases" / "links") {
      parameter("where") { uql =>
        get {
          complete {
            implicit val offset: Option[UnifiedQueryOffset] = None
            implicit val limit: Option[UnifiedQueryLimit] = None
            casesResource.getCaseLinksByFilter(UQLParser.parseUQL( s"""entity = "${ResourceType.SupportCase}" and $uql"""))
          }
        }
      }
    } ~ path("case") {
      parameter("where") { uql =>
        get {
          complete {
            casesResource.getCasesByFilter(
              UQLContext.conditionToSOQLConditionWithoutLimit(
                UQLParser.parseUQL( s"""entity = "${ResourceType.SupportCase}" and $uql""").condition
              )(UnifiedResultFromResourceType(ResourceType.SupportCase)))
          }
        }
      }
    }
}
