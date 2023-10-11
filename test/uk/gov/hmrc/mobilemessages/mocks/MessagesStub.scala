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

package uk.gov.hmrc.mobilemessages.mocks

import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilemessages.config.WSHttpImpl
import uk.gov.hmrc.mobilemessages.connector.model.{UpstreamMessageHeadersResponse, UpstreamMessageResponse}
import uk.gov.hmrc.mobilemessages.domain.MessageCountResponse

import scala.concurrent.{ExecutionContext, Future}

trait MessagesStub extends MockFactory {

  def messagesGetSuccess(response: UpstreamMessageHeadersResponse)(implicit http: WSHttpImpl): Unit =
    (http
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[UpstreamMessageHeadersResponse],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(Future successful response)

  def messagesGetFailure(response: Exception)(implicit http: WSHttpImpl): Unit =
    (http
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[UpstreamMessageHeadersResponse],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(Future failed response)

  def messageByGetSuccess(response: UpstreamMessageResponse)(implicit http: WSHttpImpl): Unit =
    (http
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[UpstreamMessageResponse],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(Future successful response)

  def messageByGetFailure(response: Exception)(implicit http: WSHttpImpl): Unit =
    (http
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[UpstreamMessageResponse],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(Future failed response)

  def renderGetSuccess(response: Future[HttpResponse])(implicit http: WSHttpImpl): Unit =
    (http
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[HttpResponse],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(response)

  def renderGetFailure(response: Exception)(implicit http: WSHttpImpl): Unit =
    (http
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[HttpResponse],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(Future failed response)

  def markAsReadPostSuccess(response: Future[HttpResponse])(implicit http: WSHttpImpl): Unit =
    (http
      .POSTEmpty(_: String, _: Seq[(String, String)])(_: HttpReads[HttpResponse],
                                                      _: HeaderCarrier,
                                                      _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(response)

  def markAsReadPostFailure(response: Exception)(implicit http: WSHttpImpl): Unit =
    (http
      .POSTEmpty(_: String, _: Seq[(String, String)])(_: HttpReads[HttpResponse],
                                                      _: HeaderCarrier,
                                                      _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(Future failed response)

  def messageCountGetSuccess(response: MessageCountResponse)(implicit http: WSHttpImpl): Unit =
    (http
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[MessageCountResponse],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(Future successful response)

  def messageCountGetFailure(response: Exception)(implicit http: WSHttpImpl): Unit =
    (http
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[MessageCountResponse],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(Future failed response)
}
