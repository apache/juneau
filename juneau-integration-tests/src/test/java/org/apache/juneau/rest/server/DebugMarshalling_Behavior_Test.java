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
package org.apache.juneau.rest.server;

import static org.apache.juneau.http.classic.header.ContentType.*;

import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Proves REST-driven serializer/parser debug <i>behavior</i> is decoupled from the JUL logger level and is instead
 * controlled by the cascading {@code debugMarshalling} setting.
 *
 * <p>
 * Two session-level {@code .debug()} observables are exercised (recursion&rarr;500 is intentionally <b>not</b>
 * treated as a valid session-debug observable, per the Option-A design correction):
 * <ul>
 * 	<li><b>Serializer traversal-stack prefix</b> &mdash; when marshalling debug is engaged, a getter-invocation failure
 * 		message is prefixed with the debug traversal stack (e.g. {@code " > [0] root:..."}) and the session collects it
 * 		as a warning ({@code "Warnings occurred in session:"}).  That prefix/warning is absent when debug is off.
 * 	<li><b>Parser buffered-input retention</b> &mdash; when marshalling debug is engaged, a malformed-parse failure
 * 		retains and quotes the buffered request content (e.g. <code>"1: {name:"</code>).  When debug is off the message
 * 		instead reads {@code "Use BEAN_debug setting to display content."}.
 * </ul>
 *
 * <p>
 * The decoupling gates {@code b04}/{@code b08} are deliberately <b>red on unmodified {@code main}</b>: on {@code main}
 * the seven REST marshalling sites keyed {@code req.isDebug()}, so raising the resolved op logger to {@code FINE}
 * engaged the debug observables above.  These tests assert the post-decoupling behavior (logger has no effect), which
 * fails against that old coupled behavior.  Verified red-on-main via the revert-and-probe technique.
 *
 * <p>
 * The op logger for a plain op is {@code <resourceClass>.<method>}; raising {@code Logger.getLogger(<resourceClass>)}
 * controls {@code isDebug()} for its ops.  {@code renderResponseStackTraces} is enabled so the failure messages
 * (and thus the observables above) surface in the response body.
 *
 * @since 10.0.0
 */
class DebugMarshalling_Behavior_Test extends TestBase {

	/** Bean whose getter throws — drives {@code SerializerSession.onBeanGetterException}. */
	public static class Boom {
		public String getOk() { return "ok"; }
		public String getBoom() { throw new RuntimeException("BOOMDETAIL"); }
	}

	/** Simple target bean for malformed-parse cases. */
	public static class Bean { public String name; }

	/** Malformed JSON body (truncated after the key) — parses to a {@link org.apache.juneau.marshall.parser.ParseException}. */
	private static final String MALFORMED = "{name:";

	@FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }

	private static void withLoggerLevel(Class<?> resourceClass, Level level, ThrowingRunnable action) throws Exception {
		var logger = Logger.getLogger(resourceClass.getName());
		var prev = logger.getLevel();
		logger.setLevel(level);
		try {
			action.run();
		} finally {
			logger.setLevel(prev);
		}
	}

	// -----------------------------------------------------------------------------------------
	// Fixtures.
	// -----------------------------------------------------------------------------------------

	/** No resource-level debugMarshalling; one op opts in, one stays default (for both serialize and parse). */
	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, renderResponseStackTraces="true")
	public static class R_Default {
		@RestGet(path="/serOff") public Boom serOff() { return new Boom(); }
		@RestGet(path="/serOn", debugMarshalling="true") public Boom serOn() { return new Boom(); }
		@RestPut(path="/parseOff") public String parseOff(@Content Bean b) { return "ok"; }
		@RestPut(path="/parseOn", debugMarshalling="true") public String parseOn(@Content Bean b) { return "ok"; }
	}

	/** Resource-level debugMarshalling=true; ops leave the annotation blank (inherit). */
	@Rest(debugMarshalling="true", serializers=JsonSerializer.class, parsers=JsonParser.class, renderResponseStackTraces="true")
	public static class R_ResourceTrue {
		@RestGet(path="/serInherit") public Boom serInherit() { return new Boom(); }
		@RestPut(path="/parseInherit") public String parseInherit(@Content Bean b) { return "ok"; }
	}

	/** JSON serializer with {@code Context.debug} enabled directly on the serializer instance. */
	public static class DebugJson extends JsonSerializer {
		public DebugJson(JsonSerializer.Builder<?> b) { super(enableDebug(b)); }
		private static JsonSerializer.Builder<?> enableDebug(JsonSerializer.Builder<?> b) { b.debug(); return b; }
	}

	/** debugMarshalling OFF, but the registered serializer has Context.debug enabled directly. */
	@Rest(serializers=DebugJson.class, renderResponseStackTraces="true")
	public static class R_DirectContextDebug {
		@RestGet(path="/ser") public Boom ser() { return new Boom(); }
	}

	private static final MockRestClient cDefault = MockRestClient.buildLax(R_Default.class);
	private static final MockRestClient cResourceTrue = MockRestClient.buildLax(R_ResourceTrue.class);

	// -----------------------------------------------------------------------------------------
	// Serializer observable — traversal-stack-prefix on a getter-invocation failure.
	// -----------------------------------------------------------------------------------------

	@Test void b01_serializer_opTrueAddsTraversalStackPrefix() throws Exception {
		// Op-level debugMarshalling=true → session debug → warning collected with " > [0] ..." traversal-stack prefix.
		cDefault.get("/serOn").accept("application/json").run()
			.assertStatus(500)
			.assertContent().isContains("> [0]", "Warnings occurred in session", "Could not call getValue");
	}

	@Test void b02_serializer_offOmitsTraversalStackPrefix() throws Exception {
		// Default (off) → the getter failure still throws (500) but with NO traversal-stack prefix and no session warning.
		cDefault.get("/serOff").accept("application/json").run()
			.assertStatus(500)
			.assertContent().isContains("Could not call getValue")
			.assertContent().isNotContains("> [0]", "Warnings occurred in session");
	}

	@Test void b03_serializer_resourceTrueInheritedByBlankOp() throws Exception {
		// Resource-level true is inherited by a blank op → traversal-stack prefix engaged.
		cResourceTrue.get("/serInherit").accept("application/json").run()
			.assertStatus(500)
			.assertContent().isContains("> [0]", "Warnings occurred in session");
	}

	@Test void b04_serializer_loggerDoesNotEngageMarshallingDebug() throws Exception {
		// RED on main: FINE → isDebug()==true → traversal-stack prefix engaged.  Post-decoupling: no prefix.
		withLoggerLevel(R_Default.class, Level.FINE, () ->
			cDefault.get("/serOff").accept("application/json").run()
				.assertStatus(500)
				.assertContent().isContains("Could not call getValue")
				.assertContent().isNotContains("> [0]", "Warnings occurred in session"));
	}

	// -----------------------------------------------------------------------------------------
	// Parser observable — buffered-input retention on a malformed-parse failure.
	// -----------------------------------------------------------------------------------------

	@Test void b05_parser_opTrueRetainsBufferedInput() throws Exception {
		// Op-level debugMarshalling=true → parser pipe buffers input → malformed-parse failure quotes it.
		cDefault.put("/parseOn", MALFORMED, APPLICATION_JSON).run()
			.assertStatus(400)
			.assertContent().isContains(MALFORMED)
			.assertContent().isNotContains("Use BEAN_debug setting to display content.");
	}

	@Test void b06_parser_offShowsBufferHint() throws Exception {
		// Default (off) → input not buffered → failure shows the "Use BEAN_debug setting" hint instead of the content.
		cDefault.put("/parseOff", MALFORMED, APPLICATION_JSON).run()
			.assertStatus(400)
			.assertContent().isContains("Use BEAN_debug setting to display content.");
	}

	@Test void b07_parser_resourceTrueInheritedByBlankOp() throws Exception {
		// Resource-level true is inherited by a blank op → buffered input retained.
		cResourceTrue.put("/parseInherit", MALFORMED, APPLICATION_JSON).run()
			.assertStatus(400)
			.assertContent().isContains(MALFORMED)
			.assertContent().isNotContains("Use BEAN_debug setting to display content.");
	}

	@Test void b08_parser_loggerDoesNotEngageMarshallingDebug() throws Exception {
		// RED on main: FINE → isDebug()==true → buffered input retained.  Post-decoupling: buffer hint (no content).
		withLoggerLevel(R_Default.class, Level.FINE, () ->
			cDefault.put("/parseOff", MALFORMED, APPLICATION_JSON).run()
				.assertStatus(400)
				.assertContent().isContains("Use BEAN_debug setting to display content.")
				.assertContent().isNotContains("1: " + MALFORMED));
	}

	// -----------------------------------------------------------------------------------------
	// Context-vs-session distinction (NOT the REST-layer observable) — a directly-set Context.debug on the
	// serializer is a Context-level setting; the REST site passes .debug(null) when debugMarshalling is off, which
	// must NOT clobber that pre-existing Context.debug.  Documents why recursion→500 (a Context-level detectRecursions
	// behavior) cannot be used as the session-debug REST observable.
	// -----------------------------------------------------------------------------------------

	@Test void b09_directContextDebugPreservedWhenRestMarshallingOff() throws Exception {
		// debugMarshalling is OFF → REST site passes .debug(null); the serializer's own Context.debug=true survives,
		// so the traversal-stack prefix still appears — proving null pass-through does not override Context-level debug.
		MockRestClient.buildLax(R_DirectContextDebug.class).get("/ser").accept("application/json").run()
			.assertStatus(500)
			.assertContent().isContains("> [0]", "Warnings occurred in session");
	}
}
