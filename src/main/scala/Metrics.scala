package anthology

import zio._
import zio.metrics._

object AppMetrics {
  
  val openaiRequestsTotal = Metric.counter("openai_requests_total")
    .tagged(MetricLabel("endpoint", "chat_completions"))
    
  val openaiRequestDuration = Metric.histogram("openai_request_duration_seconds", 
    MetricKeyType.Histogram.Boundaries(Chunk(0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0)))
    .tagged(MetricLabel("endpoint", "chat_completions"))
    
  val storiesProcessedTotal = Metric.counter("stories_processed_total")
  
  val storyProcessingDuration = Metric.histogram("story_processing_duration_seconds", 
    MetricKeyType.Histogram.Boundaries(Chunk(0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0)))
  
  val errorsTotal = Metric.counter("errors_total")
  
  def trackOpenAIRequest[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
    effect
      .timed
      .tap { case (duration, _) =>
        openaiRequestDuration.update(duration.toMillis.toDouble / 1000.0) *>
        openaiRequestsTotal.increment
      }
      .map(_._2)
      .tapError(_ => errorsTotal.tagged("type", "openai").increment)
      
  def trackStoryProcessing[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
    effect
      .timed
      .tap { case (duration, _) =>
        storyProcessingDuration.update(duration.toMillis.toDouble / 1000.0) *>
        storiesProcessedTotal.increment
      }
      .map(_._2)
      .tapError(_ => errorsTotal.tagged("type", "story_processing").increment)
}