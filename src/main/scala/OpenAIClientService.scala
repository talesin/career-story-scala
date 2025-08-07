package anthology

import zio._
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletion

trait OpenAIClientService {
  def createChat(params: ChatCompletionCreateParams): ZIO[Any, AppError, ChatCompletion]
}

object OpenAIClientService {
  val live: URLayer[AppConfig, OpenAIClientService] = 
    ZLayer.scoped {
      for {
        config <- ZIO.service[AppConfig]
        client = OpenAIOkHttpClient.builder()
          .apiKey(config.openAIApiKey)
          .build()
      } yield new OpenAIClientService {
        def createChat(params: ChatCompletionCreateParams): ZIO[Any, AppError, ChatCompletion] =
          ZIO.attempt(client.chat().completions().create(params))
            .mapError { 
              case e: java.net.SocketTimeoutException => 
                AppError.NetworkError(s"OpenAI API call timed out after ${config.timeout}", Some(e))
              case e: Throwable => 
                AppError.OpenAIError(s"OpenAI API error: ${e.getMessage}", Some(e))
            }
            .timeoutFail(AppError.NetworkError(s"OpenAI API call timed out after ${config.timeout}"))(config.timeout)
            .retry(Schedule.exponential(1.second) && Schedule.recurs(3))
      }
    }
} 