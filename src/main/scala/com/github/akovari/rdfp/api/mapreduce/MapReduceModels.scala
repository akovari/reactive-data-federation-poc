package com.github.akovari.rdfp.api.mapreduce

import java.util
import java.util.Map.Entry
import java.util.function.Function
import javax.script.ScriptEngine

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import com.google.common.collect._
import com.github.akovari.rdfp.api.mapreduce.MapReduceModels.{CallbackType, Emitable, EmitableScala}

import scala.collection.GenTraversable
import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
 * Created by akovari on 27.2.2015.
 */

object MapReduceModels {
  type Emitable = Multimap[AnyRef, Any]
  type EmitableScala = Map[AnyRef, List[Any]]
  type CallbackType[T, V] = Function[T, V]

  implicit def emitableToEmitableScala(e: Emitable): EmitableScala =
    (for {i <- e.asMap().entrySet().asScala} yield {
      (i.getKey, valueAsCollection(i))
    }).toMap

  implicit def emitableScalaToEmitable(e: Map[AnyRef, Any]): Emitable = {
    val r: Emitable = newEmitable
    for {i <- e.asJava.entrySet().asScala} {
      r.putAll(i.getKey, valueAsCollection(i).asJava)
    }
    r
  }

  def mergeMaps[K, V](l: Multimap[K, V], r: Multimap[K, V])(implicit log: LoggingAdapter): Multimap[K, V] = {
    val s = newEmitable[K, V]

    def add(entrySet: List[Entry[K, V]]) =
      for {i <- entrySet}
        s.putAll(i.getKey, valueAsCollection(i).asJava)

    add(l.entries().asScala.toList)
    add(r.entries().asScala.toList)
//    log.debug(s"L=$l, R=$r, O=$s")
    s
  }

  def flat[V](list: List[V]): List[V] = list flatten {
    case i: List[_] => flat(i.asInstanceOf[List[V]])
    case e => List(e)
  }

  private def valueAsCollection[K, V](i: Entry[K, V]): List[V] = flat(i.getValue match {
    case c: util.Collection[_] => c.asInstanceOf[util.Collection[V]].asScala.toList
    case c: GenTraversable[_] => c.asInstanceOf[GenTraversable[V]].toList
    case o => List(o)
  })

    //if (i.getValue.isInstanceOf[util.Collection[V]]) i.getValue.asInstanceOf[util.Collection[V]].asScala.toList else List(i.getValue)

  def newEmitable[K, V] = Multimaps.synchronizedMultimap(ArrayListMultimap.create[K, V]())
}

sealed trait Event
sealed trait ReceivedEvent extends Event
sealed trait SentEvent extends Event

case class EvaluateScript(body: String, args: Seq[String] = Seq.empty) extends ReceivedEvent

case object InvokeLoadFunction extends ReceivedEvent
case class EmitEvent(data: Emitable) extends ReceivedEvent
case class EmitKVEvent(key: AnyRef, value: Any) extends ReceivedEvent
case object PhaseDoneEvent extends ReceivedEvent {val instance = this}

case class PhaseProcessingFailed(phase: State, exception: Throwable) extends ReceivedEvent

case class FinalizedData(data: EmitableScala) extends SentEvent

case class MapReducerFailure(cause: Any) extends Exception(cause.toString) with SentEvent


sealed trait State

case object Idle extends State

case object LoadPhase extends State
case object MapPhase extends State
case object ReducePhase extends State
case object FinalizePhase extends State


sealed trait Data

case object Uninitialized extends Data
case class EvaluatedScriptEngine(engine: ScriptEngineActor, args: Seq[String] = Seq.empty) extends Data
case class EmittedData(engine: ScriptEngineActor, data: Emitable) extends Data


case class ScriptEngineActor(engine: Future[ScriptEngine], sender: ActorRef)

case class DataWithCallback[T, R](data: List[Any], cb: CallbackType[T, R])
