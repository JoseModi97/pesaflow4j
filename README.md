# PesaFlow4J

[![Maven Central](https://img.shields.io/maven-central/v/io.github.josemodi97/pesaflow4j-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.josemodi97/pesaflow4j-core)
[![Javadoc](https://javadoc.io/badge2/io.github.josemodi97/pesaflow4j-core/javadoc.svg)](https://javadoc.io/doc/io.github.josemodi97/pesaflow4j-core)
[![CI](https://github.com/JoseModi97/pesaflow4j/actions/workflows/ci.yml/badge.svg)](https://github.com/JoseModi97/pesaflow4j/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 8+](https://img.shields.io/badge/Java-8%2B-blue.svg)](#compatibility)

**PesaFlow4J** is a dependency-free Java SDK for the Kenya **PesaFlow / eCitizen PaymentAPI**: build HMAC-signed checkout payloads, render instant pay buttons, trigger **Safaricom M-Pesa STK push** prompts, and verify inbound webhook/IPN notifications — with a plain, framework-agnostic Java API that drops into Spring Boot, Jakarta EE, Quarkus, Micronaut, plain servlets, or a bare `public static void main`.

It is an independent, community-built SDK. It is not produced, endorsed, or supported by eCitizen, PesaFlow, or Safaricom.

---

## Why PesaFlow4J

- **Runs everywhere** — `pesaflow4j-core` compiles and is tested against the **Java 8 language level**, so the same jar runs unmodified on Java 8, 11, 17, 21, and every release after it. No forced upgrade of your runtime to adopt it. (`pesaflow4j-jakarta` needs Java 11+ and `pesaflow4j-spring-boot3-starter` needs Java 17+ — inherent to the ecosystems they target, Jakarta EE 9+ and Spring Boot 3 respectively, not a PesaFlow4J limitation.)
- **Zero runtime dependencies at the core** — `pesaflow4j-core` uses only `javax.crypto`, `java.net`, and `java.math` from the JDK itself. Nothing to conflict with your existing dependency tree. Framework modules add only what that framework already requires (a servlet API, Spring itself) — never a third one.
- **Framework-neutral by design** — the core has no compile-time knowledge of Spring, servlets, or any web framework, so it works identically in a Spring Boot controller, a Quarkus resource, a Lambda handler, or a CLI tool.
- **Correct by construction** — `BigDecimal` for money (never a lossy `double`), a builder API instead of stringly-typed maps, and a timing-safe HMAC comparison for webhook signatures to avoid side-channel timing attacks.
- **Available for both Maven and Gradle** — published to Maven Central with a standard POM, and the source tree itself builds under both Maven and Gradle, so contributors aren't locked into one toolchain.
- **Async-friendly** — every blocking network call has a `CompletableFuture`-returning sibling, so it composes cleanly with reactive and async codebases without pulling in a reactive library.
- **A real JPMS module, not just a manifest hint** — `pesaflow4j-core` ships as a multi-release jar with a genuine `module-info.java` for Java 9+, and swaps in a `java.net.http.HttpClient`-based transport (HTTP/2, negotiated automatically) for Java 11+ — both completely transparent to callers, and both verified with real module-path execution rather than assumed correct.
- **Ships as a native binary too** — `pesaflow4j-cli` builds with GraalVM `native-image` (`./gradlew nativeCompile` / `mvn -Pnative package`) for a dependency-free, instant-startup executable, alongside the regular JVM fat jar.

---

## Compatibility

| | Supported |
|---|---|
| **Java** | 8, 11, 17, 21, 25, and every release in between (compiled at the `--release 8` bytecode level) |
| **Build tools (as a dependency)** | Any Maven 3.x or Gradle version — it's a plain jar + standard POM once published |
| **Build tools (building this repo)** | Maven 3.6.3+, Gradle 8.0+ (the CLI module's fat-jar packaging needs Shadow 8.x, which requires Gradle 8.0+) |
| **Frameworks** | Plain Java, Spring / Spring Boot (2.x and 3.x, dedicated starters), Jakarta EE & Java EE servlets (dedicated adapters), Quarkus, Micronaut, Vert.x, Android (API 26+, via desugaring) |
| **Module system** | Works on the classpath (Java 8+) *and* as a real named module on the module path (Java 9+) — `pesaflow4j-core` ships as a multi-release jar with a genuine `module-info.java`, verified with `jar --validate` and real module-path execution, not just a manifest hint |

---

## Installation

### Maven

```xml
<dependency>
  <groupId>io.github.josemodi97</groupId>
  <artifactId>pesaflow4j-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("io.github.josemodi97:pesaflow4j-core:0.1.0")
```

### Gradle (Groovy DSL)

```groovy
implementation 'io.github.josemodi97:pesaflow4j-core:0.1.0'
```

> Building from source instead? `mvn install` / `./gradlew publishToMavenLocal` from a clone of this repo installs to your local repository. See [PLAN.md](PLAN.md#publishing-to-maven-central) for the release process.

---

## Modules

`pesaflow4j-core` is the only one you need for a plain Java app. Add one of these on top for framework glue:

| Artifact | For | Brings |
|---|---|---|
| `pesaflow4j-core` | Any Java 8+ app | `Pesaflow4jClient`, signing, verification. No dependencies. |
| `pesaflow4j-servlet` | Tomcat 8/9, Spring Boot 2, plain Java EE servlets | `Pesaflow4jServletWebhookHandler` — verifies a notification from an `HttpServletRequest`. |
| `pesaflow4j-jakarta` | Tomcat 10+, Spring Boot 3, Jakarta EE 9+ | The same handler, for the `jakarta.servlet` namespace. |
| `pesaflow4j-spring-boot2-starter` | Spring Boot 2.x | Auto-configured `Pesaflow4jClient` bean from `pesaflow4j.*` properties, opt-in webhook endpoint + events. |
| `pesaflow4j-spring-boot3-starter` | Spring Boot 3.x | Same, for the Jakarta namespace (Java 17+ floor). |
| `pesaflow4j-cli` | Terminal / CI | `checkout`, `status`, `verify` subcommands — sign, submit, poll, and debug payments without writing code. |

### Servlet adapters

```java
Pesaflow4jServletWebhookHandler handler = new Pesaflow4jServletWebhookHandler(client)
        .onSuccess((result, req, res) -> markOrderPaid(result.getReference(), result.getAmountPaid()))
        .onFailure((result, req, res) -> log.warn(result.getDescription()));

// inside your doPost(HttpServletRequest req, HttpServletResponse res):
handler.handle(req, res);
```

(`pesaflow4j-jakarta` is the identical API under `jakarta.servlet` imports instead of `javax.servlet`.)

### Spring Boot starters

```yaml
# application.yml
pesaflow4j:
  api-client-id: ${PESAFLOW_CLIENT_ID}
  api-key: ${PESAFLOW_API_KEY}
  secret: ${PESAFLOW_SECRET}
  service-id: ${PESAFLOW_SERVICE_ID}
  webhook:
    enabled: true
    path: /payments/notify
```

That's it — a `Pesaflow4jClient` bean is now available for injection, and `POST /payments/notify` is live. React to a payment without writing a controller:

```java
@EventListener
void onPaid(Pesaflow4jPaymentVerifiedEvent event) {
    orderService.markPaid(event.getResult().getReference(), event.getResult().getAmountPaid());
}
```

Use `pesaflow4j-spring-boot2-starter` on Spring Boot 2.x, `pesaflow4j-spring-boot3-starter` on 3.x — pick the one matching your Spring Boot major version.

### CLI

```bash
# build & print a signed payload (add --submit to actually send it)
java -jar pesaflow4j-cli.jar checkout --client-id ... --api-key ... --secret ... --service-id ... \
    --amount 500 --reference INV-0001 --description "School fees" --name "Jane Doe" --id-number 12345678

# poll settlement status
java -jar pesaflow4j-cli.jar status --reference INV-0001 --status-url https://...

# debug a captured webhook payload's signature, piped straight from curl/a log
cat captured-notification.txt | java -jar pesaflow4j-cli.jar verify --stdin
```

Every flag falls back to a `PESAFLOW4J_*` environment variable, so CI pipelines can omit credentials from the command line entirely.

### Project scaffolding: Maven & Gradle plugins

`pesaflow4j-maven-plugin` is on Maven Central and usable today. `pesaflow4j-gradle-plugin` is built, tested, and CI-verified, but not yet published to the [Gradle Plugin Portal](https://plugins.gradle.org) — the `plugins { id(...) }` snippet below won't resolve until that's done (see [PLAN.md](PLAN.md#publishing-pesaflow4j-gradle-plugin)). Until then, build and `./gradlew publishToMavenLocal` it yourself from a clone of this repo.

```bash
# Maven
mvn io.github.josemodi97:pesaflow4j-maven-plugin:init

# Gradle (after adding: plugins { id("io.github.josemodi97.pesaflow4j") version "0.1.0" })
./gradlew pesaflow4jInit
```

Both write a placeholder `pesaflow4j.properties` into your project (without overwriting one that already exists) — fill in your credentials and you're ready to build a `Pesaflow4jClient`.

They also **auto-detect which framework you're using** from your project's own dependencies — no flag needed. A `jakarta.servlet-api`/`javax.servlet-api` dependency, or a Spring Boot 2.x/3.x dependency, gets you a working, compile-verified example file (a `PesaflowNotifyServlet` for servlet targets, a `PesaflowPaymentListener` for Spring Boot — see [Servlet adapters](#servlet-adapters) and [Spring Boot starters](#spring-boot-starters) above) dropped into `src/main/java/pesaflow4j/`, plus `pesaflow4j.webhook.enabled=true` added to the properties file for the Spring Boot case. The detection result is always logged, and you can override it explicitly when it guesses wrong or you want a different target:

```bash
# Maven
mvn io.github.josemodi97:pesaflow4j-maven-plugin:init -Dpesaflow4j.framework=spring-boot3
```
```kotlin
// Gradle
tasks.named<Pesaflow4jInitTask>("pesaflow4jInit") { framework.set("spring-boot3") }
```

Supported values: `auto` (default), `plain`, `servlet`, `jakarta`, `spring-boot2`, `spring-boot3`.

---

## Quickstart

### 1. Configure

```java
Pesaflow4jConfig config = Pesaflow4jConfig.builder()
        .apiClientId("YOUR_API_CLIENT_ID")
        .apiKey("YOUR_API_KEY")
        .secret("YOUR_MERCHANT_SECRET")
        .serviceId("YOUR_SERVICE_ID")
        .build();

Pesaflow4jClient client = new Pesaflow4jClient(config);
```

Prefer environment variables (containers, CI, 12-factor apps)? `Pesaflow4jConfig.fromEnvironment()` reads `PESAFLOW4J_CLIENT_ID`, `PESAFLOW4J_API_KEY`, `PESAFLOW4J_SECRET`, `PESAFLOW4J_SERVICE_ID`, `PESAFLOW4J_GATEWAY_URL`, `PESAFLOW4J_STATUS_URL`, and `PESAFLOW4J_CURRENCY`.

### 2. Build a signed checkout and render a pay button

```java
CheckoutRequest payment = CheckoutRequest.builder()
        .amount(500)
        .reference("INV-0001")
        .description("School fees")
        .name("Jane Doe")
        .idNumber("12345678")
        .phone("0712345678")          // normalized to 254712345678 automatically
        .sendStkPush(true)            // triggers an M-Pesa STK push on submit
        .callbackUrl("https://yourapp.example.com/payments/success")
        .notifyUrl("https://yourapp.example.com/payments/notify")
        .build();

String html = client.payButton(payment, "Pay with PesaFlow", PayButtonOptions.create()
        .cssClass("btn btn-success btn-lg"));
```

`payButton(...)` returns a self-contained, HMAC-signed `<form>` — no JavaScript, no iframe wiring. Need the raw payload instead (custom UI, mobile app, React/Vue)? Use `client.checkout(payment)`.

### 3. Verify an inbound webhook / IPN notification

```java
// e.g. inside a Spring @PostMapping("/payments/notify"), a servlet doPost,
// or any framework's request-body-to-Map binding:
VerifyResult result = client.verify(callbackParams);

if (result.isSuccess()) {
    // result.getReference(), result.getAmountPaid() (BigDecimal), etc.
    markOrderPaid(result.getReference(), result.getAmountPaid());
} else {
    log.warn("PesaFlow4J: rejected notification - {}", result.getDescription());
}
```

The signature check uses a timing-safe byte comparison, and `verify()` only reports success when **both** the HMAC signature is valid **and** the status is a recognized success status (`paid`, `settled`, `success`, `successful`, `completed`, `complete` by default — configurable via `Pesaflow4jConfig.builder().successStatuses(...)`).

### 4. Or skip the browser entirely — initiate a payment headlessly

```java
PaymentSubmissionResult submission = client.initiatePayment(payment);
System.out.println(submission.getHttpStatus() + " " + submission.getResponseBody());

// Async variant for reactive/non-blocking codebases:
client.initiatePaymentAsync(payment)
      .thenAccept(r -> System.out.println(r.getHttpStatus()));
```

### 5. Poll settlement status

```java
Pesaflow4jConfig config = Pesaflow4jConfig.builder()
        // ...
        .statusUrl("https://payments.ecitizen.go.ke/.../status-endpoint")
        .build();

PaymentStatusResult status = client.checkPaymentStatus("INV-0001");
```

---

## API Reference Overview

| Method | Description |
|---|---|
| `client.checkout(CheckoutRequest)` | Builds a signed checkout payload and returns it with the gateway URL |
| `client.payButton(CheckoutRequest, ...)` | Renders a ready-to-use, self-submitting HTML pay button |
| `client.initiatePayment(CheckoutRequest)` | Signs and POSTs a payment directly, no browser/HTML required |
| `client.initiatePaymentAsync(CheckoutRequest)` | `CompletableFuture` variant of `initiatePayment` |
| `client.checkPaymentStatus(String reference)` | Polls the configured status endpoint for settlement status |
| `client.verify(Map<String,String>)` | Verifies an inbound webhook/IPN signature and status |
| `client.isPaid(Map<String,String>)` | Shorthand for `verify(...).isSuccess()` |
| `PhoneNormalizer.normalize(String)` | Normalizes Kenyan MSISDNs (`07...`, `01...`, `+254...` → `254...`) |

Full Javadoc: <https://javadoc.io/doc/io.github.josemodi97/pesaflow4j-core> (populates once published to Maven Central).

---

## Security & Best Practices

1. **Keep secrets out of source control.** Load `apiKey` / `secret` from environment variables, a secrets manager, or `Pesaflow4jConfig.fromEnvironment()` — never hardcode them.
2. **Exempt the notification endpoint from CSRF protection.** The gateway POSTs server-to-server from outside your application's origin.
3. **Timing-safe comparison.** Webhook signatures are checked with a constant-time byte comparison (`HmacSigner.timingSafeEquals`) to avoid leaking timing information.
4. **Always normalize phone numbers** with `PhoneNormalizer.normalize(...)` before sending an STK push — Safaricom/Airtel require the `2547XXXXXXXX` / `2541XXXXXXXX` shape.
5. **Treat `verify()`'s `raw` map as untrusted input** beyond what PesaFlow4J itself validates — don't trust unrelated fields in the payload without your own checks.

---

## Roadmap

All eight modules in the table above are implemented and tested — with both a real Maven and a real Gradle build, `pesaflow4j-core` shipping as a genuine multi-release JPMS module verified via real module-path execution, and GraalVM native-image wired (and CI-verified) for the CLI. What's left is mostly *not* code: Maven Central publishing itself (needs real Sonatype credentials) and richer `init` scaffolding beyond a placeholder properties file. See [PLAN.md](PLAN.md) for the full architecture, what's actually left, and why.

## Contributing

Issues and pull requests are welcome — see [PLAN.md](PLAN.md) for the module layout. Build everything with `mvn test` or `./gradlew test` from the repo root; `pesaflow4j-gradle-plugin` is a standalone Gradle build (`cd pesaflow4j-gradle-plugin && ./gradlew test`) since a Gradle plugin project can't sanely nest inside the reactor it builds.

## License

MIT License. See [LICENSE](LICENSE) for details.
