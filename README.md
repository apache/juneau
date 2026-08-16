# Juneau Microservice Starter — Jetty

A clone-and-go [Apache Juneau](https://juneau.apache.org) microservice, deployed on embedded Jetty.

Clone this template (single-branch of `apache/juneau`):

```bash
git clone -b microservice-jetty-starter --single-branch https://github.com/apache/juneau.git my-app
cd my-app
```

## Prerequisites
- JDK 17+
- (Optional) Docker

## Quick start

```bash
./mvnw clean package
java -jar target/my-app.jar
```

Open <http://localhost:10000/helloWorld>. Add an `Accept` header (`application/json`, `text/xml`,
`text/html`) to see the same POJO three ways. The `api` link shows the auto-generated Swagger page.
The port comes from `juneau.cfg` (`[Jetty] port`).

## Run with Docker

```bash
./mvnw -q clean package
docker build -t my-app .
docker run --rm -p 10000:10000 my-app
```

## What's inside

- `App` — boots Jetty + Juneau via `JettyMicroservice.run(...)`.
- `RootResources` — router page; `HelloWorldResource` — POJO endpoint + `@Path`/`@Query` endpoint.
- `GreetingApi` — typed `@Remote` client, exercised by `HelloWorldResourceTest`.
- `juneau.cfg` — Jetty runtime config; `my-app.yaml` — Juneau app config.

## Documentation

Full docs: <https://juneau.apache.org/docs/topics/StarterProjects>.
Framework docs: <https://juneau.apache.org>.
