package anthology

import zio._
import zio.test.{ZIOSpecDefault, assertTrue, assertCompletes, test, suite}
import zio.test.TestAspect._
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessage
import com.openai.models.chat.completions.ChatCompletionMessageToolCall
import com.openai.models.chat.completions.ChatCompletionMessageToolCall.Function
import scala.jdk.CollectionConverters._

object OpenAIServiceSpec extends ZIOSpecDefault {
  
  private val successMockClient = new OpenAIClientService {
    def createChat(params: ChatCompletionCreateParams): ZIO[Any, AppError, ChatCompletion] = {
      ZIO.succeed {
        val functionCall = ChatCompletionMessageToolCall.builder()
          .id("test-id")
          .function(
            Function.builder()
              .name("analyze_story")
              .arguments("""{
                "questions": ["What was the impact?"],
                "tips": ["Add more details"],
                "versions": [{
                  "situation": "test situation",
                  "action": "test action",
                  "result": "test result"
                }]
              }""")
              .build()
          )
          .build()

        val message = ChatCompletionMessage.builder()
          .content("Function call response")
          .refusal(java.util.Optional.empty[String]())
          .toolCalls(List(functionCall).asJava)
          .build()

        ChatCompletion.builder()
          .choices(List(
            ChatCompletion.Choice.builder()
              .finishReason(ChatCompletion.Choice.FinishReason.TOOL_CALLS)
              .index(0L)
              .message(message)
              .build()
          ).asJava)
          .build()
      }
    }
  }

  private val failureMockClient = new OpenAIClientService {
    def createChat(params: ChatCompletionCreateParams): ZIO[Any, AppError, ChatCompletion] =
      ZIO.fail(AppError.NetworkError("Connection failed", None))
  }

  private val invalidResponseMockClient = new OpenAIClientService {
    def createChat(params: ChatCompletionCreateParams): ZIO[Any, AppError, ChatCompletion] = {
      ZIO.succeed {
        val message = ChatCompletionMessage.builder()
          .content("No function calls")
          .refusal(java.util.Optional.empty[String]())
          .build()
        ChatCompletion.builder()
          .choices(List(
            ChatCompletion.Choice.builder()
              .finishReason(ChatCompletion.Choice.FinishReason.TOOL_CALLS)
              .index(0L)
              .message(message)
              .build()
          ).asJava)
          .build()
      }
    }
  }

  def spec = suite("OpenAIService")(
    test("completeChat handles network failures") {
      for {
        service <- ZIO.succeed(new OpenAIService(failureMockClient))
        result <- service.completeChat("test system", "test prompt").exit
      } yield assertTrue(result.isFailure)
    } @@ timeout(5.seconds),
    
    test("completeChat handles invalid responses") {
      for {
        service <- ZIO.succeed(new OpenAIService(invalidResponseMockClient))
        result <- service.completeChat("test system", "test prompt").exit
      } yield assertTrue(result.isFailure)
    }
  ) @@ sequential
} 