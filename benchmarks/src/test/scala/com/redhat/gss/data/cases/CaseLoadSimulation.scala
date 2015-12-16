package com.redhat.gss.data.cases

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import spray.http.MediaTypes._


/**
 * User: akovari
 * Date: 5/31/14
 * Time: 10:33 AM
 */
class CaseLoadSimulation extends Simulation {
  val cases = Seq("00001002", "00001016", "00001024")

  val httpProtocol = http
    .baseURL("http://localhost:8080")
    .acceptEncodingHeader(`application/json`.toString())

  def randCaseNumber = {
    val rnd = new scala.util.Random
    cases(rnd.nextInt(cases length))
  }

  val randomChain = for {
    i <- 1 to cases.length
  } yield (100.0 / cases.length, exec(
      http("Case")
        .get(s"/case/${randCaseNumber}")
    ))

  val scn = scenario("Get a bunch of cases").randomSwitch(randomChain: _*)

  setUp(scn.inject(atOnceUsers(cases.length)))
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.count.is(cases.length),
      global.responseTime.max.lessThan((30 seconds).toMillis.toInt))
}
