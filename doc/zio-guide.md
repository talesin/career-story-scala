# ZIO Reference Guide for AI Code Agents (Scala)

## Overview

ZIO is a zero-dependency Scala library for asynchronous and concurrent programming based on pure functional principles. Key concepts include:

- **ZIO[R, E, A]**: Describes an effect that requires an environment `R`, might fail with an error of type `E`, and succeeds with a value of type `A`.
- **ZLayer**: For dependency injection.
- **ZStream**: For processing sequences of data.
- **ZIO Test**: For effectful testing.

---

## Type Aliases

```scala
// Common type aliases
import zio._

// UIO[A] is ZIO[Any, Nothing, A]
type UIO[A] = ZIO[Any, Nothing, A]

// Task[A] is ZIO[Any, Throwable, A]
type Task[A] = ZIO[Any, Throwable, A]

// RIO[R, A] is ZIO[R, Throwable, A]
type RIO[R, A] = ZIO[R, Throwable, A]
```

---

## Basic ZIO Usage

```scala
val program: UIO[Unit] =
  for {
    _ <- Console.printLine("Hello, ZIO!")
    _ <- ZIO.sleep(1.second)
  } yield ()
```

---

## Error Handling

```scala
val effect: IO[String, Int] = ZIO.fail("Boom!")

val recovered: UIO[Int] = effect.catchAll(_ => ZIO.succeed(42))
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
```

```scala
def program: ZIO[Logger, Nothing, Unit] =
  ZIO.serviceWithZIO[Logger](_.log("Starting app"))

val runtime = program.provideLayer(loggerLayer)
```

---

## Fibers (Concurrency)

```scala
for {
  fiber <- ZIO.succeed(println("Running")).fork
  _     <- fiber.join
} yield ()
```

---

## Streams

```scala
import zio.stream._

val stream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)
val sum: UIO[Int] = stream.runFold(0)(_ + _)
```

---

## Resource Safety

```scala
val managed: ZIO[Scope, Throwable, Unit] =
  ZIO.acquireReleaseWith(acquire = ZIO.succeed(println("open")))(_ => ZIO.succeed(println("close")))
```

---

## Testing

```scala
import zio.test._
import zio.test.Assertion._

val spec = test("Addition works") {
  assertTrue(1 + 1 == 2)
}
```

---

## Retry with Schedule

```scala
val flaky: IO[String, String] = ZIO.fail("not yet")
val retried = flaky.retry(Schedule.recurs(5))
```

---

## Software Transactional Memory (STM)

```scala
import zio.stm._

for {
  ref1 <- TRef.make(0).commit
  _    <- ref1.update(_ + 1).commit
  v    <- ref1.get.commit
} yield v
```

---

## FiberRef (Scoped Fiber Local Values)

```scala
for {
  fiberRef <- FiberRef.make(0)
  _        <- fiberRef.set(42)
  value    <- fiberRef.get
} yield value
```

---

## Contextual Data with ZEnvironment

```scala
case class AppConfig(port: Int)
val layer: ULayer[AppConfig] = ZLayer.succeed(AppConfig(8080))

val app = ZIO.serviceWith[AppConfig](_.port)
```

---

## Advanced ZIO Patterns

### Composing Layers

```scala
val dbLayer: ULayer[Database] = ZLayer.succeed(new LiveDatabase)
val loggingLayer: ULayer[Logger] = ZLayer.succeed(new LiveLogger)

val composedLayer = dbLayer ++ loggingLayer
```

### Observability

```scala
import zio.LogAnnotation
ZIO.logAnnotate(LogAnnotation.Name, "MainProgram")(
  ZIO.logInfo("Started")
)
```

### Metrics

```scala
import zio.metrics._
val counter = Metric.counter("requests")
counter.increment
```

### Scheduling

```scala
val policy = Schedule.exponential(1.second) && Schedule.recurs(5)
ZIO.attempt(unstableCall()).retry(policy)
```

---

## Resources

- Official Site: https://zio.dev
- Source Code: https://github.com/zio/zio
- Discord: https://discord.gg/TWb2mBz
- Documentation: https://zio.dev/reference/
