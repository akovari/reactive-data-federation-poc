package com.github.akovari.rdfp.api.mapreduce

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshaller._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.ExecutionContext

/**
  * Created by akovari on 07.11.15.
  */
class MapReduceService(mapReduceResource: MapReduceQueryResource)(implicit executionContext: ExecutionContext, materializer: Materializer)
  extends Directives with Json4sSupport {
  implicit val serialization = jackson.Serialization // or native.Serialization

  import com.github.akovari.rdfp._

  val route =
    path("mr") {
      parameterSeq { params =>
        pathEnd {
          post {
            requestEntityPresent {
              decodeRequest {
                entity(as[String]) { body =>
                  complete {
                    mapReduceResource.evaluate(body, params.map(_._2))
                  }
                }
              }
            }
          }
        }
      }
    }
}
