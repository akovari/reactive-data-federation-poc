package com.github.akovari.rdfp.api.mapreduce.impl

import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{LoggingFSM, _}
import akka.util.Timeout
import com.github.akovari.rdfp.api.mapreduce._
import com.github.akovari.rdfp.Configuration

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object MapReduceConstants {
  implicit val defaultQueryTimeout = Timeout(Configuration().getDuration("mapreduce.queryTimeout", TimeUnit.SECONDS) seconds)
  val defaultPhaseTimeout = Configuration().getDuration("mapreduce.phaseTimeout", TimeUnit.SECONDS) seconds

  def api(implicit system: akka.actor.ActorSystem) = system.actorOf(Props[UnifiedMapReduceApi])
}

case class NoEmitInvokedInPhase(phase: State) extends Exception(phase.toString)

/**
 * Created by akovari on 27.2.2015.
 */
class MapReducer extends LoggingFSM[State, Data] with ActorLogging {

  import com.github.akovari.rdfp.api.mapreduce.MapReduceModels._
  import com.github.akovari.rdfp.api.mapreduce.impl.MapReduceConstants._
  import com.github.akovari.rdfp.api.mapreduce.impl.MapReduceJavaScriptEngine._

  override def logDepth = 12
  implicit val system = context.system
  implicit val executionContext = context.dispatcher
  implicit val loggingAdapter = log

  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
    case e: Exception =>
      Escalate
  }

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(EvaluateScript(scriptBody, args), Uninitialized) =>
      goto(LoadPhase) using EvaluatedScriptEngine(ScriptEngineActor(evalWithActorRef(self, scriptBody), sender), args)
  }

  onTransition {
    case Idle -> LoadPhase =>
      (nextStateData: @unchecked) match {
        case EvaluatedScriptEngine(ScriptEngineActor(engine, _), args) =>
          val s = self
          invokeFunction(engine, 'load, args: _*).onFailure {
            case e => s ! PhaseProcessingFailed(LoadPhase, e)
          }
      }
  }

  when(LoadPhase, stateTimeout = defaultPhaseTimeout) {
    case Event(EmitEvent(data), EvaluatedScriptEngine(engine, _)) =>
      stay using EmittedData(engine, data)
    case Event(EmitEvent(data), EmittedData(engine, curdata)) =>
      stay using EmittedData(engine, mergeMaps(curdata, data))
    case Event(EmitKVEvent(key, value), EvaluatedScriptEngine(engine, _)) =>
      stay using EmittedData(engine, Map(key -> value))
    case Event(EmitKVEvent(key, value), EmittedData(engine, curdata)) =>
      stay using EmittedData(engine, mergeMaps(curdata, Map(key -> value)))
    case Event(PhaseDoneEvent, EmittedData(engine, data)) =>
      goto(MapPhase) using EmittedData(engine, newEmitable)
    case Event(PhaseDoneEvent, EvaluatedScriptEngine(engine, _)) =>
      // No emit event from LoadPhase
      stop(FSM.Failure(NoEmitInvokedInPhase(LoadPhase)))
  }

  onTransition {
    case LoadPhase -> MapPhase =>
      (stateData: @unchecked) match {
        case EmittedData(ScriptEngineActor(engine, senderActor), data) =>
          val s = self
          Future.sequence(data.map { entry =>
            val f = invokeFunction(engine, 'map, entry._1, entry._2.asJava)
            f.onFailure {
              case e => s ! PhaseProcessingFailed(MapPhase, e)
            }
            f
          }).onSuccess {
            case _ => s ! PhaseDoneEvent
          }
        case EvaluatedScriptEngine(engine, _) =>
      }
  }

  when(MapPhase, stateTimeout = defaultPhaseTimeout) {
    case Event(EmitEvent(data), EmittedData(engine, curdata)) =>
      stay using EmittedData(engine, mergeMaps(curdata, data))
    case Event(EmitKVEvent(key, value), EmittedData(engine, curdata)) =>
      stay using EmittedData(engine, mergeMaps(curdata, Map(key -> value)))
    case Event(PhaseDoneEvent, EmittedData(engine, data)) if data.size() == 0 =>
      stop(FSM.Failure(NoEmitInvokedInPhase(MapPhase)))
    case Event(PhaseDoneEvent, EmittedData(engine, data)) =>
      goto(ReducePhase) using EmittedData(engine, newEmitable)
  }

  onTransition {
    case MapPhase -> ReducePhase =>
      (stateData: @unchecked) match {
        case EmittedData(ScriptEngineActor(engine, senderActor), data) =>
          val s = self
          Future.sequence(data.map { entry =>
            val f = invokeFunction(engine, 'reduce, entry._1, entry._2.asJava)
            f.onFailure {
              case e => s ! PhaseProcessingFailed(ReducePhase, e)
            }
            f
          }).onSuccess {
            case _ => s ! PhaseDoneEvent
          }
      }
  }

  when(ReducePhase, stateTimeout = defaultPhaseTimeout) {
    case Event(EmitEvent(data), EmittedData(engine, curdata)) =>
      stay using EmittedData(engine, mergeMaps(curdata, data))
    case Event(EmitKVEvent(key, value), EmittedData(engine, curdata)) =>
      stay using EmittedData(engine, mergeMaps(curdata, Map(key -> value)))
    case Event(PhaseDoneEvent, EmittedData(engine, data)) if data.size() == 0 =>
      stop(FSM.Failure(NoEmitInvokedInPhase(ReducePhase)))
    case Event(PhaseDoneEvent, EmittedData(engine, data)) =>
      goto(FinalizePhase) using EmittedData(engine, newEmitable)
  }

  onTransition {
    case ReducePhase -> FinalizePhase =>
      (stateData: @unchecked) match {
        case EmittedData(ScriptEngineActor(engine, senderActor), data) =>
          val s = self
          invokeFunction(engine, 'finalize, data).onComplete {
            case Success(_) => s ! PhaseDoneEvent
            case Failure(e) => s ! PhaseProcessingFailed(FinalizePhase, e)
          }
      }
  }

  when(FinalizePhase, stateTimeout = defaultPhaseTimeout) {
    case Event(EmitEvent(data), EmittedData(engine, curdata)) =>
      stay using EmittedData(engine, mergeMaps(curdata, data))
    case Event(EmitKVEvent(key, value), EmittedData(engine, curdata)) =>
      stay using EmittedData(engine, mergeMaps(curdata, Map(key -> value)))
    case Event(PhaseDoneEvent, EmittedData(ScriptEngineActor(engine, senderActor), data)) =>
      senderActor ! FinalizedData(data)
      goto(Idle) using Uninitialized
  }


  onTransition {
    case FinalizePhase -> Idle =>
      (stateData: @unchecked) match {
        case EmittedData(ScriptEngineActor(engine, senderActor), data) =>
          log.debug(s"Finishing Map/Reduce Query with $data")
      }
  }

  whenUnhandled {
    case Event(StateTimeout, s) =>
      log.warning("Received StateTimeout in state {}", s)
      stop(FSM.Failure(StateTimeout))
    case Event(PhaseProcessingFailed(p, e), s) =>
//      log.error(e, s"Phase Processing Failed: $p")
      stop(FSM.Failure(e))
    case Event(DataWithCallback(data, cb), s) =>
      cb(data.asJava)
      stay
    case Event(e, s) =>
      log.warning("Received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case from -> to => log.debug(s"Transitioning state $from -> $to using $nextStateData")
  }

  onTermination {
    case StopEvent(FSM.Failure(cause), state, data) => {
      val lastEvents = getLog.mkString("\n\t")
      val msg =
        s"""$cause
            |
            |
            |Failure in state $state with data $data.
                                                      |
                                                      |Events leading up to this point:
                                                      |$lastEvents""".stripMargin
      (data: @unchecked) match {
        case EvaluatedScriptEngine(ScriptEngineActor(engine, senderActor), _) => senderActor ! MapReducerFailure(msg)
        case EmittedData(ScriptEngineActor(engine, senderActor), _) => senderActor ! MapReducerFailure(msg)
      }
    }
  }

  initialize()
}
