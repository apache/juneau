<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Apache Juneau — MCP Example

A **runnable, copy-me reference implementation** of both the server and client sides of the
Model Context Protocol (MCP), built on Apache Juneau against the **`2026-07-28` (v2)** revision.

It is deliberately tiny: one in-memory *notes* service exercised by every major MCP surface, so
you can read the whole thing and run it in a few minutes.

## What it demonstrates

A single domain — an in-memory `title → body` note store (`NoteStore`) — is exposed through:

| MCP surface | Where | What to look at |
|---|---|---|
| **tool** | `publishNote(title, body)` | stores a note, then *pushes* a change to subscribers |
| **tool + elicitation / MRTR** | `deleteNote(title)` | returns `input_required` to confirm, then resumes |
| **prompt + completion** | `summarize(title)` | renders a prompt; `title` argument auto-completes |
| **resource** | `note:///index` | lists all note titles |
| **resource template + completion** | `note:///{title}` | reads one note; `{title}` auto-completes |
| **subscription** | `subscriptions/listen` | held-open SSE; `publishNote` fires `onResourceUpdated` |

## Key files

| File | Role |
|---|---|
| [`ExampleMcpServer.java`](src/main/java/org/apache/juneau/examples/mcp/ExampleMcpServer.java) | The MCP server — every surface registered in `createMcpConfig()` |
| [`NoteStore.java`](src/main/java/org/apache/juneau/examples/mcp/NoteStore.java) | The tiny in-memory domain state |
| [`ExampleServer.java`](src/main/java/org/apache/juneau/examples/mcp/ExampleServer.java) | Embedded-Jetty launcher (`main` + `start(port)`) |
| [`ExampleClient.java`](src/main/java/org/apache/juneau/examples/mcp/ExampleClient.java) | Guided `McpClient` walkthrough of every surface |
| [`spring/SpringExampleMcpServer.java`](src/main/java/org/apache/juneau/examples/mcp/spring/SpringExampleMcpServer.java) | Spring Boot variant using `SpringMcpRestServlet` |
| [`spring/SpringExampleApplication.java`](src/main/java/org/apache/juneau/examples/mcp/spring/SpringExampleApplication.java) | `@SpringBootApplication` launcher |
| [`ExampleMcpEndToEnd_Test.java`](src/test/java/org/apache/juneau/examples/mcp/ExampleMcpEndToEnd_Test.java) | In-process end-to-end proof |

## Run it

Build the module (and its dependencies) first:

```bash
mvn -pl juneau-examples/juneau-examples-mcp -am clean install
```

> **Note:** these `exec:java` commands use `-f juneau-examples/juneau-examples-mcp/pom.xml`, not
> `-pl juneau-examples/juneau-examples-mcp`. Because `.mvn/maven.config` sets `--also-make`
> globally, `-pl` resolves `exec:java` against the reactor's root project (which has no
> `exec.mainClass` configured) rather than this module. `-f` runs Maven directly against this
> module's POM instead.

### 1. The embedded-Jetty server + client walkthrough

Start the server (defaults to port `5000`; pass a port to override):

```bash
mvn -f juneau-examples/juneau-examples-mcp/pom.xml exec:java \
  -Dexec.mainClass=org.apache.juneau.examples.mcp.ExampleServer
```

In another terminal, run the client walkthrough (defaults to `http://localhost:5000/`):

```bash
mvn -f juneau-examples/juneau-examples-mcp/pom.xml exec:java \
  -Dexec.mainClass=org.apache.juneau.examples.mcp.ExampleClient
```

The client prints a numbered, top-to-bottom transcript: discovery → listing → tool call →
templated resource read → completion → prompt → a live subscription notification → an
auto-answered `deleteNote` elicitation.

### 2. The Spring Boot variant

```bash
mvn -f juneau-examples/juneau-examples-mcp/pom.xml exec:java \
  -Dexec.mainClass=org.apache.juneau.examples.mcp.spring.SpringExampleApplication
```

This serves the MCP endpoint at **`http://localhost:8080/mcp`** on embedded Tomcat. Its one
`greet` tool resolves a Spring-managed `GreetingService` through the per-request `BeanStore` —
the distinguishing feature of `SpringMcpRestServlet` over the plain `McpRestServlet`.

The Spring app only registers `greet` — none of the notes surfaces from part 1 — so
`ExampleClient`'s full walkthrough is not applicable here (it calls `publishNote`, which this
server doesn't have, and would abort with a `Tool not found` error). To exercise it manually,
call `server/discover` and `tools/call` directly instead:

```bash
curl -s http://localhost:8080/mcp -H 'Content-Type: application/json' -d \
  '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"greet","arguments":{"name":"world"}}}'
```

### 3. The test

```bash
mvn -f juneau-examples/juneau-examples-mcp/pom.xml test
```

`ExampleMcpEndToEnd_Test` boots the server in-process on an ephemeral port and drives the real
client through every surface, asserting each outcome.

## How the wiring works

- **Server:** `ExampleMcpServer extends McpRestServlet` (the v2 base). `createMcpConfig()` lists
  the tools/prompts/resources; `createMcpOptions()` advertises capabilities. `SseSerializer` is
  registered on `@Rest` so the subscription stream can negotiate `text/event-stream`.
- **Launcher:** a Jetty `Server` plus the servlet are placed in a `BasicBeanStore` and handed to
  a `Microservice` with `JettyConfiguration`, which auto-mounts the `@Rest` servlet at `/`.
- **Client:** `McpClient` (v2) — `connect()` does the mandatory `server/discover` handshake;
  `callTool`/`readResource`/`getPrompt`/`complete`/`listen`/`callToolWithElicitation` do the rest.
