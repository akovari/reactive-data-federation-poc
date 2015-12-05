package com.github.akovari.rdfp.data

import spray.json.DefaultJsonProtocol

/**
  * Created by akovari on 07.11.15.
  */
object Models {

  case class Case(caseNumber: String,
                  status: String)

  case class CaseLink(id: Int, url: String)

}

trait JsonProtocols extends DefaultJsonProtocol {
  import com.github.akovari.rdfp.data.Models._

  implicit val caseFmt = jsonFormat2(Case.apply)
  implicit val caseLinkFmt = jsonFormat2(CaseLink.apply)
}
