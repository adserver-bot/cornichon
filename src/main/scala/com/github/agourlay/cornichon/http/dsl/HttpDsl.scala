package com.github.agourlay.cornichon.http.dsl

import akka.http.scaladsl.model.{ HttpHeader, StatusCode }
import cats.data.Xor
import com.github.agourlay.cornichon.core._
import com.github.agourlay.cornichon.core.dsl.Dsl
import com.github.agourlay.cornichon.http._
import spray.json.{ JsValue, _ }
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._

trait HttpDsl extends Dsl {
  this: HttpFeature ⇒

  implicit val requestTimeout: FiniteDuration = 2000 millis

  sealed trait Request { val name: String }

  sealed trait WithoutPayload extends Request {
    def apply(url: String, params: Seq[(String, String)] = Seq.empty, headers: Seq[HttpHeader] = Seq.empty) =
      Step(
        title = s"HTTP $name to $url",
        action = s ⇒ {
          val x = this match {
            case GET    ⇒ Get(url, params, headers)(s)
            case DELETE ⇒ Delete(url, params, headers)(s)
          }
          x.map { case (jsonRes, session) ⇒ (true, session) }.fold(e ⇒ throw e, identity)
        },
        expected = true)
  }

  sealed trait WithPayload extends Request {
    def apply(url: String, payload: JsValue, params: Seq[(String, String)] = Seq.empty, headers: Seq[HttpHeader] = Seq.empty) =
      Step(
        title = s"HTTP $name to $url",
        action = s ⇒ {
          val x = this match {
            case POST ⇒ Post(payload, url, params, headers)(s)
            case PUT  ⇒ Put(payload, url, params, headers)(s)
          }
          x.map { case (jsonRes, session) ⇒ (true, session) }.fold(e ⇒ throw e, identity)
        },
        expected = true)
  }

  case object GET extends WithoutPayload { val name = "GET" }

  case object DELETE extends WithoutPayload { val name = "DELETE" }

  case object POST extends WithPayload { val name = "POST" }

  case object PUT extends WithPayload { val name = "PUT" }

  def status_is(status: Int) = assertSession(LastResponseStatusKey, status.toString)

  def headers_contain(headers: (String, String)*) =
    assertSessionWithMap(LastResponseHeadersKey, true, sessionHeaders ⇒ {
      val sessionHeadersValue = sessionHeaders.split(",")
      headers.forall { case (name, value) ⇒ sessionHeadersValue.contains(s"$name:$value") }
    })

  def showLastStatus = showSession(LastResponseStatusKey)

  def response_body_is(jsValue: JsValue, ignoredKeys: String*) =
    assertSessionWithMap(LastResponseJsonKey, jsValue, sessionValue ⇒ {
      if (ignoredKeys.isEmpty) sessionValue.parseJson
      else sessionValue.parseJson.asJsObject.fields.filterKeys(!ignoredKeys.contains(_)).toJson
    })

  def response_body_is(mapFct: JsValue ⇒ String, jsValue: String) =
    assertSessionWithMap(LastResponseJsonKey, jsValue, sessionValue ⇒ {
      mapFct(sessionValue.parseJson)
    })

  def showLastResponseJson = showSession(LastResponseJsonKey)

  def showLastResponseHeaders = showSession(LastResponseHeadersKey)

  def response_body_array_is[A](mapFct: JsArray ⇒ A, expected: A) = {
    assertSessionWithMap[A](LastResponseJsonKey, expected, sessionValue ⇒ {
      val sessionJSON = sessionValue.parseJson
      sessionJSON match {
        case arr: JsArray ⇒ mapFct(arr)
        case _            ⇒ throw new RuntimeException(s"Expected JSON Array but got $sessionJSON")
      }
    })
  }

  def response_body_array_size_is(size: Int) = response_body_array_is(_.elements.size, size)

  def response_body_array_contains(element: JsValue) = response_body_array_is(_.elements.contains(element), true)

  def response_body_array_not_contain(element: JsValue) = response_body_array_is(_.elements.contains(element), false)
}