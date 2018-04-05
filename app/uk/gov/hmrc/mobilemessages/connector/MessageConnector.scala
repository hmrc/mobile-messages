/*
 * Copyright 2018 HM Revenue & Customs
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
import javax.inject.{Inject, Named}

import org.apache.commons.codec.CharEncoding.UTF_8
import org.joda.time.DateTime
import play.twirl.api.Html
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.mobilemessages.config.WSHttp
import uk.gov.hmrc.mobilemessages.connector.model.{UpstreamMessageHeadersResponse, UpstreamMessageResponse}
import uk.gov.hmrc.mobilemessages.controllers.action.Authority
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageHeader, MessageId, UnreadMessage}
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

class MessageConnector @Inject()(@Named("messages") val messageBaseUrl: String,
                                     val http: WSHttp) extends SessionCookieEncryptionSupport with HttpErrorFunctions {
  implicit val now: DateTime = DateTimeUtils.now

  def exception(key: String) = throw new Exception(s"Failed to find $key")

  def messages()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] = {
    http.GET[UpstreamMessageHeadersResponse](s"$messageBaseUrl/messages").
      map(messageHeaders => messageHeaders.items)
  }

  def getMessageBy(id: MessageId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Message] = {
    http.GET[UpstreamMessageResponse](s"$messageBaseUrl/messages/${id.value}").
      map(upstreamResponseMessage =>
        upstreamResponseMessage.toMessageUsing(upstreamResponseMessage.renderUrl.service))
  }

  def render(message: Message, hc: HeaderCarrier)(implicit ec: ExecutionContext, auth: Option[Authority]): Future[Html] = {
    val authToken: Authorization = hc.authorization.getOrElse(throw new IllegalArgumentException("Failed to find auth header!"))
    val userId = auth.getOrElse(throw new IllegalArgumentException("Failed to find the user!"))

    val keys = Seq(SessionKeys.authToken -> encode(authToken.value, UTF_8), SessionKeys.userId -> userId.authId)

    val session: (String, String) = withSession(keys: _ *)
    implicit val updatedHc: HeaderCarrier = hc.withExtraHeaders(session)

    http.GET[HttpResponse](message.renderUrl).map(response => Html(response.body))
  }

  def markAsRead(message: UnreadMessage)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.POSTEmpty[HttpResponse](message.markAsReadUrl)
  }
}