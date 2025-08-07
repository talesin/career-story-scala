package anthology

import zio._
import zio.json._

case class AppConfig(
  openAIApiKey: String,
  storiesPath: String = "src/test/career-stories.json",
  promptsPath: String = "src/test/story-prompts.json",
  timeout: Duration = 30.seconds
)

object AppConfig {
  implicit val codec: JsonCodec[AppConfig] = DeriveJsonCodec.gen[AppConfig]
  
  val live: ZLayer[Any, AppError.ConfigurationError, AppConfig] = 
    ZLayer.fromZIO(
      System.env("OPENAI_API_KEY")
        .someOrFail(AppError.ConfigurationError("OPENAI_API_KEY environment variable not set"))
        .map(apiKey => AppConfig(openAIApiKey = apiKey))
        .mapError(_ => AppError.ConfigurationError("OPENAI_API_KEY environment variable not set"))
    )
}