# PesaFlow4J — Architecture & Roadmap

This document is the working plan for PesaFlow4J: a Java SDK for the Kenya
PesaFlow / eCitizen PaymentAPI, built as an independent product with its own
name, package namespace, and API design — not a line-for-line port of any
other language's SDK.

## 1. Status

| Module | Status | Description |
|---|---|---|
| `pesaflow4j-core` | ✅ **Implemented** | Signing, verification, HTTP submission, phone normalization. Zero runtime dependencies. |
| `pesaflow4j-servlet` | ✅ **Implemented** | `javax.servlet` webhook handler, for Tomcat 8/9, Spring Boot 2, older Java EE. |
| `pesaflow4j-jakarta` | ✅ **Implemented** | `jakarta.servlet` webhook handler, for Tomcat 10+, Spring Boot 3, Jakarta EE 9+. |
| `pesaflow4j-spring-boot2-starter` | ✅ **Implemented** | Auto-configured `Pesaflow4jClient` bean + `pesaflow4j.*` property binding + opt-in webhook controller/events, for Spring Boot 2.x. |
| `pesaflow4j-spring-boot3-starter` | ✅ **Implemented** | Same, for Spring Boot 3.x (Jakarta namespace, Java 17 floor). |
| `pesaflow4j-cli` | ✅ **Implemented** | `checkout`, `status`, `verify` subcommands (picocli), distributed as a runnable fat jar (Gradle Shadow / Maven Shade) and a GraalVM native-image binary. |
| `pesaflow4j-maven-plugin` | ✅ **Implemented & published** | `mvn io.github.josemodi97:pesaflow4j-maven-plugin:init` — auto-detects your framework and scaffolds a working example, not just a placeholder properties file. Live on Maven Central. |
| `pesaflow4j-gradle-plugin` | ✅ Implemented, ⏳ submitted, pending approval | `./gradlew pesaflow4jInit` — the same auto-detecting scaffolding, as a standalone-built Gradle plugin. `publishPlugins` ran successfully for `0.1.0`; new plugin IDs get a first-time manual review by Gradle before `plugins { id(...) }` resolves publicly (§6b). |
| `pesaflow4j-bom` | ✅ **Implemented** | Bill-of-materials (`java-platform` / `<packaging>pom</packaging>`) pinning matching versions of every library module. |

Why core first: every other module is a thin adapter around it. Shipping a
correct, well-tested core let every framework adapter and tool above be
added without ever touching the signing/verification logic again.

**Build verification**: every module here has been built and tested with
*both* a real Gradle 8.11 and a real Apache Maven 3.9.9 in this environment —
not just written and assumed correct. Current count: **72 distinct tests, 0
failures**, across both build systems independently (Maven: `mvn test`,
53 tests including `pesaflow4j-maven-plugin`; Gradle: `./gradlew test` at the
root, 37 tests, plus `cd pesaflow4j-gradle-plugin && ./gradlew test` for the
standalone plugin build, 19 more).

**Also done, beyond the original plan**: `pesaflow4j-core` ships as a real
multi-release JPMS module (§3) with a `java.net.http.HttpClient` variant for
Java 11+, both verified with real module-path execution and real HTTP
User-Agent inspection, not just assumed from the compiled bytecode; GraalVM
native-image is wired (and CI-verified) for the CLI; `release.yml` publishes
the whole reactor, not just `pesaflow4j-core`; and both scaffolding plugins
auto-detect the consuming project's framework from its own dependencies
(§5 Phase 4) instead of requiring a flag.

**Not yet done** — real next steps, not filler: Maven Central publishing
itself (§6, needs real credentials only obtainable outside this
environment) and the SEO/discoverability checklist (§7, needs a public
repo to act on).

## 2. Package & artifact identity

- **Maven `groupId`**: `io.github.josemodi97` — uses Sonatype Central
  Publishing's GitHub-ownership verification path (no domain purchase or DNS
  TXT record required, since `github.com/JoseModi97` already proves
  ownership).
- **Java package root**: `io.github.josemodi97.pesaflow4j`.
- **Artifact naming**: `pesaflow4j-<module>` (`pesaflow4j-core`,
  `pesaflow4j-servlet`, `pesaflow4j-maven-plugin`, ...) — consistent,
  greppable, and matches the GitHub repo name for discoverability.
- **Automatic-Module-Name**: `io.github.josemodi97.pesaflow4j.core` is set on
  the core jar's manifest today, ahead of a real `module-info.java` (see §3),
  so JPMS consumers already get a stable module name instead of an
  auto-derived one that would change if the jar filename ever changes.

## 3. Compatibility strategy: "forward and backward compatible"

**Floor: Java 8.** Both build files compile with `--release 8`
(`maven.compiler.release` / Gradle's `options.release.set(8)`), which cross-compiles
against the Java 8 API signature set regardless of which JDK runs the build —
verified in this repo by building with JDK 21 and then executing the
resulting jar's public API directly on a **Java 8** JVM (see
`.github/workflows/ci.yml`, job `jdk8-runtime-smoke-test`). This is the same
technique used by Gson, Jackson, and Guava to stay on Java 8 while building
with modern tooling.

**Ceiling: none by design.** No compiled dependency, no reflection into
internal JDK APIs, no removed API used — the jar keeps running unmodified as
new JDKs ship. CI additionally builds and tests on JDK 11, 17, and 21 on every
push (both via Maven and Gradle) to catch any accidental use of a
deprecated-for-removal API early.

**Implemented as a multi-release jar** (both build systems, `pesaflow4j-core` only):

1. **A real JPMS module** (`src/main/java9/module-info.java`, compiled at
   `--release 9`, packaged into `META-INF/versions/9/module-info.class`).
   `exports` covers exactly the four public packages
   (`io.github.josemodi97.pesaflow4j`, `.model`, `.exception`, `.util`);
   `.internal` (the HMAC signer, HTML escaper, HTTP transport) stays
   encapsulated even from module-path consumers. Module name:
   `io.github.josemodi97.pesaflow4j.core` — the same value the
   `Automatic-Module-Name` manifest attribute used to carry, now replaced
   by a real descriptor (the attribute is meaningless once a jar has an
   explicit module and was removed).
2. **A `java.net.http.HttpClient` variant of `HttpTransport`** for Java 11+
   (`src/main/java11/.../HttpTransport.java`, `--release 11`, packaged into
   `META-INF/versions/11/`), negotiating HTTP/2 automatically with a fallback
   to HTTP/1.1. The Java 8 base (`HttpURLConnection`) stays the fallback for
   everyone else. Same public API on both — callers never know which one
   is active.
3. **Toolchain**: Gradle uses two extra source sets (`java9`, `java11`)
   compiled with `options.release.set(...)` and assembled into the jar's
   `META-INF/versions/` tree by the `jar` task directly (`--patch-module` is
   needed for the standalone module-info compile, since its `exports`
   clauses must validate against the main module's own classes, not an
   external classpath dependency). Maven uses `maven-compiler-plugin`'s
   built-in `multiReleaseOutput=true` executions, which handle the
   `--patch-module` wiring automatically — no Moditect or other extra
   plugin needed on either side.
4. **How this was verified**, since a wrong multi-release jar fails *silently*
   at runtime rather than at compile time — this was not just built and
   assumed correct:
   - `jar --validate --file <jar>` (the JDK's own MRJAR consistency
     checker) passes on both the Gradle- and Maven-built jars.
   - `jar --describe-module --release 9` confirms the descriptor exports
     exactly the four intended packages and correctly lists `.internal` as
     `contains` (present, not exported) rather than missing it or leaking it.
   - **Real module-path execution**: a throwaway consumer program run with
     `--module-path <jar> --add-modules io.github.josemodi97.pesaflow4j.core`
     confirms `Pesaflow4jClient.class.getModule().isNamed()` is `true` (a
     real named module, not silently falling back to the unnamed module)
     and that `checkout(...)` still works correctly through that boundary.
   - **Real HTTP-variant selection, not just presence in the jar**: a local
     `com.sun.net.httpserver.HttpServer` captures the `User-Agent` header of
     an actual `initiatePayment(...)` call. On Java 21, it's
     `Java-http-client/21...` — the `HttpClient` variant's own default,
     proving the 11+ override is genuinely selected at runtime, not merely
     compiled correctly. Stripping `META-INF/versions/11/` from a copy of
     the jar and re-running the same test flips the observed header to
     `Java/21...` (`HttpURLConnection`'s default), confirming the base
     variant is independently complete and that MRJAR fallback resolution
     works in both directions.
   - This methodology is now wired into CI as the `mrjar-smoke-test` job
     (module-path + variant-selection checks on JDK 21) and folded into
     `jdk8-runtime-smoke-test` (the same `User-Agent` check, expected to
     show `Java/...` on a *real* Java 8 JVM, which has no concept of
     multi-release jars at all and can only ever see the base entries).
   - **Not verified**: real execution on an actual Java 8 JVM (none is
     installed in the environment this was built in) — the
     `jdk8-runtime-smoke-test` CI job is what closes that gap on push, not
     anything run locally.

## 4. Build tooling: Maven and Gradle, both first-class

The repository root is buildable with **either** tool from the same source
tree — not "Maven is canonical and Gradle just consumes the Central
artifact," but two real, independently-runnable builds:

- `pom.xml` (root, `packaging=pom`) + `pesaflow4j-core/pom.xml`
- `settings.gradle.kts` + `build.gradle.kts` (root) + `pesaflow4j-core/build.gradle.kts`

```bash
# Either of these works from a clean clone:
mvn -pl pesaflow4j-core -am test
./gradlew :pesaflow4j-core:test
```

This matters for the stated goal ("used in all Java projects, frameworks,
etc.") because contributors and downstream teams standardize on one or the
other, and neither should be a second-class citizen of this project. The same
duality now extends to *consumers* too: `pesaflow4j-maven-plugin` gives a
Maven project `mvn io.github.josemodi97:pesaflow4j-maven-plugin:init`, and
`pesaflow4j-gradle-plugin` gives a Gradle project the equivalent
`pesaflow4jInit` task — both scaffold the same placeholder
`pesaflow4j.properties`.

`pesaflow4j-gradle-plugin` lives in its own directory with its **own**
`settings.gradle.kts` — it is a standalone Gradle build, not a subproject of
the root reactor (a `java-gradle-plugin` project awkwardly nests inside the
thing it builds, and this also keeps the root reactor's dependency graph
acyclic). Build/test it with:

```bash
cd pesaflow4j-gradle-plugin && ./gradlew test
```

`pesaflow4j-maven-plugin`, by contrast, *is* an ordinary module in the root
Maven reactor (`mvn -pl pesaflow4j-maven-plugin -am test`) — Maven plugins
don't have the same nesting problem Gradle plugins do.

**Version floors, precisely stated** (verified against each plugin's own
published POM `<prerequisites>`, not guessed):

- **As a *dependency*** (`pesaflow4j-core`, `-servlet`, `-jakarta`, the two
  Spring Boot starters): no floor at all — any Gradle or Maven version that
  can resolve a plain Maven Central artifact works, since nothing about the
  published jar or POM is build-tool-version-sensitive.
- **Building this repo**: **Maven 3.6.3+** (the real prerequisite of
  `maven-compiler-plugin` 3.13.0, `maven-shade-plugin` 3.6.0, and
  `maven-plugin-plugin` 3.13.1 — all three declare it explicitly) and
  **Gradle 8.0+** (not 7+, an earlier draft of this doc got this wrong: the
  Shadow plugin used for `pesaflow4j-cli`'s fat jar — `com.gradleup.shadow`
  8.x — requires Gradle 8.0 per its own compatibility table). Tested in
  this environment with a real Gradle 8.11.1 and a freshly-downloaded real
  Apache Maven 3.9.9, both well above those floors.
- **Using `pesaflow4j-gradle-plugin`** in your own build: effectively
  **Gradle 8.x** — a Gradle plugin binds to the `gradleApi()` version it was
  compiled against (8.11 here via `java-gradle-plugin`'s auto-added
  dependency), so consumers need a Gradle in that same major line, not an
  arbitrary older one. `pesaflow4j-maven-plugin` doesn't have this
  constraint beyond the Maven 3.6.3+ floor above, since Maven's plugin ABI
  is far more stable across versions.

## 5. Roadmap

### Phase 1 — Core (done)
`Pesaflow4jConfig`, `Pesaflow4jGateway` (low-level signing/verification),
`Pesaflow4jClient` (high-level facade: `checkout`, `payButton`,
`initiatePayment`/`initiatePaymentAsync`, `checkPaymentStatus`, `verify`,
`isPaid`), `PhoneNormalizer`. Golden-vector HMAC tests (expected hashes
computed independently, not derived from the library's own code) plus
behavioral tests for validation, tampering rejection, and HTML escaping.

### Phase 2 — Framework adapters (done)
- `pesaflow4j-servlet` / `pesaflow4j-jakarta`: `Pesaflow4jServletWebhookHandler`
  — a small class (not a forced `HttpServlet` base class, so it drops into
  any existing servlet/filter/controller) that adapts
  `HttpServletRequest.getParameterMap()` to `Pesaflow4jClient.verify(Map)`
  and writes the JSON response, with `onSuccess`/`onFailure` callbacks.
  Servlet-api dependency is `provided`/`compileOnly` in both, since the
  container always supplies it.
- Spring Boot starters: `Pesaflow4jProperties` (`@ConfigurationProperties("pesaflow4j")`)
  bound to `Pesaflow4jConfig`, an auto-configured `Pesaflow4jClient` bean
  (only activates once `pesaflow4j.api-client-id` is set — an unconfigured
  app doesn't fail to start just because the starter is on the classpath),
  and an opt-in `@RestController` webhook endpoint
  (`pesaflow4j.webhook.enabled=true`) that republishes the outcome as a
  `Pesaflow4jPaymentVerifiedEvent`/`Pesaflow4jPaymentRejectedEvent` so
  application code reacts with `@EventListener` instead of writing a
  controller. Two starters (Boot 2 / Boot 3) because the servlet namespace
  changed (`javax.servlet` → `jakarta.servlet`) *and* Spring Boot 3 itself
  requires Java 17+ — both are real compatibility boundaries, not something
  PesaFlow4J can paper over with reflection.

### Phase 3 — CLI (done)
`pesaflow4j-cli`: a picocli-based tool with `checkout` (build/print a signed
payload, or `--submit` it directly), `status` (poll settlement status), and
`verify` (check a captured webhook payload's signature — `--data k=v`
repeated, or `--stdin` for a form-encoded body piped from `curl`/a log
capture). Distributed as a runnable fat jar (Gradle Shadow plugin /
Maven Shade plugin), and as a GraalVM native-image binary (`gradle
:pesaflow4j-cli:nativeCompile` / `mvn -Pnative package`) — the picocli
annotation processor (`-Aproject=true`) generates the required
`META-INF/native-image/**` reflection config on both sides, verified by
inspecting the actual generated JSON files in the build output. CI builds
and smoke-tests the real native binary on a GraalVM JDK (the
`native-image-build` job); this repo's own dev environment has neither
GraalVM nor the MSVC linker Windows native-image needs, so the actual
`.exe`/binary link step could not be verified locally — only the AOT
config generation and the plugin wiring itself were.

### Phase 4 — Build-tool plugins + BOM (done)
- `pesaflow4j-maven-plugin`: an `init` goal (`Pesaflow4jInitMojo`) that
  writes a placeholder `pesaflow4j.properties`, skipping the write if one
  already exists.
- `pesaflow4j-gradle-plugin`: the same behavior as a `pesaflow4jInit` task
  (`Pesaflow4jInitTask`), in its own standalone Gradle build (see §4).
- `pesaflow4j-bom`: `java-platform` (Gradle) / `<packaging>pom</packaging>`
  with `<dependencyManagement>` (Maven) pinning `pesaflow4j-core`,
  `-servlet`, `-jakarta`, and both Spring Boot starters to the same version.
- **Richer scaffolding (done)**: both `init` implementations now generate a
  working, compile-verified integration example — not just the placeholder
  properties file — for `servlet`, `jakarta`, `spring-boot2`, and
  `spring-boot3` (a `PesaflowNotifyServlet` or `PesaflowPaymentListener`,
  written to `src/main/java/pesaflow4j/`, plus `pesaflow4j.webhook.enabled`
  added to the properties file for the Spring Boot cases). "Compile-verified"
  is literal: each test compiles the generated source with
  `javax.tools.JavaCompiler` against the real dependency jars
  (`pesaflow4j-servlet`, `pesaflow4j-jakarta`, both Spring Boot starters,
  the relevant `*servlet-api`), so a wrong generated import or method
  signature fails the test suite, not just looks plausible.
- **Framework auto-detection (done)**: the `framework` parameter defaults
  to `auto` on both plugins, which inspects the *consuming* project's own
  declared dependencies (Maven: `MavenProject.getDependencies()` plus its
  parent POM; Gradle: every `Configuration`'s declared — not resolved —
  dependencies, deferred to `project.afterEvaluate` since the consumer's
  `dependencies { }` block hasn't run yet when the plugin itself is
  applied, plus whether the `org.springframework.boot` plugin is applied)
  to pick `servlet`, `jakarta`, `spring-boot2`, or `spring-boot3`
  automatically — falling back to `plain` when nothing relevant is found.
  This is a heuristic (a project pulling Spring Boot some unusual way,
  e.g. only via an imported BOM with no direct starter dependency, won't
  be detected), so the result is always logged (`getLog().info(...)` on
  Maven, `getLogger().lifecycle(...)` on Gradle — Gradle hides INFO by
  default, so lifecycle is the level that actually shows up in a plain
  run) with the reason, and can be overridden with an explicit
  `-Dpesaflow4j.framework=...` / `framework.set("...")`. Verified with
  real dependency-set fixtures for the detector logic itself
  (`FrameworkDetectorTest` on both sides — Gradle's uses a real
  `ProjectBuilder`-constructed project, not a mock) plus a true end-to-end
  functional test: a synthetic Gradle consumer project declaring
  `jakarta.servlet-api` and nothing else, run through real TestKit with no
  `framework` set at all, correctly gets the jakarta example and the
  expected log line.

### Phase 5 — Publish and promote
Maven Central release of every module (§6) — this is the one phase that
cannot be completed from inside this environment, since it requires a real
Sonatype account, a real GPG key, and real GitHub repository secrets — plus
the SEO/discoverability checklist in §7.

## 6. Publishing to Maven Central

1. **Create a Sonatype Central account** at <https://central.sonatype.com>
   and verify the `io.github.josemodi97` namespace (GitHub-ownership
   verification: Sonatype gives you a short code to publish as a public
   GitHub Gist under the `JoseModi97` account).
2. **Generate a GPG signing key** (`gpg --gen-key`), publish the public key to
   a keyserver (`gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`)
   — Central rejects unsigned releases.
3. **Generate a Central Publishing Portal user token** (Account → Generate
   User Token) — this replaces the old OSSRH username/password.
4. **Add repository secrets** in GitHub (Settings → Secrets → Actions):
   `CENTRAL_USERNAME`, `CENTRAL_PASSWORD` (the token pair from step 3),
   `GPG_PRIVATE_KEY` (armored private key), `GPG_PASSPHRASE`.
5. **Cut a release**: bump `<version>` in the root `pom.xml` and every
   module's `pom.xml` (`mvn versions:set -DnewVersion=X.Y.Z
   -DprocessAllModules` from the root does this in one shot), and pass
   `-PpesaflowVersion=X.Y.Z` for the Gradle side (root `build.gradle.kts`
   reads that project property; `pesaflow4j-gradle-plugin` — a separate
   build — takes the same property independently since it isn't part of the
   root reactor). Tag `vX.Y.Z`, push the tag — `.github/workflows/release.yml`
   builds every module in the reactor, signs, and uploads to the Central
   Publishing Portal (`autoPublish=false` by default, so releases can be
   reviewed in the portal UI before the final "Publish" click; flip to
   `true` once confident in the pipeline).
6. Allow a few hours for propagation to Maven Central's search index and
   `repo.maven.apache.org` before the README's dependency snippets resolve
   for consumers.

**Versioning**: strict [SemVer](https://semver.org/). `0.x.y` while the public
API (`Pesaflow4jConfig`, `Pesaflow4jClient`, `CheckoutRequest`, result types)
is still open to breaking changes based on early feedback; `1.0.0` once the
core module's API is considered stable — after Phase 2 adapters have
exercised it in real frameworks, not before.

### 0.1.0 release retrospective — real bugs a local-only workflow never caught

The 0.1.0 release (2026-09-10) was this project's first real contact with
GitHub Actions and the actual Central Publishing API, after weeks of
everything passing locally. Four genuine, previously-invisible bugs
surfaced, in order:

1. **`ci.yml` triggered on branch `main`; the repo's actual default branch
   is `master`.** CI had never run — not once — on any push, since git
   init. Only `release.yml` fired (tag pushes aren't branch-restricted).
   Fixed by triggering on both.
2. **`central-publishing-maven-plugin:0.7.0` couldn't parse its own
   server's response.** Sonatype's API added a `"warnings"` field to
   `DeploymentApiResponse`; the plugin's Jackson deserialization wasn't
   configured to ignore unknown fields, so `mvn deploy` reported
   `BUILD FAILURE` — even though the bundle had already uploaded
   successfully and a real deployment existed in the portal, staged and
   awaiting publish. Bumped to `0.11.0`. Lesson: a Maven Central
   `BUILD FAILURE` is not proof nothing was uploaded — check the portal's
   Deployments tab, specifically the "Uploaded bundle successfully,
   deploymentId: ..." log line, before assuming a clean retry is needed.
3. **No Gradle wrapper was ever committed**, on either the root reactor or
   the standalone `pesaflow4j-gradle-plugin` build. Locally this was
   invisible (a pinned Gradle 8.11.1 was installed once and reused for
   every command all along). CI's `gradle build` picked up whatever
   version the runner happened to have — Gradle 9.7.1 — which broke the
   Shadow plugin's task graph in `pesaflow4j-cli`
   (`You can't map a property that does not exist: propertyName=mainClassName`).
   This is exactly the failure mode Gradle wrappers exist to prevent, and
   it should have been added on day one, not discovered via a broken CI
   run. Fixed: `gradlew`/`gradlew.bat`/`gradle-wrapper.{jar,properties}`
   pinning 8.11.1, committed to both builds, `ci.yml` switched from bare
   `gradle` to `./gradlew` everywhere.
4. **The wrapper script lost its executable bit** when committed from a
   Windows machine (`git ls-files -s gradlew` showed mode `100644`, not
   `100755`) — invisible on Windows, but CI's Linux runners hit
   `Process completed with exit code 126` (permission denied) trying to
   run `./gradlew` directly. Fixed with `git update-index --chmod=+x`.
5. A smaller, related version-drift bug: `pesaflow4j-gradle-plugin`'s test
   suite depended on `pesaflow4j-core:0.1.0-SNAPSHOT` from mavenLocal via
   a separately hardcoded constant, and its CI job never ran `mvn install`
   to populate mavenLocal in the first place — so it failed even once the
   Gradle-version issue was fixed. The hardcoded constant had also gone
   stale the moment 0.1.0 (non-SNAPSHOT) was released. Fixed by adding the
   missing `mvn -DskipTests install` step to that CI job, and replacing
   the separate constant with a single shared `pesaflow4jReactorVersion`
   property so the plugin's own version and its test dependencies can't
   drift from each other again.

All 8 CI jobs are green as of commit `2c7f79c` — including, for the first
time, real proof (not a local proxy) that the GraalVM native-image binary
actually builds and the base HTTP transport actually runs on a genuine
Java 8 JVM, neither of which this development environment could verify
directly (see SS3 and the CLI section of SS5).

## 6b. Publishing `pesaflow4j-gradle-plugin`

Unlike every other module, this one doesn't go to Maven Central — Gradle
plugins are conventionally published to the **Gradle Plugin Portal**
(plugins.gradle.org), a separate service with its own account and API
key. `pesaflow4j-gradle-plugin/build.gradle.kts` has `com.gradle.plugin-publish`
wired (`website`, `vcsUrl`, keyword `tags` for the portal's own search —
the same discoverability goal as SS7, applied to this second registry).

**Status: submitted, awaiting Gradle's first-time review.** The account
(step 1) and API key (step 2) below were created by the project owner, who
handed the key pair over directly; `./gradlew publishPlugins -PpesaflowVersion=0.1.0`
was then run directly against the real portal (not through
`release-gradle-plugin.yml`, since the `v0.1.0` tag already existed from
the Maven Central release before this workflow did — a fresh tag wasn't
worth cutting just to exercise the same command CI would run) and
returned:

> Your new plugin io.github.josemodi97.pesaflow4j has been submitted for
> approval by Gradle engineers. The request should be processed within the
> next few days, at which point you will be contacted via email.

This is the standard first-time flow (see step 5 below) — the upload
itself succeeded; `plugins { id("io.github.josemodi97.pesaflow4j") }`
simply won't resolve publicly until the review clears.

**A real bug surfaced on the very first publish attempt**: it failed with
`Cannot perform signing task ':signPesaflow4jPluginMarkerMavenPublication'
because it has no configured signatory`. The bare `signing` plugin had
been applied to this build file out of habit, copying the pattern from
the Maven-Central-bound reactor modules — but the Plugin Portal doesn't
require (or want) GPG-signed artifacts, and nothing in this file ever
called `signing.sign(...)`, so the task existed with nothing to configure
it. Fixed by removing the `signing` plugin from this project entirely
(commit `3b007ba`) — it was never needed here.

1. **Create a Gradle Plugin Portal account** at
   <https://plugins.gradle.org> (GitHub OAuth sign-in works here too). — done
2. **Generate an API key pair**: profile page → API Keys → Generate. Shows
   a key and a secret, once. — done
3. **Add repository secrets**: `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`
   — `.github/workflows/release-gradle-plugin.yml` reads them as
   `ORG_GRADLE_PROJECT_gradle.publish.key` / `...secret` (Gradle's
   convention for mapping an env var to a project property — note bash's
   `export` rejects dotted identifiers, so a direct local run needs
   `env 'ORG_GRADLE_PROJECT_gradle.publish.key=...' ./gradlew ...`
   instead), the same property names `com.gradle.plugin-publish` looks for
   locally via `gradle.publish.key`/`gradle.publish.secret` in
   `~/.gradle/gradle.properties` or `./gradlew login`. — done (both secrets
   set on the repo, for future tag-triggered releases)
4. **Push a version tag** (`git tag vX.Y.Z && git push --tags`) for the
   *next* release — `release-gradle-plugin.yml` triggers off the same tag
   pattern as `release.yml`, so both will publish together from one tag
   push going forward. It extracts the version from the tag itself
   (`-PpesaflowVersion=...`), rather than whatever `pesaflow4jVersion`
   fallback happens to be hardcoded in the build file, so this can't go
   stale the way the SS test-dependency version already did once (see the
   retrospective above). Not exercised yet, since 0.1.0 was published
   directly rather than through this workflow (see above) — worth
   confirming end-to-end on the next release.
5. **First-time publish note**: a brand-new plugin ID needs to pass the
   Portal's initial review before it's publicly listed (usually a few
   days per Gradle's own message above); subsequent versions of an
   already-approved ID publish immediately, no re-review.

## 7. SEO & discoverability checklist

Ordered roughly by effort-to-impact ratio:

- [ ] **GitHub repo topics** (Settings → About → gear icon): `java`,
      `pesaflow`, `ecitizen`, `kenya`, `payment-gateway`, `mpesa`,
      `stk-push`, `spring-boot`, `maven`, `gradle`, `fintech`,
      `mobile-money`, `sdk`, `payments`, `safaricom`. GitHub's topic pages
      (e.g. `github.com/topics/payment-gateway`) are indexed by Google and
      are a real discovery path for "java payment gateway kenya"-style
      searches.
- [ ] **Repo description field** (the one-liner shown under the repo name
      and in search results) — keep it keyword-dense and human-readable,
      e.g. *"Java SDK for the Kenya PesaFlow / eCitizen PaymentAPI — M-Pesa
      STK push, webhook verification, Maven & Gradle, Java 8+."*
- [ ] **Publish to Maven Central** (§6) — this is the single highest-impact
      step. Once indexed, the artifact becomes searchable directly inside
      IntelliJ IDEA's "Add Dependency" dialog, Eclipse's Maven/Gradle
      dependency search, VS Code's Java extension autocomplete, and
      `search.maven.org` / `central.sonatype.com` — all of which read
      Central's index, not GitHub.
- [ ] **`javadoc.io`** — free hosted Javadoc for any Central-published
      artifact, no setup beyond the badge URL already in the README
      (`javadoc.io/doc/io.github.josemodi97/pesaflow4j-core`). Populates
      automatically post-publish; itself a Google-indexed page per class.
- [ ] **Maven Central POM metadata completeness** — `<description>`,
      `<url>`, `<scm>`, `<developers>`, `<licenses>` are already filled in
      both build files (§required by Central; also what search.maven.org
      displays and indexes).
- [ ] **`pkg.go.dev`-style third-party indexes**: [mvnrepository.com](https://mvnrepository.com)
      and [libraries.io](https://libraries.io) both auto-index from Central
      on their own crawl schedule — checked directly (browser, not just
      assumed): neither has a public self-service "request/submit" form,
      so there is no manual action either of us can take here. An earlier
      draft of this checklist claimed a "request update" option exists;
      that was wrong and has been corrected. Purely a waiting item.
- [x] **"Awesome" list PRs**: opened
      [akullpp/awesome-java#1317](https://github.com/akullpp/awesome-java/pull/1317)
      (one line under `### Financial`, right next to the existing Stripe
      entry — same "payment gateway API integration" shape). Checked
      `MadeInKenya/madeinkenya.github.io` too — initially held off, since
      its guidelines ask for ~10+ GitHub stars and a maintainer
      social-media link, and this repo had neither. Submitted anyway at
      the project owner's explicit request: opened
      [MadeInKenya/madeinkenya.github.io#39](https://github.com/MadeInKenya/madeinkenya.github.io/pull/39)
      (one line under the `Java` section), with the gap disclosed directly
      in the PR body rather than glossed over, so their maintainers can
      decide with full context instead of us quietly working around their
      own stated bar.
- [x] **A short launch write-up**, titled around the actual search intent
      (*"Accepting M-Pesa and PesaFlow Payments From a Java App"* rather
      than just the product name, since that's the phrase people actually
      type into Google before they know the library exists) — drafted in
      first person as the maintainer's own voice. Published as the
      [v0.1.0 GitHub Release notes](https://github.com/JoseModi97/pesaflow4j/releases/tag/v0.1.0)
      (linked from the repo's main page, indexed, no external account
      needed to publish it there); also delivered as a standalone file for
      the project owner to additionally cross-post to dev.to, Hashnode, or
      elsewhere at their discretion — that step needs their own account,
      so it's the one piece of this item that stays theirs to do.
- [ ] **Answer/ask on Stack Overflow** under existing `ecitizen`/`pesaflow`/
      `m-pesa` tags — inherently reactive, not something to action ahead of
      time: there is no real usage question to answer yet for a same-day
      project. Revisit once one exists.
- [ ] **Keep the GitHub Actions CI badge green and visible** in the README —
      a passing-CI badge is a real (if minor) trust/ranking signal for people
      landing from search, not just decoration.

None of this substitutes for §6 (Central publishing) — it's the prerequisite
every other item on this list depends on for a working install snippet.

## 8. Testing strategy

- **Golden-vector tests** (`Pesaflow4jGatewayTest`) assert against HMAC
  values computed by an independent throwaway program, not derived from
  `HmacSigner` itself — so a shared bug in both the signer and its test
  can't hide.
- **Behavioral tests** (`Pesaflow4jClientTest`) cover the client-facing
  contract: status-based success/failure, tamper rejection, HTML escaping of
  untrusted fields in `payButton`.
- **Runtime compatibility test**: the CI `jdk8-runtime-smoke-test` job is the
  actual proof of "runs on Java 8," not just a compiler flag — it builds on
  a modern JDK and *executes* the public API on a Java 8 JVM.
- **Servlet adapters** (`pesaflow4j-servlet`, `pesaflow4j-jakarta`): Mockito
  fakes for `HttpServletRequest`/`HttpServletResponse`, exercising the
  success/tamper/already-committed-response paths against a real, signed
  notification (not a mocked HMAC).
- **Spring Boot starters**: `ApplicationContextRunner` — a real Spring
  context is built per test, asserting the `Pesaflow4jClient` bean both
  *does* appear once configured and *doesn't* appear when unconfigured
  (proving the `@ConditionalOnProperty` guard actually works, not just that
  it's annotated), plus a plain unit test of the webhook controller.
- **CLI**: `picocli.CommandLine(...).execute(...)` invoked directly with
  captured `System.out`/`System.in` — runs the real argument parser and
  command wiring, not a hand-rolled substitute.
- **Gradle plugin**: a Gradle TestKit functional test that applies the
  plugin to a synthetic throwaway project and runs the real task — proves
  the plugin ID, task registration, and file-scaffolding logic all work
  together, not just that the Java class compiles.
- **Maven plugin**: a plain JUnit test instantiating the Mojo directly
  (no Maven runtime needed) and asserting on the file it writes.
- Every module above was additionally verified by running the *actual*
  build for real — `./gradlew test` (root reactor + the standalone
  `pesaflow4j-gradle-plugin` build) and `mvn test` (a real downloaded Apache
  Maven 3.9.9, since none was preinstalled) both pass, 72 tests, 0 failures,
  as of this writing — plus, since then, the real GitHub Actions CI run
  itself (all 8 jobs green), which is what actually caught the Gradle
  wrapper/version-drift bugs in the 0.1.0 retrospective above that local
  runs alone never surfaced.

## 9. Security notes

- Webhook signature comparison is constant-time (`HmacSigner.timingSafeEquals`)
  to avoid leaking match-length information through response timing.
- `payButton()` HTML-escapes every field value and attribute before
  interpolation (`HtmlEscaper`), since payer-supplied fields (name, etc.) end
  up in the rendered form.
- The core module never logs credentials or payloads itself — logging is left
  to the caller, who controls what's safe to persist.
- No secret is ever transmitted in the clear beyond what the gateway's own
  documented protocol already requires (the HMAC secret never leaves the
  process; only its signature does).
