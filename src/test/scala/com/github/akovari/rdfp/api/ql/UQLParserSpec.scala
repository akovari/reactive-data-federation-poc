package com.github.akovari.rdfp.api.ql

import com.github.akovari.rdfp.api.ql.UQLParser._
import org.joda.time.DateTime
import org.parboiled.scala.parserunners.ReportingParseRunner
import org.parboiled.scala.testing.ParboiledTest
import org.scalatest.{FlatSpec, Matchers}


class UQLParserSpec extends FlatSpec with Matchers with ParboiledTest {
  type Result = AstNode
  val parser = new UQLParser() {
    override val buildParseTree = true
  }

  "a string without parenthesis" should "be an ident" in {
    val rule = parser.UQLIdent
    parse(ReportingParseRunner(rule), "createdBy") {
      parsingResult.result should be(Some(StringNode("createdBy")))
    }
  }

  "a string with parenthesis" should "be a string" in {
    val rule = parser.UQLString
    parse(ReportingParseRunner(rule), """"createdBy"""") {
      parsingResult.result should be(Some(StringNode("createdBy")))
    }
  }

  "equals condition" should "work with a string" in {
    val rule = parser.UQLEqualsCondition
    parse(ReportingParseRunner(rule), """createdBy = "Adam"""") {
      parsingResult.result should be(Some(EqualsCondition("createdBy", StringNode("Adam"))))
    }
  }

  "like condition" should "work with a string" in {
    val rule = parser.UQLLikeCondition
    parse(ReportingParseRunner(rule), """createdBy like "%Adam%"""") {
      parsingResult.result should be(Some(LikeCondition("createdBy", StringNode("%Adam%"))))
    }
  }

  "IS condition" should "work with a boolean" in {
    val rule = parser.UQLEqualsCondition
    parse(ReportingParseRunner(rule), """closed is true""") {
      parsingResult.result should be(Some(EqualsCondition("closed", True)))
    }
  }

  "equals condition" should "work with an escaped string" in {
    val rule = parser.UQLEqualsCondition
    parse(ReportingParseRunner(rule), """createdBy = "\"Adam\""""") {
      parsingResult.result should be(Some(EqualsCondition("createdBy", StringNode( """\"Adam\""""))))
    }
  }

  "notequals condition" should "work with != and a number" in {
    val rule = parser.UQLNotEqualsCondition
    parse(ReportingParseRunner(rule), """items != 5""") {
      parsingResult.result should be(Some(NotEqualsCondition("items", NumberNode(5))))
    }
  }

  "lower condition" should "work with a number" in {
    val rule = parser.UQLLowerCondition
    parse(ReportingParseRunner(rule), """items < 5""") {
      parsingResult.result should be(Some(LowerCondition("items", NumberNode(5))))
    }
  }

  "greater condition" should "work with a number" in {
    val rule = parser.UQLGreaterCondition
    parse(ReportingParseRunner(rule), """items > 5""") {
      parsingResult.result should be(Some(GreaterCondition("items", NumberNode(5))))
    }
  }

  "in condition" should "work with a set of number" in {
    val rule = parser.UQLInCondition
    parse(ReportingParseRunner(rule), """items in [5, 4,3]""") {
      parsingResult.result should be(Some(InCondition("items", ArrayNode(List(NumberNode(5), NumberNode(4), NumberNode(3))))))
    }
  }

  "in condition" should "work with a set of strings" in {
    val rule = parser.UQLInCondition
    parse(ReportingParseRunner(rule), """items in ["test", "test2"]""") {
      parsingResult.result should be(Some(InCondition("items", ArrayNode(List(StringNode("test"), StringNode("test2"))))))
    }
  }

  "not in condition" should "work with a set of strings" in {
    val rule = parser.UQLNotInCondition
    parse(ReportingParseRunner(rule), """items not in ["test", "test2"]""") {
      parsingResult.result should be(Some(NotInCondition("items", ArrayNode(List(StringNode("test"), StringNode("test2"))))))
    }
  }

  "includes condition" should "work with a set of strings" in {
    val rule = parser.UQLIncludesCondition
    parse(ReportingParseRunner(rule), """items includes ["test", "test2"]""") {
      parsingResult.result should be(Some(IncludesCondition("items", ArrayNode(List(StringNode("test"), StringNode("test2"))))))
    }
  }

  "excludes condition" should "work with a set of strings" in {
    val rule = parser.UQLExcludesCondition
    parse(ReportingParseRunner(rule), """items excludes ["test"]""") {
      parsingResult.result should be(Some(ExcludesCondition("items", ArrayNode(List(StringNode("test"))))))
    }
  }

  "lower or equal condition" should "work with a number" in {
    val rule = parser.UQLLowerOrEqualCondition
    parse(ReportingParseRunner(rule), """items <= 5""") {
      parsingResult.result should be(Some(LowerOrEqualCondition("items", NumberNode(5))))
    }
  }

  "greater or equal condition" should "work with a number" in {
    val rule = parser.UQLGreaterOrEqualCondition
    parse(ReportingParseRunner(rule), """items >= 5""") {
      parsingResult.result should be(Some(GreaterOrEqualCondition("items", NumberNode(5))))
    }
  }

  "date formatter" should "work with a date in yyyy/mm/dd format" in {
    val rule = parser.UQLGreaterOrEqualCondition
    DateTime.parse("2011/05/07", UQLParser.dateTimeFormatter) shouldEqual DateTime.parse("2011-05-07T00:00:00.000")
  }

  "greater or equal condition" should "work with a date" in {
    val rule = parser.UQLGreaterOrEqualCondition
    parse(ReportingParseRunner(rule), """items >= 2011/05/07""") {
      parsingResult.result should be(Some(GreaterOrEqualCondition("items", DateTimeNode(DateTime.parse("2011-05-07T00:00:00.000")))))
    }
  }

  "greater or equal condition" should "work with a timestamp zulu" in {
    val rule = parser.UQLGreaterOrEqualCondition
    parse(ReportingParseRunner(rule), """items >= 2011-05-07T12:23:00.000Z""") {
      parsingResult.result should be(Some(GreaterOrEqualCondition("items", DateTimeNode(UQLParser.dateTimeFormatter.parseDateTime("2011-05-07T12:23:00.000Z")))))
    }
  }

  "greater or equal condition" should "work with a timestamp" in {
    val rule = parser.UQLGreaterOrEqualCondition
    parse(ReportingParseRunner(rule), """items >= 2011-05-07T12:23:00.000+00:00""") {
      parsingResult.result should be(Some(GreaterOrEqualCondition("items", DateTimeNode(UQLParser.dateTimeFormatter.parseDateTime("2011-05-07T12:23:00.000Z")))))
    }
  }

  "boolean condition" should "work with 2 simple conditions joined with and" in {
    val rule = parser.UQLAndCondition
    parse(ReportingParseRunner(rule), """createdBy != "Adam" and items = 6""") {
      parsingResult.result should be(Some(AndCondition(
        NotEqualsCondition("createdBy", StringNode("Adam")),
        EqualsCondition("items", NumberNode(6)))
      ))
    }
  }

  "boolean condition" should "work with 2 simple conditions joined with or" in {
    val rule = parser.UQLOrCondition
    parse(ReportingParseRunner(rule), """createdBy != "Adam" or items > 6""") {
      parsingResult.result should be(Some(OrCondition(
        NotEqualsCondition("createdBy", StringNode("Adam")),
        GreaterCondition("items", NumberNode(6)))
      ))
    }
  }

  "nested condition" should "work with a simple condition" in {
    val rule = parser.UQLNestedCondition
    parse(ReportingParseRunner(rule), """(items = 6)""") {
      parsingResult.result should be(Some(EqualsCondition("items", NumberNode(6))))
    }
  }

  "nested condition" should "work with 2 simple conditions joined with or" in {
    val rule = parser.UQLNestedCondition
    parse(ReportingParseRunner(rule), """(createdBy != "Adam" or items = 6)""") {
      parsingResult.result should be(Some(OrCondition(
        NotEqualsCondition("createdBy", StringNode("Adam")),
        EqualsCondition("items", NumberNode(6)))
      ))
    }
  }

  "boolean condition" should "work with 1 simple and 1 nested conditions joined with or" in {
    val rule = parser.UQLAnyCondition
    parse(ReportingParseRunner(rule), """createdBy != "Adam" or (items = 6 and row = 9.5)""") {
      parsingResult.result should be(Some(OrCondition(
        NotEqualsCondition("createdBy", StringNode("Adam")),
        AndCondition(
          EqualsCondition("items", NumberNode(6)),
          EqualsCondition("row", NumberNode(9.5)))
      )))
    }
  }

  "entity" should "work an entity name" in {
    val rule = parser.UQLEntity
    parse(ReportingParseRunner(rule), """entity = "Solutions"""") {
      parsingResult.result should be(Some(Entity("Solutions")))
    }
  }

  "filter" should "work with an entity and a simple filter" in {
    val rule = parser.UQLFilter
    parse(ReportingParseRunner(rule), """entity = "Solutions" and createdBy = "Adam"""") {
      parsingResult.result should be(Some(Filter(
        Entity("Solutions"),
        EqualsCondition("createdBy", StringNode("Adam")))
      ))
    }
  }

  "a null" should "be null" in {
    val rule = parser.UQLNull
    parse(ReportingParseRunner(rule), "null") {
      parsingResult.result should be(Some(Null))
    }
  }

  "complex query #1 subpart #1" should "parse" in {
    val rule = parser.UQLAnyCondition
    parse(ReportingParseRunner(rule),
      """(ownerId is "005A0000003CWSAIA4" and (status is "Waiting on Red Hat" or internalStatus is "Waiting on Owner"))""".trim) {
      parsingResult.matched should be(true)
    }
  }

  "complex query #1 subpart #2" should "parse" in {
    val rule = parser.UQLAnyCondition
    parse(ReportingParseRunner(rule),
      """(ownerId is "005A0000003CWSAIA4" and (status is "Waiting on Red Hat" or internalStatus is "Waiting on Owner")) or (ftsRole like "%25sshumake%25" and status ne "Closed")""".trim) {
      parsingResult.matched should be(true)
    }
  }

  "complex query #1 subpart #3" should "parse" in {
    val rule = parser.UQLAnyCondition
    parse(ReportingParseRunner(rule),
      """((internalStatus is "Waiting on Collaboration" and (status ne "Closed")) and nnoSuperRegion is null)""".trim) {
      parsingResult.matched should be(true)
    }
  }

  "complex query #1 subpart #4" should "parse" in {
    val rule = parser.UQLAnyCondition
    parse(ReportingParseRunner(rule),
      """((internalStatus is "Waiting on Collaboration" and (status ne "Closed")) and nnoSuperRegion is null) or (isFTS is true and (ftsRole is "")) and requiresSecureHandling is false""".trim) {
      parsingResult.matched should be(true)
    }
  }

  "complex query #1 - ALL - subparts #2 OR #4" should "parse" in {
    val rule = parser.UQLAnyCondition
    parse(ReportingParseRunner(rule),
      """((((ownerId is "005A0000003CWSAIA4" and (status is "Waiting on Red Hat" or internalStatus is "Waiting on Owner")) or (ftsRole like "%25sshumake%25" and status ne "Closed")) or (((internalStatus is "Waiting on Collaboration" and (status ne "Closed")) and nnoSuperRegion is null) or (isFTS is true and (ftsRole is "")))) and requiresSecureHandling is false)""".trim) {
      parsingResult.result should be(Some(
        AndCondition(
          OrCondition(
            OrCondition(
              AndCondition(
                EqualsCondition("ownerId", StringNode("005A0000003CWSAIA4")),
                OrCondition(
                  EqualsCondition("status", StringNode("Waiting on Red Hat")),
                  EqualsCondition("internalStatus", StringNode("Waiting on Owner"))
                )
              ),
              AndCondition(
                LikeCondition("ftsRole", StringNode("%25sshumake%25")),
                NotEqualsCondition("status", StringNode("Closed"))
              )
            ),
            OrCondition(
              AndCondition(
                AndCondition(
                  EqualsCondition("internalStatus", StringNode("Waiting on Collaboration")),
                  NotEqualsCondition("status", StringNode("Closed"))
                ),
                EqualsCondition("nnoSuperRegion", Null)
              ),
              AndCondition(
                EqualsCondition("isFTS", BooleanNode(true)),
                EqualsCondition("ftsRole", StringNode("")))
            )
          ),
          EqualsCondition("requiresSecureHandling", BooleanNode(false))
        )
      ))
    }
  }

  "who coaches in geo filter parsed" should "be" in {
    val filter = s""" entity="User" and ( (roleSbrName = "JBoss Base AS") and ( ( (roleName = "unified-sbr-coach-primary") or (roleName = "unified-sbr-coach-backup") ) and (roleSuperRegion = "EMEA") ) )"""
    val rule = parser.UQLFilter
    parse(ReportingParseRunner(rule), filter.trim) {
      parsingResult.result should be(Some(
        Filter(Entity("User"),
          AndCondition(
            EqualsCondition("roleSbrName", StringNode("JBoss Base AS")),
            AndCondition(
              OrCondition(
                EqualsCondition("roleName", StringNode("unified-sbr-coach-primary")),
                EqualsCondition("roleName", StringNode("unified-sbr-coach-backup"))),
              EqualsCondition("roleSuperRegion", StringNode("EMEA"))))))
      )
    }
  }
}
