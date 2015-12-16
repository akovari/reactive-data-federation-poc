package com.github.akovari.rdfp.api.mapreduce

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.google.common.collect.ArrayListMultimap
import com.github.akovari.rdfp.util.JacksonWrapper
import com.github.akovari.rdfp.api.mapreduce.impl.MapReducer
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.Await

/**
 * Created by akovari on 27.2.2015.
 */
class MapReduceActorSpec extends TestKit(ActorSystem()) with FlatSpecLike with Matchers with DefaultTimeout with ImplicitSender with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val executionContext = system.dispatcher
  implicit val log = system.log

  private def newMultiMap[K, V](k: K, v: V) = {
    val m: ArrayListMultimap[K, V] = ArrayListMultimap.create()
    m.put(k, v)
    m
  }

  private def newMultiMap[K, V](k: K, v: Iterable[V]) = {
    val m: ArrayListMultimap[K, V] = ArrayListMultimap.create()
    m.putAll(k, v.asJava)
    m
  }

  "Map/Reduce Models" should """merge with 2 simple vals""" in {
    import com.github.akovari.rdfp.api.mapreduce.MapReduceModels._

      val l = newMultiMap("k", 1)
      val r = newMultiMap("k", 2)
      val o = newMultiMap("k", List(1, 2))

    mergeMaps(l, r) should equal(o)
    }

  "Map/Reduce Models" should """merge with 1 simple and 1 collection vals""" in {
    import com.github.akovari.rdfp.api.mapreduce.MapReduceModels._

    val l = newMultiMap("k", 1)
      val r = newMultiMap("k", List(2, 3))
      val o = newMultiMap("k", List(1, 2, 3))

    mergeMaps(l, r) should equal(o)
    }

  "Map/Reduce Models" should """merge with 2 simple vals with some duplicates""" in {
    import com.github.akovari.rdfp.api.mapreduce.MapReduceModels._

    val l = newMultiMap("k", 1)
      val r = newMultiMap("k", 1)
      val o = newMultiMap("k", List(1, 1))

    mergeMaps(l, r) should equal(o)
    }

  "Map/Reduce Models" should """merge with 2 collection vals with some duplicates""" in {
    import com.github.akovari.rdfp.api.mapreduce.MapReduceModels._

    val l = newMultiMap("k", List(1))
      val r = newMultiMap("k", List(1))
      val o = newMultiMap("k", List(1, 1))

    mergeMaps(l, r) should equal(o)
    }

  "Map/Reduce Models" should """2 collection vals with some duplicates should convert to Scala Map""" in {
    import com.github.akovari.rdfp.api.mapreduce.MapReduceModels._

    val l = newMultiMap("k", List(1))
      val r = newMultiMap("k", List(1))
      val o = Map("k" -> List(1, 1))

    emitableToEmitableScala(mergeMaps(l, r).asInstanceOf[Emitable]) should equal(o)
    }

  "Map/Reduce FSM" should """process a simple M/R script""" in {
      val script =
        """
          |load = function() {
          |    for (i = 0; i < 10; i++) {
          |        emit('data' + i,  i);
          |    }
          |
          |    done();
          |};
          |
          |map = function(k, v) {
          |   emit('data', v);
          |};
          |
          |reduce = function(k, v) {
          |    emit(k, v.length);
          |};
          |
          |finalize = function(data) {
          |    emitM(data);
          |};
        """.stripMargin

      val mr = system.actorOf(Props[MapReducer])
      val res = (mr ? EvaluateScript(script))
      val ret = Await.result(res.map {
        case FinalizedData(data) => JacksonWrapper.serialize(data)
        case MapReducerFailure(e) => log.warning("M/R Error", e)
      }, timeout.duration)

    ret should be(
        """
          |{"data":[10]}
        """.stripMargin.trim)
    }


  "Map/Reduce FSM" should """process a simple M/R script without emit in load""" in {
      val script =
        """
          |var load = function() {
          |    done();
          |};
          |
          |var map = function(k, v) {
          |    done();
          |};
          |
          |var reduce = function(k, v) {
          |};
          |
          |var finalize = function(data) {
          |};
        """.stripMargin

      val mr = system.actorOf(Props[MapReducer])

      mr ! EvaluateScript(script)
      // not sure why this doesn't work, maybe some incompatibility between akka-testkit and specs2
      //      expectMsg(MapReducerFailure(NoEmitInvokedInPhase(MapPhase))) must beAnInstanceOf[MapReducerFailure]
    expectMsgType[MapReducerFailure] shouldBe a[MapReducerFailure]
    }

  "Map/Reduce FSM" should """process a simple M/R script without emit in map""" in {
      val script =
        """
          |var load = function() {
          |    emit('test', 1);
          |    done();
          |};
          |
          |var map = function(k, v) {
          |    done();
          |};
          |
          |var reduce = function(k, v) {
          |};
          |
          |var finalize = function(data) {
          |};
        """.stripMargin

      val mr = system.actorOf(Props[MapReducer])

      mr ! EvaluateScript(script)
      // not sure why this doesn't work, maybe some incompatibility between akka-testkit and specs2
      //      expectMsg(MapReducerFailure(NoEmitInvokedInPhase(MapPhase))) must beAnInstanceOf[MapReducerFailure]
    expectMsgType[MapReducerFailure] shouldBe a[MapReducerFailure]
    }

  "Map/Reduce FSM" should """process a simple M/R script without emit in reduce""" in {
      val script =
        """
          |var load = function() {
          |    emit('test', 1);
          |    done();
          |};
          |
          |var map = function(k, v) {
          |    emit(k, v);
          |};
          |
          |var reduce = function(k, v) {
          |};
          |
          |var finalize = function(data) {
          |};
        """.stripMargin

      val mr = system.actorOf(Props[MapReducer])

      mr ! EvaluateScript(script)
      // not sure why this doesn't work, maybe some incompatibility between akka-testkit and specs2
      //      expectMsg(MapReducerFailure(NoEmitInvokedInPhase(MapPhase))) must beAnInstanceOf[MapReducerFailure]
    expectMsgType[MapReducerFailure] shouldBe a[MapReducerFailure]
    }

  "Map/Reduce FSM" should """process a script with a syntax error""" in {
      val script =
        """
          |.$
        """.stripMargin

      val mr = system.actorOf(Props[MapReducer])

      mr ! EvaluateScript(script)
      // not sure why this doesn't work, maybe some incompatibility between akka-testkit and specs2
      //      expectMsg(MapReducerFailure(NoEmitInvokedInPhase(MapPhase))) must beAnInstanceOf[MapReducerFailure]
    expectMsgType[MapReducerFailure] shouldBe a[MapReducerFailure]
    }
}
