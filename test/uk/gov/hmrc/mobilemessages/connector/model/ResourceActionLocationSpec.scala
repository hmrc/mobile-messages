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

package uk.gov.hmrc.mobilemessages.connector.model

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

class ResourceActionLocationSpec extends UnitSpec {

  "to Url method" should {

    "append path to url correctly" in {
      val servicesConfigMock = MockitoSugar.mock[ServicesConfig]
      when(servicesConfigMock.baseUrl("service")).thenReturn("http://localhost:3030")

      val expectedUrl = "http://localhost:3030/path/to/resource"

      ResourceActionLocation("service", "/path/to/resource").toUrlUsing(servicesConfigMock.baseUrl("service")) shouldBe expectedUrl
      ResourceActionLocation("service", "path/to/resource").toUrlUsing(servicesConfigMock.baseUrl("service")) shouldBe expectedUrl

      when(servicesConfigMock.baseUrl("service")).thenReturn("http://localhost:3030/")

      ResourceActionLocation("service", "/path/to/resource").toUrlUsing(servicesConfigMock.baseUrl("service")) shouldBe expectedUrl
      ResourceActionLocation("service", "path/to/resource").toUrlUsing(servicesConfigMock.baseUrl("service")) shouldBe expectedUrl
    }
  }
}
