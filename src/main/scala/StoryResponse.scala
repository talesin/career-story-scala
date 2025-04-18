package anthology

import zio.json._

case class StoryVersion(
  situation: String,
  action: String,
  result: String
)

object StoryVersion {
  implicit val decoder: JsonDecoder[StoryVersion] = DeriveJsonDecoder.gen[StoryVersion]
  implicit val encoder: JsonEncoder[StoryVersion] = DeriveJsonEncoder.gen[StoryVersion]
}

case class StoryResponse(
  questions: Option[List[String]] = None,
  tips: Option[List[String]] = None,
  versions: Option[List[StoryVersion]] = None
)

object StoryResponse {
  implicit val decoder: JsonDecoder[StoryResponse] = DeriveJsonDecoder.gen[StoryResponse]
  implicit val encoder: JsonEncoder[StoryResponse] = DeriveJsonEncoder.gen[StoryResponse]
} 