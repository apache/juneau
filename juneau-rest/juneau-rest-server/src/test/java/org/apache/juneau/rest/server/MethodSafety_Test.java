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

import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.server.filter.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link MethodSafety} and {@link Mutating @Mutating}: the safe-method set, the per-operation rule, and
 * the startup failure a real {@link Rest @Rest} resource gets when it declares a mutating operation behind a safe
 * method.
 *
 * <p>
 * The group-c tests construct real {@link RestContext} instances, because the claim being made is that the
 * application does not start &mdash; not that a static method throws when called directly.  They use
 * {@code eagerInit} so the failure lands in the constructor, which is what "fail at boot" means.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"unused" // Fixture methods and classes are reached only reflectively or through annotations, so they appear unused
})
class MethodSafety_Test extends org.apache.juneau.TestBase {

	//-----------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------

	/** Carries one method per shape the rule cares about; group b reflects over these rather than dispatching. */
	static class Fix_Methods {
		@Mutating public void declared() { /* fixture body; only the @Mutating declaration matters to the check */ }
		@Mutating("the stored credential") public void declaredWithNote() { /* fixture body; only the annotation/note matter */ }
		public void undeclared() { /* fixture body; deliberately lacks @Mutating */ }
	}

	/** A superclass declaration must still be seen when the subclass overrides the method. */
	static class Fix_Parent {
		@Mutating public void inherited() { /* fixture body; only the @Mutating declaration matters to the check */ }
	}

	static class Fix_Child extends Fix_Parent {
		@Override public void inherited() { /* fixture override; deliberately re-declares without @Mutating */ }
	}

	static java.lang.reflect.Method method(Class<?> c, String name) throws Exception {
		return c.getMethod(name);
	}

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - the safe-method set, and its agreement with the boundary
	//-----------------------------------------------------------------------------------------------------------

	@Test void a01_safeMethods() {
		assertTrue(MethodSafety.isSafe("GET"));
		assertTrue(MethodSafety.isSafe("HEAD"));
		assertTrue(MethodSafety.isSafe("OPTIONS"));
		assertTrue(MethodSafety.isSafe("TRACE"));
	}

	@Test void a02_unsafeMethods() {
		assertFalse(MethodSafety.isSafe("POST"));
		assertFalse(MethodSafety.isSafe("PUT"));
		assertFalse(MethodSafety.isSafe("PATCH"));
		assertFalse(MethodSafety.isSafe("DELETE"));
	}

	@Test void a03_caseInsensitive() {
		assertTrue(MethodSafety.isSafe("get"));
		assertTrue(MethodSafety.isSafe("Get"));
	}

	@Test void a04_nullAndUnknownAreNotSafe() {
		// Fail-closed: a method this framework does not know must not inherit the read-only promise.
		assertFalse(MethodSafety.isSafe(null));
		assertFalse(MethodSafety.isSafe("QUERY"));
		assertFalse(MethodSafety.isSafe(""));
		assertFalse(MethodSafety.isSafe("*"));
	}

	@Test void a05_theBoundaryAndTheBootCheckAgreeOnEveryMethod() {
		// The point of the delegation. If these two ever disagree, the boot check would clear a method the
		// boundary treats as a write (or worse, the reverse) and both controls would be quietly wrong. Asserted
		// over the safe set and a spread of others rather than trusting that one delegates to the other today.
		for (var m : new String[] { "GET", "HEAD", "OPTIONS", "TRACE", "POST", "PUT", "PATCH", "DELETE", "QUERY", "", "*" })
			assertEquals(MethodSafety.isSafe(m), ! LoopbackBoundary.isStateChanging(m), "disagreement on '" + m + "'");
		assertEquals(MethodSafety.isSafe(null), ! LoopbackBoundary.isStateChanging(null), "disagreement on null");
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - the per-operation rule
	//-----------------------------------------------------------------------------------------------------------

	@Test void b01_declaredMutatingOnGet_rejected() throws Exception {
		var m = method(Fix_Methods.class, "declared");
		var e = assertThrows(RuntimeException.class, () -> MethodSafety.checkOperation("GET", m));
		assertTrue(e.getMessage().contains("Fix_Methods.declared"), e.getMessage());
		assertTrue(e.getMessage().contains("safe method"), e.getMessage());
	}

	@Test void b02_declaredMutatingOnEveryOtherSafeMethod_rejected() throws Exception {
		var m = method(Fix_Methods.class, "declared");
		for (var httpMethod : new String[] { "HEAD", "OPTIONS", "TRACE" })
			assertThrows(RuntimeException.class, () -> MethodSafety.checkOperation(httpMethod, m), httpMethod);
	}

	@Test void b03_declaredMutatingOnWildcard_rejected() throws Exception {
		// @RestOp(method="*") answers GET along with everything else, so the operation is reachable by a safe
		// method and the write checks would not run for that arrival.
		var m = method(Fix_Methods.class, "declared");
		var e = assertThrows(RuntimeException.class, () -> MethodSafety.checkOperation("*", m));
		assertTrue(e.getMessage().contains("every method"), e.getMessage());
	}

	@Test void b04_declaredMutatingOnUnsafeMethods_allowed() throws Exception {
		var m = method(Fix_Methods.class, "declared");
		for (var httpMethod : new String[] { "POST", "PUT", "PATCH", "DELETE", "QUERY" })
			assertDoesNotThrow(() -> MethodSafety.checkOperation(httpMethod, m), httpMethod);
	}

	@Test void b05_undeclaredOnGet_allowed() throws Exception {
		// The documented limit, asserted so it is a stated property rather than an accident: the check finds
		// contradictions, not omissions. A handler that mutates and says nothing is invisible here.
		var m = method(Fix_Methods.class, "undeclared");
		assertDoesNotThrow(() -> MethodSafety.checkOperation("GET", m));
	}

	@Test void b06_noteIsIncludedInTheFailure() throws Exception {
		var m = method(Fix_Methods.class, "declaredWithNote");
		var e = assertThrows(RuntimeException.class, () -> MethodSafety.checkOperation("GET", m));
		assertTrue(e.getMessage().contains("the stored credential"), e.getMessage());
	}

	@Test void b07_failureNamesTheRestOpInferenceTrap() throws Exception {
		// The message has to mention it: in the @RestOp case the developer never typed GET, so a message that
		// only says "bound to GET" reads as wrong rather than as informative.
		var m = method(Fix_Methods.class, "declared");
		var e = assertThrows(RuntimeException.class, () -> MethodSafety.checkOperation("GET", m));
		assertTrue(e.getMessage().contains("infers the method"), e.getMessage());
	}

	@Test void b08_inheritedDeclarationIsSeenThroughAnOverride() throws Exception {
		// Method.getAnnotation would return null here; the check resolves through MethodInfo for this reason.
		var m = method(Fix_Child.class, "inherited");
		assertThrows(RuntimeException.class, () -> MethodSafety.checkOperation("GET", m));
	}

	@Test void b09_nullHttpMethodIsNotAContradiction() throws Exception {
		// Not safe, so nothing is being claimed twice. Fail-closed at request time covers it.
		var m = method(Fix_Methods.class, "declared");
		assertDoesNotThrow(() -> MethodSafety.checkOperation(null, m));
	}

	@Test void b10_nullJavaMethodRejected() {
		assertThrows(IllegalArgumentException.class, () -> MethodSafety.checkOperation("GET", null));
		assertThrows(IllegalArgumentException.class, () -> MethodSafety.check(null));
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - the boot failure, against real resources
	//-----------------------------------------------------------------------------------------------------------

	@Rest(eagerInit = "true")
	static class Fix_MutatingGet {
		@Mutating("the run's armed state")
		@RestGet("/arm")
		public String arm() { return "armed"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_MutatingPost {
		@Mutating("the run's armed state")
		@RestPost("/arm")
		public String arm() { return "armed"; }
	}

	/**
	 * The case the check exists for: no {@code method=}, and a Java method name whose prefix is none of
	 * get/put/post/delete, so the resolved method defaults to {@code GET} without the developer writing it.
	 */
	@Rest(eagerInit = "true")
	static class Fix_MutatingBareRestOp {
		@Mutating("the stored credential")
		@RestOp
		public String armRelease() { return "armed"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_MutatingWildcard {
		@Mutating
		@RestOp(method = "*", path = "/anything")
		public String anything() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_PlainGet {
		@RestGet("/page")
		public String page() { return "page"; }
	}

	@Test void c01_resourceWithMutatingGet_failsToInitialize() {
		var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_MutatingGet.class, Fix_MutatingGet::new)));
		assertTrue(messageChain(e).contains("Fix_MutatingGet.arm"), messageChain(e));
		assertTrue(messageChain(e).contains("the run's armed state"), messageChain(e));
	}

	@Test void c02_resourceWithMutatingPost_initializes() throws Exception {
		var ctx = new RestContext(argsOf(Fix_MutatingPost.class, Fix_MutatingPost::new));
		assertSize(1, ctx.getRestOperations().getOpContexts());
	}

	@Test void c03_bareRestOpInferringGet_failsToInitialize() {
		// The headline case. Nobody typed GET; the framework inferred it, and the operation would have sat behind
		// every write check while looking correct at the call site.
		var e = assertThrows(Exception.class,
			() -> new RestContext(argsOf(Fix_MutatingBareRestOp.class, Fix_MutatingBareRestOp::new)));
		assertTrue(messageChain(e).contains("Fix_MutatingBareRestOp.armRelease"), messageChain(e));
	}

	@Test void c04_wildcardMethod_failsToInitialize() {
		var e = assertThrows(Exception.class,
			() -> new RestContext(argsOf(Fix_MutatingWildcard.class, Fix_MutatingWildcard::new)));
		assertTrue(messageChain(e).contains("Fix_MutatingWildcard.anything"), messageChain(e));
	}

	@Test void c05_resourceWithNoMutatingAnnotation_initializes() throws Exception {
		// Strictly additive: a resource that never mentions @Mutating behaves exactly as it did before the check
		// existed, which is what makes turning this on safe for every application already in the wild.
		var ctx = new RestContext(argsOf(Fix_PlainGet.class, Fix_PlainGet::new));
		assertSize(1, ctx.getRestOperations().getOpContexts());
	}

	//-----------------------------------------------------------------------------------------------------------
	// e - the effective method a request is judged on
	//-----------------------------------------------------------------------------------------------------------

	/** A header lookup that knows no headers. */
	static final UnaryOperator<String> NO_HEADERS = x -> null;

	/** A header lookup holding exactly one header, matched case-insensitively as a servlet container would. */
	static UnaryOperator<String> header(String name, String value) {
		return x -> name.equalsIgnoreCase(x) ? value : null;
	}

	@Test void e01_noOverrideKeepsRequestLineMethod() {
		assertEquals("GET", MethodSafety.effectiveMethod("GET", null, NO_HEADERS));
		assertEquals("GET", MethodSafety.effectiveMethod("GET", "page=2&sort=name", NO_HEADERS));
	}

	@Test void e02_queryMethodParamOverrides() {
		assertEquals("DELETE", MethodSafety.effectiveMethod("GET", "method=DELETE", NO_HEADERS));
		assertEquals("DELETE", MethodSafety.effectiveMethod("GET", "page=2&method=DELETE", NO_HEADERS));
	}

	@Test void e03_queryParamCaseInsensitive() {
		assertEquals("DELETE", MethodSafety.effectiveMethod("GET", "method=dElEtE", NO_HEADERS));
		assertEquals("DELETE", MethodSafety.effectiveMethod("GET", "METHOD=delete", NO_HEADERS));
	}

	@Test void e04_eachOverrideHeader() {
		for (var h : List.of("X-Method", "X-HTTP-Method-Override", "X-HTTP-Method", "X-Method-Override"))
			assertEquals("PATCH", MethodSafety.effectiveMethod("GET", null, header(h, "PATCH")), h);
	}

	@Test void e05_malformedPercentEscapeFailsToUnsafe() {
		// A claim that cannot be decoded is still a claim; reading it as "no override" would let it slip through as GET.
		assertFalse(MethodSafety.isSafe(MethodSafety.effectiveMethod("GET", "method=%ZZ", NO_HEADERS)));
		assertFalse(MethodSafety.isSafe(MethodSafety.effectiveMethod("GET", "method=%", NO_HEADERS)));
	}

	@Test void e06_nullQueryAndNullHeaders() {
		assertEquals("GET", MethodSafety.effectiveMethod("GET", null, NO_HEADERS));
		assertEquals("HEAD", MethodSafety.effectiveMethod("HEAD", "", NO_HEADERS));
		assertEquals("GET", MethodSafety.effectiveMethod("GET", "method=&method", NO_HEADERS));
	}

	@Test void e07_unknownOverrideValueIsUnsafe() {
		assertFalse(MethodSafety.isSafe(MethodSafety.effectiveMethod("GET", "method=FROB", NO_HEADERS)));
	}

	@Test void e08_duplicateMethodParamTakesUnsafeIfAnyUnsafe() {
		assertEquals("DELETE", MethodSafety.effectiveMethod("GET", "method=GET&method=DELETE", NO_HEADERS));
		assertEquals("DELETE", MethodSafety.effectiveMethod("GET", "method=options&method=delete", NO_HEADERS));
	}

	@Test void e09_postOverriddenToGetStaysUnsafe() {
		// The request line is the floor: an override can raise the severity of a request but never lower it.
		assertEquals("POST", MethodSafety.effectiveMethod("POST", "method=GET", NO_HEADERS));
		assertEquals("POST", MethodSafety.effectiveMethod("post", null, header("X-HTTP-Method-Override", "GET")));
	}

	@Test void e10_safeOverrideOfASafeMethodKeepsTheRequestLine() {
		assertEquals("GET", MethodSafety.effectiveMethod("GET", "method=HEAD", header("X-Method", "OPTIONS")));
	}

	@Test void e12_safeQueryClaimDoesNotMaskUnsafeHeaderClaim() {
		assertEquals("DELETE", MethodSafety.effectiveMethod("GET", "method=HEAD", header("X-Method", "DELETE")));
	}

	@Test void e11_nullHeaderFunctionRejected() {
		assertThrows(IllegalArgumentException.class, () -> MethodSafety.effectiveMethod("GET", null, null));
	}

	@Test void f01_overrideHeadersOrderIsFrozen() {
		assertEquals(List.of("X-Method", "X-HTTP-Method-Override", "X-HTTP-Method", "X-Method-Override"), MethodSafety.overrideHeaders());
		var headers = MethodSafety.overrideHeaders();
		assertThrows(UnsupportedOperationException.class, () -> headers.add("X-Other"));
	}

	@Test void f02_queryClaims_noClaims() {
		assertEquals(List.of(), MethodSafety.queryClaims(null));
		assertEquals(List.of(), MethodSafety.queryClaims(""));
		assertEquals(List.of(), MethodSafety.queryClaims("page=2&methods=PUT&method"));
	}

	@Test void f03_queryClaims_everyValueInOrderUntrimmed() {
		assertEquals(List.of("GET", "DELETE", " put "), MethodSafety.queryClaims("method=GET&page=1&method=DELETE&METHOD=%20put%20"));
	}

	@Test void f04_queryClaims_decodesNameAndValue() {
		assertEquals(List.of("PUT", "A B"), MethodSafety.queryClaims("%6Dethod=PUT&method=A+B"));
	}

	@Test void f05_queryClaims_malformedEscapeKeepsRawText() {
		assertEquals(List.of("%ZZ", ""), MethodSafety.queryClaims("method=%ZZ&method="));
	}

	//-----------------------------------------------------------------------------------------------------------
	// r - @RestOp(read=true), validated when the operation table is built
	//-----------------------------------------------------------------------------------------------------------

	@Rest(eagerInit = "true")
	static class Fix_ReadView {
		@RestOp(method = "VIEW", path = "/x", read = true)
		public String view() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadGet {
		@RestOp(method = "GET", path = "/x", read = true)
		public String page() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadPost {
		@RestOp(method = "POST", path = "/x", read = true)
		public String write() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadPut {
		@RestOp(method = "PUT", path = "/x", read = true)
		public String write() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadPatch {
		@RestOp(method = "PATCH", path = "/x", read = true)
		public String write() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadDelete {
		@RestOp(method = "DELETE", path = "/x", read = true)
		public String write() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadConnect {
		@RestOp(method = "CONNECT", path = "/x", read = true)
		public String write() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadStar {
		@RestOp(method = "*", path = "/x", read = true)
		public String anything() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadMethodKeyword {
		@RestOp(method = "METHOD", path = "/x", read = true)
		public String anything() { return "x"; }
	}

	@SuppressWarnings({
		"java:S114" // Snake_case fixture interface names are intentional test-local naming
	})
	public interface Fix_RrpcInterface {
		String echo(String x);
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadRrpc {
		@RestOp(method = "RRPC", path = "/proxy/*", read = true)
		public Fix_RrpcInterface proxy() { return x -> x; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadMutating {
		@Mutating("the view counter")
		@RestOp(method = "VIEW", path = "/x", read = true)
		public String view() { return "x"; }
	}

	static class Fix_ReadParent {
		@RestOp(method = "VIEW", path = "/x", read = true)
		public String view() { return "x"; }
	}

	/** Names its own verb without {@code read}, so the parent's {@code read} is replaced along with its verb. */
	@Rest(eagerInit = "true")
	static class Fix_ReadChildRedeclaresVerb extends Fix_ReadParent {
		@Override
		@RestOp(method = "VIEW", path = "/x")
		public String view() { return "x"; }
	}

	/** Names no verb, so the parent supplies both the verb and {@code read}. */
	@Rest(eagerInit = "true")
	static class Fix_ReadChildPathOnly extends Fix_ReadParent {
		@Override
		@RestOp(path = "/y")
		public String view() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadSvl {
		@RestOp(method = "$S{MethodSafety_Test.r08}", path = "/x", read = true)
		public String op() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_NoRead {
		@RestOp(method = "VIEW", path = "/x")
		public String view() { return "x"; }
	}

	static class Fix_ReadParentNoRead {
		@RestOp(method = "VIEW", path = "/x")
		public String view() { return "x"; }
	}

	/** A {@code read=true} that names no verb would be silently ignored, so it fails instead. */
	@Rest(eagerInit = "true")
	static class Fix_ReadMisplaced extends Fix_ReadParentNoRead {
		@Override
		@RestOp(path = "/y", read = true)
		public String view() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadInferredDelete {
		@RestOp(path = "/x", read = true)
		public void deleteFoo() { /* fixture body; only the inferred verb matters to the check */ }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadInferredGet {
		@RestOp(path = "/x", read = true)
		public String getFoo() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadLowercase {
		@RestOp(method = "view", path = "/x", read = true)
		public String view() { return "x"; }
	}

	@Rest(eagerInit = "true")
	static class Fix_ReadTrace {
		@RestOp(method = "TRACE", path = "/x", read = true)
		public String trace() { return "x"; }
	}

	/** The child's verb resolves to empty, so the verb and {@code read} both fall through to the parent. */
	@Rest(eagerInit = "true")
	static class Fix_ReadSvlEmpty extends Fix_ReadParent {
		@Override
		@RestOp(method = "$S{MethodSafety_Test.r13.unset}", path = "/x")
		public String view() { return "x"; }
	}

	static RestOpContext onlyOp(Class<?> c, Supplier<?> s) throws Exception {
		var ops = new RestContext(argsOf(c, s)).getRestOperations().getOpContexts();
		assertSize(1, ops);
		return ops.get(0);
	}

	@Test void r01_readOnCustomVerb_ok() throws Exception {
		var op = onlyOp(Fix_ReadView.class, Fix_ReadView::new);
		assertEquals("VIEW", op.getHttpMethod());
		assertTrue(op.isRead());
	}

	@Test void r02_readOnSafeVerb_ok() throws Exception {
		assertTrue(onlyOp(Fix_ReadGet.class, Fix_ReadGet::new).isRead());
	}

	@Test void r03_readOnUnsafeStandardVerb_fails() {
		var fixtures = List.<Map.Entry<Class<?>,Supplier<?>>>of(
			Map.entry(Fix_ReadPost.class, Fix_ReadPost::new),
			Map.entry(Fix_ReadPut.class, Fix_ReadPut::new),
			Map.entry(Fix_ReadPatch.class, Fix_ReadPatch::new),
			Map.entry(Fix_ReadDelete.class, Fix_ReadDelete::new),
			Map.entry(Fix_ReadConnect.class, Fix_ReadConnect::new));
		for (var f : fixtures) {
			var e = assertThrows(Exception.class, () -> new RestContext(argsOf(f.getKey(), f.getValue())), f.getKey().getSimpleName());
			assertTrue(messageChain(e).contains(f.getKey().getSimpleName() + ".write"), messageChain(e));
			assertTrue(messageChain(e).contains("read=true"), messageChain(e));
		}
	}

	@Test void r04_readOnWildcard_fails() {
		var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_ReadStar.class, Fix_ReadStar::new)));
		assertTrue(messageChain(e).contains("Fix_ReadStar.anything"), messageChain(e));
		e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_ReadMethodKeyword.class, Fix_ReadMethodKeyword::new)));
		assertTrue(messageChain(e).contains("Fix_ReadMethodKeyword.anything"), messageChain(e));
	}

	@Test void r05_readOnRrpc_fails() {
		var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_ReadRrpc.class, Fix_ReadRrpc::new)));
		assertTrue(messageChain(e).contains("Fix_ReadRrpc.proxy"), messageChain(e));
	}

	@Test void r06_readWithMutating_fails() {
		var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_ReadMutating.class, Fix_ReadMutating::new)));
		assertTrue(messageChain(e).contains("Fix_ReadMutating.view"), messageChain(e));
		assertTrue(messageChain(e).contains("@Mutating"), messageChain(e));
	}

	@Test void r07_readFromVerbAnnotation() throws Exception {
		assertFalse(onlyOp(Fix_ReadChildRedeclaresVerb.class, Fix_ReadChildRedeclaresVerb::new).isRead());
		var op = onlyOp(Fix_ReadChildPathOnly.class, Fix_ReadChildPathOnly::new);
		assertEquals("VIEW", op.getHttpMethod());
		assertTrue(op.isRead());
	}

	@Test void r08_readWithSvlMethod() throws Exception {
		var key = "MethodSafety_Test.r08";
		try {
			System.setProperty(key, "DELETE");
			var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_ReadSvl.class, Fix_ReadSvl::new)));
			assertTrue(messageChain(e).contains("Fix_ReadSvl.op"), messageChain(e));
			System.setProperty(key, "VIEW");
			assertTrue(onlyOp(Fix_ReadSvl.class, Fix_ReadSvl::new).isRead());
		} finally {
			System.clearProperty(key);
		}
	}

	@Test void r09_readDefaultFalse() throws Exception {
		assertFalse(onlyOp(Fix_NoRead.class, Fix_NoRead::new).isRead());
		assertFalse(RestOpAnnotation.DEFAULT.read());
	}

	@Test void r10_readOnNonVerbAnnotation_fails() {
		var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_ReadMisplaced.class, Fix_ReadMisplaced::new)));
		assertTrue(messageChain(e).contains("Fix_ReadMisplaced.view"), messageChain(e));
		assertTrue(messageChain(e).contains("does not supply its method"), messageChain(e));
	}

	@Test void r11_readOnInferredUnsafeVerb_fails() throws Exception {
		var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_ReadInferredDelete.class, Fix_ReadInferredDelete::new)));
		assertTrue(messageChain(e).contains("Fix_ReadInferredDelete.deleteFoo"), messageChain(e));
		var op = onlyOp(Fix_ReadInferredGet.class, Fix_ReadInferredGet::new);
		assertEquals("GET", op.getHttpMethod());
		assertTrue(op.isRead());
	}

	@Test void r12_readLowercaseAndTrace() throws Exception {
		var op = onlyOp(Fix_ReadLowercase.class, Fix_ReadLowercase::new);
		assertEquals("VIEW", op.getHttpMethod());
		assertTrue(op.isRead());
		assertTrue(onlyOp(Fix_ReadTrace.class, Fix_ReadTrace::new).isRead());
	}

	@Test void r13_readSvlEmptyFallsThrough() throws Exception {
		var op = onlyOp(Fix_ReadSvlEmpty.class, Fix_ReadSvlEmpty::new);
		assertEquals("VIEW", op.getHttpMethod());
		assertTrue(op.isRead());
	}

	@Test void r14_restOpAnnotationCarriesRead() {
		var a = RestOpAnnotation.create().method("VIEW").read(true).build();
		var b = RestOpAnnotation.create().method("VIEW").read(true).build();
		var c = RestOpAnnotation.create().method("VIEW").build();
		assertTrue(a.read());
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertTrue(a.toString().contains("read=true"), a.toString());
	}

	@Test void r15_directCall_normalizesMethod() throws Exception {
		var m = Fix_ReadView.class.getMethod("view");
		for (var httpMethod : List.of("rrpc", "Rrpc", "method", "METHOD", "delete", "Post"))
			assertThrows(RuntimeException.class, () -> MethodSafety.checkOperation(httpMethod, m, true), httpMethod);
		for (var httpMethod : List.of("view", "VIEW", "get"))
			assertDoesNotThrow(() -> MethodSafety.checkOperation(httpMethod, m, true), httpMethod);
	}

	/** The framework wraps init failures, so assertions match against the whole cause chain. */
	static String messageChain(Throwable t) {
		var sb = new StringBuilder();
		for (var x = t; x != null; x = x.getCause())
			sb.append(x.getMessage()).append(" | ");
		return sb.toString();
	}
}
