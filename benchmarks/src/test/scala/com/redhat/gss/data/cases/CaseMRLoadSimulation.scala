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
class CaseMRLoadSimulation extends Simulation {
  val cases = Seq.fill(50)(Seq("00001002", "00001016", "00001024")).flatten

  val httpProtocol = http
    .baseURL("http://localhost:8080")
    .acceptEncodingHeader(`application/json`.toString())

  def randCaseNumber = {
    val rnd = new scala.util.Random
    cases(rnd.nextInt(cases length))
  }

  val query =
    s"""
      |var load = function (caseIds) {
      |    var caseIdsCond = StringUtils.join(Arrays.asList(StringUtils.split(caseIds, ',')).stream().map(function (i) {
      |        return '"' + i + '"'
      |    }).toArray(), ',');
      |
      |    query(function (cb) {
      |        with (imports) {
      |            return new CaseLinksQuery('caseId in [' + caseIdsCond + ']', cb)
      |        }
      |    }).then(function (cases) {
      |        emit('all_links', cases);
      |    }).finally(function () {
      |        done();
      |    });
      |};
      |
      |var map = function (k, v) {
      |    for (var idx = 0; idx < v.length; idx++) {
      |        emit(v[idx].caseNumber(), {
      |            count: 1
      |        });
      |    }
      |};
      |
      |var reduce = function (k, v) {
      |    try {
      |        var reducedVal = {
      |            count: 0
      |        };
      |        for (var idx = 0; idx < v.length; idx++) {
      |            reducedVal.count += v[idx].count;
      |        }
      |        emit(k, reducedVal.count);
      |    } catch (err) {
      |        log.error(err);
      |    }
      |};
      |
      |var finalize = function (data) {
      |    emitM(data);
      |};
    """.stripMargin

  val randomChain = for {
    i <- 1 to cases.length
  } yield (100.0 / cases.length, exec(
      http("Map/Reduce")
        .post(s"/mr").queryParam("caseIds", Seq(randCaseNumber).mkString(","))
        .body(StringBody(query)).asJSON
    ))

  val scn = scenario("Run a bunch of M/Rs").randomSwitch(randomChain: _*)

  setUp(scn.inject(atOnceUsers(cases.length)))
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.count.is(cases.length),
      global.responseTime.max.lessThan((30 seconds).toMillis.toInt))
}
