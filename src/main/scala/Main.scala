import zio._
import zio.json._
import java.io.IOException
import anthology.{AppConfig, AppError, AppMetrics, CareerStories, CareerStory, OpenAIService, StoryPrompts, StoryPrompt, StoryResponse, OpenAIClientService}

object Main extends ZIOAppDefault {

  private def loadStories(path: String): ZIO[Any, AppError, CareerStories] =
    ZIO.readFile(path)
      .mapError(e => AppError.FileError(s"Failed to read stories file: $path", Some(e)))
      .flatMap { json =>
        ZIO.fromEither(json.fromJson[CareerStories])
          .mapError(e => AppError.JsonParsingError(s"Failed to parse career stories JSON", json))
      }

  private def loadPrompts(path: String): ZIO[Any, AppError, StoryPrompts] =
    ZIO.readFile(path)
      .mapError(e => AppError.FileError(s"Failed to read prompts file: $path", Some(e)))
      .flatMap { json =>
        ZIO.fromEither(json.fromJson[StoryPrompts])
          .mapError(e => AppError.JsonParsingError(s"Failed to parse prompts JSON", json))
      }

  private def printStoryAnalysis(story: CareerStory, response: StoryResponse): ZIO[Any, IOException, Unit] =
    Console.printLine(s"\nStory: ${story.title} (${story.id})") *>
    Console.printLine("Analysis:") *>
    Console.printLine("----------") *>
    Console.printLine("Questions:") *>
    ZIO.foreach(response.questions.getOrElse(List.empty)) { q =>
      Console.printLine(s"- $q")
    } *>
    Console.printLine("Tips:") *>
    ZIO.foreach(response.tips.getOrElse(List.empty)) { t =>
      Console.printLine(s"- $t")
    } *>
    Console.printLine("Versions:") *>
    ZIO.foreach(response.versions.getOrElse(List.empty)) { version =>
      Console.printLine(s"  Situation: ${version.situation}") *>
      Console.printLine(s"  Action: ${version.action}") *>
      Console.printLine(s"  Result: ${version.result}")
    } *>
    Console.printLine("----------")

  private def processStory(story: CareerStory, storyPrompt: StoryPrompt): ZIO[OpenAIService, AppError, Unit] = {
    val effect = for {
      _ <- ZIO.logInfo(s"Processing story: ${story.title}")
      prompt = storyPrompt.user.replace("${story}", story.situation)
      openAiService <- ZIO.service[OpenAIService]
      response <- openAiService.completeChat(
        systemMessage = storyPrompt.system,
        prompt = prompt
      )
      _ <- printStoryAnalysis(story, response).mapError(e => AppError.FileError(s"Failed to print story analysis", Some(e)))
      _ <- ZIO.logInfo(s"Completed processing story: ${story.title}")
    } yield ()
    
    AppMetrics.trackStoryProcessing(effect)
  }

  def run: ZIO[Any, Throwable, Unit] = {
    val program = for {
      config <- ZIO.service[AppConfig]
      
      // Load data files
      _ <- ZIO.logInfo("Loading career stories and prompts")
      stories <- loadStories(config.storiesPath)
      prompts <- loadPrompts(config.promptsPath)
      
      // Get the story analysis prompt
      storyPrompt <- ZIO.fromOption(prompts.prompts.get("story_analysis"))
        .orElseFail(AppError.ValidationError("Story analysis prompt not found"))
      
      // Process each story
      _ <- ZIO.logInfo(s"Processing ${stories.stories.length} stories")
      _ <- ZIO.foreach(stories.stories) { story =>
        processStory(story, storyPrompt)
      }
      
      _ <- ZIO.logInfo("All stories processed successfully")
    } yield ()

    program.provide(
      AppConfig.live,
      OpenAIClientService.live,
      OpenAIService.live
    )
  }
}