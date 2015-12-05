package com.github.akovari.rdfp.data.cases

import akka.actor.TypedActor
import akka.event.Logging
import com.github.akovari.rdfp.api.ql.UQLContext.UnifiedResult.{UnifiedQueryLimit, UnifiedQueryOffset}
import com.github.akovari.rdfp.api.ql.UQLParser.Filter
import com.github.akovari.rdfp.data.{ResourceType, DatabaseConnection}
import com.github.akovari.typesafeSalesforce.cxf.enterprise
import com.github.akovari.typesafeSalesforce.cxf.enterprise.Case_
import com.github.akovari.typesafeSalesforce.util.SalesForceConnection
import org.jooq.{Record, Result}
import scala.concurrent.Future
import scala.collection.JavaConverters._


import com.github.akovari.rdfp.data.Models._
import com.github.akovari.typesafeSalesforce.{query => sfquery}
import com.github.akovari.rdfp.data.cases.PostgresQueryCache._
import com.github.akovari.rdfp.api.ql.UQLContext
import com.github.akovari.rdfp.api.ql.db.SQLTables._

/**
  * Created by akovari on 06.11.15.
  */
trait CasesResource {
  def getCase(caseNumber: String)(implicit conn: SalesForceConnection): Future[Case]

  def getCasesByFilter(filter: sfquery.Filter)(implicit conn: SalesForceConnection): Future[Seq[Case]]

  def getCaseLinksByFilter(filter: Filter)(implicit offset: Option[UnifiedQueryOffset], limit: Option[UnifiedQueryLimit]): Future[Seq[CaseLink]]
}

class CasesResourceImpl extends CasesResource {
  import UQLContext._

  implicit val executionContext = TypedActor.context.dispatcher
  private implicit val log = Logging(TypedActor.context.system, TypedActor.context.self)

  override def getCase(caseNumber: String)(implicit conn: SalesForceConnection): Future[Case] = {
    import sfquery._
    val q = SalesForceQueryCache.caseQueryWithFilter(Case_.caseNumber :== caseNumber, None)
    log.debug(s"[getCase] $q")

    conn.query(q).mapTo[Seq[enterprise.Case]].map(_.head).map { c =>
      Case(c.getCaseNumber, c.getStatus)
    }
  }

  override def getCasesByFilter(filter: sfquery.Filter)(implicit conn: SalesForceConnection): Future[Seq[Case]] = {
    val q = SalesForceQueryCache.caseQueryWithFilter(filter, None)
    log.debug(s"[getCaseLinksByFilter] $q")

    conn.query(q).mapTo[Seq[enterprise.Case]].map(_.map { c =>
      Case(c.getCaseNumber, c.getStatus)
    })
  }

  override def getCaseLinksByFilter(filter: Filter)(implicit offset: Option[UnifiedQueryOffset], limit: Option[UnifiedQueryLimit]): Future[Seq[CaseLink]] = {
    DatabaseConnection.dsl.map { implicit dsl =>
      implicit val filterCtx: FilterContext = filter

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = caseLinksSelectQuery
          applyQueryCondition(q, condition)
          log.debug(s"[getCaseLinksByFilter] $q")

          val physicalModels = q.fetch().asInstanceOf[Result[Record]].asScala.toSeq

          physicalModels.map { r =>
            CaseLink(r.getValue(CASE_LINKS_T.ID).intValue(), r.getValue(CASE_LINKS_T.URL))
          }
      }
    }
  }
}
