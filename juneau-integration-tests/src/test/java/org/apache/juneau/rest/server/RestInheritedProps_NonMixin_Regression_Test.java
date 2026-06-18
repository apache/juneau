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

import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.httppart.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Blast-radius regression guard for the mixin-override property-resolution migration ("cleanup #3").
 *
 * <p>
 * That migration moves several {@code @Rest} properties &mdash; {@code defaultRequestHeaders},
 * {@code defaultResponseHeaders}, {@code defaultRequestAttributes}, {@code defaultCharset}, {@code maxInput},
 * and {@code messages} &mdash; from their ad-hoc resolution mechanisms (plain {@code getRestAnnotationsTopDown()},
 * single-annotation reads, and the {@code messages} special-case) onto the unified {@code noInherit}-aware
 * host&rarr;mixin walk so they can carry {@code @Mixin} override slots.
 *
 * <p>
 * That migration changes the resolution <i>mechanism</i> for these properties on <b>non-mixin</b> resources
 * too.  This suite pins the resolved values at the {@link RestContext}/{@link RestOpContext} API level (the
 * same direct-construction approach as {@code NoInherit_Test}, avoiding HTTP-serialization noise) for plain
 * resources &mdash; in particular the parent&rarr;child <b>class-extension inheritance</b> these properties
 * already exhibit.  Authored and made green <b>before</b> the retrofit; it must pass unchanged afterward.
 */
class RestInheritedProps_NonMixin_Regression_Test extends TestBase {

	@SuppressWarnings({
		"resource" // RestContext is a Closeable test fixture; lifecycle managed by the test, not a real leak.
	})
	private static RestContext ctx(Class<? extends BasicRestResource> c) throws Exception {
		var o = c.getDeclaredConstructor().newInstance();
		return new RestContext(new RestContext.Args(c, null, null, () -> o, "", null, null, null, RestContext.ContextKind.ROOT)).postInit().postInitChildFirst();
	}

	private static String hdr(org.apache.juneau.http.header.HttpHeaderList l, String name) {
		var h = l.getFirst(name);
		return h == null ? "<none>" : h.getValue();
	}

	private static String attr(NamedAttributeMap m, String name) {
		var a = m.get(name);
		return a == null ? "<none>" : String.valueOf(a.getValue());
	}

	private static RestOpContext op(RestContext c) {
		return c.getRestOperations().getOpContexts().get(0);
	}

	// =================================================================================
	// A. defaultRequestHeaders — parent declares; subclass adds/overrides.
	//    Pins setDefault parent-wins semantics: a key declared by both keeps the PARENT value
	//    (setDefault does not overwrite), and the child only contributes net-new keys.
	// =================================================================================

	@Rest(defaultRequestHeaders={"Foo: parent-foo", "Bar: parent-bar"})
	public static class A_Parent extends BasicRestResource {}

	@Rest(defaultRequestHeaders={"Foo: child-foo", "Baz: child-baz"})
	public static class A_Child extends A_Parent {}

	@Test void a01_defaultRequestHeaders_parent() throws Exception {
		var h = ctx(A_Parent.class).getDefaultRequestHeaders();
		assertEquals("parent-foo", hdr(h, "Foo"));
		assertEquals("parent-bar", hdr(h, "Bar"));
		assertEquals("<none>", hdr(h, "Baz"));
	}

	@Test void a02_defaultRequestHeaders_childChain() throws Exception {
		var h = ctx(A_Child.class).getDefaultRequestHeaders();
		assertEquals("parent-foo", hdr(h, "Foo"));   // parent wins (setDefault doesn't overwrite)
		assertEquals("parent-bar", hdr(h, "Bar"));   // inherited
		assertEquals("child-baz", hdr(h, "Baz"));    // child net-new
	}

	// =================================================================================
	// B. defaultResponseHeaders — parent declares; subclass adds/overrides.
	// =================================================================================

	@Rest(defaultResponseHeaders={"X-P: parent", "X-Q: parent-q"})
	public static class B_Parent extends BasicRestResource {}

	@Rest(defaultResponseHeaders={"X-P: child", "X-R: child-r"})
	public static class B_Child extends B_Parent {}

	@Test void b01_defaultResponseHeaders_parent() throws Exception {
		var h = ctx(B_Parent.class).getDefaultResponseHeaders();
		assertEquals("parent", hdr(h, "X-P"));
		assertEquals("parent-q", hdr(h, "X-Q"));
	}

	@Test void b02_defaultResponseHeaders_childChain() throws Exception {
		var h = ctx(B_Child.class).getDefaultResponseHeaders();
		assertEquals("parent", hdr(h, "X-P"));     // parent wins
		assertEquals("parent-q", hdr(h, "X-Q"));   // inherited
		assertEquals("child-r", hdr(h, "X-R"));    // child net-new
	}

	// =================================================================================
	// C. defaultRequestAttributes — parent declares; subclass adds/overrides.
	// =================================================================================

	@Rest(defaultRequestAttributes={"p1: parent1", "p2: parent2"})
	public static class C_Parent extends BasicRestResource {}

	@Rest(defaultRequestAttributes={"p1: child1", "p3: child3"})
	public static class C_Child extends C_Parent {}

	@Test void c01_defaultRequestAttributes_parent() throws Exception {
		var m = ctx(C_Parent.class).getDefaultRequestAttributes();
		assertEquals("parent1", attr(m, "p1"));
		assertEquals("parent2", attr(m, "p2"));
		assertEquals("<none>", attr(m, "p3"));
	}

	@Test void c02_defaultRequestAttributes_childChain() throws Exception {
		var m = ctx(C_Child.class).getDefaultRequestAttributes();
		// Unlike default headers (setDefault parent-wins), request attributes are a map where the CHILD's
		// value overwrites the parent's for a shared key — this asymmetry is exactly what the retrofit must preserve.
		assertEquals("child1", attr(m, "p1"));    // child wins
		assertEquals("parent2", attr(m, "p2"));   // inherited
		assertEquals("child3", attr(m, "p3"));    // child net-new
	}

	// =================================================================================
	// D. defaultCharset (op-level) — single-annotation read, inherited on subclass.
	// =================================================================================

	@Rest(defaultCharset="utf-16")
	public static class D_Parent extends BasicRestResource {
		@RestGet(path="/x")
		public String x() { return "x"; }
	}

	@Rest
	public static class D_ChildInherits extends D_Parent {}

	@Rest(defaultCharset="us-ascii")
	public static class D_ChildOverrides extends D_Parent {}

	@Test void d01_defaultCharset_parent() throws Exception {
		assertEquals(Charset.forName("utf-16"), op(ctx(D_Parent.class)).getDefaultCharset());
	}

	@Test void d02_defaultCharset_childInherits() throws Exception {
		assertEquals(Charset.forName("utf-16"), op(ctx(D_ChildInherits.class)).getDefaultCharset());
	}

	@Test void d03_defaultCharset_childOverrides() throws Exception {
		assertEquals(Charset.forName("us-ascii"), op(ctx(D_ChildOverrides.class)).getDefaultCharset());
	}

	// =================================================================================
	// E. maxInput (op-level) — single-annotation read, inherited on subclass.
	// =================================================================================

	@Rest(maxInput="10")
	public static class E_Parent extends BasicRestResource {
		@RestGet(path="/x")
		public String x() { return "x"; }
	}

	@Rest
	public static class E_ChildInherits extends E_Parent {}

	@Rest(maxInput="100")
	public static class E_ChildOverrides extends E_Parent {}

	@Test void e01_maxInput_parent() throws Exception {
		assertEquals(10L, op(ctx(E_Parent.class)).getMaxInput());
	}

	@Test void e02_maxInput_childInherits() throws Exception {
		assertEquals(10L, op(ctx(E_ChildInherits.class)).getMaxInput());
	}

	@Test void e03_maxInput_childOverrides() throws Exception {
		assertEquals(100L, op(ctx(E_ChildOverrides.class)).getMaxInput());
	}

	// =================================================================================
	// F. messages — parent declares a bundle; key lookups + bundle-miss sentinel.
	// =================================================================================

	@Rest(messages="RestInheritedPropsMessages")
	public static class F_Parent extends BasicRestResource {}

	@Test void f01_messages_parentBundle() throws Exception {
		assertEquals("parent-value1", ctx(F_Parent.class).getMessages().getString("key1"));
	}

	@Test void f02_messages_missingKeyFallsThrough() throws Exception {
		assertEquals("{!nope}", ctx(F_Parent.class).getMessages().getString("nope"));
	}
}
