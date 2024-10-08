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

import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.UpstreamErrorResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future

trait AuthorisationStub {

  type GrantAccess = Option[String] ~ Option[String]

  def stubAuthorisationGrantAccess(response: GrantAccess)(implicit authConnector: AuthConnector): Unit =
    when(authConnector.authorise[GrantAccess](any(), any())(any(), any())).thenReturn(Future successful response)

  def stubAuthorisationUnauthorised()(implicit authConnector: AuthConnector): Unit =
    when(authConnector.authorise[GrantAccess](any(), any())(any(), any()))
      .thenReturn(Future failed UpstreamErrorResponse("401", 401, 401))

}
