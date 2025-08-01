/*
 * Copyright 2023 HM Revenue & Customs
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

import java.net.URLEncoder.encode
import java.time.LocalDateTime
import com.typesafe.config.Config

import javax.inject.{Inject, Named}
import org.apache.commons.codec.CharEncoding.UTF_8
import org.apache.commons.codec.binary.Base64
import play.api.Configuration
import play.api.libs.crypto.CookieSigner
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, *}
import uk.gov.hmrc.mobilemessages.connector.model.{UpstreamMessageHeadersResponse, UpstreamMessageResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageCountResponse, MessageHeader, MessageId, UnreadMessage}

import scala.concurrent.{ExecutionContext, Future}

class MessageConnector @Inject() (@Named("secure-message") val messageBaseUrl: String,
                                  @Named("sa-message-renderer") val saMessageRendererBaseUrl: String,
                                  @Named("ats-message-renderer") val atsMessageRendererBaseUrl: String,
                                  @Named("secure-message-renderer") val secureMessageRendererBaseUrl: String,
                                  @Named("two-way-message") val twoWayMessageBaseUrl: String,
                                  configuration: Configuration,
                                  val cookieSigner: CookieSigner,
                                  val http: HttpClientV2
                                 )
    extends SessionCookieEncryptionSupport
    with HttpErrorFunctions {

  override lazy val config: Config = configuration.underlying

  implicit val now: LocalDateTime = LocalDateTime.now

  lazy val servicesToUrl: Map[String, String] = Map(
    "secure-message"          -> messageBaseUrl,
    "sa-message-renderer"     -> saMessageRendererBaseUrl,
    "ats-message-renderer"    -> atsMessageRendererBaseUrl,
    "secure-message-renderer" -> secureMessageRendererBaseUrl,
    "two-way-message"         -> twoWayMessageBaseUrl
  )

  def messages(lang: Option[String]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] =
    http
      .get(url"$messageBaseUrl/secure-messaging/messages?lang=$lang")
      .execute[UpstreamMessageHeadersResponse]
      .map(_.items)

  def messageCount(
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MessageCountResponse] =
    http
      .get(url"$messageBaseUrl/secure-messaging/messages/count")
      .execute[MessageCountResponse]

  def getMessageBy(
    id: MessageId,
    lang: Option[String]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Message] =
    http
      .get(url"$messageBaseUrl/secure-messaging/messages/${Base64.encodeBase64String(id.value.getBytes)}?lang=$lang")
      .execute[UpstreamMessageResponse]
      .map(_.toMessageUsing(servicesToUrl))

  def render(
    message: Message,
    lang: Option[String],
    hc: HeaderCarrier
  )(implicit ec: ExecutionContext): Future[Html] = {
    val authToken: Authorization =
      hc.authorization.getOrElse(throw new IllegalArgumentException("Failed to find auth header!"))

    val keys = Seq(SessionKeys.authToken -> encode(authToken.value, UTF_8))

    val session: (String, String) = withSession(keys*)
    implicit val updatedHc: HeaderCarrier = hc.withExtraHeaders(session)

    val headerLang: String = lang.getOrElse("")

    http
      .get(url"${message.renderUrl}")
      .setHeader("Accept-Language" -> headerLang)
      .execute[HttpResponse]
      .map(response => Html(response.body))
  }

  def markAsRead(
    message: UnreadMessage
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    http
      .post(url"${message.markAsReadUrl}")
      .execute[HttpResponse]

}
