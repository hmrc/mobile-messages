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

import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mobilemessages.config.WSHttp
import uk.gov.hmrc.mobilemessages.domain.MessageHeader
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

trait EntityResolverConnector extends SessionCookieEncryptionSupport {

  def http: HttpGet

  val entityResolverBaseUrl: String

  private val returnReadAndUnreadMessages = "Both"

  def messages(utr: SaUtr)
              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] = {
    http.GET[Seq[MessageHeader]](s"$entityResolverBaseUrl/message/sa/$utr?read=$returnReadAndUnreadMessages")
  }
}

object EntityResolverConnector extends EntityResolverConnector with ServicesConfig {
  override def http = WSHttp

  override lazy val entityResolverBaseUrl: String = baseUrl("entity-resolver")
}

