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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.rest.client.mcp.v20260728.*;
import org.junit.jupiter.api.*;

/**
 * Proves the example actually works: boots {@link ExampleServer} in-process on an ephemeral port and drives
 * the real {@link McpClient} through every surface the example demonstrates &mdash; discovery, listing, a
 * tool call, a templated resource read, a completion, a prompt, a subscription change notification, and an
 * elicitation (MRTR) round-trip &mdash; asserting the observable outcome of each.
 *
 * <p>
 * It reuses {@link ExampleClient#connect(String)} so the exact client wiring the walkthrough uses is what is
 * being tested. Each test publishes its own distinctly-titled note (rather than relying on notes published by
 * other tests), so tests remain independent even though the server/client pair is shared across the class.
 */
class ExampleMcpEndToEnd_Test extends TestBase {

	private static ExampleServer server;
	private static McpClient client;

	@BeforeAll
	static void setUp() throws Exception {
		server = ExampleServer.start(0);
		client = ExampleClient.connect(server.getRootUrl().toString());
	}

	@AfterAll
	static void tearDown() throws Exception {
		if (client != null)
			client.close();
		if (server != null)
			server.close();
	}

	// -------- server/discover --------

	@Test
	void a01_discover_reportsServerIdentityAndCapabilities() {
		var discovered = client.discoveredServer();
		assertBean(discovered, "serverInfo{name,version},capabilities{resources{subscribe}}",
			"{juneau-notes-example,1.0.0},{{true}}");
	}

	@Test
	void a02_discover_reportsInstructions() {
		// HIGH-1 regression guard: instructions are configured via McpOptions (createMcpOptions()), not
		// McpServerConfig; on revision 2026-07-28 the latter is silently ignored for server/discover.
		var discovered = client.discoveredServer();
		assertEquals("A tiny notes service. Use 'publishNote' to add notes, read them via the "
			+ "'note:///{title}' resource template, subscribe for change notifications, and try "
			+ "'deleteNote' to see an elicitation (confirm) round-trip.", discovered.getInstructions());
	}

	// -------- listing --------

	@Test
	void b01_listTools_exposesRegisteredTools() throws Exception {
		assertBean(client.listTools(), "tools{#{name}}", "{[{publishNote},{deleteNote}]}");
	}

	@Test
	void b02_listPrompts_exposesRegisteredPrompts() throws Exception {
		assertBean(client.listPrompts(), "prompts{#{name}}", "{[{summarize}]}");
	}

	@Test
	void b03_listResources_exposesRegisteredResources() throws Exception {
		assertBean(client.listResources(), "resources{#{name}}", "{[{note-index}]}");
	}

	@Test
	void b04_listResourceTemplates_exposesRegisteredTemplate() throws Exception {
		var templates = client.listResourceTemplates().getResourceTemplates();
		assertEquals(1, templates.size());
		assertBean(templates.get(0), "name,uriTemplate", "note,note:///{title}");
	}

	// -------- tool call + resource read --------

	@Test
	void c01_publishThenReadViaTemplate_roundTrips() throws Exception {
		var stored = client.callTool("publishNote", Map.of("title", "groceries", "body", "Milk, eggs, bread"));
		assertEquals("Stored note 'groceries' (17 chars).", stored.firstText());

		var read = client.readResource(NoteStore.uriFor("groceries"));
		var contents = read.getContents();
		assertEquals(1, contents.size());
		assertEquals("Milk, eggs, bread", ((TextResourceContents)contents.get(0)).getText());
	}

	@Test
	void c02_readIndex_listsPublishedNoteTitle() throws Exception {
		client.callTool("publishNote", Map.of("title", "index-check", "body", "x"));
		var contents = client.readResource(NoteStore.SCHEME + "index").getContents();
		assertEquals(1, contents.size());
		assertTrue(((TextResourceContents)contents.get(0)).getText().contains("index-check"));
	}

	// -------- completion --------

	@Test
	void d01_completion_suggestsMatchingNoteTitles() throws Exception {
		client.callTool("publishNote", Map.of("title", "groceries-for-completion", "body", "x"));
		var ref = new ResourceTemplateReference().setUri(NoteStore.SCHEME + "{title}");
		var completion = client.complete(ref, "title", "groceries-for-completion", null);
		assertEquals(List.of("groceries-for-completion"), completion.getCompletion().getValues());
	}

	// -------- prompt --------

	@Test
	void e01_prompt_rendersMessageFromStoredNote() throws Exception {
		client.callTool("publishNote", Map.of("title", "prompt-note", "body", "Milk, eggs, bread"));
		var prompt = client.getPrompt("summarize", Map.of("title", "prompt-note"));
		var messages = prompt.getMessages();
		assertEquals(1, messages.size());
		assertEquals(Role.USER, messages.get(0).getRole());
		assertTrue(((TextContent)messages.get(0).getContent()).getText().contains("Milk, eggs, bread"));
	}

	// -------- subscription --------

	@Test
	void f01_subscription_deliversResourceUpdatedOnPublish() throws Exception {
		var noteUri = NoteStore.uriFor("subscribed");
		var updates = new LinkedBlockingQueue<String>();
		var acknowledged = new CountDownLatch(1);

		var handle = client.listen(
			new SubscriptionFilter().setResourceSubscriptions(List.of(noteUri)),
			new McpSubscriptionListener() {
				@Override public void onAcknowledged(SubscriptionFilter honoredFilter) { acknowledged.countDown(); }
				@Override public void onResourceUpdated(String uri) { updates.add(uri); }
			});
		try {
			assertTrue(acknowledged.await(15, TimeUnit.SECONDS), "subscription was not acknowledged");
			client.callTool("publishNote", Map.of("title", "subscribed", "body", "hi"));
			assertEquals(noteUri, updates.poll(15, TimeUnit.SECONDS));
		} finally {
			handle.close();
		}
	}

	// -------- deleteNote elicitation (MRTR) --------

	@Test
	void g01_deleteWithElicitation_confirmsThenDeletes() throws Exception {
		client.callTool("publishNote", Map.of("title", "trash", "body", "delete me"));

		var result = client.callToolWithElicitation("deleteNote", Map.of("title", "trash"), requests -> {
			var answers = new LinkedHashMap<String,ElicitResult>();
			requests.keySet().forEach(id -> answers.put(id,
				new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("confirm", true)));
			return answers;
		});
		assertEquals("Deleted note 'trash'.", result.firstText());

		// And it is really gone from the index.
		var contents = client.readResource(NoteStore.SCHEME + "index").getContents();
		assertFalse(((TextResourceContents)contents.get(0)).getText().contains("trash"));
	}

	@Test
	void g02_deleteWithElicitation_declineKeepsNote() throws Exception {
		client.callTool("publishNote", Map.of("title", "keep", "body", "keep me"));

		var result = client.callToolWithElicitation("deleteNote", Map.of("title", "keep"), requests -> {
			var answers = new LinkedHashMap<String,ElicitResult>();
			requests.keySet().forEach(id -> answers.put(id, new ElicitResult().setAction(ElicitAction.DECLINE)));
			return answers;
		});
		assertEquals("Deletion of 'keep' was cancelled.", result.firstText());

		var contents = client.readResource(NoteStore.SCHEME + "index").getContents();
		assertTrue(((TextResourceContents)contents.get(0)).getText().contains("keep"));
	}
}
