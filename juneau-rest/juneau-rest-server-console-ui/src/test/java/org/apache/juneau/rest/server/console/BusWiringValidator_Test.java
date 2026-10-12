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
package org.apache.juneau.rest.server.console;

import static java.nio.charset.StandardCharsets.*;
import static java.util.stream.Collectors.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Unit coverage for {@link BusWiringValidator} and the {@link CardTypeHandler} bus defaults
 * (spec §5.4, §5.5, §6.1, §6.2, §11.2).  The full rule matrix, with the real {@code datatables} handler, is the shared
 * {@code bus-wiring-corpus.json}, run by {@code $CUIF}'s {@code BusWiring_Corpus_Test}.  This class pins what the
 * corpus cannot: the defaults, {@code Problem}, the helpers, and the registry hooks driven by stub types.
 */
class BusWiringValidator_Test extends TestBase {

	/** A stub adopter type: {@code roles}/{@code ops} of null mean "unknown to Java". */
	private record Stub(String type, Set<String> roles, Set<String> ops, List<String> publishes) implements CardTypeHandler {
		@Override public JsonMap toFragment(CardSource source) { return new JsonMap(); }
		@Override public List<String> implicitTopics(JsonMap card) { return publishes; }
		@Override public Set<String> acceptedRoles() { return roles; }
		@Override public Set<String> acceptedOps() { return ops; }
	}

	private static final CardTypeRegistry REGISTRY = CardTypeRegistry.standard().copy()
		.add(new Stub("grid", Set.of("highlight"), Set.of("reload"), List.of("grid.picked")))
		.add(new Stub("meter", null, null, List.of()))
		.build();

	private static List<String> problems(String contract) throws Exception {
		return BusWiringValidator.validate(JsonMap.ofString(contract), REGISTRY).stream().map(Object::toString).toList();
	}

	private static String page(String rest) {
		return "{\"version\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[]," + rest + "}";
	}

	@Test void a01_defaults() {
		var bare = new CardTypeHandler() {
			@Override public String type() { return "bare"; }
			@Override public JsonMap toFragment(CardSource source) { return new JsonMap(); }
		};
		assertEquals(List.of(), bare.implicitTopics(new JsonMap()));
		assertNull(bare.acceptedRoles());
		assertNull(bare.acceptedOps());
		var html = new HtmlCardType();
		assertEquals(List.of(), html.implicitTopics(JsonMap.of("id", "a", "type", "html")));
		assertEquals(Set.of(), html.acceptedRoles());
		assertEquals(Set.of(), html.acceptedOps());
	}

	@Test void a02_validFixtureHasNoProblems() throws Exception {
		try (var in = getClass().getResourceAsStream("/contracts/valid-bus-wiring.json")) {
			var json = new String(in.readAllBytes(), UTF_8);
			assertEquals(List.of(), BusWiringValidator.validate(JsonMap.ofString(json), CardTypeRegistry.standard()));
		}
	}

	@Test void a03_problemToString() {
		var p = new BusWiringValidator.Problem("E-52", "topic 'ops.jobs' is declared publisher=server but no bridge carries it downstream");
		assertEquals("E-52 topic 'ops.jobs' is declared publisher=server but no bridge carries it downstream", p.toString());
	}

	@Test void a04_stubTypePublishesAndRoles() throws Exception {
		assertEquals(List.of(
			"E-44 card 'b' (type 'grid') has no role 'sparkle'; accepted: highlight, params, refresh",
			"E-41 card 'b' subscribes to 'grid.pick' but nothing publishes it; did you mean 'grid.picked'?"
		), problems(page("\"cards\":["
			+ "{\"id\":\"a\",\"type\":\"grid\"},"
			+ "{\"id\":\"b\",\"type\":\"grid\",\"subscribes\":["
			+ "{\"topic\":\"grid.picked\",\"as\":\"highlight\"},"
			+ "{\"topic\":\"card:a\",\"as\":\"sparkle\"},"
			+ "{\"topic\":\"grid.pick\",\"as\":\"refresh\"}]},"
			+ "{\"id\":\"m\",\"type\":\"meter\",\"subscribes\":[{\"topic\":\"card:a\",\"as\":\"anything\"}]}]")));
	}

	@Test void a05_ribbonTargetUsesAcceptedOps() throws Exception {
		assertEquals(List.of(
			"E-43 card 'g' (type 'grid') does not handle op 'collapse-all'"
		), problems(page("\"cards\":["
			+ "{\"id\":\"g\",\"type\":\"grid\"},"
			+ "{\"id\":\"m\",\"type\":\"meter\"},"
			+ "{\"id\":\"t\",\"type\":\"x-table\",\"ribbon\":["
			+ "{\"type\":\"refresh\",\"target\":\"g\"},"
			+ "{\"type\":\"collapseAll\",\"target\":\"g\"},"
			+ "{\"type\":\"collapseAll\",\"target\":\"m\"}]}]")));
	}

	@Test void a06_unregisteredTypeIsSkipped() throws Exception {
		assertEquals(List.of(), problems(page("\"cards\":["
			+ "{\"id\":\"a\",\"type\":\"chart\"},"
			+ "{\"id\":\"b\",\"type\":\"chart\",\"subscribes\":[{\"topic\":\"filter:a\",\"as\":\"filter\"}]}]")));
	}

	@Test void a07_helpers() {
		assertEquals(0, BusWiringValidator.distance("app.a", "app.a"));
		assertEquals(2, BusWiringValidator.distance("app.region-pick", "app.region-picked"));
		assertTrue(BusWiringValidator.topicMatches("ssc.alert:*", "ssc.alert:disk"));
		assertFalse(BusWiringValidator.topicMatches("ssc.alert:*", "ssc.alert"));
		assertFalse(BusWiringValidator.topicMatches("ssc.alert:disk", "ssc.alert:*"));
	}

	@Test void a08_everyProblemIsCollected() throws Exception {
		var ps = problems(page("\"topics\":[{\"topic\":\"ops.jobs\",\"retain\":true,\"publisher\":\"server\"}],"
			+ "\"cards\":[{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"src\":\"/x/{id}\",\"subscribes\":["
			+ "{\"topic\":\"card:h\",\"as\":\"refresh\"},"
			+ "{\"topic\":\"card:gone\",\"as\":\"params\"}]}]"));
		assertEquals(List.of(
			"E-49 card 'h' subscribes to its own topic 'card:h'; use ctx.subscribe with {echo:true} in JS if this is intended",
			"E-42 topic 'card:gone' names card 'gone', which is not on this page",
			"E-47 card 'h': params subscription needs map",
			"E-47 card 'h': token '{id}' in src is not mapped by any params subscription",
			"E-52 topic 'ops.jobs' is declared publisher=server but no bridge carries it downstream"
		), ps);
	}

	@Test void a08b_misspelledSelectionTopicIsE42() throws Exception {
		// The browser defers this check to the server on purpose (juneau-bus.js), so it is pinned here.
		var ps = problems(page("\"cards\":["
			+ "{\"id\":\"changes\",\"type\":\"html\",\"template\":\"c\"},"
			+ "{\"id\":\"tasks\",\"type\":\"html\",\"template\":\"t\",\"src\":\"/t/{changeId}\",\"subscribes\":["
			+ "{\"topic\":\"selection:chanegs\",\"as\":\"params\",\"map\":{\"changeId\":\"ids.0\"}}]}]"));
		assertEquals(List.of("E-42 topic 'selection:chanegs' names card 'chanegs', which is not on this page"), ps.stream().map(x -> x.split("; ")[0]).filter(x -> x.startsWith("E-42")).toList());
	}

	@Test void a09_schemaInvalidWiringDoesNotThrow() throws Exception {
		// A topics entry with no publisher, carried by a bridge and a ribbon publish item.
		assertDoesNotThrow(() -> problems(page("\"topics\":[{\"topic\":\"app.a\",\"retain\":true}],"
			+ "\"bridges\":[{\"id\":\"b\",\"transport\":\"websocket\",\"session\":\"/s\",\"downstream\":[\"app.a\"],\"upstream\":[\"app.a\"]}],"
			+ "\"cards\":[{\"id\":\"t\",\"type\":\"x-table\",\"ribbon\":[{\"type\":\"publish\",\"title\":\"Go\",\"topic\":\"app.a\"}]}]")));
		// A ribbon item with a target but no type.
		assertDoesNotThrow(() -> problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"x-table\",\"ribbon\":[{\"target\":\"t\"}]}]")));
	}

	@Test void a10_retainConflictBetweenCards() throws Exception {
		assertEquals(List.of(
			"E-46 topic 'app.focus' is declared with retain=true here and retain=false at card 'a'"
		), problems(page("\"cards\":["
			+ "{\"id\":\"a\",\"type\":\"html\",\"template\":\"a\",\"publishes\":[{\"topic\":\"app.focus\",\"retain\":false}]},"
			+ "{\"id\":\"b\",\"type\":\"html\",\"template\":\"b\",\"publishes\":[{\"topic\":\"app.focus\",\"retain\":true}]}]")));
	}

	@Test void a11_bridgeSessionAndMaxAttemptsEdges() throws Exception {
		assertEquals(List.of(
			"E-51 bridge 'b': session must be a same-origin path, got '/\\x'",
			"E-51 bridge 'b': maxAttempts must be an integer of at least 1"
		), problems(page("\"bridges\":[{\"id\":\"b\",\"transport\":\"sse\",\"session\":\"/\\\\x\",\"downstream\":[],\"maxAttempts\":2.5}],\"cards\":[]")));
	}

	@Test void a12_jobTopicWithoutBridge() throws Exception {
		assertEquals(List.of(
			"E-52 topic 'job:*' is declared publisher=server but no bridge carries it downstream"
		), problems(page("\"topics\":[{\"topic\":\"job:*\",\"retain\":true,\"publisher\":\"server\"}],\"cards\":[]")));
	}

	private static final String TERM_TOPICS = "\"topics\":[{\"topic\":\"term.input-status\",\"retain\":false,\"publisher\":\"server\"}]";
	private static final String TERM_BRIDGES = "\"bridges\":[{\"id\":\"term\",\"transport\":\"websocket\",\"session\":\"/c/juneau-bus/session\","
		+ "\"downstream\":[\"term.input-status\"],\"upstream\":[\"term.input\"]}]";
	private static final String TERM_PUBLISHES = "\"publishes\":[{\"topic\":\"term.input\",\"retain\":false}]";
	private static final String TERM_SUBSCRIBES = "\"subscribes\":[{\"topic\":\"term.input-status\",\"as\":\"input-status\"}]";

	/** A page with one terminal card with input on; each argument is the wiring fragment to include (null to omit). */
	private static String terminalPage(String topics, String bridges, String publishes, String subscribes) {
		var card = Stream.of("\"id\":\"term\",\"type\":\"terminal\",\"terminal\":{\"bytesUrl\":\"/t/juneau-terminal/t1/bytes\",\"input\":true,\"inputId\":\"t1\"}", publishes, subscribes)
			.filter(Objects::nonNull).collect(joining(","));
		return page(Stream.of(topics, bridges, "\"cards\":[{" + card + "}]").filter(Objects::nonNull).collect(joining(",")));
	}

	private static List<String> e58(List<String> problems) {
		return problems.stream().filter(x -> x.startsWith("E-58")).toList();
	}

	@Test void a13_terminalInputFullyWiredHasNoProblems() throws Exception {
		assertEquals(List.of(), problems(terminalPage(TERM_TOPICS, TERM_BRIDGES, TERM_PUBLISHES, TERM_SUBSCRIBES)));
	}

	@Test void a14_terminalInputMissingPublish() throws Exception {
		assertEquals(List.of("E-58 terminal card 'term' has input: true but its publishes lacks 'term.input'"),
			e58(problems(terminalPage(TERM_TOPICS, TERM_BRIDGES, null, TERM_SUBSCRIBES))));
	}

	@Test void a15_terminalInputMissingSubscribe() throws Exception {
		assertEquals(List.of("E-58 terminal card 'term' has input: true but its subscribes lacks 'term.input-status' as 'input-status'"),
			e58(problems(terminalPage(TERM_TOPICS, TERM_BRIDGES, TERM_PUBLISHES, null))));
	}

	@Test void a16_terminalInputMissingServerTopic() throws Exception {
		assertEquals(List.of("E-58 terminal card 'term' has input: true but no topics entry declares 'term.input-status' with publisher=server"),
			e58(problems(terminalPage(null, TERM_BRIDGES, TERM_PUBLISHES, TERM_SUBSCRIBES))));
	}

	@Test void a17_terminalInputMissingBridge() throws Exception {
		assertEquals(List.of("E-58 terminal card 'term' has input: true but no websocket bridge carries 'term.input' upstream and 'term.input-status' downstream"),
			e58(problems(terminalPage(TERM_TOPICS, null, TERM_PUBLISHES, TERM_SUBSCRIBES))));
	}

	@Test void a18_terminalInputOffIsNotChecked() throws Exception {
		assertEquals(List.of(), problems(page("\"cards\":["
			+ "{\"id\":\"a\",\"type\":\"terminal\",\"terminal\":{\"bytesUrl\":\"/b\"}},"
			+ "{\"id\":\"b\",\"type\":\"terminal\",\"terminal\":{\"bytesUrl\":\"/b\",\"input\":false}}]")));
	}

	@Test void a19_nonTerminalCardWithTerminalMapIsNotChecked() throws Exception {
		assertEquals(List.of(), problems(page("\"cards\":["
			+ "{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"terminal\":{\"input\":true}}]")));
	}
}
