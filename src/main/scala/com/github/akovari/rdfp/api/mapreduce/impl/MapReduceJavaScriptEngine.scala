package com.github.akovari.rdfp.api.mapreduce.impl

import java.util.concurrent.TimeUnit
import javax.script.{Invocable, ScriptEngine, ScriptEngineManager}

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import com.github.akovari.rdfp.Configuration

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

/**
 * Created by akovari on 27.2.2015.
 */
object MapReduceJavaScriptEngine extends MapReduceScriptEngine {
  val defaultEngineFutureTimeout = Configuration().getDuration("mapreduce.engineFutureTimeout", TimeUnit.SECONDS) seconds
  private val engineManager = new ScriptEngineManager(null)

  override def evalWithActorRef(ref: ActorRef, text: String)(implicit ec: ExecutionContext, log: LoggingAdapter, system: ActorSystem): Future[ScriptEngine] = Future {
    val engine = engineManager.getEngineByMimeType("text/javascript").ensuring(_ != null)
    def load(s: String) = Source.fromInputStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(s)).bufferedReader()
    engine.put("_actorRef", ref)
    engine.put("_executionContext", ec)
    engine.put("_scriptEngine", engine)
    engine.put("_defaultEngineFutureTimeout", defaultEngineFutureTimeout.toMillis)
    engine.put("log", log)
    engine.put("api", MapReduceConstants.api)
    //    engine.eval(
    //      """
    //        |from com.redhat.gss.unified.mapreduce import UserReportingQuery
    //        |
    //        |def emit(k, v):
    //        | from com.redhat.gss.unified.mapreduce import EmitEvent
    //        | from com.google.common.collect import Multimaps
    //        | _actorRef.tell(EmitEvent(Multimaps.forMap({k: v})), _actorRef)
    //        |
    //        |def emitM(data):
    //        | from com.redhat.gss.unified.mapreduce import EmitEvent
    //        | _actorRef.tell(EmitEvent(data), _actorRef)
    //        |
    //        |def done():
    //        | from com.redhat.gss.unified.mapreduce import PhaseDoneEvent
    //        | _actorRef.tell(PhaseDoneEvent.instance(), _actorRef)
    //        |
    //        |def query(msg):
    //        | from com.redhat.gss.unified.mapreduce import ApiQuery
    //        | from akka.pattern.Patterns import ask
    //        | from aplus import Promise
    //        | return ask(api, ApiQuery(msg, _actorRef), _defaultEngineFutureTimeout)
    //      """.stripMargin)
    engine.eval(load("js/polyfills.js"))
//    engine.eval(load("js/bower_components/immutable/dist/immutable.min.js"))
    engine.eval(load("js/bower_components/rsvp/rsvp.min.js"))
//    engine.eval(load("js/bower_components/moment/min/moment-with-locales.min.js"))
//    engine.eval(load("js/bower_components/moment-timezone/builds/moment-timezone-with-data.min.js"))
    engine.eval(
      s"""
         |var imports = new JavaImporter(com.github.akovari.rdfp.data, com.github.akovari.rdfp.api.mapreduce, org.joda.time);
         |var StringUtils = Java.type("org.apache.commons.lang.StringUtils");
         |var Collectors = Java.type("java.util.stream.Collectors");
         |var Arrays = Java.type("java.util.Arrays");
         |
         |/*var cloneArray = function(aImmut) {
         | var Arrays = Java.type("java.util.Arrays");
         |
         | var a = aImmut;
         | if(typeof aImmut.toJS === 'function') {
         |   a = aImmut.toJS();
         | }
         | return Arrays["copyOf(java.lang.Object[], int)"](a, a.length.intValue());
         |};
         |
         |var cloneMap = function(mImmut) {
         | return mImmut.mapEntries(function(entry) {return [entry[0], cloneArray(entry[1])]});
         |};
         |
         |var flattenArray = function(aImmut) {
         | var a = aImmut;
         | if(typeof aImmut.toJS === 'function') {
         |   a = aImmut.toJS();
         | }
         | return [].concat.apply([], a);
         |};*/
         |
         |var ask = function(msg) {
         | log.debug("Calling ask with: " + msg);
         | with(imports) {
         |   api.tell(new ApiQuery(msg, _actorRef), _actorRef);
         | }
         |};
         |
         |var seqAsJava = function(c) {
         |  var WrapAsJava = Java.type("scala.collection.JavaConversions")
         |  return WrapAsJava.seqAsJavaList(c);
         |};
         |
         |var mapAsScala = function(c) {
         |  var WrapAsJava = Java.type("scala.collection.JavaConversions")
         |  return WrapAsJava.mapAsScalaMap(c);
         |};
         |
         |var query = function(q) {return new RSVP.Promise(function(resolve, reject) {
         |   with(imports) {
         |     var loaded = function(data) {
         |       resolve(data);
         |     };
         |     ask(q(loaded));
         |   }
         | }).catch(function(err) {
         |   log.error(err);
         | })
         |};
         |
         |var emit = function(k, v) {
         | with(imports) {
         |   _actorRef.tell(new EmitKVEvent(k, v), _actorRef);
         | }
         |};
         |
         |var emitM = function(args) {
         | with(imports) {
         |   _actorRef.tell(new EmitEvent(args), _actorRef);
         | }
         |};
         |
         |var done = function(args) {
         | with(imports) {
         |   _actorRef.tell(PhaseDoneEvent.instance(), _actorRef);
         | }
         |};
         |""".stripMargin)
    engine.eval(java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", ""))
    engine
  }

  override def invokeFunction(engine: Future[ScriptEngine], f: Symbol, args: AnyRef*)(implicit ec: ExecutionContext, log: LoggingAdapter): Future[Any] = engine.map { e =>
    log.debug(s"Invoking Function ${f.name} with Arguments $args")
    e.asInstanceOf[Invocable].invokeFunction(f.name, args: _*)
  }
}
