package com.github.akovari.rdfp.api.ql

import com.github.akovari.rdfp.data.ResourceType
import com.github.akovari.rdfp.api.ql.UQLContext.UnifiedResult.{UnifiedQueryLimit, UnifiedQueryOffset}
import com.github.akovari.rdfp.api.ql.db.SQLTables
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.{MockConnection, MockDataProvider, MockExecuteContext, MockResult}
import org.jooq.{JoinType, SQLDialect}
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by akovari on 10.09.14.
 */
class UQLJooqSpec extends FlatSpec with Matchers {

  import SQLTables._
  import UQLContext._

  val provider = new MockDataProvider {
    override def execute(p1: MockExecuteContext): Array[MockResult] = ???
  }
  val connection = new MockConnection(provider)
  val dsl = DSL.using(connection, SQLDialect.POSTGRES)

  def parser = new UQLParser()

  implicit val offset: Option[UnifiedQueryOffset] = None
  implicit val limit: Option[UnifiedQueryLimit] = None

  "simple data type conditions" should "work with a single string eq condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and createdById="Unassigned"""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)r.created_by(\s*)=(\s*)'Unassigned'""".r)
      }
    }

  "simple data type conditions" should "work with a single null ne condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and createdById != null""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)r.created_by(\s*)is(\s*)not(\s*)null""".r)
      }
    }

  "simple data type conditions" should "work with a single int ne condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and createdById != 5""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)r.created_by(\s*)<>(\s*)5""".r)
      }
    }

  "simple data type conditions" should "work with a single float ne condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and createdById != 5.4""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)r.created_by(\s*)<>(\s*)5.4""".r)
      }
    }

  "simple data type conditions" should "work with a single boolean eq condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and createdById = true""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)r.created_by(\s*)=(\s*)true""".r)
      }
    }

  "simple data type conditions" should "work with a single int gt condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and col > 5""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)r.col(\s*)>(\s*)5""".r)
      }
    }

  "simple data type conditions" should "work with an and int gt and string eq condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and col > 5 and col2 = "test"""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)\((\s*)r.col(\s*)>(\s*)5(\s*)and(\s*)r.col2(\s*)=(\s*)'test'(\s*)\)""".r)
      }
    }

  "simple data type conditions" should "work with an and int gt or string eq condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and col > 5 or col2 = "test"""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)\((\s*)r.col(\s*)>(\s*)5(\s*)or(\s*)r.col2(\s*)=(\s*)'test'(\s*)\)""".r)
      }
    }

  "simple data type conditions" should "work with nested conditions condition" in {
      implicit val filterCtx: FilterContext = parser.parseUQL( """entity="SupportCase" and col > 5 or (col2 = "test" and (col3 = 5 or col4 < 6.5))""")

      filterForResourceType(ResourceType.SupportCase) {
        implicit resourceType => condition =>
          val q = dsl.selectQuery()
          applyQueryCondition(q, condition)

          q.toString should fullyMatch regex( """select(\s*).*(\s*)where(\s*)\((\s*)r.col(\s*)>(\s*)5(\s*)or(\s*)\((\s*)r.col2(\s*)=(\s*)'test'(\s*)and(\s*)\((\s*)r.col3(\s*)=(\s*)5(\s*)or(\s*)r.col4(\s*)<(\s*)6.5(\s*)(\s*)\)(\s*)(\s*)\)(\s*)(\s*)\)""".r)
      }
    }
}
