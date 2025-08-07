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

This is a Scala 3 application that analyzes career stories using OpenAI's API with ZIO for functional programming. The application follows ZIO's service pattern with dependency injection through layers:

### Service Layers
1. **API Key Layer**: Extracts OpenAI API key from `OPENAI_API_KEY` environment variable
2. **OpenAIClientService.live**: HTTP client wrapper around OpenAI Java client
3. **OpenAIService.live**: Business logic for structured function calling

### Core Flow
Main.scala orchestrates: JSON data loading → OpenAI analysis via function calling → formatted output

### Key Components
- **Function Calling**: Uses OpenAI's `analyze_story` function to ensure structured `StoryResponse` format
- **ZIO Layers**: Services composed via `provide()` for dependency injection
- **JSON Handling**: zio-json for serialization of case classes (CareerStory, StoryResponse, etc.)

## Configuration

- Requires `OPENAI_API_KEY` environment variable (can use `.envrc` with direnv)
- Test data in `src/test/`: career stories and prompts as JSON files

## Development References

- `doc/zio-guide.md`: Comprehensive ZIO reference with patterns, error handling, layers, concurrency, and testing
- `doc/zio-direct-guide.md`: ZIO Direct syntax guide for cleaner code using `defer` blocks and `.run` instead of for-comprehensions