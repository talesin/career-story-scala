package anthology

import zio.json._

sealed trait AppError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object AppError {
  final case class ConfigurationError(message: String) extends AppError
  final case class FileError(message: String, cause: Option[Throwable] = None) extends AppError
  final case class JsonParsingError(message: String, jsonString: String) extends AppError
  final case class OpenAIError(message: String, cause: Option[Throwable] = None) extends AppError
  final case class ValidationError(message: String) extends AppError
  final case class NetworkError(message: String, cause: Option[Throwable] = None) extends AppError
}