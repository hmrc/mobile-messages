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

trait StubApplicationConfiguration {

  val config: Map[String, Any] = Map[String, Any](
    "auditing.enabled" -> false,
    "microservice.services.datastream.host" -> "localhost",
    "microservice.services.datastream.port" -> "1234",
    "microservice.services.datastream.enabled" -> false,
    "microservice.services.service-locator.enabled" -> false,
    "microservice.services.service-locator.host" -> "localhost",
    "microservice.services.service-locator.port" -> "9602",
    "appName" -> "mobile-messages",
    "microservice.services.auth.host" -> "localhost",
    "microservice.services.auth.port" -> "8500",
    "microservice.services.ntc.host" -> "localhost",
    "microservice.services.ntc.port" -> "4567",
    "microservice.services.sa-message-renderer.host" -> "localhost",
    "microservice.services.sa-message-renderer.port" -> "8089",
    "microservice.services.ats-message-renderer.host" -> "localhost",
    "microservice.services.ats-message-renderer.port" -> "8089",
    "microservice.services.secure-message-renderer.host" -> "localhost",
    "microservice.services.secure-message-renderer.port" -> "8089",
    "microservice.services.test-renderer-service.host" -> "localhost",
    "microservice.services.test-renderer-service.port" -> "9999",
    "microservice.services.message.host" -> "localhost",
    "microservice.services.message.port" -> "8910",
    "router.regex" -> ".*",
    "router.prefix" -> "/sandbox",
    "router.regex" -> "X-MOBILE-USER-ID",
    "application.secret" -> "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G",
    "cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW=="
  )
}
