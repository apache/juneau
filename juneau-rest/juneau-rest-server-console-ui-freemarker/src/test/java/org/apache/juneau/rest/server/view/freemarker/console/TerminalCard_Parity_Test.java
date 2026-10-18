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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import freemarker.template.*;

/**
 * A terminal card built with {@link PageSpec} and one authored as a {@code <@card type="terminal">} JSON5 body reject the
 * same invalid input with the same message, and accept the same valid input with the same fragment.
 */
class TerminalCard_Parity_Test extends TestBase {

	private static final CardDirectiveModel MODEL = new CardDirectiveModel(
		CardRequirements.create().build(new ToolkitPackRegistry()), CardTypeRegistry.standard());

	/** The card body as the builder spells it: the keys of the {@code {contractVersion, terminal}} envelope, minus {@code contractVersion}. */
	private static Map<String,Object> body(Object terminal) {
		var m = new LinkedHashMap<String,Object>();
		m.put("terminal", terminal);
		return m;
	}

	private static Map<String,Object> with(Map<String,Object> m, String k, Object v) {
		var out = new LinkedHashMap<>(m);
		out.put(k, v);
		return out;
	}

	static Stream<Arguments> invalid() {
		return Stream.of(
			Arguments.of("placeholderBytesUrl", body(Map.of("bytesUrl", "/t/{id}/bytes"))),
			Arguments.of("noBytesUrl", body(Map.of("title", "x"))),
			Arguments.of("absoluteBytesUrl", body(Map.of("bytesUrl", "https://evil.example/b"))),
			Arguments.of("traversalEventsUrl", body(Map.of("bytesUrl", "/b", "eventsUrl", "/a/../b"))),
			Arguments.of("inputWithoutInputId", body(Map.of("bytesUrl", "/b", "input", true))),
			Arguments.of("badInputId", body(Map.of("bytesUrl", "/b", "inputId", "a b"))),
			Arguments.of("badRefreshMs", body(Map.of("bytesUrl", "/b", "refreshMs", 0))),
			Arguments.of("badTailBytes", body(Map.of("bytesUrl", "/b", "tailBytes", 0))),
			Arguments.of("unknownTerminalKey", body(Map.of("bytesUrl", "/b", "nope", 1))),
			Arguments.of("unknownOptionKey", body(Map.of("bytesUrl", "/b", "terminal", Map.of("rows", 3)))),
			Arguments.of("scrollbackTooBig", body(Map.of("bytesUrl", "/b", "terminal", Map.of("scrollback", 1_000_001)))),
			Arguments.of("convertEolNotBoolean", body(Map.of("bytesUrl", "/b", "terminal", Map.of("convertEol", "yes")))),
			Arguments.of("noTerminalObject", new LinkedHashMap<String,Object>()),
			Arguments.of("terminalNotObject", body("/b")),
			Arguments.of("unknownTopLevelKey", with(body(Map.of("bytesUrl", "/b")), "output", Map.of())),
			Arguments.of("wrongContractVersion", with(body(Map.of("bytesUrl", "/b")), "contractVersion", "2")));
	}

	/** The message the template path throws: the handler behind {@code <@card type="terminal">}, fed the same body as JSON5. */
	private static String templateMessage(Map<String,Object> body) {
		var envelope = new LinkedHashMap<String,Object>();
		envelope.put("contractVersion", "1");
		envelope.putAll(body);
		var source = CardSource.create("terminal", "build").body(Json5.of(envelope)).build();
		var target = new TerminalCardType();
		return assertThrows(IllegalArgumentException.class, () -> target.toFragment(source)).getMessage();
	}

	private static String builderMessage(Map<String,Object> body) {
		var card = CardSpec.of("terminal", "build");
		body.forEach(card::body);
		var spec = PageSpec.create().card(card);
		var arg = new PageCapture();
		return assertThrows(TemplateModelException.class, () -> spec.applyTo(arg, MODEL)).getMessage();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalid")
	void a01_invalidInput_rejectedIdentically(String name, Map<String,Object> body) {
		assertEquals(templateMessage(body), builderMessage(body), name);
	}

	@Test void a02_validInput_sameFragment() throws Exception {
		var terminal = Map.<String,Object>of("bytesUrl", "/t/juneau-terminal/r1/bytes", "refreshMs", 10, "input", true, "inputId", "r1",
			"terminal", Map.of("convertEol", true, "scrollback", 0));
		var template = new TerminalCardType().toFragment(
			CardSource.create("terminal", "build").body(Json5.of(Map.of("contractVersion", "1", "terminal", terminal))).build());
		var cap = new PageCapture();
		PageSpec.create().card(CardSpec.of("terminal", "build").body("terminal", terminal)).applyTo(cap, MODEL);
		assertEquals(template.get("terminal"), cap.unplacedCards().get(0).toMap().get("terminal"));
	}

	/** The directive's rendered error for a fixture must contain exactly the builder's message for the same attributes. */
	@ParameterizedTest(name = "{0}")
	@CsvSource({"tm-card-src,/x,", "bad-tm-card-template,,t1", "bad-tm-card-both,/x,t1"})
	void a03_srcAndTemplateRejectedLikeTheDirective(String fixture, String src, String template) {
		var card = CardSpec.of("terminal", "log").body("terminal", Map.of("bytesUrl", "/b"));
		if (src != null)
			card.src(src);
		if (template != null)
			card.template(template);
		var spec = PageSpec.create().card(card);
		var arg2 = new PageCapture();
		var ex = assertThrows(TemplateModelException.class, () -> spec.applyTo(arg2, MODEL));
		var body = C1Fixtures.renderError(fixture);
		assertTrue(body.contains(ex.getMessage()), () -> "expected '" + ex.getMessage() + "' in:\n" + body);
	}
}
