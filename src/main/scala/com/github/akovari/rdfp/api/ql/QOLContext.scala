package com.github.akovari.rdfp.api.ql

import com.github.akovari.rdfp.api.ql.db.SOQLContext
import com.github.akovari.typesafeSalesforce.query.{DescendingOrder, SimpleColumn, AscendingOrder, Order}

/**
 * Created by akovari on 03/07/15.
 */
object QOLContext extends QOL2SOQLTransformer

sealed trait QOL2SOQLTransformer {
  def orderToSQOLOrder(tableOrder: QOLParser.TableOrder)(implicit resourceType: UQLContext.UnifiedResult.UnifiedResultFromResourceType): Seq[Order[_]] = tableOrder.fieldOrders.map {
    case QOLParser.AscFieldOrder(f) => AscendingOrder(SOQLContext.fieldToEntityField(f.value))
    case QOLParser.DescFieldOrder(f) => DescendingOrder(SOQLContext.fieldToEntityField(f.value))
  }
}
