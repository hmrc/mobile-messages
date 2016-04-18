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


