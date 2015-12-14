package com.github.akovari

import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats}

/**
  * Created by akovari on 14.12.15.
  */
package object rdfp {
  implicit val json4sJacksonFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

}
