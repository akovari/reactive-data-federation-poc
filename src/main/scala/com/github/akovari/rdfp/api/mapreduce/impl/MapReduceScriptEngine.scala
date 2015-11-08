package com.github.akovari.rdfp.api.mapreduce.impl

import javax.activation.MimeType
import javax.script.ScriptEngine

import akka.actor.{ActorSystem, ActorRef}
import akka.event.LoggingAdapter

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by akovari on 3.3.2015.
 */

trait MapReduceScriptEngine {
  def evalWithActorRef(ref: ActorRef, text: String)(implicit ec: ExecutionContext, log: LoggingAdapter, system: ActorSystem): Future[ScriptEngine]

  def invokeFunction(engine: Future[ScriptEngine], f: Symbol, args: AnyRef*)(implicit ec: ExecutionContext, log: LoggingAdapter): Future[Any]
}
