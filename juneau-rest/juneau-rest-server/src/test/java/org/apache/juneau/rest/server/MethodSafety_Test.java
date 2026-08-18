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

import static org.junit.jupiter.api.Assertions.*;

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
class MethodSafety_Test extends org.apache.juneau.TestBase {

	//-----------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------

	/** Carries one method per shape the rule cares about; group b reflects over these rather than dispatching. */
	@SuppressWarnings("unused")
	static class Fix_Methods {
		@Mutating public void declared() {}
		@Mutating("the stored credential") public void declaredWithNote() {}
		public void undeclared() {}
	}

	/** A superclass declaration must still be seen when the subclass overrides the method. */
	@SuppressWarnings("unused")
	static class Fix_Parent {
		@Mutating public void inherited() {}
	}

	@SuppressWarnings("unused")
	static class Fix_Child extends Fix_Parent {
		@Override public void inherited() {}
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
		var e = assertThrows(RuntimeException.class,
			() -> MethodSafety.checkOperation("GET", method(Fix_Methods.class, "declared")));
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
		var e = assertThrows(RuntimeException.class,
			() -> MethodSafety.checkOperation("*", method(Fix_Methods.class, "declared")));
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
		assertDoesNotThrow(() -> MethodSafety.checkOperation("GET", method(Fix_Methods.class, "undeclared")));
	}

	@Test void b06_noteIsIncludedInTheFailure() throws Exception {
		var e = assertThrows(RuntimeException.class,
			() -> MethodSafety.checkOperation("GET", method(Fix_Methods.class, "declaredWithNote")));
		assertTrue(e.getMessage().contains("the stored credential"), e.getMessage());
	}

	@Test void b07_failureNamesTheRestOpInferenceTrap() throws Exception {
		// The message has to mention it: in the @RestOp case the developer never typed GET, so a message that
		// only says "bound to GET" reads as wrong rather than as informative.
		var e = assertThrows(RuntimeException.class,
			() -> MethodSafety.checkOperation("GET", method(Fix_Methods.class, "declared")));
		assertTrue(e.getMessage().contains("infers the method"), e.getMessage());
	}

	@Test void b08_inheritedDeclarationIsSeenThroughAnOverride() throws Exception {
		// Method.getAnnotation would return null here; the check resolves through MethodInfo for this reason.
		assertThrows(RuntimeException.class,
			() -> MethodSafety.checkOperation("GET", method(Fix_Child.class, "inherited")));
	}

	@Test void b09_nullHttpMethodIsNotAContradiction() throws Exception {
		// Not safe, so nothing is being claimed twice. Fail-closed at request time covers it.
		assertDoesNotThrow(() -> MethodSafety.checkOperation(null, method(Fix_Methods.class, "declared")));
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
		assertEquals(1, ctx.getRestOperations().getOpContexts().size());
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
		assertEquals(1, ctx.getRestOperations().getOpContexts().size());
	}

	/** The framework wraps init failures, so assertions match against the whole cause chain. */
	static String messageChain(Throwable t) {
		var sb = new StringBuilder();
		for (var x = t; x != null; x = x.getCause())
			sb.append(x.getMessage()).append(" | ");
		return sb.toString();
	}
}
