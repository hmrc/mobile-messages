/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.mobilemessages.controllers.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.mobilemessages.domain.{RenderMessageLocation, RenderMessageLocationImpl}

class RenderMessageLocationImplSpec extends AnyWordSpec with Matchers {

  val mockBaseUrl: String => String = {
    case "testService" => "https://test.service/"
    case other         => s"https://$other.default/"
  }

  val impl = new RenderMessageLocationImpl(mockBaseUrl)

  "RenderMessageLocationImpl.toUrl" should {
    "generate full URL based on service and partial URL" in {
      val location = RenderMessageLocation("testService", "/messages/123")

      val fullUrl: String = impl.toUrl(location)

      fullUrl shouldBe "https://test.service//messages/123"
    }

    "fallback for unknown service names" in {
      val location = RenderMessageLocation("unknownService", "/fallback")

      val fullUrl = impl.toUrl(location)

      fullUrl shouldBe "https://unknownService.default//fallback"
    }
  }

  "RenderMessageLocation JSON formats" should {
    "serialize and deserialize properly" in {
      val location = RenderMessageLocation("someService", "/message/abc")
      val json = Json.toJson(location)
      val parsed = json.as[RenderMessageLocation]

      parsed shouldBe location
    }
  }
}
