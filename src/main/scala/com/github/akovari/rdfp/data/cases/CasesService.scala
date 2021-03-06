package com.github.akovari.rdfp.data.cases

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Directives
import com.github.akovari.rdfp.api.ql.UQLContext.UnifiedResult.{UnifiedQueryLimit, UnifiedQueryOffset, UnifiedResultFromResourceType}
import com.github.akovari.rdfp.api.ql.{UQLContext, UQLParser}
import com.github.akovari.rdfp.data.ResourceType
import com.github.akovari.typesafeSalesforce.util.SalesForceConnection
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.ExecutionContext

/**
  * Created by akovari on 07.11.15.
  */
class CasesService(casesResource: (() => CasesResource))(implicit executionContext: ExecutionContext, sfdcConn: SalesForceConnection, actorSystem: ActorSystem)
  extends Directives with Json4sSupport {
  implicit val serialization = jackson.Serialization // or native.Serialization

  import com.github.akovari.rdfp._

  val route =
    path("case" / Rest) {
      caseNumber =>
        get {
          complete {
            withResource(casesResource()) {
              _.getCase(caseNumber)
            }
          }
        }
    } ~ path("cases" / "links") {
      parameter("where") { uql =>
        get {
          complete {
            implicit val offset: Option[UnifiedQueryOffset] = None
            implicit val limit: Option[UnifiedQueryLimit] = None
            withResource(casesResource()) {
              _.getCaseLinksByFilter(UQLParser.parseUQL( s"""entity = "${ResourceType.SupportCase}" and $uql"""))
            }
          }
        }
      }
    } ~ path("case") {
      parameter("where") { uql =>
        get {
          complete {
            withResource(casesResource()) {
              _.getCasesByFilter(
                UQLContext.conditionToSOQLConditionWithoutLimit(
                  UQLParser.parseUQL( s"""entity = "${ResourceType.SupportCase}" and $uql""").condition
                )(UnifiedResultFromResourceType(ResourceType.SupportCase)))
            }
          }
        }
      }
    }
}
