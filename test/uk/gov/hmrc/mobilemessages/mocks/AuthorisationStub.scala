/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpReads, Upstream4xxResponse}
import uk.gov.hmrc.mobilemessages.controllers.auth.AuthorityRecord

import scala.concurrent.{ExecutionContext, Future}

trait AuthorisationStub extends MockFactory {

  type GrantAccess = Option[String] ~ ConfidenceLevel

  def stubAuthorisationGrantAccess(response: GrantAccess)(implicit authConnector: AuthConnector): Unit =
    (authConnector.authorise(_: Predicate, _: Retrieval[GrantAccess])(_:HeaderCarrier, _: ExecutionContext)).
      expects(*,*,*,*).returning(Future successful response)

  def stubAuthorisationUnauthorised()(implicit authConnector: AuthConnector): Unit =
    (authConnector.authorise(_: Predicate, _: Retrieval[GrantAccess])(_ :HeaderCarrier, _: ExecutionContext)).
      expects(*,*,*,*).returning(Future failed Upstream4xxResponse("401", 401, 401))

  def stubAuthoritySuccess(response: AuthorityRecord)(implicit http: CoreGet): Unit =
    (http.GET(_: String)(_: HttpReads[AuthorityRecord], _: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *).returns(Future successful response)

  def stubAuthorityFailure()(implicit http:CoreGet): Unit =
    (http.GET(_: String)(_: HttpReads[AuthorityRecord], _: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *).returns(Future failed Upstream4xxResponse("401", 401, 401))
}
