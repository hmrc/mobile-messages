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
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilemessages.domain.Accounts

class AccountsSpec extends AnyWordSpec with Matchers {

  "Accounts JSON format" should {

    "serialize Accounts with both Nino and SaUtr" in {
      val accounts = Accounts(Some(Nino("AA123456A")), Some(SaUtr("1234567890")))
      val json = Json.toJson(accounts)

      (json \ "nino").as[String]  shouldBe "AA123456A"
      (json \ "saUtr").as[String] shouldBe "1234567890"
    }

    "serialize Accounts with only Nino" in {
      val accounts = Accounts(Some(Nino("AA123456A")), None)
      val json = Json.toJson(accounts)

      (json \ "nino").as[String] shouldBe "AA123456A"
      (json \ "saUtr").toOption  shouldBe empty
    }

    "serialize Accounts with only SaUtr" in {
      val accounts = Accounts(None, Some(SaUtr("1234567890")))
      val json = Json.toJson(accounts)

      (json \ "nino").toOption    shouldBe empty
      (json \ "saUtr").as[String] shouldBe "1234567890"
    }

    "serialize Accounts with neither Nino nor SaUtr" in {
      val accounts = Accounts(None, None)
      val json = Json.toJson(accounts)

      (json \ "nino").toOption  shouldBe empty
      (json \ "saUtr").toOption shouldBe empty
    }

    "deserialize JSON with both Nino and SaUtr" in {
      val json = Json.parse("""{"nino":"AA123456A","saUtr":"1234567890"}""")
      val result = json.as[Accounts]

      result shouldBe Accounts(Some(Nino("AA123456A")), Some(SaUtr("1234567890")))
    }

    "deserialize JSON with only Nino" in {
      val json = Json.parse("""{"nino":"AA123456A"}""")
      val result = json.as[Accounts]

      result shouldBe Accounts(Some(Nino("AA123456A")), None)
    }

    "deserialize JSON with only SaUtr" in {
      val json = Json.parse("""{"saUtr":"1234567890"}""")
      val result = json.as[Accounts]

      result shouldBe Accounts(None, Some(SaUtr("1234567890")))
    }

    "deserialize JSON with neither Nino nor SaUtr" in {
      val json = Json.parse("""{}""")
      val result = json.as[Accounts]

      result shouldBe Accounts(None, None)
    }
  }
}
