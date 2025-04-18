package anthology

import zio.json._

case class CareerStory(
  id: String,
  title: String,
  situation: String,
  action: Option[String],
  result: Option[String]
)

object CareerStory {
  implicit val decoder: JsonDecoder[CareerStory] = DeriveJsonDecoder.gen[CareerStory]
  implicit val encoder: JsonEncoder[CareerStory] = DeriveJsonEncoder.gen[CareerStory]
}

case class CareerStories(stories: List[CareerStory])

object CareerStories {
  implicit val decoder: JsonDecoder[CareerStories] = DeriveJsonDecoder.gen[CareerStories]
  implicit val encoder: JsonEncoder[CareerStories] = DeriveJsonEncoder.gen[CareerStories]
} 