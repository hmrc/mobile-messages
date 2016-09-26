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

import java.net.URLEncoder
import java.util.UUID

import org.apache.commons.codec.CharEncoding
import org.joda.time.DateTime
import play.api.Play._
import uk.gov.hmrc.mobilemessages.connector.model.{UpstreamMessageResponse, UpstreamMessageHeadersResponse}
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageHeader, MessageId, UnreadMessage}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.{Authorization, SessionId}

import play.api.Logger
import uk.gov.hmrc.mobilemessages.domain.RenderMessageLocation
import RenderMessageLocation.{formats, toUrl}
import uk.gov.hmrc.play.controllers.RestFormats
import play.api.libs.json.{JsObject, Json}


trait MessageConnector extends SessionCookieEncryptionSupport {

  import play.twirl.api.Html
  import uk.gov.hmrc.play.http._

  import scala.concurrent.{ExecutionContext, Future}

  def http: HttpGet with HttpPost

  val messageBaseUrl: String
  val provider: String
  val token: String
  val id: String

  def now: DateTime

  def messages()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] = {
    http.GET[UpstreamMessageHeadersResponse](s"$messageBaseUrl/messages").
      map(messageHeaders => messageHeaders.items)
  }

  def getMessageBy(id: MessageId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Message] = {
    http.GET[UpstreamMessageResponse](s"$messageBaseUrl/messages/${id.value}").
      map(_.toMessageUsing(MessageConnector.asInstanceOf[ServicesConfig]))
  }

  def render(message: Message, hc: HeaderCarrier)(implicit ec: ExecutionContext, auth: Option[Authority]): Future[Html] = {
    val authToken: Authorization = hc.authorization.getOrElse(throw new IllegalArgumentException("Failed to find auth header!"))
    val userId = auth.getOrElse(throw new IllegalArgumentException("Failed to find the user!"))

    // TODO These keys below are not really (and never been) tested - would be nice to write integration tests for them
    val keys = Seq(
      SessionKeys.sessionId -> SessionId(s"session-${UUID.randomUUID}").value,
      SessionKeys.authProvider -> provider,
      SessionKeys.name -> id,
      SessionKeys.authToken -> URLEncoder.encode(authToken.value, CharEncoding.UTF_8),
      SessionKeys.userId -> userId.authId,
      SessionKeys.token -> token,
      SessionKeys.lastRequestTimestamp -> now.getMillis.toString)

    val session: (String, String) = withSession(keys: _ *)
    implicit val updatedHc = hc.withExtraHeaders(session)
    http.GET[Html](message.renderUrl)
  }

  def markAsRead(message: UnreadMessage)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.POSTEmpty(message.markAsReadUrl)
  }
}

object MessageConnector extends MessageConnector with ServicesConfig {

  import uk.gov.hmrc.mobilemessages.config.WSHttp
  import uk.gov.hmrc.time.DateTimeUtils

  override def http = WSHttp

  override lazy val messageBaseUrl: String = baseUrl("message")

  override def now: DateTime = DateTimeUtils.now

  def exception(key: String) = throw new Exception(s"Failed to find $key")

  override lazy val provider = current.configuration.getString("provider").getOrElse(exception("provider"))
  override lazy val token = current.configuration.getString("token").getOrElse(exception("token"))
  override lazy val id = current.configuration.getString("id").getOrElse(exception("id"))

}
