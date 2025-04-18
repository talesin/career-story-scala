package anthology

import zio._
import com.openai.client.okhttp.OpenAIOkHttpClient
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

class OpenAIService(apiKey: String) {
  private val client = OpenAIOkHttpClient.builder()
    .apiKey(apiKey)
    .build()

  private val analyzeStoryFunction = FunctionDefinition.builder()
    .name("analyze_story")
    .description("Analyze a career story and provide structured feedback")
    .parameters(
      FunctionParameters.builder()
        .putAdditionalProperty("type", JsonValue.from("object"))
        .putAdditionalProperty(
          "properties",
          JsonValue.from(Map(
            "questions" -> Map(
              "type" -> "array",
              "items" -> Map("type" -> "string"),
              "description" -> "Questions to help improve the story"
            ).asJava,
            "tips" -> Map(
              "type" -> "array",
              "items" -> Map("type" -> "string"),
              "description" -> "Tips for improving the story"
            ).asJava,
            "versions" -> Map(
              "type" -> "array",
              "items" -> Map(
                "type" -> "object",
                "properties" -> Map(
                  "situation" -> Map(
                    "type" -> "string",
                    "description" -> "The situation part of the story"
                  ).asJava,
                  "action" -> Map(
                    "type" -> "string",
                    "description" -> "The action part of the story"
                  ).asJava,
                  "result" -> Map(
                    "type" -> "string",
                    "description" -> "The result part of the story"
                  ).asJava
                ).asJava,
                "required" -> List("situation", "action", "result").asJava
              ).asJava,
              "description" -> "Three different versions of the story in SAR format"
            ).asJava
          ).asJava)
        )
        .putAdditionalProperty("required", JsonValue.from(List().asJava))
        .build()
    )
    .build()

  private def createChatParams(systemMessage: String, prompt: String): ChatCompletionCreateParams = {
    ChatCompletionCreateParams.builder()
      .model(ChatModel.GPT_4O_MINI)
      .addSystemMessage(systemMessage)
      .addUserMessage(prompt)
      .addTool(
        com.openai.models.chat.completions.ChatCompletionTool.builder()
          .function(analyzeStoryFunction)
          .build()
      )
      .build()
  }

  private def parseResponse(response: String): IO[RuntimeException, StoryResponse] = {
    ZIO.fromEither(response.fromJson[StoryResponse])
      .tapError(e => ZIO.debug(s"Failed to parse response: $response"))
      .mapError(e => new RuntimeException(s"Failed to parse response: $e"))
  }

  def completeChat(systemMessage: String, prompt: String): IO[Throwable, StoryResponse] = {
    for {
      params <- ZIO.succeed(createChatParams(systemMessage, prompt))
      response <- ZIO.attempt(client.chat().completions().create(params))
      toolCall <- ZIO.attempt(response.choices().get(0).message().toolCalls().get().get(0))
        .orElseFail(new RuntimeException("No tool calls in response"))
      function <- ZIO.succeed(toolCall.function())
      _ <- ZIO.fail(new RuntimeException(s"Unexpected function call: ${function.name()}"))
        .when(function.name() != "analyze_story")
      result <- parseResponse(function.arguments().toString())
    } yield result
  }
}

object OpenAIService {
  def make(apiKey: String): UIO[OpenAIService] = 
    ZIO.succeed(new OpenAIService(apiKey))
} 