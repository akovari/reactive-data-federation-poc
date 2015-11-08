package com.github.akovari.rdfp

import akka.actor.{TypedProps, TypedActor, ActorRefFactory, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.akovari.rdfp.data.cases._
import com.github.akovari.typesafeSalesforce.util.{SalesForceConnectionImpl, SalesForceConnection}

/**
  * Created by akovari on 06.11.15.
  */

object System extends App {
  implicit val system: ActorSystem = ActorSystem("rdfp")

  def actorRefFactory: ActorRefFactory = system

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val log = system.log

  implicit val sfdcConn: SalesForceConnection = new SalesForceConnectionImpl()
  val casesResource: CasesResource = TypedActor(system).typedActorOf(TypedProps[CasesResourceImpl])

  val bindingFuture = Http().bindAndHandle(new CasesService(casesResource).route, "localhost", 8080)

  sys.addShutdownHook(system.terminate())
  bindingFuture.map(f => sys.addShutdownHook(f.unbind()))
}
