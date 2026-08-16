# Juneau Microservice Starter — Spring Boot

A clone-and-go [Apache Juneau](https://juneau.apache.org) microservice, deployed as a Spring Boot app.
Build and run it in under five minutes.

Clone this template (single-branch of `apache/juneau`):

```bash
git clone -b microservice-springboot-starter --single-branch https://github.com/apache/juneau.git my-app
cd my-app
```

## Prerequisites

- JDK 17+
- (Optional) Docker

## Quick start

```bash
# Run from source (hot Spring Boot launcher)
./mvnw spring-boot:run

# …or package and run the jar
./mvnw clean package
java -jar target/my-app-1.0.0-SNAPSHOT.jar
```

Then open <http://localhost:10000/helloWorld>. Add an `Accept` header (`application/json`, `text/xml`,
`text/html`) to see the same POJO served three ways. The `api` link shows the auto-generated Swagger page.

## Run with Docker

```bash
./mvnw -q clean package
docker build -t my-app .
docker run --rm -p 10000:10000 my-app
```

## What's inside

- `HelloWorldResource` — returns a `Greeting` POJO (JSON/XML/HTML content negotiation) and reads
  `HelloWorld/message` from `my-app.yaml`.
- `GreetingApi` — a typed `@Remote` client, exercised by `HelloWorldResourceTest` via `MockRestClient`.
- `my-app.yaml` — Juneau-native YAML config; `application.yaml` — Spring config.

## Rename for your project

Change `com.example` / `my-app` (in `pom.xml` and the `com.example.myapp` package) to your own coordinates.

## Documentation

Full docs: <https://juneau.apache.org/docs/topics/StarterProjects>.
For the framework: <https://juneau.apache.org>.
