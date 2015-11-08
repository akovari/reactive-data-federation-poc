package com.github.akovari.rdfp.api.ql.db

import com.github.akovari.rdfp.api.ql.UQLContext
import com.github.akovari.rdfp.api.ql.UQLContext.IllegalUQLFieldException
import com.github.akovari.typesafeSalesforce.query.SimpleColumn
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

/**
 * Created by akovari on 12.09.14.
 */
object SOQLContext {
  private val config = ConfigFactory.load("ql/soqlMappings.conf")

  def fieldToEntityField(field: String)(implicit resourceType: UQLContext.UnifiedResult.UnifiedResultFromResourceType): SimpleColumn[Any] = {
    val tablesConfs = config.getConfig(resourceType.value.toString)
    val tableNames = config.getObject(resourceType.value.toString).keySet().asScala
    val tableFound = tableNames.find(table => tablesConfs.hasPath(s"$table.fields.$field"))

    if (tableFound.isDefined) SimpleColumn(tablesConfs.getString(s"${tableFound.get}.fields.$field"))
    else throw IllegalUQLFieldException(s"""Invalid Field "$field"""")
  }
}
