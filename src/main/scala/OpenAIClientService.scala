package anthology

import zio._
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletion

trait OpenAIClientService {
  def createChat(params: ChatCompletionCreateParams): Task[ChatCompletion]
}

object OpenAIClientService {
  val live: URLayer[String, OpenAIClientService] = 
    ZLayer.scoped {
      for {
        apiKey <- ZIO.service[String]
        client = OpenAIOkHttpClient.builder()
          .apiKey(apiKey)
          .build()
      } yield new OpenAIClientService {
        def createChat(params: ChatCompletionCreateParams): Task[ChatCompletion] =
          ZIO.attempt(client.chat().completions().create(params))
      }
    }
} 