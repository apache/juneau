/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.examples.mcp;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.client.mcp.v20260728.*;

/**
 * A guided, end-to-end walkthrough of every {@link ExampleMcpServer} surface using the v2
 * {@link McpClient}.
 *
 * <p>
 * Run {@link #main(String[]) main} (optionally passing the server endpoint; defaults to
 * {@code http://localhost:5000/}) after starting {@link ExampleServer}. Each numbered step prints
 * what it is doing and what it got back, so the transcript reads top-to-bottom like a tutorial.
 *
 * <p>
 * The client advertises the {@code elicitation} capability, which the server requires before it will
 * pause a call for input; without it the {@code deleteNote} elicitation step would be rejected.
 *
 * <p>
 * <b>Console output caveat:</b> every printed line below is produced by re-serializing the already-parsed
 * response bean with {@code Json.of(...)}, purely for readable console output. That re-serialization drops
 * the polymorphic content-block {@code "type"} discriminator (e.g. {@code "text"}/{@code "audio"}) the real
 * wire format carries, so what you see here is not byte-for-byte what came off the wire.
 */
@SuppressWarnings({
	"java:S106" // Example walkthrough intentionally prints to stdout; console output is the demo's deliverable.
})
public final class ExampleClient {

	/** The {@code title} argument/variable name shared by the tool, prompt, and resource-template calls below. */
	private static final String TITLE_ARG = "title";

	/** The sample note title used throughout this walkthrough. */
	private static final String GROCERIES_NOTE_TITLE = "groceries";

	private ExampleClient() {}

	/**
	 * Runs the walkthrough against a running {@link ExampleServer}.
	 *
	 * @param args Optional single argument: the server endpoint (defaults to {@code http://localhost:5000/}).
	 * @throws Exception If any step fails.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - example main() kept simple for demo readability
	})
	public static void main(String[] args) throws Exception {
		var endpoint = args.length > 0 ? args[0] : "http://localhost:" + ExampleServer.DEFAULT_PORT + "/";
		try (var client = connect(endpoint)) {
			run(client);
		}
	}

	/**
	 * Builds and connects a client that advertises the elicitation capability.
	 *
	 * @param endpoint The MCP server endpoint URL.
	 * @return A connected client (its mandatory {@code server/discover} handshake already done).
	 * @throws IOException If the connection or handshake fails.
	 */
	@SuppressWarnings("resource") // returned client is owned and closed by the caller (see main() above).
	public static McpClient connect(String endpoint) throws IOException {
		return McpClient.connect(McpClient.builder()
			.endpoint(endpoint)
			.clientInfo(new Implementation().setName("juneau-notes-example-client").setVersion("1.0.0"))
			// Required for the deleteNote elicitation step: the server only pauses for input when the
			// client says it can answer.
			.clientCapabilities(new ClientCapabilities().setElicitation(new ElicitationCapability())));
	}

	/**
	 * Executes each numbered step of the walkthrough against an already-connected client.
	 *
	 * @param client The connected MCP client.
	 * @throws Exception If any step fails.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - example walkthrough kept simple for demo readability
	})
	public static void run(McpClient client) throws Exception {

		section("1. server/discover — who are we talking to?");
		var discovered = client.discoveredServer();
		System.out.println("   server info:  " + Json.of(discovered.getServerInfo()));
		System.out.println("   versions:     " + Json.of(discovered.getSupportedVersions()));
		System.out.println("   capabilities: " + Json.of(discovered.getCapabilities()));
		System.out.println("   instructions: " + discovered.getInstructions());

		section("2. Discovery — list every advertised surface");
		System.out.println("   tools:             " + Json.of(client.listTools().getTools()));
		System.out.println("   prompts:           " + Json.of(client.listPrompts().getPrompts()));
		System.out.println("   resources:         " + Json.of(client.listResources().getResources()));
		System.out.println("   resourceTemplates: " + Json.of(client.listResourceTemplates().getResourceTemplates()));

		section("3. tools/call publishNote — store a note (and notify subscribers)");
		System.out.println("   -> " + client.callToolText("publishNote", Map.of(TITLE_ARG, GROCERIES_NOTE_TITLE, "body", "Milk, eggs, bread")));

		section("4. resources/read note:///groceries — read it back via the template");
		var read = client.readResource(NoteStore.uriFor(GROCERIES_NOTE_TITLE));
		System.out.println("   " + Json.of(read.getContents()));

		section("5. resources/read note:///index — the fixed index resource");
		System.out.println("   " + Json.of(client.readResource(NoteStore.SCHEME + "index").getContents()));

		section("6. completion/complete — complete the template's {title} variable for prefix 'gr'");
		var ref = new ResourceTemplateReference().setUri(NoteStore.SCHEME + "{title}");
		var completion = client.complete(ref, TITLE_ARG, "gr", null);
		System.out.println("   suggestions: " + Json.of(completion.getCompletion()));

		section("7. prompts/get summarize — render a prompt from the stored note");
		var prompt = client.getPrompt("summarize", Map.of(TITLE_ARG, GROCERIES_NOTE_TITLE));
		System.out.println("   " + Json.of(prompt.getMessages()));

		section("8. subscriptions/listen — receive a live change notification");
		runSubscriptionDemo(client);

		section("9. tools/call deleteNote — an elicitation (confirm) round-trip, auto-answered");
		var deleted = client.callToolWithElicitation("deleteNote", Map.of(TITLE_ARG, GROCERIES_NOTE_TITLE), requests -> {
			// The server asked one or more questions; answer each with ACCEPT + confirm=true. A real client
			// would present these to a user (the schema in each request says how to render the control).
			System.out.println("   server asked: " + Json.of(requests));
			var answers = new LinkedHashMap<String,ElicitResult>();
			requests.keySet().forEach(id -> answers.put(id,
				new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("confirm", true)));
			return answers;
		});
		System.out.println("   -> " + deleted.firstText());

		section("10. resources/read note:///index — confirm the note is gone");
		System.out.println("   " + Json.of(client.readResource(NoteStore.SCHEME + "index").getContents()));

		System.out.println("\nWalkthrough complete.");
	}

	/**
	 * Subscribes for changes to a note URI, publishes that note, and prints the change frame that arrives
	 * over the held-open SSE stream.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - example walkthrough kept simple for demo readability
	})
	private static void runSubscriptionDemo(McpClient client) throws Exception {
		var noteUri = NoteStore.uriFor("todo");
		var updates = new LinkedBlockingQueue<String>();
		var acknowledged = new CountDownLatch(1);

		try (var handle = client.listen(
			new SubscriptionFilter().setResourceSubscriptions(List.of(noteUri)),
			new McpSubscriptionListener() {
				@Override public void onAcknowledged(SubscriptionFilter honoredFilter) { acknowledged.countDown(); }
				@Override public void onResourceUpdated(String uri) { updates.add(uri); }
				@Override public void onError(Throwable t) { System.out.println("   subscription error: " + t); }
			})) {
			if (! acknowledged.await(10, TimeUnit.SECONDS))
				System.out.println("   (warning: subscription was not acknowledged in time)");
			else
				System.out.println("   subscription acknowledged; publishing '" + noteUri + "' ...");

			client.callTool("publishNote", Map.of(TITLE_ARG, "todo", "body", "Write MCP example"));

			var updatedUri = updates.poll(10, TimeUnit.SECONDS);
			if (updatedUri == null)
				System.out.println("   (warning: no resources/updated notification arrived in time)");
			else
				System.out.println("   -> received resources/updated for: " + updatedUri);
		}
	}

	private static void section(String title) {
		System.out.println("\n=== " + title + " ===");
	}
}
