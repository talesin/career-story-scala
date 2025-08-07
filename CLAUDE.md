# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

```bash
sbt compile    # Compile the project
sbt run        # Run the main application  
sbt console    # Start Scala 3 REPL
sbt test       # Run all tests
```

## Architecture

This is a Scala 3 application that analyzes career stories using OpenAI's API with ZIO for functional programming. The application follows ZIO's service pattern with dependency injection through layers and includes comprehensive error handling, retry policies, and observability.

### Service Layers
1. **AppConfig.live**: Configuration service that extracts settings from environment variables
2. **OpenAIClientService.live**: HTTP client wrapper with retry policies, timeouts, and proper error mapping
3. **OpenAIService.live**: Business logic for structured function calling with metrics and logging

### Core Flow
Main.scala orchestrates: Configuration loading → JSON data loading → OpenAI analysis via function calling → formatted console output

### Key Components
- **Error Handling**: Custom ADT-based errors (`AppError`) replacing generic exceptions
- **Function Calling**: Uses OpenAI's `analyze_story` function to ensure structured `StoryResponse` format
- **ZIO Layers**: Services composed via `provide()` for dependency injection
- **JSON Handling**: zio-json for serialization of case classes (CareerStory, StoryResponse, etc.)
- **Retry & Timeouts**: Exponential backoff retry with configurable timeouts for resilience
- **Observability**: Structured logging and metrics tracking for request duration and error rates
- **Testing**: Comprehensive test coverage with mock services for error scenarios

### Error Types
- `ConfigurationError`: Missing or invalid configuration
- `FileError`: File system operation failures  
- `JsonParsingError`: JSON serialization/deserialization issues
- `OpenAIError`: OpenAI API specific errors
- `ValidationError`: Data validation failures
- `NetworkError`: Network connectivity issues

## Configuration

- Requires `OPENAI_API_KEY` environment variable (can use `.envrc` with direnv)
- Test data in `src/test/`: career stories and prompts as JSON files
- Configurable timeout (defaults to 30 seconds) for OpenAI API calls

## Development References

- `doc/zio-guide.md`: Comprehensive ZIO reference with patterns, error handling, layers, concurrency, and testing
- `doc/zio-direct-guide.md`: ZIO Direct syntax guide for cleaner code using `defer` blocks and `.run` instead of for-comprehensions