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

import static org.apache.juneau.commons.utils.StringUtils.parseLongWithSuffix;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for op-level annotation overrides on:
 * <ul>
 *   <li>{@link RestOpContext#getDefaultCharset()} — exercises the {@code v.isPresent()} branch in
 *       {@code findDefaultCharset()}.
 *   <li>{@link RestOpContext#getMaxInput()} — exercises the {@code v.isPresent()} branch in
 *       {@code findMaxInput()}.
 *   <li>{@link RestOpContext#createSession} — exercises the explicit-debug branch in
 *       {@code findDebugEnablement()} via {@code @RestGet(debug="true")}.
 * </ul>
 *
 * <p>These cases were not covered by {@code NoInherit_Test}, which only exercises class-level
 * {@code @Rest} fallbacks and the {@code noInherit} skip path.
 */
class RestOpContext_OpLevelOverrides_Test extends TestBase {

	private static RestContext build(Class<?> c) throws Exception {
		var o = c.getDeclaredConstructor().newInstance();
		return new RestContext(new RestContextInit(c, () -> o)).postInit().postInitChildFirst();
	}

	private static RestOpContext op(RestContext ctx) {
		return ctx.getRestOperations().getOpContexts().get(0);
	}

	@Rest(defaultCharset = "UTF-8")
	public static class A {
		@RestGet(defaultCharset = "ISO-8859-1")
		public void get() {}
	}

	@Test void a01_opLevelDefaultCharset_overridesClassLevel() throws Exception {
		var ctx = build(A.class);
		assertEquals(StandardCharsets.ISO_8859_1, op(ctx).getDefaultCharset(),
			"@RestGet(defaultCharset=...) should override class-level @Rest(defaultCharset=...)");
	}

	@Rest(maxInput = "1M")
	public static class B {
		@RestOp(method = "get", maxInput = "9M")
		public void get() {}
	}

	@Test void b01_opLevelMaxInput_overridesClassLevel() throws Exception {
		var ctx = build(B.class);
		assertEquals(parseLongWithSuffix("9M"), op(ctx).getMaxInput(),
			"@RestOp(maxInput=...) should override class-level @Rest(maxInput=...)");
	}

	@Rest
	public static class C {
		@RestGet(debug = "true")
		public void get() {}
	}

	@Test void c01_opLevelDebug_buildsSuccessfully() throws Exception {
		// Building the context invokes findDebugEnablement() in the constructor; a non-empty
		// debug attribute exercises the "v.isPresent()" branch in that finder.
		var ctx = build(C.class);
		assertNotNull(op(ctx));
	}
}
