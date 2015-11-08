package com.github.akovari.rdfp.data.cases

import akka.actor.TypedActor
import akka.event.Logging
import com.github.akovari.typesafeSalesforce.cxf.enterprise
import com.github.akovari.typesafeSalesforce.cxf.enterprise.Case_
import com.github.akovari.typesafeSalesforce.util.SalesForceConnection
import scala.concurrent.Future

import com.github.akovari.rdfp.data.Models._
import com.github.akovari.typesafeSalesforce.query._

/**
  * Created by akovari on 06.11.15.
  */
trait CasesResource {
  def getCase(caseNumber: String)(implicit conn: SalesForceConnection): Future[Case]
}

class CasesResourceImpl extends CasesResource {
  implicit val executionContext = TypedActor.context.dispatcher
  private implicit val log = Logging(TypedActor.context.system, TypedActor.context.self)

  override def getCase(caseNumber: String)(implicit conn: SalesForceConnection): Future[Case] = {
    val q = SalesForceQueryCache.caseQueryWithFilter(Case_.caseNumber :== caseNumber, None)

    conn.query(q).mapTo[Seq[enterprise.Case]].map(_.head).map { c =>
      Case(c.getCaseNumber, c.getStatus)
    }
  }
}
