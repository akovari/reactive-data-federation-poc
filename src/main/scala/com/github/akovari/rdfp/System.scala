package com.github.akovari.rdfp

import akka.actor.{TypedProps, TypedActor, ActorRefFactory, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.{Materializer, ActorMaterializer}
import com.github.akovari.rdfp.api.mapreduce.{MapReduceQueryResourceImpl, MapReduceQueryResource, MapReduceService}
import com.github.akovari.rdfp.data.cases._
import com.github.akovari.typesafeSalesforce.util.{SalesForceConnectionImpl, SalesForceConnection}

import scala.concurrent.ExecutionContext

/**
  * Created by akovari on 06.11.15.
  */

trait Core {
  implicit val system: ActorSystem

  implicit val materializer: Materializer

  implicit val executionContext: ExecutionContext = system.dispatcher
}


trait BootedCore extends Core with Api {
  implicit lazy val system: ActorSystem = ActorSystem("rdfp")

  val actorRefFactory: ActorRefFactory = system

  implicit val materializer: Materializer = ActorMaterializer()

  val log = system.log

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  sys.addShutdownHook(system.terminate())
  bindingFuture.map(f => sys.addShutdownHook(f.unbind()))
}

trait CoreActors {
  this: Core =>

  def casesResource: () => CasesResource = {() =>
    val r: CasesResource = TypedActor(system).typedActorOf(TypedProps[CasesResourceImpl])
    r
  }

  def mapReduceResource: () => MapReduceQueryResource = {() =>
    val r: MapReduceQueryResource = TypedActor(system).typedActorOf(TypedProps[MapReduceQueryResourceImpl])
    r
  }
}

trait Api extends Directives with CoreActors with Core {

  import Configuration._

  val route = new CasesService(casesResource).route ~ new MapReduceService(mapReduceResource).route
}

object System extends App with BootedCore with Core with CoreActors with Api
