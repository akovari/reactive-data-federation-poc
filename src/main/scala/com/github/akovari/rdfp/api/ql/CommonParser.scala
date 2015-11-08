package com.github.akovari.rdfp.api.ql

import org.parboiled.scala._

/**
 * Created by akovari on 03/07/15.
 */
trait CommonParser extends Parser {
  def Ident: Rule0 = rule {
    (("a" - "z" | "A" - "Z" | "_") ~ zeroOrMore(("a" - "z" | "A" - "Z" | "_" | "0" - "9")))
  }

  def Character: Rule0 = rule {
    EscapedChar | NormalChar
  }

  def EscapedChar: Rule0 = rule {
    "\\" ~ (anyOf("\"\\/bfnrt") | Unicode)
  }

  def NormalChar: Rule0 = rule {
    !anyOf("\"\\") ~ ANY
  }

  def Unicode: Rule0 = rule {
    "u" ~ HexDigit ~ HexDigit ~ HexDigit ~ HexDigit
  }

  def Integer: Rule0 = rule {
    optional("-") ~ (("1" - "9") ~ Digits | Digit)
  }

  def Digits: Rule0 = rule {
    oneOrMore(Digit)
  }

  def Digit: Rule0 = rule {
    "0" - "9"
  }

  def HexDigit: Rule0 = rule {
    "0" - "9" | "a" - "f" | "A" - "Z"
  }

  def Frac: Rule0 = rule {
    "." ~ Digits
  }

  def Exp: Rule0 = rule {
    ignoreCase("e") ~ optional(anyOf("+-")) ~ Digits
  }

  /**
   * @see https://github.com/sirthias/parboiled/wiki/Handling-Whitespace
   */
  override implicit def toRule(string: String) =
    if (string.endsWith(" "))
      str(string.trim) ~ WhiteSpace
    else
      str(string)

  def WhiteSpace: Rule0 = rule {
    zeroOrMore(anyOf(" \n\r\t\f"))
  }

  case class ParsingException(msg: String) extends Exception(msg)

}
