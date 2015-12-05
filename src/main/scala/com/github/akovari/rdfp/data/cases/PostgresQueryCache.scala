package com.github.akovari.rdfp.data.cases

import com.github.akovari.rdfp.api.ql.UQLContext
import com.github.akovari.rdfp.api.ql.db.SQLTables._
import org.jooq.DSLContext

/**
  * Created by akovari on 17.11.15.
  */
object PostgresQueryCache {
  def caseLinksSelectQuery(implicit dsl: DSLContext, resourceType: UQLContext.UnifiedResult.UnifiedResultFromResourceType) = {
    val q = dsl.selectQuery(CASE_LINKS_T)
    q.addSelect(fields(CASE_LINKS_T))
    q
  }
}
