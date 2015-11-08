package com.github.akovari.rdfp.api.ql

import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatterBuilder, DateTimeFormat}
import org.parboiled.errors.ErrorUtils
import org.parboiled.scala._

/**
 * Created by akovari on 18.8.2014.
 */
object UQLParser {
  val True = BooleanNode(value = true)
  val False = BooleanNode(value = false)
  val dateTimeFormatter = new DateTimeFormatterBuilder().append(null, Array(
    DateTimeFormat.forPattern("yyyy/MM/dd").withZoneUTC().getParser,
    DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC().getParser,
    ISODateTimeFormat.dateTime().withZoneUTC().getParser)
  ).toFormatter

  sealed abstract class AstNode

  sealed abstract class AstValue[+T](value: T) extends AstNode

  sealed abstract class Condition extends AstNode {
    def hasField(name: String): Boolean = false
  }

  sealed abstract class BooleanCondition(left: Condition, right: Condition) extends Condition {
    override def hasField(name: String): Boolean = left.hasField(name) || right.hasField(name)
  }

  sealed abstract class SimpleCondition(name: String, value: AstNode) extends Condition {
    override def hasField(name: String): Boolean = this.name == name
  }

  sealed abstract class NegativeCondition(name: String, value: AstNode) extends SimpleCondition(name, value)

  sealed abstract class NullValue extends AstValue(null)

  case class EqualsCondition(name: String, value: AstNode) extends SimpleCondition(name, value)

  case class NotEqualsCondition(name: String, value: AstNode) extends NegativeCondition(name, value)

  case class LikeCondition(name: String, value: AstNode) extends SimpleCondition(name, value)

  case class ILikeCondition(name: String, value: AstNode) extends SimpleCondition(name, value)

  case class LowerCondition(name: String, value: AstNode) extends SimpleCondition(name, value)

  case class GreaterCondition(name: String, value: AstNode) extends SimpleCondition(name, value)

  case class LowerOrEqualCondition(name: String, value: AstNode) extends SimpleCondition(name, value)

  case class GreaterOrEqualCondition(name: String, value: AstNode) extends SimpleCondition(name, value)

  case class InCondition(name: String, value: ArrayNode) extends SimpleCondition(name, value)

  case class NotInCondition(name: String, value: ArrayNode) extends NegativeCondition(name, value)

  case class IncludesCondition(name: String, value: ArrayNode) extends SimpleCondition(name, value)

  case class ExcludesCondition(name: String, value: ArrayNode) extends NegativeCondition(name, value)

  case class AndCondition(left: Condition, right: Condition) extends BooleanCondition(left, right)

  case class OrCondition(left: Condition, right: Condition) extends BooleanCondition(left, right)

  case class Entity(value: String) extends AstValue(value)

  case class Filter(entity: Entity, condition: Condition) extends AstNode

  case class ArrayNode(value: List[AstValue[_]]) extends AstValue(value)

  case class StringNode(value: String) extends AstValue(value)

  case class NumberNode(value: BigDecimal) extends AstValue(value)

  case class DateTimeNode(value: DateTime) extends AstValue(value)

  case class BooleanNode(value: Boolean) extends AstValue(value)

  case object Null extends NullValue
}

class UQLParser extends CommonParser {

  import UQLParser._

  def UQLFilter: Rule1[Filter] = rule {
    UQLEntity ~ ANDSign ~ UQLAnyCondition ~~> ((entity, condition) => Filter(entity, condition)) ~ EOI
  }

  def UQLEntity: Rule1[Entity] = rule {
    "entity" ~ EqualsSign ~ UQLString ~~> (name => Entity(name.value))
  }

  def UQLAnyCondition: Rule1[Condition] = rule(UQLBooleanCondition | UQLCondition | UQLNestedCondition)

  def UQLBooleanCondition = rule(UQLAndCondition | UQLOrCondition)

  def UQLNestedCondition: Rule1[Condition] = rule {
    LeftParen ~ UQLAnyCondition ~ RightParen
  }

  def LeftParen: Rule0 = rule(WhiteSpace ~ "(" ~ WhiteSpace)

  def RightParen: Rule0 = rule(WhiteSpace ~ ")" ~ WhiteSpace)

  def UQLAndCondition: Rule1[AndCondition] = rule {
    (UQLCondition | UQLNestedCondition) ~ ANDSign ~ (UQLCondition | UQLNestedCondition) ~~> ((left, right) => AndCondition(left, right))
  }

  def UQLOrCondition: Rule1[OrCondition] = rule {
    (UQLCondition | UQLNestedCondition) ~ ORSign ~ (UQLCondition | UQLNestedCondition) ~~> ((left, right) => OrCondition(left, right))
  }

  def ANDSign: Rule0 = rule(WhiteSpace ~ ignoreCase("AND") ~ WhiteSpace)

  def ORSign: Rule0 = rule(WhiteSpace ~ ignoreCase("OR") ~ WhiteSpace)

  def UQLCondition: Rule1[Condition] = rule(UQLEqualsCondition | UQLNotEqualsCondition | UQLLowerCondition |
    UQLGreaterCondition | UQLLowerOrEqualCondition | UQLGreaterOrEqualCondition | UQLInCondition |
    UQLIncludesCondition | UQLExcludesCondition | UQLLikeCondition | UQLNotInCondition | UQLILikeCondition)

  def UQLEqualsCondition: Rule1[EqualsCondition] = rule {
    UQLIdent ~ EqualsSign ~ Value ~~> ((name, value) => EqualsCondition(name.value, value))
  }

  def UQLNotEqualsCondition: Rule1[NotEqualsCondition] = rule {
    UQLIdent ~ NotEqualsSign ~ Value ~~> ((name, value) => NotEqualsCondition(name.value, value))
  }

  def UQLLowerCondition: Rule1[LowerCondition] = rule {
    UQLIdent ~ LowerThanSign ~ Value ~~> ((name, value) => LowerCondition(name.value, value))
  }

  def UQLGreaterCondition: Rule1[GreaterCondition] = rule {
    UQLIdent ~ GreaterThanSign ~ Value ~~> ((name, value) => GreaterCondition(name.value, value))
  }

  def UQLLowerOrEqualCondition: Rule1[LowerOrEqualCondition] = rule {
    UQLIdent ~ LowerOrEqualThanSign ~ Value ~~> ((name, value) => LowerOrEqualCondition(name.value, value))
  }

  def UQLGreaterOrEqualCondition: Rule1[GreaterOrEqualCondition] = rule {
    UQLIdent ~ GreaterOrEqualThanSign ~ Value ~~> ((name, value) => GreaterOrEqualCondition(name.value, value))
  }

  def UQLInCondition: Rule1[InCondition] = rule {
    UQLIdent ~ InSign ~ UQLArray ~~> ((name, value) => InCondition(name.value, value))
  }

  def UQLNotInCondition: Rule1[NotInCondition] = rule {
    UQLIdent ~ NotSign ~ InSign ~ UQLArray ~~> ((name, value) => NotInCondition(name.value, value))
  }

  def UQLIncludesCondition: Rule1[IncludesCondition] = rule {
    UQLIdent ~ IncludesSign ~ UQLArray ~~> ((name, value) => IncludesCondition(name.value, value))
  }

  def UQLExcludesCondition: Rule1[ExcludesCondition] = rule {
    UQLIdent ~ ExcludesSign ~ UQLArray ~~> ((name, value) => ExcludesCondition(name.value, value))
  }

  def UQLLikeCondition: Rule1[LikeCondition] = rule {
    UQLIdent ~ LikeSign ~ Value ~~> ((name, value) => LikeCondition(name.value, value))
  }

  def UQLILikeCondition: Rule1[ILikeCondition] = rule {
    UQLIdent ~ (InsensitiveSign ~ LikeSign | ILikeSign) ~ Value ~~> ((name, value) => ILikeCondition(name.value, value))
  }

  def EqualsSign: Rule0 = rule(WhiteSpace ~ ("=" | ignoreCase("IS")) ~ WhiteSpace)

  def NotEqualsSign: Rule0 = rule(WhiteSpace ~ ("!=" | ignoreCase("NE")) ~ WhiteSpace)

  def LowerThanSign: Rule0 = rule(WhiteSpace ~ ("<" | ignoreCase("LT")) ~ WhiteSpace)

  def GreaterThanSign: Rule0 = rule(WhiteSpace ~ (">" | ignoreCase("GT")) ~ WhiteSpace)

  def LowerOrEqualThanSign: Rule0 = rule(WhiteSpace ~ ("<=" | ignoreCase("LTE")) ~ WhiteSpace)

  def GreaterOrEqualThanSign: Rule0 = rule(WhiteSpace ~ (">=" | ignoreCase("GTE")) ~ WhiteSpace)

  def InSign: Rule0 = rule(WhiteSpace ~ ignoreCase("IN") ~ WhiteSpace)

  def IncludesSign: Rule0 = rule(WhiteSpace ~ ignoreCase("INCLUDES") ~ WhiteSpace)

  def ExcludesSign: Rule0 = rule(WhiteSpace ~ ignoreCase("EXCLUDES") ~ WhiteSpace)

  def LikeSign: Rule0 = rule(WhiteSpace ~ ignoreCase("LIKE") ~ WhiteSpace)

  def ILikeSign: Rule0 = rule(WhiteSpace ~ ignoreCase("ILIKE") ~ WhiteSpace)

  def InsensitiveSign: Rule0 = rule(WhiteSpace ~ ignoreCase("INSENSITIVE") ~ WhiteSpace)

  def NotSign: Rule0 = rule(WhiteSpace ~ ignoreCase("NOT") ~ WhiteSpace)

  def Value[_]: Rule1[AstValue[_]] = rule {
    UQLString | UQLDate | UQLNumber | UQLArray | UQLTrue | UQLFalse | UQLNull
  }

  def UQLString: Rule1[StringNode] = rule(WhiteSpace ~ "\"" ~ zeroOrMore(Character) ~> StringNode ~ "\"" ~ WhiteSpace)

  def UQLIdent: Rule1[StringNode] = rule(WhiteSpace ~ (Ident ~> StringNode) ~ WhiteSpace)

  def UQLTrue: Rule1[BooleanNode] = rule { WhiteSpace ~ "true" ~ WhiteSpace ~ push(True) }

  def UQLFalse: Rule1[BooleanNode] = rule { WhiteSpace ~ "false" ~ WhiteSpace ~ push(False) }

  def UQLNull: Rule1[NullValue] = rule { WhiteSpace ~ "null" ~ WhiteSpace ~ push(Null) }

  def UQLNumber: Rule1[NumberNode] = rule {
    WhiteSpace ~ group(Integer ~ optional(Frac ~ optional(Exp))) ~> (s => NumberNode(BigDecimal(s))) ~ WhiteSpace
  }

  def UQLArray: Rule1[ArrayNode] = rule { WhiteSpace ~ "[ " ~ WhiteSpace ~ zeroOrMore(Value, separator = ", ") ~ WhiteSpace ~ "] " ~ WhiteSpace ~~> ArrayNode }

  def UQLDate: Rule1[DateTimeNode] = rule {
    WhiteSpace ~ group(nTimes(4, Digit) ~ ("/"|"-") ~ nTimes(2, Digit) ~ ("/"|"-") ~ nTimes(2, Digit) ~ optional("T" ~ nTimes(2, Digit) ~ ":" ~ nTimes(2, Digit) ~ ":" ~ nTimes(2, Digit) ~ "." ~ nTimes(3, Digit) ~ ("Z" | (("+" | "-") ~ nTimes(2, Digit) ~ ":" ~ nTimes(2, Digit))))) ~> (d => DateTimeNode(dateTimeFormatter.parseDateTime(d)))
  }

  def parseUQL(uql: String): Filter = {
    val parsingResult = ReportingParseRunner(UQLFilter).run(uql)
    parsingResult.result match {
      case Some(astRoot) => astRoot
      case None => throw new ParsingException(s"""Invalid UQL query: \n${ErrorUtils.printParseErrors(parsingResult)}""")
    }
  }
}
