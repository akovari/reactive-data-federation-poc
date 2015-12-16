package com.github.akovari.rdfp.api.ql

import com.github.akovari.rdfp.api.ql.QOLParser._
import org.parboiled.scala.parserunners.ReportingParseRunner
import org.parboiled.scala.testing.ParboiledTest
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by akovari on 03/07/15.
 */
class QOLParserSpec extends FlatSpec with Matchers with ParboiledTest {
  type Result = AstNode
  val parser = new QOLParser() {
    override val buildParseTree = true
  }

  "a simple field" should "work with just an ident" in {
    val rule = parser.QOLField
    parse(ReportingParseRunner(rule), "createdBy") {
      parsingResult.result should be(Some(Field("createdBy")))
    }
  }

  "a simple field asc order" should "work with ident and asc keyword" in {
    val rule = parser.QOLAscFieldOrder
    parse(ReportingParseRunner(rule), "createdBy asc") {
      parsingResult.result should be(Some(AscFieldOrder(Field("createdBy"))))
    }
  }

  "a simple field desc order" should "work with ident and desc keyword" in {
    val rule = parser.QOLDescFieldOrder
    parse(ReportingParseRunner(rule), "createdBy desc") {
      parsingResult.result should be(Some(DescFieldOrder(Field("createdBy"))))
    }
  }

  "a simple field desc order" should "work with ident and desc keyword and result in a TableOrder" in {
    val rule = parser.QOLTableOrder
    parse(ReportingParseRunner(rule), "createdBy desc") {
      parsingResult.result should be(Some(TableOrder(List(DescFieldOrder(Field("createdBy"))))))
    }
  }

  "two simple fields asc, desc orders" should "work result in a correct TableOrder" in {
    val rule = parser.QOLTableOrder
    parse(ReportingParseRunner(rule), "id asc, createdBy desc") {
      parsingResult.result should be(Some(TableOrder(List(AscFieldOrder(Field("id")), DescFieldOrder(Field("createdBy"))))))
    }
  }
}
