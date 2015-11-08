package com.github.akovari.rdfp.api.ql

import org.parboiled.errors.ErrorUtils
import org.parboiled.scala._

/**
 * Created by akovari on 03/07/15.
 */

object QOLParser {

  abstract class AstNode

  sealed abstract class AstValue[+T](value: T) extends AstNode

  sealed abstract class FieldOrder extends AstNode

  case class Field(value: String) extends AstValue(value)

  case class AscFieldOrder(field: Field) extends FieldOrder

  case class DescFieldOrder(field: Field) extends FieldOrder

  case class TableOrder(fieldOrders: Seq[FieldOrder]) extends AstNode

}

class QOLParser extends CommonParser {

  import QOLParser._

  def QOLTableOrder: Rule1[TableOrder] = rule {
    QOLFieldOrder ~ zeroOrMore(WhiteSpace ~ "," ~ WhiteSpace ~ QOLFieldOrder) ~~> ((order, orders) => TableOrder(order :: orders)) ~ EOI
  }

  def QOLFieldOrder: Rule1[FieldOrder] = rule(QOLAscFieldOrder | QOLDescFieldOrder)

  def QOLAscFieldOrder: Rule1[AscFieldOrder] = rule {
    (QOLField ~ WhiteSpace ~ ignoreCase("ASC")) ~~> (field => AscFieldOrder(field))
  }

  def QOLDescFieldOrder: Rule1[DescFieldOrder] = rule {
    (QOLField ~ WhiteSpace ~ ignoreCase("DESC")) ~~> (field => DescFieldOrder(field))
  }

  def QOLField: Rule1[Field] = rule {
    Ident ~> (field => Field(field))
  }

  def parseQOL(qol: String): TableOrder = {
    val parsingResult = ReportingParseRunner(QOLTableOrder).run(qol)
    parsingResult.result match {
      case Some(astRoot) => astRoot
      case None => throw new ParsingException(s"""Invalid QOL order: \n${ErrorUtils.printParseErrors(parsingResult)}""")
    }
  }
}
