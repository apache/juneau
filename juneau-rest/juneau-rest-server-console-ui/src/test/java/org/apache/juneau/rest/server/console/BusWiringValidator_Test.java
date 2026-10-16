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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Unit coverage for {@link BusWiringValidator} and the {@link CardTypeHandler} bus defaults
 *.  The full rule matrix, with the real {@code datatables} handler, is the shared
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

	// ---- chart wiring rules (E-72..E-77), target lookups ---------------------------------------------------------

	// juneau-charts.js ships from this module, so the path below is relative to the console-ui module root.
	@Test void e01_extensionNamePattern_matchesJs() throws Exception {
		var js = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir", "."),
			"src/main/resources/org/apache/juneau/console/juneau-charts.js"), UTF_8);
		assertContains("const EXTENSION_NAME_RE = /" + BusWiringValidator.EXTENSION_NAME.pattern() + "/;", js);
	}

	@Test void e02_filterSectionOwnsFilterTopic() throws Exception {
		assertEquals(List.of(), problems(page("\"dashboard\":{\"rail\":{\"sections\":[{\"id\":\"range\"}]}},\"cards\":["
			+ "{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"subscribes\":[{\"topic\":\"filter:range\",\"as\":\"refresh\"}]}]")));
		assertEquals(List.of("E-42 topic 'filter:gone' names card 'gone', which is not on this page"),
			problems(page("\"cards\":[{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"subscribes\":[{\"topic\":\"filter:gone\",\"as\":\"refresh\"}]}]")));
	}

	@Test void e03_cmdRibbonTargetsDataSource() throws Exception {
		var ribbon = "\"ribbon\":[{\"type\":\"publish\",\"title\":\"R\",\"topic\":\"cmd:%s\"}]";
		assertEquals(List.of(), problems(page("\"dataSources\":[{\"id\":\"orders\"}],\"cards\":["
			+ "{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\"," + ribbon.formatted("orders") + "}]")));
		assertEquals(List.of("E-42 topic 'cmd:gone' names card 'gone', which is not on this page"),
			problems(page("\"dataSources\":[{\"id\":\"orders\"}],\"cards\":["
				+ "{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\"," + ribbon.formatted("gone") + "}]")));
		// the ribbon "target" path (R-E3a) consults the same set
		assertEquals(List.of(), problems(page("\"dataSources\":[{\"id\":\"orders\"}],\"cards\":["
			+ "{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"ribbon\":[{\"type\":\"refresh\",\"target\":\"orders\"}]}]")));
	}

	@Test void e04_fnNestedInOption_isCheckedForName() throws Exception {
		assertEquals(List.of("E-74 'Bad' is not a valid $fn name"),
			problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\",\"option\":{\"series\":[{\"label\":{\"formatter\":{\"$fn\":\"Bad\"}}}]}}]")));
		assertEquals(List.of(), problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\","
			+ "\"option\":{\"series\":[{\"label\":{\"formatter\":{\"$fn\":\"compact\"}}}]}}]")));
	}

	@Test void e05_fnInHelper_isCheckedForName() throws Exception {
		assertEquals(List.of("E-74 'Bad' is not a valid $fn name"),
			problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\",\"helper\":{\"kind\":\"scatter\",\"symbolSize\":{\"$fn\":\"Bad\"}}}]")));
		assertEquals(List.of(), problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\","
			+ "\"helper\":{\"kind\":\"scatter\",\"symbolSize\":{\"$fn\":\"symbolSizeBy\"}}}]")));
		// a legacy flat card carries the helper keys at top level
		assertEquals(List.of("E-74 'Bad' is not a valid $fn name"),
			problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\",\"kind\":\"line\",\"symbolSize\":{\"$fn\":\"Bad\"}}]")));
	}

	// The legacy flat walk matches ChartCardType: the helper keys are walked as one map (so series[*].data is skipped), and only
	// when the card names a kind (an option-only card's top-level keys are not helper keys).
	@Test void e07_legacyFlatWalk_skipsSeriesData_andNeedsKind() throws Exception {
		assertEquals(List.of(), problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\",\"kind\":\"line\","
			+ "\"series\":[{\"field\":\"a\",\"data\":[{\"$fn\":\"Bad\"}]}]}]")));
		assertEquals(List.of("E-74 'Bad' is not a valid $fn name"), problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\",\"kind\":\"line\","
			+ "\"series\":[{\"field\":\"a\",\"label\":{\"$fn\":\"Bad\"}}]}]")));
		assertEquals(List.of(), problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\",\"option\":{\"series\":[]},"
			+ "\"x\":{\"$fn\":\"Bad\"}}]")));
	}

	// A chart's zoom publish is only legal when it is the retained implicit one (the chart handler lists it); with retain:false
	// it is an ordinary declared publish of a framework family, so E-45, whatever subscribes to it.
	@Test void e06_chartZoomPublishedWithoutRetain_withSubscriber() throws Exception {
		assertEquals(List.of("E-45 'zoom' is a framework family; framework topics are implicit and may not be declared"), problems(page("\"cards\":[{\"id\":\"t\",\"type\":\"chart\",\"publishes\":[{\"topic\":\"zoom:t\",\"retain\":false}]},"
			+ "{\"id\":\"u\",\"type\":\"html\",\"template\":\"h\",\"subscribes\":[{\"topic\":\"zoom:t\",\"as\":\"refresh\"}]}]")));
	}

	private static final String READER = "{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"subscribes\":[{\"topic\":\"data:orders\",\"as\":\"refresh\"}]}";

	@Test void g01_dataSource_isTheOnePublisher() throws Exception {
		assertEquals(List.of(), problems(page("\"dataSources\":[{\"id\":\"orders\",\"kind\":\"source\",\"url\":\"/x\"}],\"cards\":[" + READER + "]")));
	}

	@Test void g02_noPublisher_isE71NotE41() throws Exception {
		assertEquals(List.of("E-71 topic 'data:orders' needs exactly one source publisher; found 0"), problems(page("\"cards\":[" + READER + "]")));
	}

	@Test void g03_sourceAndScriptTopic_isE71() throws Exception {
		assertEquals(List.of("E-71 topic 'data:orders' needs exactly one source publisher; found 2"), problems(page(
			"\"topics\":[{\"topic\":\"data:orders\",\"retain\":true,\"publisher\":\"script\"}],"
			+ "\"dataSources\":[{\"id\":\"orders\",\"kind\":\"source\",\"url\":\"/x\"}],\"cards\":[]")));
	}

	@Test void g04_derivedFromMissingSource_isE71() throws Exception {
		assertEquals(List.of("E-71 topic 'data:nope' needs exactly one source publisher; found 0"), problems(page(
			"\"dataSources\":[{\"id\":\"orders\",\"kind\":\"derived\",\"from\":\"nope\",\"transforms\":[]}],\"cards\":[" + READER + "]")));
	}

	// Regression check: the source-op rules still hold once sources publish data: topics.
	@Test void g05_sourceOps() throws Exception {
		var src = "\"dataSources\":[{\"id\":\"orders\",\"kind\":\"source\",\"url\":\"/x\"}],";
		assertEquals(List.of(), problems(page(src + "\"cards\":[{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"ribbon\":["
			+ "{\"type\":\"refresh\",\"title\":\"R\",\"target\":\"orders\"},"
			+ "{\"type\":\"publish\",\"title\":\"C\",\"topic\":\"cmd:orders\",\"payload\":{\"schemaVersion\":1,\"op\":\"refresh\",\"options\":{\"clearCache\":true}}}]}]")));
		assertEquals(List.of("E-43 card 'orders' (type 'dataSource') does not handle op 'zoomTo'"), problems(page(src
			+ "\"cards\":[{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"ribbon\":["
			+ "{\"type\":\"publish\",\"title\":\"Z\",\"topic\":\"cmd:orders\",\"payload\":{\"schemaVersion\":1,\"op\":\"zoomTo\"}}]}]")));
		assertEquals(List.of("E-43 card 'orders' (type 'dataSource') does not handle op 'pause-polling'"), problems(page(src
			+ "\"cards\":[{\"id\":\"h\",\"type\":\"html\",\"template\":\"h\",\"ribbon\":[{\"type\":\"pausePolling\",\"target\":\"orders\"}]}]")));
	}

	@Test void g06_extensionNames() throws Exception {
		assertEquals(List.of("E-74 'Bad' is not a valid parser name", "E-74 'x..y' is not a valid builder name", "E-74 'Top N' is not a valid transform name"),
			problems(page("\"dataSources\":[{\"id\":\"a\",\"kind\":\"source\",\"url\":\"/x\",\"parser\":\"Bad\",\"builder\":\"x..y\"},"
				+ "{\"id\":\"b\",\"kind\":\"derived\",\"from\":\"a\",\"transforms\":[{\"use\":\"Top N\"}]}],\"cards\":[]")));
	}

	@Test void g07_unreadSource_isFine() throws Exception {
		assertEquals(List.of(), problems(page("\"dataSources\":[{\"id\":\"orders\",\"kind\":\"source\",\"url\":\"/x\"}],\"cards\":[]")));
	}

	@Test void g08_twoReaders_oneE71() throws Exception {
		var other = READER.replace("\"id\":\"h\"", "\"id\":\"h2\"");
		assertEquals(List.of("E-71 topic 'data:orders' needs exactly one source publisher; found 0"), problems(page("\"cards\":[" + READER + "," + other + "]")));
	}

	@Test void g09_duplicateSourceId_isE76Only() throws Exception {
		assertEquals(List.of("E-76 data source 'orders': id duplicates data source 'orders'"), problems(page(
			"\"dataSources\":[{\"id\":\"orders\",\"kind\":\"source\",\"url\":\"/x\"},{\"id\":\"orders\",\"kind\":\"source\",\"url\":\"/y\"}],\"cards\":[" + READER + "]")));
	}

	@Test void g10_derivedChain_isFine() throws Exception {
		assertEquals(List.of(), problems(page("\"dataSources\":[{\"id\":\"raw\",\"kind\":\"source\",\"url\":\"/x\"},"
			+ "{\"id\":\"mid\",\"kind\":\"derived\",\"from\":\"raw\",\"transforms\":[]},"
			+ "{\"id\":\"orders\",\"kind\":\"derived\",\"from\":\"mid\",\"transforms\":[]}],\"cards\":[" + READER + "]")));
	}

	private static String derived(String... idFrom) {
		var sb = new StringBuilder("\"dataSources\":[{\"id\":\"src\",\"kind\":\"source\",\"url\":\"/x\"}");
		for (var i = 0; i < idFrom.length; i += 2)
			sb.append(",{\"id\":\"").append(idFrom[i]).append("\",\"kind\":\"derived\",\"from\":\"").append(idFrom[i + 1]).append("\",\"transforms\":[]}");
		return sb.append("],\"cards\":[]").toString();
	}

	@Test void g11_derivedSelfReference_isE76() throws Exception {
		assertEquals(List.of("E-76 data source 'a': derived source 'a' references itself"), problems(page(derived("a", "a"))));
	}

	@Test void g12_derivedTwoCycle_isE76() throws Exception {
		assertEquals(List.of("E-76 data source 'a': derived source cycle: a -> b -> a"), problems(page(derived("b", "a", "a", "b"))));
	}

	@Test void g13_derivedThreeCycle_isE76_fromSmallestId() throws Exception {
		assertEquals(List.of("E-76 data source 'a': derived source cycle: a -> b -> c -> a"), problems(page(derived("c", "a", "b", "c", "a", "b"))));
	}

	@Test void g14_leadInToCycle_reportsTheCycleOnce() throws Exception {
		assertEquals(List.of("E-76 data source 'b': derived source cycle: b -> c -> b"), problems(page(derived("a", "b", "b", "c", "c", "b", "z", "c"))));
	}

	@Test void g15_derivedOfDerivedChain_isFine() throws Exception {
		assertEquals(List.of(), problems(page(derived("a", "src", "b", "a", "c", "b"))));
	}

	@Test void g16_twoDisjointCycles_eachReportedOnce_sortedBySmallestId() throws Exception {
		assertEquals(List.of(
			"E-76 data source 'a': derived source cycle: a -> b -> a",
			"E-76 data source 'd': derived source cycle: d -> e -> d"
		), problems(page(derived("d", "e", "e", "d", "a", "b", "b", "a"))));
	}

	@Test void g17_derivedFromMissingSource_isE71Only() throws Exception {
		assertEquals(List.of("E-71 topic 'data:nope' needs exactly one source publisher; found 0"), problems(page(derived("a", "nope"))));
	}

	@Test void g18_cycleEnteredAtLargerId_isRotatedToSmallest() throws Exception {
		assertEquals(List.of("E-76 data source 'b': derived source cycle: b -> c -> b"), problems(page(derived("a", "c", "c", "b", "b", "c"))));
	}

	@Test void g19_dashboardStatus_declaredByTopicOrSource() throws Exception {
		assertEquals(List.of(), problems("{\"contractVersion\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[],\"cards\":[],"
			+ "\"topics\":[{\"topic\":\"data:orders\",\"retain\":true,\"publisher\":\"script\"}],"
			+ "\"dashboard\":{\"id\":\"d\",\"grid\":1,\"panels\":[],\"status\":[\"data:orders\"]}}"));
		assertEquals(List.of(), problems("{\"contractVersion\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[],\"cards\":[],"
			+ "\"dataSources\":[{\"id\":\"orders\",\"kind\":\"source\"}],"
			+ "\"dashboard\":{\"id\":\"d\",\"grid\":1,\"panels\":[],\"status\":[\"data:orders\"]}}"));
	}

	@Test void g20_noDashboard_noE77() throws Exception {
		assertEquals(List.of(), problems("{\"contractVersion\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[],\"cards\":[]}"));
	}
}
