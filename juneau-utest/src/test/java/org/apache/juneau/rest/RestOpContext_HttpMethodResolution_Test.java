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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link RestOpContext#findHttpMethod()} / {@code httpMethodFromAnnotation()} /
 * {@code normalizeHttpMethod()}.
 *
 * <p>Specifically targets these previously uncovered branches:
 * <ul>
 *   <li>{@code @RestPatch} / {@code @RestOptions} fixed-verb branches.
 *   <li>{@code @RestOp(method="...")} explicit verb branch.
 *   <li>{@code @RestOp("VERB /path")} value-parsing branch (with and without space).
 *   <li>{@code @RestOp("/path")} when value has no leading verb token.
 *   <li>{@code "METHOD"}-to-{@code "*"} wildcard normalization.
 *   <li>Java method name inference fallback when no annotation supplies a verb.
 * </ul>
 */
class RestOpContext_HttpMethodResolution_Test extends TestBase {

	private static RestContext build(Class<?> c) throws Exception {
		var o = c.getDeclaredConstructor().newInstance();
		return new RestContext(new RestContextInit(c, () -> o)).postInit().postInitChildFirst();
	}

	private static String verbOf(RestContext ctx, String javaMethodName) {
		return ctx.getRestOperations().getOpContexts().stream()
			.filter(op -> javaMethodName.equals(op.getJavaMethod().getName()))
			.findFirst()
			.orElseThrow()
			.getHttpMethod();
	}

	@Rest
	public static class A {
		@RestPatch
		public void doPatch() {}

		@RestOptions
		public void doOptions() {}
	}

	@Test void a01_restPatch_yieldsPATCH() throws Exception {
		var ctx = build(A.class);
		assertEquals("PATCH", verbOf(ctx, "doPatch"));
	}

	@Test void a02_restOptions_yieldsOPTIONS() throws Exception {
		var ctx = build(A.class);
		assertEquals("OPTIONS", verbOf(ctx, "doOptions"));
	}

	@Rest
	public static class B {
		@RestOp(method = "head")
		public void someName() {}

		@RestOp("trace /x")
		public void valueWithSpace() {}

		@RestOp("connect")
		public void valueNoSpace() {}
	}

	@Test void b01_restOpMethodAttribute_explicitVerb() throws Exception {
		var ctx = build(B.class);
		assertEquals("HEAD", verbOf(ctx, "someName"));
	}

	@Test void b02_restOpValue_withSpace_parsesLeadingVerb() throws Exception {
		var ctx = build(B.class);
		assertEquals("TRACE", verbOf(ctx, "valueWithSpace"));
	}

	@Test void b03_restOpValue_singleToken_usedAsVerb() throws Exception {
		var ctx = build(B.class);
		assertEquals("CONNECT", verbOf(ctx, "valueNoSpace"));
	}

	@Rest
	public static class C {
		// "METHOD" wildcard literal must be normalized to "*".
		@RestOp(method = "method")
		public void wildcardVerb() {}

		// No verb anywhere; falls back to detectHttpMethod() which picks up the "doGet"-style prefix.
		@RestOp(path = "/x")
		public void doGet() {}
	}

	@Test void c01_methodLiteral_normalizesToWildcard() throws Exception {
		var ctx = build(C.class);
		assertEquals("*", verbOf(ctx, "wildcardVerb"));
	}

	@Test void c02_noAnnotationVerb_inferredFromJavaMethodName() throws Exception {
		var ctx = build(C.class);
		assertEquals("GET", verbOf(ctx, "doGet"));
	}
}
