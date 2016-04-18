package uk.gov.hmrc.mobilemessages.domain

import org.joda.time.{DateTime, LocalDate}

case class MessageHeader(id: String,
                         subject: String,
                         validFrom: LocalDate,
                         readTime: Option[DateTime],
                         readTimeUrl: String,
                         sentInError: Boolean)

object MessageHeader {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.{Json, Reads, _}
  import uk.gov.hmrc.play.controllers.RestFormats

  implicit val reads = {
    implicit val dateTimeForm = RestFormats.dateTimeFormats
    val read = Json.reads[MessageHeader]
    val legacyRead: Reads[MessageHeader] = (
      (__ \ "id").read[String] and
        (__ \ "subject").read[String] and
        (__ \ "validFrom").read[LocalDate] and
        (__ \ "readTime").readNullable[DateTime] and
        (__ \ "readTimeUrl").read[String] and
        (__ \ "detail" \ "linkType").read[String] and
        (__ \ "sentInError").read[Boolean]
      ) (convertFromLegacy _)
    read orElse legacyRead
  }

  private def convertFromLegacy(id: String,
                                subject: String,
                                validFrom: LocalDate,
                                readTime: Option[DateTime],
                                readTimeUrl: String,
                                linkType: String,
                                sentInError: Boolean) = new MessageHeader(id, subject, validFrom, readTime, readTimeUrl, sentInError)
}