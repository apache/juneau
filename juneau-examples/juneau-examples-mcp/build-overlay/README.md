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
| [`secured/`](src/main/java/org/apache/juneau/examples/mcp/secured/) | The OAuth 2.1-secured variant — see [below](#4-the-oauth-21-secured-variant) |

## Run it

This is a standalone Maven project (this `pom.xml` is at the root of the unzipped archive), so
every command below is a plain `mvn ...` invocation — no `-f`/`-pl`/reactor flags needed.

Build it first:

```bash
mvn clean install
```

### 1. The embedded-Jetty server + client walkthrough

Start the server (defaults to port `5000`; pass a port to override):

```bash
mvn exec:java -Dexec.mainClass=org.apache.juneau.examples.mcp.ExampleServer
```

In another terminal, run the client walkthrough (defaults to `http://localhost:5000/`):

```bash
mvn exec:java -Dexec.mainClass=org.apache.juneau.examples.mcp.ExampleClient
```

The client prints a numbered, top-to-bottom transcript: discovery → listing → tool call →
templated resource read → completion → prompt → a live subscription notification → an
auto-answered `deleteNote` elicitation.

### 2. The Spring Boot variant

```bash
mvn exec:java -Dexec.mainClass=org.apache.juneau.examples.mcp.spring.SpringExampleApplication
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
mvn test
```

`ExampleMcpEndToEnd_Test` boots the server in-process on an ephemeral port and drives the real
client through every surface, asserting each outcome.

### 4. The OAuth 2.1-secured variant

`org.apache.juneau.examples.mcp.secured` wraps the exact same notes service in an OAuth 2.1
resource-server gate — every JSON-RPC call now requires a valid bearer token, and the two
mutating tools (`publishNote`/`deleteNote`) additionally require a step-up `mcp.write` scope on
top of the baseline `mcp.read` (see below). It is entirely **self-contained and
offline-runnable**: alongside the secured server, an in-process
[`OfflineAuthorizationServer.java`](src/main/java/org/apache/juneau/examples/mcp/secured/OfflineAuthorizationServer.java)
stands in for a real authorization server (AS) — it generates its own RSA signing key, publishes
the public half of it directly to the validator (no JWKS HTTP fetch needed), and answers a real
RFC 6749 §4.4 client-credentials token request, as well as a real RFC 8414 authorization-server
metadata discovery request. Nothing here talks to the network or requires any setup; see that
class's javadoc for the full design rationale, including exactly what this offline stand-in does
**not** implement (no real scope-authorization decision beyond a fixed allowlist, no refresh
tokens, no user auth).

> **Client secrets don't belong on a command line.** `SecuredExampleServer.main` prints the demo
> client secret to the console purely so this walkthrough has something to copy/paste. A real
> client should read its secret from an environment variable or a file it controls, never accept
> it as a CLI argument (`argv` is visible to every other process on the host via `/proc` or `ps`).

Start the secured server (defaults to port `5001`; pass a port to override). It prints a startup
banner with everything the client needs — copy those three values:

```bash
mvn exec:java -Dexec.mainClass=org.apache.juneau.examples.mcp.secured.SecuredExampleServer
```

In another terminal, paste the three printed values into the secured client walkthrough:

```bash
mvn exec:java -Dexec.mainClass=org.apache.juneau.examples.mcp.secured.SecuredExampleClient \
  -Dexec.args="<endpoint> <clientId> <clientSecret>"
```

The client walks through seven beats: (1) a raw, header-level HTTP call showing the exact `401` +
`WWW-Authenticate: Bearer ...` challenge (including the RFC 9728 `resource_metadata` pointer) an
unauthenticated request gets; (2) the same call again, this time through the real `McpClient` SDK,
showing the ergonomic failure mode (a bare `IOException`, since the gate's rejection body is
plain text, not a JSON-RPC envelope `McpClient` can parse into a typed result); (3) fetching the
RFC 9728 Protected Resource Metadata document the challenge pointed at;
(4) RFC 8414 discovery against the authorization server the PRM document named, resolving its
`token_endpoint` (the token endpoint is never hardcoded or passed on the command line); (5)
acquiring a real `mcp.read`-scoped bearer token and successfully reading a resource with it; (6)
the SAME read-only token attempting `publishNote` — a scoped `403 insufficient_scope` step-up
challenge naming `mcp.write`; (7) acquiring a second token carrying both `mcp.read` and
`mcp.write` and successfully publishing a note with it.

[`SecuredExampleMcpEndToEnd_Test.java`](src/test/java/org/apache/juneau/examples/mcp/secured/SecuredExampleMcpEndToEnd_Test.java)
boots both servers in-process on ephemeral ports and asserts the same rejected/accepted paths
without needing two terminals — run it the same way as `ExampleMcpEndToEnd_Test` (part 3 above).

## How the wiring works

- **Server:** `ExampleMcpServer extends McpRestServlet` (the v2 base). `createMcpConfig()` lists
  the tools/prompts/resources; `createMcpOptions()` advertises capabilities. `SseSerializer` is
  registered on `@Rest` so the subscription stream can negotiate `text/event-stream`.
- **Launcher:** a Jetty `Server` plus the servlet are placed in a `BasicBeanStore` and handed to
  a `Microservice` with `JettyConfiguration`, which auto-mounts the `@Rest` servlet at `/`.
- **Client:** `McpClient` (v2) — `connect()` does the mandatory `server/discover` handshake;
  `callTool`/`readResource`/`getPrompt`/`complete`/`listen`/`callToolWithElicitation` do the rest.
- **Security (part 4):** `SecuredExampleMcpServer extends ExampleMcpServer`, overriding only
  `createMcpOptions()` to add `.resourceServer(rs -> rs.setEnabled(true)...)` — a `JwtTokenValidator`
  (from `juneau-rest-server-auth-jwt`) validates the bearer, and the RFC 9728 well-known metadata
  route is served automatically once RS auth is enabled. The client side uses
  `juneau-rest-client-mcp-auth`'s `McpTokenProvider.clientCredentials()` to acquire a token and
  `McpAuthInterceptor` (via `.interceptor(tokens.interceptor())`) to attach it to every request.
