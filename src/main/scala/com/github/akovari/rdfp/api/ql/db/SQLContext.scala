package com.github.akovari.rdfp.api.ql.db

import com.github.akovari.rdfp.api.ql.UQLContext.IllegalUQLFieldException
import com.github.akovari.rdfp.api.ql.UQLContext.UnifiedResult.UnifiedResultFromResourceType
import com.github.akovari.rdfp.api.ql.db.SQLContext._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import org.jooq._
import com.github.akovari.rdfp.api.ql.UQLContext

/**
 * Created by akovari on 12.09.14.
 */
object SQLContext {
  protected val sqlConfig = ConfigFactory.load("ql/sqlMappings.conf")
  protected val metricsConfig = ConfigFactory.load("ql/metricsMappings.conf")

  def config(implicit entityType: UQLContext.UnifiedResult.UnifiedEntityType[_]) = entityType match {
    case resourceType: UQLContext.UnifiedResult.UnifiedResultFromResourceType => sqlConfig
  }

  def fieldToTableColumn(field: String)(implicit entityType: UQLContext.UnifiedResult.UnifiedEntityType[_]) = {
    val tablesConfs = config.getConfig(entityType.value.toString)
    val tableNames = config.getObject(entityType.value.toString).keySet().asScala
    val tableFound = tableNames.find(table => tablesConfs.hasPath(s"$table.columns.$field"))

    if (tableFound.isDefined) tablesConfs.getString(s"${tableFound.get}.alias") + "." + tablesConfs.getString(s"${tableFound.get}.columns.$field")
    else throw IllegalUQLFieldException(s"""Invalid Field "$field"""")
  }

  def tableColumnToField[R <: Record, R2 <: Record, T](table: Table[R2], column: TableField[R, T], suffix: Option[String] = None)(implicit entityType: UQLContext.UnifiedResult.UnifiedEntityType[_]) = {
    val cols = config.getConfig(s"${entityType.value.toString}.${table.getName + suffix.getOrElse("")}.columns")
    cols.entrySet().asScala.find(_.getValue.unwrapped == column.getName).map(_.getKey)
  }

  def aliasForTable[R <: Record](entityType: UQLContext.UnifiedResult.UnifiedEntityType[_], table: Table[R], suffix: Option[String] = None) =
    config(entityType).getString(s"${entityType.value.toString}.${table.getName + suffix.getOrElse("")}.alias")
}

sealed trait UnifiedTables {
}

object SQLTables {
  def fields(tables: Table[_]*) = tables.map(_.fields()).flatten.toList.asJava
}
