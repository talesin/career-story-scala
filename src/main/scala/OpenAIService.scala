package anthology

import zio._
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.ChatModel
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.core.JsonValue
import com.openai.core.JsonObject
import com.fasterxml.jackson.core.JsonProcessingException
import com.openai.core.ObjectMappers.jsonMapper
import scala.collection.JavaConverters._
import zio.json._

class OpenAIService(client: OpenAIClientService) {
  private def jmap[K, V](entries: (K, V)*): java.util.Map[K, V] = 
    Map(entries*).asJava

  private val analyzeStoryFunction = FunctionDefinition.builder()
    .name("analyze_story")
    .description("Analyze a career story and provide structured feedback")
    .parameters(
      FunctionParameters.builder()
        .putAdditionalProperty("type", JsonValue.from("object"))
        .putAdditionalProperty(
          "properties",
          JsonValue.from(jmap(
            "questions" -> jmap(
              "type" -> "array",
              "items" -> jmap("type" -> "string"),
              "description" -> "Questions to help improve the story"
            ),
            "tips" -> jmap(
              "type" -> "array",
              "items" -> jmap("type" -> "string"),
              "description" -> "Tips for improving the story"
            ),
            "versions" -> jmap(
              "type" -> "array",
              "items" -> jmap(
                "type" -> "object",
                "properties" -> jmap(
                  "situation" -> jmap(
                    "type" -> "string",
                    "description" -> "The situation part of the story"
                  ),
                  "action" -> jmap(
                    "type" -> "string",
                    "description" -> "The action part of the story"
                  ),
                  "result" -> jmap(
                    "type" -> "string",
                    "description" -> "The result part of the story"
                  )
                ),
                "required" -> List("situation", "action", "result").asJava
              ),
              "description" -> "Three different versions of the story in SAR format"
            )
          ))
        )
        .putAdditionalProperty("required", JsonValue.from(List().asJava))
        .build()
    )

  private def createChatParams(systemMessage: String, prompt: String): ChatCompletionCreateParams = {
    ChatCompletionCreateParams.builder()
      .model(ChatModel.GPT_4O_MINI)
      .addSystemMessage(systemMessage)
      .addUserMessage(prompt)
      .addTool(
        com.openai.models.chat.completions.ChatCompletionTool.builder()
          .function(analyzeStoryFunction.build())
          .build()
      )
      .build()
  }

  private def parseResponse(response: String): ZIO[Any, AppError, StoryResponse] = {
    ZIO.fromEither(response.fromJson[StoryResponse])
      .tapError(e => ZIO.logError(s"Failed to parse OpenAI response: $e"))
      .mapError(e => AppError.JsonParsingError(s"Failed to parse OpenAI response: $e", response))
  }

  def completeChat(systemMessage: String, prompt: String): ZIO[Any, AppError, StoryResponse] = {
    val effect = for {
      _ <- ZIO.logDebug(s"Creating OpenAI chat completion request")
      params <- ZIO.succeed(createChatParams(systemMessage, prompt))
      response <- client.createChat(params)
      toolCall <- ZIO.attempt(response.choices().get(0).message().toolCalls().get().get(0))
        .mapError(e => AppError.OpenAIError("No tool calls found in OpenAI response", Some(e)))
      function <- ZIO.succeed(toolCall.function())
      _ <- ZIO.when(function.name() != "analyze_story") {
        ZIO.fail(AppError.ValidationError(s"Unexpected function call: ${function.name()}"))
      }
      _ <- ZIO.logDebug(s"Received function call: ${function.name()}")
      result <- parseResponse(function.arguments().toString())
      _ <- ZIO.logDebug("Successfully parsed OpenAI response")
    } yield result
    
    AppMetrics.trackOpenAIRequest(effect)
  }
}

object OpenAIService {
  val live: URLayer[OpenAIClientService, OpenAIService] = 
    ZLayer.fromFunction(new OpenAIService(_))
} 