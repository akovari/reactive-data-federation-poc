package com.github.akovari.rdfp.data.cases

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import com.github.akovari.rdfp.data.JsonProtocols
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
    }
}
