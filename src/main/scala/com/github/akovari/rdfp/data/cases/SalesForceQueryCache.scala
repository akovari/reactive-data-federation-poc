package com.github.akovari.rdfp.data.cases

import com.github.akovari.typesafeSalesforce.cxf.{enterprise => e}
import com.github.akovari.typesafeSalesforce.query.SelectQuery._
import com.github.akovari.typesafeSalesforce.query._
import shapeless.{HList, HNil}

/**
  * User: akovari
  * Date: 28.6.2014
  * Time: 16:00
  */
object SalesForceQueryCache {
  def caseQueryWithFilter[O <: HList](filter: Filter, orderBy: Option[OrderList[O]]) =
    (select(e.SObject_.id :: e.Case_.caseNumber :: (e.Case_.account :/ e.Account_.accountNumber) :: e.Case_.status :: HNil) from e.Case_ where (filter) orderBy (orderBy.getOrElse(OrderList((e.SObject_.id asc) :: HNil)))).query

}
