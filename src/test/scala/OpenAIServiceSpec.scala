package anthology

import zio._
import zio.test.{ZIOSpecDefault, assertTrue, test, suite}
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessage
import com.openai.models.chat.completions.ChatCompletionMessageToolCall
import com.openai.models.chat.completions.ChatCompletionMessageToolCall.Function
import scala.jdk.CollectionConverters._

object OpenAIServiceSpec extends ZIOSpecDefault {
  private val mockClientService = new OpenAIClientService {
    def createChat(params: ChatCompletionCreateParams): Task[ChatCompletion] = {
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
          .toolCalls(List(functionCall).asJava)
          .build()

        ChatCompletion.builder()
          .choices(List(
            ChatCompletion.Choice.builder()
              .message(message)
              .build()
          ).asJava)
          .build()
      }
    }
  }

  def spec = suite("OpenAIService")(
    test("completeChat returns structured response") {
      for {
        service <- ZIO.succeed(new OpenAIService(mockClientService))
        response <- service.completeChat(
          systemMessage = "test system",
          prompt = "test prompt"
        )
      } yield assertTrue(
        response.questions.contains(List("What was the impact?")),
        response.tips.contains(List("Add more details")),
        response.versions.exists(_.head.situation == "test situation")
      )
    }
  )
} 