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

package uk.gov.hmrc.mobilemessages.utils

import com.ning.http.util.Base64
import uk.gov.hmrc.crypto.{AesCrypto, Encrypter, PlainText}

class UnitTestCrypto extends AesCrypto {
  override protected val encryptionKey: String = "hwdODU8hulPkolIryPRkVW=="
}

object EncryptionUtils {
  def encrypted(value: String, encrypter: Encrypter = new UnitTestCrypto) = {
    Base64.encode(encrypter.encrypt(PlainText(value)).value.getBytes)
  }
}
