package com.github.akovari.rdfp.data

import com.github.akovari.rdfp.data.Models.Case
import spray.json.DefaultJsonProtocol

/**
  * Created by akovari on 07.11.15.
  */
object Models {

  case class Case(caseNumber: String,
                  status: String)

}

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val caseFmt = jsonFormat2(Case.apply)
}
