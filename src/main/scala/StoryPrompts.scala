package anthology

import zio.json._

case class StoryPrompt(system: String, user: String)

object StoryPrompt {
  implicit val decoder: JsonDecoder[StoryPrompt] = DeriveJsonDecoder.gen[StoryPrompt]
  implicit val encoder: JsonEncoder[StoryPrompt] = DeriveJsonEncoder.gen[StoryPrompt]
}

case class StoryPrompts(prompts: Map[String, StoryPrompt])

object StoryPrompts {
  implicit val decoder: JsonDecoder[StoryPrompts] = DeriveJsonDecoder.gen[StoryPrompts]
  implicit val encoder: JsonEncoder[StoryPrompts] = DeriveJsonEncoder.gen[StoryPrompts]
} 