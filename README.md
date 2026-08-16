# Juneau Microservice Starter — Tomcat (WAR)

A clone-and-go [Apache Juneau](https://juneau.apache.org) microservice, packaged as a WAR for Tomcat 10.1+.

Clone this template (single-branch of `apache/juneau`):

```bash
git clone -b microservice-tomcat-starter --single-branch https://github.com/apache/juneau.git my-app
cd my-app
```

## Prerequisites
- JDK 17+
- (Optional) Docker

## Quick start (Docker)

```bash
./mvnw -q clean package
docker build -t my-app .
docker run --rm -p 8080:8080 my-app
```

Open <http://localhost:8080/helloWorld>. Add an `Accept` header (`application/json`, `text/xml`,
`text/html`) to see the same POJO three ways. The `api` link shows the auto-generated Swagger page.

## Deploy to an existing Tomcat

```bash
./mvnw -q clean package
cp target/my-app.war "$CATALINA_HOME/webapps/ROOT.war"
```

For a configured greeting, also copy `my-app.yaml` to `$CATALINA_HOME/bin/` (or the working directory Tomcat uses for configuration).

## What's inside

- `RootResources` — router servlet registered at `/*` via `WEB-INF/web.xml`.
- `HelloWorldResource` — POJO endpoint + `@Path`/`@Query` endpoint; reads `my-app.yaml`.
- `GreetingApi` — typed `@Remote` client, exercised by `HelloWorldResourceTest`.

## Documentation

Full docs: <https://juneau.apache.org/docs/topics/StarterProjects>.
Framework docs: <https://juneau.apache.org>.
