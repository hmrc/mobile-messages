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

import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

trait MessageConnector {

  import uk.gov.hmrc.domain.SaUtr
  import uk.gov.hmrc.mobilemessages.domain.MessageHeader
  import uk.gov.hmrc.play.http._

  def http: HttpGet with HttpPost

  val messageBaseUrl: String

  def messages(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Seq[MessageHeader]] =
    http.GET[Seq[MessageHeader]](s"$messageBaseUrl/message/sa/$utr?read=Both") //TODO confirm querystring is needed


  //I believe these will be passed in the readTimeUrl
//  def message(utr: String, messageId: String)(implicit hc: HeaderCarrier): Future[Message] =
//    message(s"sa/$utr/$messageId")
//
//  private [connector] def message(url: String)(implicit hc: HeaderCarrier): Future[Message] =
//    http.GET[Message](s"$messageBaseUrl/message/$url")
}

object MessageConnector extends MessageConnector with ServicesConfig {
  import uk.gov.hmrc.mobilemessages.config.WSHttp

  override def http = WSHttp

  override lazy val messageBaseUrl: String = baseUrl("message")
}
