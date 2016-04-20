/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobilemessages.connector

import org.joda.time.DateTime
import uk.gov.hmrc.play.config.ServicesConfig


trait MessageConnector {

  import play.api.libs.json.{JsObject, Json}
  import uk.gov.hmrc.domain.SaUtr
  import uk.gov.hmrc.mobilemessages.domain.{MessageHeader, RenderMessageLocation}
  import RenderMessageLocation.{formats, toUrl}
  import play.twirl.api.Html
  import uk.gov.hmrc.play.controllers.RestFormats
  import uk.gov.hmrc.play.http._

  import scala.concurrent.{ExecutionContext, Future}

  def http: HttpGet with HttpPost

  val messageBaseUrl: String

  def now : DateTime

  private val returnReadAndUnreadMessages = "Both"

  def messages(utr: SaUtr)(implicit hc: HeaderCarrier, ec : ExecutionContext): Future[Seq[MessageHeader]] =
    http.GET[Seq[MessageHeader]](s"$messageBaseUrl/message/sa/$utr?read=$returnReadAndUnreadMessages")

  def readMessage(url : String)(implicit hc: HeaderCarrier, ec : ExecutionContext) : Future[Html] = {
    import RestFormats.dateTimeWrite
    http.POST[JsObject, RenderMessageLocation](s"$messageBaseUrl$url", Json.obj("readTime" -> now)).flatMap{
      renderMessageLocation =>
        http.GET[Html](renderMessageLocation) //TODO is this the correct response?
    }
  }
}

object MessageConnector extends MessageConnector with ServicesConfig {
  import uk.gov.hmrc.mobilemessages.config.WSHttp
  import uk.gov.hmrc.time.DateTimeUtils

  override def http = WSHttp

  override lazy val messageBaseUrl: String = baseUrl("message")

  override def now: DateTime = DateTimeUtils.now
}
