package com.github.akovari.rdfp.data

/**
  * Created by akovari on 07.11.15.
  */
object Models {

  case class Case(caseNumber: String,
                  status: String)

  case class CaseLink(id: Int, caseNumber: String, url: String)

}
