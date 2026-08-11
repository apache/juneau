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
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.apache.juneau.BasicTestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the ergonomic client-side MRTR (SEP-2322) auto-resume helpers
 * {@link McpClient#callToolWithElicitation}, {@link McpClient#getPromptWithElicitation}, and
 * {@link McpClient#readResourceWithElicitation}: the caller supplies an
 * {@link McpElicitationHandler} and the client auto-detects each {@code input_required} pause, invokes the
 * handler for the round's requests, and re-issues the call with the collected {@code inputResponses} and carried
 * {@code requestState} until a terminal result is reached.
 */
@SuppressWarnings({
	"resource" // The Recorder HttpTransport test double (`t`) and the client(...) test-helper factory (@Owning; callers close via try-with-resources) are short-lived test fixtures.
})
class McpClient_Elicitation_Test extends TestBase {

	/**
	 * A stub transport that returns a fixed sequence of canned JSON-RPC wire responses (the last one repeats for
	 * any extra calls) while recording every request it received so a test can assert on the resume payload
	 * (echoed {@code inputResponses}/{@code requestState}).
	 */
	private static final class Recorder implements HttpTransport {
		private final List<String> responses;
		final List<JsonRpcRequest> requests = new ArrayList<>();
		private int i;

		Recorder(String... responses) {
			this.responses = List.of(responses);
		}

		@Override
		public TransportResponse execute(TransportRequest request) throws TransportException {
			try {
				var baos = new ByteArrayOutputStream();
				request.getBody().writeTo(baos);
				requests.add(JsonParser.DEFAULT.read(baos.toString(StandardCharsets.UTF_8), JsonRpcRequest.class));
			} catch (IOException e) {
				throw new TransportException("Failed reading stub request body.", e);
			}
			var wire = responses.get(Math.min(i, responses.size() - 1));
			i++;
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)))
				.build();
		}
	}

	private static McpClient client(HttpTransport transport) {
		return McpClient.builder().endpoint("http://x/mcp").transport(transport).build();
	}

	/** The wire params ({@code inputResponses}/{@code requestState}) the server saw on the Nth (0-based) request. */
	@SuppressWarnings("unchecked")
	private static Map<String,Object> paramsOf(Recorder r, int n) {
		return (Map<String,Object>) r.requests.get(n).getParams();
	}

	private static String inputRequired(String requestState, String... requestIds) {
		var sb = new StringBuilder();
		for (var id : requestIds) {
			if (!sb.isEmpty())
				sb.append(',');
			sb.append('"').append(id).append("\":{\"message\":\"Pick ").append(id).append("\",\"requestedSchema\":{\"type\":\"object\"}}");
		}
		return "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"inputRequests\":{" + sb + "},\"requestState\":\"" + requestState + "\",\"resultType\":\"input_required\"}}";
	}

	private static String completeTool(String text) {
		return "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}],\"resultType\":\"complete\"}}";
	}

	/** Answers every request in a round with ACCEPT + one content entry keyed by the request id. */
	private static McpElicitationHandler acceptAll() {
		return requests -> {
			var out = new LinkedHashMap<String,ElicitResult>();
			requests.forEach((id, req) -> out.put(id, new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", id + "-answer")));
			return out;
		};
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Happy paths
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_callTool_singleRound_handlerAnswers_terminalResultReturned() throws Exception {
		var t = new Recorder(inputRequired("tok1", "q1"), completeTool("done"));
		var seen = new AtomicReference<Map<String,ElicitRequest>>();
		try (var c = client(t)) {
			var result = c.callToolWithElicitation("ask", Map.of("k", "v"), requests -> {
				seen.set(requests);
				return acceptAll().elicit(requests);
			});
			assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
		}
		// Handler saw the one pending request; the resume call carried the answer + echoed the requestState.
		assertTrue(seen.get().containsKey("q1"));
		assertEquals("Pick q1", seen.get().get("q1").getMessage());
		assertEquals(2, t.requests.size());
		var resume = paramsOf(t, 1);
		assertEquals("tok1", resume.get("requestState"));
		assertTrue(((Map<?,?>) resume.get("inputResponses")).containsKey("q1"));
	}

	@Test void a02_callTool_multipleRequestsInOneRound_answeredTogether() throws Exception {
		var t = new Recorder(inputRequired("tok1", "q1", "q2"), completeTool("done"));
		var count = new AtomicInteger();
		try (var c = client(t)) {
			var result = c.callToolWithElicitation("ask", null, requests -> {
				count.incrementAndGet();
				assertEquals(2, requests.size());
				return acceptAll().elicit(requests);
			});
			assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
		}
		assertEquals(1, count.get());
		var responses = (Map<?,?>) paramsOf(t, 1).get("inputResponses");
		assertTrue(responses.containsKey("q1"));
		assertTrue(responses.containsKey("q2"));
	}

	@Test void a03_callTool_multipleSequentialRounds_handlerInvokedPerRound() throws Exception {
		var t = new Recorder(inputRequired("tok1", "q1"), inputRequired("tok2", "q2"), completeTool("done"));
		var rounds = new AtomicInteger();
		try (var c = client(t)) {
			var result = c.callToolWithElicitation("ask", null, requests -> {
				rounds.incrementAndGet();
				return acceptAll().elicit(requests);
			});
			assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
		}
		assertEquals(2, rounds.get());
		assertEquals(3, t.requests.size());
		// Each resume carried the requestState the immediately-preceding pause echoed.
		assertEquals("tok1", paramsOf(t, 1).get("requestState"));
		assertEquals("tok2", paramsOf(t, 2).get("requestState"));
	}

	@Test void a04_getPrompt_singleRound_terminalResultReturned() throws Exception {
		var complete = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"description\":\"done\",\"messages\":[],\"resultType\":\"complete\"}}";
		var t = new Recorder(inputRequired("tok1", "q1"), complete);
		try (var c = client(t)) {
			var result = c.getPromptWithElicitation("greet", null, acceptAll());
			assertEquals("done", result.getDescription());
		}
		assertEquals(2, t.requests.size());
	}

	@Test void a05_readResource_singleRound_terminalResultReturned() throws Exception {
		var complete = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"contents\":[{\"type\":\"resourceText\",\"uri\":\"file:///a\",\"text\":\"body\"}]}}";
		var t = new Recorder(inputRequired("tok1", "q1"), complete);
		try (var c = client(t)) {
			var result = c.readResourceWithElicitation("file:///a", acceptAll());
			assertEquals(1, result.getContents().size());
		}
		assertEquals(2, t.requests.size());
	}

	@Test void a06_callTool_noPause_terminalResultReturnedWithoutInvokingHandler() throws Exception {
		var t = new Recorder(completeTool("done"));
		var invoked = Flag.create();
		try (var c = client(t)) {
			var result = c.callToolWithElicitation("ask", null, requests -> {
				invoked.set();
				return acceptAll().elicit(requests);
			});
			assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
		}
		assertFalse(invoked.isSet());
		assertEquals(1, t.requests.size());
	}

	@Test void a07_callTool_incompleteAnswerInRound_onlyProvidedIdEchoedAndLoopProceeds() throws Exception {
		// Handler answers only q1 of the two requested ids - legitimate (see McpElicitationHandler#elicit): the
		// resume call must carry only the id the handler actually answered, not a fabricated answer for q2.
		var t = new Recorder(inputRequired("tok1", "q1", "q2"), completeTool("done"));
		try (var c = client(t)) {
			var result = c.callToolWithElicitation("ask", null, requests -> {
				assertEquals(2, requests.size());
				var out = new LinkedHashMap<String,ElicitResult>();
				out.put("q1", new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", "q1-answer"));
				return out;
			});
			assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
		}
		assertEquals(2, t.requests.size());
		var responses = (Map<?,?>) paramsOf(t, 1).get("inputResponses");
		assertEquals(1, responses.size());
		assertTrue(responses.containsKey("q1"));
		assertFalse(responses.containsKey("q2"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Decline / cancel
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_callTool_handlerDeclines_answerSentAndTerminalReturned() throws Exception {
		var t = new Recorder(inputRequired("tok1", "q1"), completeTool("declined-path"));
		try (var c = client(t)) {
			var result = c.callToolWithElicitation("ask", null,
				requests -> Map.of("q1", new ElicitResult().setAction(ElicitAction.DECLINE)));
			assertEquals("declined-path", ((TextContent) result.getContent().get(0)).getText());
		}
		// The decline was echoed back to the server as a real inputResponses answer (the server, not the client,
		// decides the terminal outcome of a declined elicitation).
		var responses = (Map<?,?>) paramsOf(t, 1).get("inputResponses");
		var q1 = (Map<?,?>) responses.get("q1");
		assertEquals("decline", q1.get("action"));
	}

	@Test void b02_callTool_handlerCancels_answerSentAndTerminalReturned() throws Exception {
		var t = new Recorder(inputRequired("tok1", "q1"), completeTool("cancelled-path"));
		try (var c = client(t)) {
			var result = c.callToolWithElicitation("ask", null,
				requests -> Map.of("q1", new ElicitResult().setAction(ElicitAction.CANCEL)));
			assertEquals("cancelled-path", ((TextContent) result.getContent().get(0)).getText());
		}
		var responses = (Map<?,?>) paramsOf(t, 1).get("inputResponses");
		var q1 = (Map<?,?>) responses.get("q1");
		assertEquals("cancel", q1.get("action"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Bounded loop guard
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_callTool_maxRoundsExceeded_throwsTypedException() throws Exception {
		// Server never terminates: every response is another input_required pause.
		var t = new Recorder(inputRequired("tok", "q1"));
		try (var c = client(t)) {
			var handler = acceptAll();
			var e = assertThrows(McpElicitationLimitException.class,
				() -> c.callToolWithElicitation("ask", null, handler, 2));
			assertEquals(2, e.getMaxRounds());
		}
		// 1 initial call + exactly maxRounds resume attempts, then the guard trips before a further re-issue.
		assertEquals(3, t.requests.size());
	}

	@Test void c02_callTool_defaultMaxRoundsGuardsRunawayServer() throws Exception {
		var t = new Recorder(inputRequired("tok", "q1"));
		try (var c = client(t)) {
			var handler = acceptAll();
			assertThrows(McpElicitationLimitException.class, () -> c.callToolWithElicitation("ask", null, handler));
		}
		// Bounded by the default, not infinite: 1 initial + DEFAULT_MAX_ELICITATION_ROUNDS resume attempts.
		assertEquals(1 + McpClient.DEFAULT_MAX_ELICITATION_ROUNDS, t.requests.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Argument guards
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_nullHandlerThrows() throws Exception {
		try (var c = client(new Recorder(completeTool("done")))) {
			assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'handler' cannot be null.",
				() -> c.callToolWithElicitation("ask", null, null));
		}
	}

	@Test void d02_nonPositiveMaxRoundsThrows() throws Exception {
		try (var c = client(new Recorder(completeTool("done")))) {
			var handler = acceptAll();
			assertThrowsWithMessage(IllegalArgumentException.class, "maxRounds must be >= 1 (was 0).",
				() -> c.callToolWithElicitation("ask", null, handler, 0));
			assertThrowsWithMessage(IllegalArgumentException.class, "maxRounds must be >= 1 (was -1).",
				() -> c.callToolWithElicitation("ask", null, handler, -1));
		}
	}

	@Test void d03_handlerReturnsNullResultThrows() throws Exception {
		var t = new Recorder(inputRequired("tok1", "q1"), completeTool("done"));
		try (var c = client(t)) {
			assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'handler result' cannot be null.",
				() -> c.callToolWithElicitation("ask", null, requests -> null));
		}
	}

	@Test void d04_handlerReturnsMapWithNullValueForRequestedIdThrows() throws Exception {
		var t = new Recorder(inputRequired("tok1", "q1"), completeTool("done"));
		try (var c = client(t)) {
			assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'results[q1]' cannot be null.",
				() -> c.callToolWithElicitation("ask", null, requests -> {
					var out = new LinkedHashMap<String,ElicitResult>();
					out.put("q1", null);
					return out;
				}));
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Escape hatch: manual callRaw resume still works unchanged alongside the ergonomic helper
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_manualCallRawResume_stillWorks() throws Exception {
		var t = new Recorder(inputRequired("tok1", "q1"), completeTool("done"));
		try (var c = client(t)) {
			var raw = c.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("ask"));
			assertTrue(ElicitationRequests.isInputRequired(raw));
			var state = ElicitationRequests.requestState(raw);
			var answers = ElicitationResponses.toInputResponse("q1",
				new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("choice", "x"));
			var resume = c.callRaw(McpMethods.TOOLS_CALL,
				new CallToolRequest().setName("ask").setInputResponses(answers).setRequestState(state));
			assertEquals("complete", resume.get("resultType"));
		}
	}
}
