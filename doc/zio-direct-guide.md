# ZIO Direct Reference Guide for AI Code Agents (Scala)

## Overview

ZIO Direct is a library that provides a direct-style syntax for writing ZIO effects, eliminating the need for explicit for-comprehensions and flatMap chains. It uses the `defer` macro to convert direct-style code to proper ZIO effects.

Key concepts:
- **defer**: Macro that converts direct-style code to ZIO effects
- **.run**: Awaits the result of a ZIO effect within a defer block
- **ZIO[R, E, A]**: Same as regular ZIO - describes an effect that requires environment `R`, might fail with error `E`, and succeeds with value `A`

---

## Setup

```scala
//> using dependency dev.zio::zio-direct:1.0.0-RC7

import zio.*
import zio.direct.*
```

---

## Type Aliases

```scala
// Same type aliases as regular ZIO
import zio._

// UIO[A] is ZIO[Any, Nothing, A]
type UIO[A] = ZIO[Any, Nothing, A]

// Task[A] is ZIO[Any, Throwable, A]
type Task[A] = ZIO[Any, Throwable, A]

// RIO[R, A] is ZIO[R, Throwable, A]
type RIO[R, A] = ZIO[R, Throwable, A]
```

---

## Basic ZIO Direct Usage

```scala
val program: UIO[Unit] = defer {
  Console.printLine("Hello, ZIO Direct!").run
  ZIO.sleep(1.second).run
}

// Compare with regular ZIO:
val regularProgram: UIO[Unit] =
  for {
    _ <- Console.printLine("Hello, ZIO!")
    _ <- ZIO.sleep(1.second)
  } yield ()
```

---

## Error Handling

```scala
val effect: IO[String, Int] = ZIO.fail("Boom!")

val recovered: UIO[Int] = defer {
  val result = effect.catchAll(_ => ZIO.succeed(42)).run
  result
}

// Or handle errors directly:
val handledDirectly: UIO[Int] = defer {
  try {
    effect.run
  } catch {
    case _: String => 42
  }
}
```

---

## Dependency Injection with ZLayer

```scala
trait Logger {
  def log(line: String): UIO[Unit]
}

object LoggerLive extends Logger {
  def log(line: String): UIO[Unit] = Console.printLine(s"[log] $line").orDie
}

val loggerLayer: ULayer[Logger] = ZLayer.succeed(LoggerLive)

def program: ZIO[Logger, Nothing, Unit] = defer {
  val logger = ZIO.service[Logger].run
  logger.log("Starting app").run
}

val runtime = defer {
  program.provideLayer(loggerLayer).run
}
```

---

## Fibers (Concurrency)

```scala
val fiberProgram = defer {
  val fiber = ZIO.succeed(println("Running")).fork.run
  fiber.join.run
}

// Multiple fibers:
val multipleFibers = defer {
  val fiber1 = ZIO.succeed(println("Task 1")).fork.run
  val fiber2 = ZIO.succeed(println("Task 2")).fork.run
  
  fiber1.join.run
  fiber2.join.run
}
```

---

## Streams

```scala
import zio.stream._

val streamProgram = defer {
  val stream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)
  val sum = stream.runFold(0)(_ + _).run
  sum
}

// Processing streams:
val streamProcessing = defer {
  val processed = ZStream(1, 2, 3, 4, 5)
    .filter(_ % 2 == 0)
    .map(_ * 2)
    .runCollect
    .run
  processed
}
```

---

## Resource Safety

```scala
val managed: ZIO[Scope, Throwable, Unit] = ZIO.scoped {
  defer {
    val resource = ZIO.acquireReleaseWith(
      acquire = ZIO.succeed(println("open"))
    )(_ => ZIO.succeed(println("close"))).run
    
    // Use resource here
    resource
  }
}

// Example from extract-stories.sc:
def extractStories(inputPath: Path) = ZIO.scoped {
  defer {
    val fileIS = ZIO
      .fromAutoCloseable(
        ZIO.attempt(new FileInputStream(inputPath.toFile()))
          .mapError(e => new Throwable(s"Error opening file: ${e.getMessage}"))
      )
      .run
      
    val workbook = ZIO
      .fromAutoCloseable(
        ZIO.attempt {
          if (inputPath.toString.toLowerCase.endsWith(".xlsx"))
            new XSSFWorkbook(fileIS)
          else
            new HSSFWorkbook(fileIS)
        }
        .mapError(e => new Throwable(s"Error reading workbook: ${e.getMessage}"))
      )
      .run
      
    // Process workbook...
    workbook
  }
}
```

---

## Testing

```scala
import zio.test._
import zio.test.Assertion._

val spec = test("Addition works with ZIO Direct") {
  defer {
    val result = ZIO.succeed(1 + 1).run
    assertTrue(result == 2)
  }
}
```

---

## Retry with Schedule

```scala
val flaky: IO[String, String] = ZIO.fail("not yet")

val retried = defer {
  val result = flaky.retry(Schedule.recurs(5)).run
  result
}

// More complex retry logic:
val complexRetry = defer {
  val policy = Schedule.exponential(1.second) && Schedule.recurs(5)
  val result = ZIO.attempt(unstableCall()).retry(policy).run
  result
}
```

---

## Software Transactional Memory (STM)

```scala
import zio.stm._

val stmProgram = defer {
  val ref1 = TRef.make(0).commit.run
  ref1.update(_ + 1).commit.run
  val value = ref1.get.commit.run
  value
}
```

---

## FiberRef (Scoped Fiber Local Values)

```scala
val fiberRefProgram = defer {
  val fiberRef = FiberRef.make(0).run
  fiberRef.set(42).run
  val value = fiberRef.get.run
  value
}
```

---

## Contextual Data with ZEnvironment

```scala
case class AppConfig(port: Int)
val layer: ULayer[AppConfig] = ZLayer.succeed(AppConfig(8080))

val app = defer {
  val config = ZIO.service[AppConfig].run
  config.port
}
```

---

## Advanced ZIO Direct Patterns

### Composing Layers

```scala
val dbLayer: ULayer[Database] = ZLayer.succeed(new LiveDatabase)
val loggingLayer: ULayer[Logger] = ZLayer.succeed(new LiveLogger)

val composedLayer = dbLayer ++ loggingLayer

val program = defer {
  val db = ZIO.service[Database].run
  val logger = ZIO.service[Logger].run
  
  logger.log("Starting database operation").run
  val result = db.query("SELECT * FROM users").run
  logger.log(s"Got ${result.size} users").run
  
  result
}
```

### Observability

```scala
import zio.LogAnnotation

val observableProgram = defer {
  ZIO.logAnnotate(LogAnnotation.Name, "MainProgram") {
    defer {
      ZIO.logInfo("Started").run
      val result = businessLogic().run
      ZIO.logInfo("Completed").run
      result
    }
  }.run
}
```

### Metrics

```scala
import zio.metrics._

val metricsProgram = defer {
  val counter = Metric.counter("requests")
  counter.increment.run
  
  val result = processRequest().run
  counter.increment.run
  
  result
}
```

### Conditional Logic

```scala
val conditionalProgram = defer {
  val condition = checkCondition().run
  
  if (condition) {
    performActionA().run
  } else {
    performActionB().run
  }
}

// Using ZIO.when/unless:
val conditionalWithWhen = defer {
  val condition = checkCondition().run
  
  ZIO.when(condition) {
    performAction()
  }.run
}
```

### Working with Collections

```scala
val collectionProgram = defer {
  val items = List(1, 2, 3, 4, 5)
  
  // Process items in parallel:
  val results = ZIO.foreachPar(items)(item => 
    processItem(item)
  ).run
  
  // Process items sequentially:
  val sequential = ZIO.foreach(items)(item =>
    processItem(item)
  ).run
  
  results ++ sequential
}
```

### File Operations (from extract-stories.sc)

```scala
def writeJsonToFile(stories: List[Story], outputPath: Path): ZIO[Any, String, Unit] = ZIO.scoped {
  defer {
    val writer = ZIO
      .fromAutoCloseable(
        ZIO.attempt(new FileWriter(outputPath.toFile()))
          .mapError(e => s"Error opening file for writing: ${e.getMessage}")
      )
      .run

    ZIO.attempt(writer.write(stories.toJson))
      .mapError(e => s"Error writing JSON file: ${e.getMessage}")
      .run
  }
}
```

---

## Key Differences from Regular ZIO

1. **No for-comprehensions**: Use `defer` blocks instead
2. **Direct await**: Use `.run` to await effects instead of `<-`
3. **Natural control flow**: Use regular `if/else`, `try/catch`, loops
4. **Cleaner syntax**: Less boilerplate, more readable code

### Before (Regular ZIO):
```scala
val program = for {
  config <- ZIO.service[Config]
  _ <- ZIO.logInfo(s"Starting with port ${config.port}")
  result <- if (config.enabled) 
    processRequest() 
  else 
    ZIO.succeed("disabled")
  _ <- ZIO.logInfo(s"Result: $result")
} yield result
```

### After (ZIO Direct):
```scala
val program = defer {
  val config = ZIO.service[Config].run
  ZIO.logInfo(s"Starting with port ${config.port}").run
  
  val result = if (config.enabled) {
    processRequest().run
  } else {
    "disabled"
  }
  
  ZIO.logInfo(s"Result: $result").run
  result
}
```

---

## Resources

- ZIO Direct Documentation: https://zio.dev/zio-direct/
- Supported Constructs: https://zio.dev/zio-direct/supported-constructs
- ZIO Official Site: https://zio.dev
- Source Code: https://github.com/zio/zio-direct