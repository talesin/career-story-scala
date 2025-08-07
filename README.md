# Career Story Analyzer

An AI-powered career story analysis tool built with Scala 3 and ZIO that helps improve career stories for job interviews and professional development.

## What it does

This application analyzes career stories using OpenAI's GPT model to provide feedback on whether they follow the effective **SAR (Situation, Action, Result)** format. For each story, it provides:

- **Questions** to help strengthen weak areas
- **Tips** for improving storytelling impact  
- **Three alternative versions** of each SAR section to choose from

The tool is designed for job seekers, career coaches, and professionals who want to craft compelling career narratives for interviews, performance reviews, or professional profiles.

## Features

- **Functional Programming**: Built with ZIO for composable, type-safe effects
- **Error Handling**: Custom error ADTs with comprehensive error recovery
- **Resilience**: Retry policies and timeouts for API calls
- **Observability**: Structured logging and metrics tracking
- **Testing**: Comprehensive test coverage with mock services

## Prerequisites

- Java 11 or higher
- sbt 1.10.x
- OpenAI API key

## Setup

1. **Set your OpenAI API key** as an environment variable:
   ```bash
   export OPENAI_API_KEY=your_openai_api_key_here
   ```

2. **Prepare your stories** in `src/test/career-stories.json`:
   ```json
   {
     "stories": [
       {
         "id": "story_1",
         "title": "Leadership Example",
         "situation": "Your career story text here..."
       }
     ]
   }
   ```

## Usage

```bash
# Compile the project
sbt compile

# Run the analyzer
sbt run

# Run tests
sbt test

# Start Scala REPL
sbt console
```

The application will:
1. Load career stories from JSON files
2. Send each story to OpenAI for analysis
3. Display structured feedback with questions, tips, and alternative versions
4. Track metrics and log progress

## Configuration

- **Stories**: Edit `src/test/career-stories.json` to add your career stories
- **Prompts**: Customize analysis prompts in `src/test/story-prompts.json`  
- **Timeout**: Default 30-second timeout for API calls (configurable via `AppConfig`)

## Architecture

The application follows functional programming principles with ZIO:

- **Service Layers**: Configuration, OpenAI client, and business logic services
- **Error Types**: Custom ADTs for different error scenarios (network, parsing, validation)
- **Metrics**: Request tracking and performance monitoring
- **Retry Logic**: Exponential backoff for resilient API calls

## Development

See `doc/zio-guide.md` and `doc/zio-direct-guide.md` for ZIO development patterns and best practices.
