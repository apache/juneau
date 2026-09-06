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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * REST-boundary tests for {@link WidgetsMixin.WidgetValidationProcessor} (WORK-J0525) &mdash; the
 * {@code ServiceLoader}-registered {@link org.apache.juneau.rest.server.processor.ResponseProcessor
 * ResponseProcessor} that calls {@link Widget#validate()} on any REST response content that is a {@link Widget},
 * giving the JSON serving path ({@link ModalDef}/{@link FormDef}) the same fail-closed gate the
 * {@code juneau-rest-server-views} HTML emitters already give every {@code Widget} they consume.
 *
 * <p>
 * This is the REST-boundary inverse of {@code WORK-J0520}'s (f1)/(f2) tests: those pin that a malformed,
 * never-{@code checked()} modal/form serializes successfully with the current contract version and does not
 * throw. Here, served over a real REST response instead of a bare {@link Json#of(Object) Json.of(...)} call, the
 * same malformed shapes now fail loud with a 500 &mdash; which is this item's entire reason to exist (see
 * {@code h2} below for the pin that {@link Json#of(Object) Json.of(...)} itself is deliberately untouched).
 *
 * <p>
 * No {@code @Rest(mixins=WidgetsMixin.class)} composition is required on any host below except {@code HostC}
 * (the double-registration guard): the validator is discovered via {@code ServiceLoader} from this module's
 * {@code META-INF/services} entry on the test classpath, with no consumer opt-in, which is the point of the
 * design (design doc &sect;4.1 reason 1).
 */
class ModalDef_ServingPathValidation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Malformed-shape builders - one per J0520 harm-table row (design doc &sect;8(a3)), built WITHOUT the
	// public factories' own argument checks so the shape survives construction and only fails at validate() time.
	//------------------------------------------------------------------------------------------------------------------

	/** Row: a {@code select} input with no options. */
	private static ModalDef noOptionSelect() {
		return ModalDef.create("Ack?").form(
			FormDef.create().field(FormDef.Input.of("sev", "Severity", "select")));
	}

	/** Row: two fields sharing one name - collide on submit. */
	private static ModalDef duplicateFieldName() {
		return ModalDef.create("Ack?").form(
			FormDef.create()
				.field(FormDef.Input.of("x", "First", "text"))
				.field(FormDef.Input.of("x", "Second", "text")));
	}

	/** Row: both {@code fields} and {@code sections} declared - mutually exclusive. */
	private static ModalDef fieldsAndSections() {
		var form = FormDef.create().field(FormDef.Input.of("a", "A", "text"));
		form.sections = List.of(FormDef.Section.of("s1", "S1").field(FormDef.Input.of("b", "B", "text")));
		return ModalDef.create("Ack?").form(form);
	}

	/** Row: a non-compiling client validation pattern. */
	private static ModalDef nonCompilingPattern() {
		return ModalDef.create("Ack?").form(
			FormDef.create().field(FormDef.Input.of("p", "P", "text").pattern("[")));
	}

	/** Row: a blank modal title, bypassing {@link ModalDef#create(String)}'s own blank check. */
	private static ModalDef blankTitle() {
		var m = new ModalDef();
		m.title = "   ";
		return m;
	}

	/** Row (acceptable-today, becomes a 500 under this hook): a confirmation field {@code kind} off the allowlist. */
	private static ModalDef fieldKindOffAllowlist() {
		var m = ModalDef.create("Ack?");
		var f = new ModalDef.Field();
		f.label = "Severity";
		f.kind = "not-a-real-kind";
		m.fields = List.of(f);
		return m;
	}

	/** Row (stays acceptable): an {@code action} input whose {@code actionId} is non-blank but resolves to nothing. */
	private static ModalDef unknownActionId() {
		return ModalDef.create("Ack?").form(
			FormDef.create().field(FormDef.Input.of("go", "Go", "action").actionId("no-such-action")));
	}

	private static ModalDef validNoForm() {
		return ModalDef.create("Ack?");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The headline - malformed Widget served over REST now fails; the full harm-table matrix.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class HostA extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/unchecked-invalid") public ModalDef uncheckedInvalid() { return noOptionSelect(); }
		@RestGet(path="/checked-valid") public ModalDef checkedValid() {
			return ModalDef.create("Ack?").form(FormDef.create().field(FormDef.Input.of("sev", "Severity", "text"))).checked();
		}
		@RestGet(path="/dup-name") public ModalDef dupName() { return duplicateFieldName(); }
		@RestGet(path="/fields-and-sections") public ModalDef fieldsAndSectionsOp() { return fieldsAndSections(); }
		@RestGet(path="/bad-pattern") public ModalDef badPattern() { return nonCompilingPattern(); }
		@RestGet(path="/blank-title") public ModalDef blankTitleOp() { return blankTitle(); }
		@RestGet(path="/bad-kind") public ModalDef badKind() { return fieldKindOffAllowlist(); }
		@RestGet(path="/unknown-action-id") public ModalDef unknownActionIdOp() { return unknownActionId(); }
	}

	private static final MockRestClient A = MockRestClient.buildLax(HostA.class);

	@Test void a1_uncheckedMalformedModal_servedOverRest_fails500_noPartialBody() throws Exception {
		A.get("/unchecked-invalid").accept("application/json").run()
			.assertStatus(500)
			.assertContent().asString().isNotContains("\"contractVersion\"", "\"fields\"");
	}

	@Test void a2_control_checkedValidModal_serves200_normalPayload() throws Exception {
		A.get("/checked-valid").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("\"contractVersion\"", "\"fields\"");
	}

	@Test void a3_harmTableMatrix_allFiveSilentlyDegradingShapes_fail500() throws Exception {
		A.get("/dup-name").run().assertStatus(500);
		A.get("/fields-and-sections").run().assertStatus(500);
		A.get("/bad-pattern").run().assertStatus(500);
		A.get("/blank-title").run().assertStatus(500);
		// a1 above is the fifth row (no-option select).
	}

	@Test void a4_kindOffAllowlist_becomes500_behaviorChangeFromDegradesSafely() throws Exception {
		// Previously degraded safely to "text" because nothing called validate() on the unchecked path; this hook
		// now enforces ModalDef.Field's kind allowlist on every response, so this becomes a loud 500 instead.
		A.get("/bad-kind").run().assertStatus(500).assertContent().asString().isContains("kind");
	}

	@Test void a4_unknownActionId_staysAcceptable_200() throws Exception {
		// Not a validate() rule (the bean cannot resolve the id against the enclosing view's action catalog) - the
		// hook must not newly reject this; the runtime paints it disabled instead.
		A.get("/unknown-action-id").run().assertStatus(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The fail-closed response shape.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b1_status_isExactly500_not400_not200() throws Exception {
		A.get("/unchecked-invalid").run().assertStatus(500);
	}

	@Test void b2_body_namesTheFailingRule() throws Exception {
		// suppressedErrorBodyMessage(...) surfaces e2.getMessage() whenever the thrown exception is itself a
		// BasicHttpException (appThrown==true) - true here regardless of renderResponseStackTraces - so the
		// InternalServerError's own message, naming the widget type and the underlying validate() failure, reaches
		// the client.
		A.get("/unchecked-invalid").run().assertStatus(500)
			.assertContent().asString().isContains("ModalDef", "select must declare at least one option");
	}

	@Test void b3_noBytesWrittenBeforeTheThrow_bodyIsTheErrorBodyOnly() throws Exception {
		var body = A.get("/unchecked-invalid").run().assertStatus(500).getContent().asString();
		assertTrue(body.startsWith("HTTP 500"), body);
		assertFalse(body.contains("\"title\""), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) No double-handling; inert off its own path.
	//------------------------------------------------------------------------------------------------------------------

	/** A {@link Widget} whose {@code validate()} counts calls instead of checking anything - always "valid". */
	static final class CountingWidget implements Widget {
		static final AtomicInteger COUNT = new AtomicInteger();
		@Override public void validate() { COUNT.incrementAndGet(); }
	}

	@Rest(mixins=WidgetsMixin.class)
	public static class HostC extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/counting") public CountingWidget counting() { return new CountingWidget(); }
		@RestGet(path="/plain") public String plain() { return "plain"; }
		@RestGet(path="/map") public Map<String,Object> map() { return Map.of("a", 1); }
		@RestGet(path="/list") public List<String> list() { return List.of("x", "y"); }
	}

	private static final MockRestClient C = MockRestClient.buildLax(HostC.class);

	@Test void c1_registeredByServiceLoaderAndMixin_validatesExactlyOnce_notTwice() throws Exception {
		// HostC composes WidgetsMixin, so both the ServiceLoader entry and the mixin's own
		// responseProcessors + mergeResponseProcessorsIntoHost=true declaration are candidates;
		// ResponseProcessorList.Builder de-duplicates by class, so validate() must run exactly once.
		var before = CountingWidget.COUNT.get();
		C.get("/counting").run().assertStatus(200);
		assertEquals(before + 1, CountingWidget.COUNT.get(), "validate() must run exactly once, not once per registration");
	}

	@Test void c2_nonWidgetContent_servesUnaffected() throws Exception {
		C.get("/plain").accept("application/json").run().assertStatus(200).assertContent().asString().is("\"plain\"");
		C.get("/map").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"a\":1");
		C.get("/list").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("\"x\"", "\"y\"");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Async - CompletableFuture<ModalDef>, covered on the MockServletRequest sync-fallback (RESTART) path.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class HostD extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/future-invalid") public CompletableFuture<ModalDef> futureInvalid() {
			return CompletableFuture.completedFuture(noOptionSelect());
		}
		@RestGet(path="/future-valid") public CompletableFuture<ModalDef> futureValid() {
			return CompletableFuture.completedFuture(validNoForm());
		}
	}

	private static final MockRestClient D = MockRestClient.buildLax(HostD.class);

	@Test void d1_malformedCompletableFuture_fails500() throws Exception {
		D.get("/future-invalid").run().assertStatus(500);
	}

	@Test void d2_validCompletableFuture_serves200() throws Exception {
		D.get("/future-valid").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("\"contractVersion\"");
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) The redundancy pin - .checked() and this hook are deliberately not deduplicated.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class HostH extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/checked") public ModalDef checkedOp() { return validNoForm().checked(); }
		@RestGet(path="/unchecked") public ModalDef uncheckedOp() { return validNoForm(); }
	}

	private static final MockRestClient H = MockRestClient.buildLax(HostH.class);

	@Test void h1_checkedAndUnchecked_validModal_serveByteIdentical() throws Exception {
		// checked() re-validates (the hook validates again); unchecked relies solely on the hook.  Both are valid,
		// so the served bytes must be identical - the assertion that keeps ".checked()-vs-hook" drift impossible.
		var checked = H.get("/checked").run().assertStatus(200).getContent().asString();
		var unchecked = H.get("/unchecked").run().assertStatus(200).getContent().asString();
		assertEquals(checked, unchecked);
	}

	@Test void h2_jsonOf_malformedModal_stillSucceeds_notTheSerializerLevelGate() throws Exception {
		// A ResponseProcessor never runs for Json.of(...): this is the pin that this item did not accidentally
		// become the serializer-level gate the design rejected (design doc fork F12(f), section 4.3(d)).  It is
		// deliberately uncomfortable to read, exactly like WORK-J0520's own (f1).
		assertDoesNotThrow(() -> Json.of(noOptionSelect()));
	}
}
