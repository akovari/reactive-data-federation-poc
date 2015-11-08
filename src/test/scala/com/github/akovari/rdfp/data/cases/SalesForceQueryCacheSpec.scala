package com.github.akovari.rdfp.data.cases

import org.scalatest.{Matchers, FlatSpec}
import com.github.akovari.typesafeSalesforce.cxf.{enterprise => e}
import com.github.akovari.typesafeSalesforce.query.SelectQuery._
import com.github.akovari.typesafeSalesforce.query._
import shapeless.{HList, HNil}

/**
 * Created by akovari on 28.10.15.
 */
class SalesForceQueryCacheSpec extends FlatSpec with Matchers {
  "caseQueryWithFilter" should "be equal to \"\"" in {
    (SalesForceQueryCache.caseQueryWithFilter(e.Case_.status :== "Waiting on Customer", None)).toString.trim should be(
      "SELECT Id, CaseNumber, Account.AccountNumber, Status FROM Case WHERE (Status = 'Waiting on Customer') ORDER BY Id ASC"
    )
  }
}
