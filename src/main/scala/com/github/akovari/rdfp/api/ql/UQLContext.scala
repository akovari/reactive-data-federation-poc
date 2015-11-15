package com.github.akovari.rdfp.api.ql

import java.sql.Timestamp

import com.github.akovari.rdfp.data.{ResourceType}
import com.github.akovari.rdfp.api.ql.QOLParser.TableOrder
import com.github.akovari.rdfp.api.ql.UQLContext.UnifiedResult.{UnifiedQueryOrderBy, UnifiedQueryLimit, UnifiedQueryOffset}
import com.github.akovari.rdfp.api.ql.db.{SQLContext, SOQLContext}
import org.apache.commons.lang.StringEscapeUtils
import org.joda.time.DateTime
import shapeless.HList

import scala.util._
import scala.collection.JavaConverters._


/**
 * Created by akovari on 20.8.2014.
 */
object UQLContext extends UQL2SQLTransformer with UQL2SOQLTransformer with UQL2NativeConditionFormatter {

  import com.github.akovari.rdfp.api.ql.UQLContext.UnifiedResult._
  import com.github.akovari.rdfp.api.ql.UQLParser._


  implicit def filterToFilterContext(filter: Filter): FilterContext = new FilterContext(filter)

  implicit def filterContextToFilter(filterCtx: FilterContext): Filter = filterCtx.filter

  def filterForResourceType[T](resourceType: ResourceType)(f: (UnifiedResultFromResourceType => UQLParser.Condition => T))(implicit filterCtx: FilterContext): T =
    Try(ResourceType.valueOf(filterCtx.filter.entity.value)) match {
      case Success(v) =>
        if (v.toString == filterCtx.filter.entity.value) f(UnifiedResultFromResourceType(v))(filterCtx.filter.condition)
        else throw InvalidResourceTypeException(filterCtx.filter.entity.value)
      case Failure(e) => throw InvalidResourceTypeException(filterCtx.filter.entity.value)
    }

  def filterForResourceTypes[T](f: PartialFunction[ResourceType, UnifiedResultFromResourceType => UQLParser.Condition => T])(implicit filterCtx: FilterContext): T =
    Try(ResourceType.valueOf(filterCtx.filter.entity.value)) match {
      case Success(v) =>
        if (v.toString == filterCtx.filter.entity.value) f(v)(UnifiedResultFromResourceType(v))(filterCtx.filter.condition)
        else throw InvalidResourceTypeException(filterCtx.filter.entity.value)
      case Failure(e) => throw InvalidResourceTypeException(filterCtx.filter.entity.value)
    }

  implicit def resourceTypeToEntityType(resourceType: UQLContext.UnifiedResult.UnifiedResultFromResourceType): UQLContext.UnifiedResult.UnifiedEntityType[_] = resourceType

  //  def filterForUri[T](uri: Uri)(f: (UnifiedResultFromString => UQLParser.Condition => T))(implicit filterCtx: FilterContext): T = ???

  sealed case class FilterContext(filter: Filter)

  case class IllegalUQLFieldException(msg: String) extends IllegalArgumentException(msg)

  case class IllegalUQLConditionException(msg: String) extends IllegalArgumentException(msg)

  case class UnexpectedUQLValueException(msg: String) extends IllegalArgumentException(msg)

  case class InvalidResourceTypeException(entity: String) extends Exception

  object UnifiedResult {

    sealed abstract class UnifiedEntityType[R](val value: R)

    sealed case class UnifiedResultFromResourceType(override val value: ResourceType) extends UnifiedEntityType[ResourceType](value = value)

    //    sealed case class UnifiedResultFromString(value: String) extends UnifiedEntityType[String](value = value)

    case class UnifiedQueryOffset(offset: Int)
    case class UnifiedQueryLimit(limit: Int)
    case class UnifiedQueryOrderBy(orderBy: String)
  }
}

sealed trait UQL2SQLTransformer {

  import UQLParser._
  import UQLContext._
  import SQLContext._
  import org.jooq
  import org.jooq._
  import org.jooq.impl.DSL

  private type ConditionType[T] = T => Field[T] => jooq.Condition

  def applyQueryCondition[R <: Record](q: SelectQuery[R], condition: UQLParser.Condition)(implicit resourceType: UnifiedResult.UnifiedEntityType[_], offset: Option[UnifiedQueryOffset], limit: Option[UnifiedQueryLimit]): Unit = {
    q.addConditions(conditionToJooqCondition(condition))
    val l: Integer = if (limit.isDefined) limit.get.limit else null
    val o: Integer = if (offset.isDefined) offset.get.offset else 0
    if (l != null && o != null) {
      q.addLimit(o, l)
    }
  }

  def applyUpdateQueryCondition[R <: Record](q:UpdateQuery[R], condition: UQLParser.Condition)(implicit resourceType: UnifiedResult.UnifiedEntityType[_]): Unit = {
    q.addConditions(conditionToJooqCondition(condition))
  }

  def applyDeleteQueryCondition[R <: Record](q:DeleteQuery[R], condition: UQLParser.Condition)(implicit resourceType: UnifiedResult.UnifiedEntityType[_]): Unit = {
    q.addConditions(conditionToJooqCondition(condition))
  }

  def conditionToPlainFields(condition: UQLParser.Condition)(implicit resourceType: UnifiedResult.UnifiedEntityType[_]): List[(Field[_], AstValue[_])] = condition match {
    case EqualsCondition(name, value) =>
      val field = DSL.field(name)
      (field.asInstanceOf[Field[_]], value.asInstanceOf[AstValue[_]]) :: Nil
    case AndCondition(left, right) => conditionToPlainFields(left) ::: conditionToPlainFields(right)
    case _ => Nil
  }

  def valueCastOrFail[T2, T](astNode: AstValue[T2], ct: Class[T]): T = {
    astNode match {
      case StringNode(value) if ct == classOf[String] =>
        value.asInstanceOf[T]
      case NumberNode(value) if ct == classOf[Int] =>
          value.toInt.asInstanceOf[T]
      case NumberNode(value) if ct == classOf[Integer] =>
        value.toInt.asInstanceOf[T]
      case NumberNode(value) if ct == classOf[Float] =>
        value.toFloat.asInstanceOf[T]
      case NumberNode(value) if ct == classOf[BigDecimal] =>
        value.asInstanceOf[T]
      case BooleanNode(value) if ct == classOf[Boolean] =>
        value.asInstanceOf[T]
      case Null if ct == classOf[Nothing] =>
        null.asInstanceOf[T]
      case DateTimeNode(value) if ct == classOf[DateTime] =>
        value.asInstanceOf[T]
      case ArrayNode(elements) if ct == classOf[Array[T]] =>
        elements.asInstanceOf[T]
      case _ => throw UnexpectedUQLValueException(astNode.toString)
    }
  }

  def applyTypedValueToTypedCondition[T, R](field: Field[T], astNode: AstNode)(cond: ConditionType[R]): jooq.Condition = astNode match {
    case StringNode(value) =>
      applyConditionToTypedField(field.asInstanceOf[Field[String]])(typeSafeCondition(cond.asInstanceOf[ConditionType[String]], value))
    case NumberNode(value) =>
      if (value.isValidInt)
        applyConditionToTypedField(field.asInstanceOf[Field[Int]])(typeSafeCondition(cond.asInstanceOf[ConditionType[Int]], value.toInt))
      else if (value.isDecimalFloat)
        applyConditionToTypedField(field.asInstanceOf[Field[Float]])(typeSafeCondition(cond.asInstanceOf[ConditionType[Float]], value.toFloat))
      else
        applyConditionToTypedField(field.asInstanceOf[Field[BigDecimal]])(typeSafeCondition(cond.asInstanceOf[ConditionType[BigDecimal]], value))
    case BooleanNode(value) =>
      applyConditionToTypedField(field.asInstanceOf[Field[Boolean]])(typeSafeCondition(cond.asInstanceOf[ConditionType[Boolean]], value))
    case Null =>
      applyConditionToTypedField(field.asInstanceOf[Field[Null]])(typeSafeCondition(cond.asInstanceOf[ConditionType[Null]], null))
    case DateTimeNode(value) =>
      applyConditionToTypedField(field.asInstanceOf[Field[Timestamp]])(typeSafeCondition(cond.asInstanceOf[ConditionType[Timestamp]], new Timestamp(value.getMillis)))
    case ArrayNode(elements) =>
      applyConditionToTypedField(field.asInstanceOf[Field[java.util.Collection[_ <: java.io.Serializable]]])(typeSafeCondition(cond.asInstanceOf[ConditionType[java.util.Collection[_ <: java.io.Serializable]]], elements.map {
        case StringNode(value) =>
          value
        case NumberNode(value) =>
          if (value.isValidInt)
            Int.box(value.toInt)
          else if (value.isDecimalFloat)
            Float.box(value.toFloat)
          else
            value
        case DateTimeNode(value) =>
          value
        case _ =>
          throw IllegalUQLFieldException(s"Collection field must be of type String, Number or DateTime.")
      }.asJavaCollection))
    case Entity(name) =>
      applyConditionToTypedField(field.asInstanceOf[Field[String]])(typeSafeCondition(cond.asInstanceOf[ConditionType[String]], name))
    case _ => throw IllegalUQLFieldException(s"""Invalid Field "${field.toString}"""")
  }

  private def conditionToJooqCondition(condition: UQLParser.Condition)(implicit resourceType: UnifiedResult.UnifiedEntityType[_]): jooq.Condition = condition match {
    case EqualsCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] =>
          if (typedVal == null) field.isNull
          else field.equal(typedVal)
      }
    case NotEqualsCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] =>
          if (typedVal == null) field.isNotNull
          else field.notEqual(typedVal)
      }
    case LikeCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] => field.like(typedVal.toString)
      }
    case ILikeCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] => field.likeIgnoreCase(typedVal.toString)
      }
    case LowerCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] => field.lessThan(typedVal)
      }
    case GreaterCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] => field.greaterThan(typedVal)
      }
    case LowerOrEqualCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] => field.lessOrEqual(typedVal)
      }
    case GreaterOrEqualCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] => field.greaterOrEqual(typedVal)
      }
    case InCondition(name, value) =>
      val field = DSL.field(fieldToTableColumn(name))

      applyTypedValueToTypedCondition(field, value) {
        typedVal: AnyRef => field: Field[AnyRef] => field.in(typedVal)
      }
    case AndCondition(left, right) =>
      conditionToJooqCondition(left).and(conditionToJooqCondition(right))
    case OrCondition(left, right) =>
      conditionToJooqCondition(left).or(conditionToJooqCondition(right))
    case _ =>
      throw new IllegalUQLConditionException(condition.toString)
  }

  private def applyConditionToTypedField[T](field: Field[T])(cond: Field[T] => jooq.Condition): jooq.Condition = cond(field)

  private def typeSafeCondition[T, F](cond: F => (Field[T]) => jooq.Condition, value: F): (Field[F]) => jooq.Condition = {
    cond(value).asInstanceOf[(Field[F]) => jooq.Condition]
  }
}

sealed trait UQL2SOQLTransformer {

  import com.github.akovari.typesafeSalesforce.query._
  import UQLContext._
  import SOQLContext._
  import QOLContext._

  def conditionToSOQLConditionWithOrder[C <: HList](condition: UQLParser.Condition, filterFunction: (Filter, Option[Seq[Order[_]]]) => SelectQuery[C])(implicit resourceType: UnifiedResult.UnifiedResultFromResourceType, offset: Option[UnifiedQueryOffset], limit: Option[UnifiedQueryLimit], orderBy: Option[UnifiedQueryOrderBy]): SelectQuery[C] = {
    val cond = conditionToSOQLConditionWithoutLimit(condition)
    val ordby = orderBy.map(o => new QOLParser().parseQOL(o.orderBy)).map(orderToSQOLOrder)
    val fcond = filterFunction(cond, ordby)
    if(limit.isDefined) SelectQuery(columns = fcond.columns, entities = fcond.entities, filter = fcond.filter, orders = fcond.orders, groupBys = fcond.groupBys, limit = limit.map(_.limit))
    else fcond
  }

  def conditionToSOQLCondition[C <: HList](condition: UQLParser.Condition, filterFunction: Filter => SelectQuery[C])(implicit resourceType: UnifiedResult.UnifiedResultFromResourceType, offset: Option[UnifiedQueryOffset], limit: Option[UnifiedQueryLimit]): SelectQuery[C] = {
    val cond = conditionToSOQLConditionWithoutLimit(condition)
    val fcond = filterFunction(cond)
    if(limit.isDefined) SelectQuery(columns = fcond.columns, entities = fcond.entities, filter = fcond.filter, orders = fcond.orders, groupBys = fcond.groupBys, limit = limit.map(_.limit))
    else fcond
  }

  def conditionToSOQLConditionWithoutLimit(condition: UQLParser.Condition)(implicit resourceType: UnifiedResult.UnifiedResultFromResourceType): Filter = condition match {
    case UQLParser.EqualsCondition(name, value) =>
      fieldToEntityField(name) :== typedValueToField(value)
    case UQLParser.NotEqualsCondition(name, value) =>
      fieldToEntityField(name) :!= typedValueToField(value)
    case UQLParser.LikeCondition(name, value) =>
      fieldToEntityField(name) like typedValueToField(value)
    case UQLParser.ILikeCondition(name, value) =>
      fieldToEntityField(name) like typedValueToField(value)
    case UQLParser.LowerCondition(name, value) =>
      fieldToEntityField(name) :< typedValueToField(value)
    case UQLParser.LowerOrEqualCondition(name, value) =>
      fieldToEntityField(name) :<= typedValueToField(value)
    case UQLParser.GreaterCondition(name, value) =>
      fieldToEntityField(name) :> typedValueToField(value)
    case UQLParser.GreaterOrEqualCondition(name, value) =>
      fieldToEntityField(name) :>= typedValueToField(value)
//    case UQLParser.InCondition(name, value) =>
//      fieldToEntityField(name) in typedValueToField(value)
//    case UQLParser.IncludesCondition(name, value) =>
//      fieldToEntityField(name) includes typedValueToField(value)
//    case UQLParser.NotInCondition(name, value) =>
//      fieldToEntityField(name) notIn typedValueToField(value)
//    case UQLParser.ExcludesCondition(name, value) =>
//      fieldToEntityField(name) excludes typedValueToField(value)
    case UQLParser.AndCondition(left, right) =>
      conditionToSOQLConditionWithoutLimit(left) and conditionToSOQLConditionWithoutLimit(right)
    case UQLParser.OrCondition(left, right) =>
      conditionToSOQLConditionWithoutLimit(left) or conditionToSOQLConditionWithoutLimit(right)
    case _ =>
      throw new IllegalUQLConditionException(condition.toString)
  }

  private def typedValueToField(astNode: UQLParser.AstNode): Field[Any] = astNode match {
    case UQLParser.StringNode(value) =>
      Field(value).asInstanceOf[Field[Any]]
    case UQLParser.NumberNode(value) =>
      if (value.isValidInt)
        Field(value.intValue()).asInstanceOf[Field[Any]]
      else if (value.isDecimalDouble)
        Field(value.doubleValue()).asInstanceOf[Field[Any]]
      else
        ???
    case UQLParser.BooleanNode(value) =>
      Field(value).asInstanceOf[Field[Any]]
    case UQLParser.Null =>
      Field.Null.asInstanceOf[Field[Any]]
    case UQLParser.DateTimeNode(value) =>
      Field(value.toDate).asInstanceOf[Field[Any]]
//    case UQLParser.ArrayNode(elements) => Field(elements.map(f => typedValueToField(f)))
    case UQLParser.Entity(name) => throw IllegalUQLFieldException(s"""Invalid Field "${astNode.toString}"""")
    case _ => throw IllegalUQLFieldException(s"""Invalid Field "${astNode.toString}"""")
  }
}

sealed trait UQL2NativeConditionFormatter {
  import UQLContext._
  import SQLContext._

  def conditionToNativeCondition(condition: UQLParser.Condition)(implicit resourceType: UnifiedResult.UnifiedEntityType[_], offset: Option[UnifiedQueryOffset], limit: Option[UnifiedQueryLimit]): String = condition match {
    case UQLParser.EqualsCondition(name, value) =>
      val v = typedValueToNativeField(value)
      if (v == "null")
        s"""(${fieldToTableColumn(name)} IS $v)"""
      else
        s"""(${fieldToTableColumn(name)} = $v)"""
    case UQLParser.NotEqualsCondition(name, value) =>
      val v = typedValueToNativeField(value)
      if (v == "null")
        s"""(${fieldToTableColumn(name)} IS NOT $v)"""
      else
        s"""(${fieldToTableColumn(name)} != $v)"""
    case UQLParser.LikeCondition(name, value) =>
      s"""(${fieldToTableColumn(name)} LIKE ${typedValueToNativeField(value)})"""
    case UQLParser.ILikeCondition(name, value) =>
      s"""(${fieldToTableColumn(name)} ILIKE ${typedValueToNativeField(value)})"""
    case UQLParser.LowerCondition(name, value) =>
      s"""(${fieldToTableColumn(name)} < ${typedValueToNativeField(value)})"""
    case UQLParser.LowerOrEqualCondition(name, value) =>
      s"""(${fieldToTableColumn(name)} <= ${typedValueToNativeField(value)})"""
    case UQLParser.GreaterCondition(name, value) =>
      s"""(${fieldToTableColumn(name)} > ${typedValueToNativeField(value)})"""
    case UQLParser.GreaterOrEqualCondition(name, value) =>
      s"""(${fieldToTableColumn(name)} >= ${typedValueToNativeField(value)})"""
    case UQLParser.InCondition(name, value) =>
      s"""(${fieldToTableColumn(name)} IN ${typedValueToNativeField(value)})"""
    case UQLParser.NotInCondition(name, value) =>
      s"""(${fieldToTableColumn(name)} NOT IN ${typedValueToNativeField(value)})"""
    case UQLParser.IncludesCondition(name, value) =>
      throw new IllegalUQLConditionException(condition.toString)
    case UQLParser.ExcludesCondition(name, value) =>
      throw new IllegalUQLConditionException(condition.toString)
    case UQLParser.AndCondition(left, right) =>
      s"""(${conditionToNativeCondition(left)} AND ${conditionToNativeCondition(right)})"""
    case UQLParser.OrCondition(left, right) =>
      s"""(${conditionToNativeCondition(left)} OR ${conditionToNativeCondition(right)})"""
    case _ =>
      throw new IllegalUQLConditionException(condition.toString)
  }

  def typedValueToNativeField(astNode: UQLParser.AstNode): String = astNode match {
    case UQLParser.StringNode(value) =>
      "'" + StringEscapeUtils.escapeSql(value) + "'"
    case UQLParser.NumberNode(value) =>
      if (value.isValidInt)
        value.toString()
      else if (value.isDecimalDouble)
        value.toString()
      else
        ???
    case UQLParser.BooleanNode(value) =>
      value.toString
    case UQLParser.Null =>
      "null"
    case UQLParser.DateTimeNode(value) =>
      "'" + value.toString + "'"
    case UQLParser.ArrayNode(elements) => "(" + elements.map(typedValueToNativeField).mkString(",") + ")"
    case UQLParser.Entity(name) => throw IllegalUQLFieldException(s"""Invalid Field "${astNode.toString}"""")
    case _ => throw IllegalUQLFieldException(s"""Invalid Field "${astNode.toString}"""")
  }
}
