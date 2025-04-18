import zio._
import zio.json._
import anthology.{CareerStories, OpenAIService, StoryPrompts, OpenAIClientService}

object Main extends ZIOAppDefault {
  val apiKeyLayer: ZLayer[Any, RuntimeException, String] = 
    ZLayer.fromZIO(
      System.env("OPENAI_API_KEY")
        .someOrFail(new RuntimeException("OPENAI_API_KEY environment variable not set"))
    )

  def run: ZIO[Any, Throwable, Unit] = {
    for {
      // Read and parse career stories
      storiesJson <- ZIO.readFile("src/test/career-stories.json")
      stories <- ZIO.fromEither(storiesJson.fromJson[CareerStories])
        .mapError(e => new RuntimeException(s"Failed to parse career stories JSON: $e"))
      
      // Read and parse story prompts
      promptsJson <- ZIO.readFile("src/test/story-prompts.json")
      prompts <- ZIO.fromEither(promptsJson.fromJson[StoryPrompts])
        .mapError(e => new RuntimeException(s"Failed to parse prompts JSON: $e"))
      
      // Get the story analysis prompt
      storyPrompt <- ZIO.fromOption(prompts.prompts.get("story_analysis"))
        .orElseFail(new RuntimeException("Story analysis prompt not found"))
      
      // Process each story
      _ <- ZIO.foreach(stories.stories) { story =>
        for {
          // Create a prompt for the story
          prompt <- ZIO.succeed(storyPrompt.user.replace("${story}", story.situation))
          
          // Get completion from OpenAI
          openAiService <- ZIO.service[OpenAIService]
          analysis <- openAiService.completeChat(
            systemMessage = storyPrompt.system,
            prompt = prompt
          )
          
          // Print results
          _ <- ZIO.succeed {
            println(s"\nStory: ${story.title} (${story.id})")
            println(s"Analysis: $analysis")
          }
        } yield ()
      }
    } yield ()
  }.provide(
    apiKeyLayer,
    OpenAIClientService.live,
    OpenAIService.live
  )
}