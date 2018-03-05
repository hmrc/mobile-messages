package uk.gov.hmrc.mobilemessages.controllers.action

import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L200, L50}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http._
import utils.AuthStub._
import utils.BaseISpec

import scala.concurrent.ExecutionContext.Implicits.global

class AccountAccessControlISpec extends BaseISpec with Eventually  {

  implicit val hc = HeaderCarrier()

  val saUtr = SaUtr("1872796160")
  val nino = Nino("CS100700A")

  def authConnector(response : HttpResponse, cl: ConfidenceLevel = L200) = new AccountAccessControl {
  }

  "grantAccess" should {
    "error with unauthorised when account has low CL" in {
      authRecordExists(nino, L50)

      intercept[ForbiddenException] {
        await(AccountAccessControl.grantAccess())
      }
    }

    "find NINO only account when CL is correct" in {
      authRecordExists(nino, L200)
      await(AccountAccessControl.grantAccess())
    }

    "fail to return authority when no NINO exists" in {
      authRecordExistsWithoutNino

      intercept[UnauthorizedException] {
        await(AccountAccessControl.grantAccess())
      }
    }

    "fail if no auth/authority returns unauthorised" in {
      unauthorised

      intercept[Upstream4xxResponse] {
        await(AccountAccessControl.grantAccess())
      }
    }
  }
}

