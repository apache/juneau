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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link VarResolverSession} — focuses on the previously uncovered branches:
 * <ul>
 * 	<li>{@code containsVars(Collection|Map|Object)} short-circuit branches
 * 	<li>{@link VarResolverSession#resolve(String[])} array overload
 * 	<li>{@link VarResolverSession#resolve(Object)} for Sets, Lists, Maps including custom collection classes
 * 	<li>{@link VarResolverSession#resolveTo(String, java.io.Writer)} null/empty paths
 * 	<li>{@link VarResolverSession#getVar(String)} resolution
 * 	<li>{@link VarResolverSession#bean(Class, Object)} / {@link VarResolverSession#getBean(Class)} / fallback to context store
 * </ul>
 */
class VarResolverSession_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------------

	/** Echoes the var argument with surrounding brackets ($E{x} -> [x]). */
	public static class EchoVar extends SimpleVar {
		public EchoVar() { super("E"); }
		@Override public String resolve(VarResolverSession session, String arg) { return "[" + arg + "]"; }
	}

	/** Custom Set with a public no-arg constructor — used to exercise the constructor-discovery branch. */
	public static class MySet<T> extends LinkedHashSet<T> {
		private static final long serialVersionUID = 1L;
		public MySet() { super(); }
	}

	/** Custom List with a public no-arg constructor. */
	public static class MyList<T> extends ArrayList<T> {
		private static final long serialVersionUID = 1L;
		public MyList() { super(); }
	}

	/** Custom Map with a public no-arg constructor. */
	public static class MyMap<K,V> extends LinkedHashMap<K,V> {
		private static final long serialVersionUID = 1L;
		public MyMap() { super(); }
	}

	/** Set without a public no-arg constructor — forces fallback to LinkedHashSet. */
	public static class NoCtorSet<T> extends LinkedHashSet<T> {
		private static final long serialVersionUID = 1L;
		public NoCtorSet(int initialCapacity) { super(initialCapacity); }
	}

	private static VarResolver vr() {
		return VarResolver.create().vars(EchoVar.class).build();
	}

	private static VarResolverSession session() {
		return vr().createSession();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(String) basic coverage
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_resolve_null() {
		assertNull(session().resolve((String) null));
	}

	@Test void a02_resolve_empty() {
		assertEquals("", session().resolve(""));
	}

	@Test void a03_resolve_literal() {
		assertEquals("hello", session().resolve("hello"));
	}

	@Test void a04_resolve_var() {
		assertEquals("[x]", session().resolve("$E{x}"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(String[]) — line 199-203
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_resolveArray_simple() {
		var s = session();
		var out = s.resolve(new String[]{"$E{a}", "literal", "$E{b}"});
		assertArrayEquals(new String[]{"[a]", "literal", "[b]"}, out);
	}

	@Test void b02_resolveArray_empty() {
		var s = session();
		var out = s.resolve(new String[0]);
		assertEquals(0, out.length);
	}

	@Test void b03_resolveArray_withNullElement() {
		var s = session();
		var out = s.resolve(new String[]{"$E{a}", null, ""});
		assertArrayEquals(new String[]{"[a]", null, ""}, out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(Object) - null + CharSequence
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_resolveObject_null() {
		assertNull(session().resolve((Object) null));
	}

	@Test void c02_resolveObject_charSequence() {
		var r = session().resolve((Object) "$E{x}");
		assertEquals("[x]", r);
	}

	@Test void c03_resolveObject_charSequence_StringBuilder() {
		// StringBuilder is a CharSequence — should be resolved to String.
		var r = session().resolve((Object) new StringBuilder("$E{x}"));
		assertEquals("[x]", r);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(Object) - arrays, including containsVars(Object) branch (line 82-89)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_resolveArrayObject_withVars() {
		var s = session();
		var in = new String[]{"$E{a}", "$E{b}"};
		var out = s.resolve((Object) in);
		assertTrue(out instanceof String[]);
		assertArrayEquals(new String[]{"[a]", "[b]"}, (String[]) out);
	}

	@Test void d02_resolveArrayObject_noVars_returnsSame() {
		// containsVars false branch — returns same instance.
		var s = session();
		var in = new String[]{"abc", "def"};
		var out = s.resolve((Object) in);
		assertSame(in, out);
	}

	@Test void d03_resolveArrayObject_nonStringElements_noVars() {
		// Object[] of integers — no $ → returns same array.
		var s = session();
		var in = new Integer[]{1, 2};
		var out = s.resolve((Object) in);
		assertSame(in, out);
	}

	@Test void d04_resolveArrayObject_emptyArray() {
		var s = session();
		var in = new String[0];
		var out = s.resolve((Object) in);
		assertSame(in, out);  // no elements → no vars → same instance
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(Object) - Set (line 243-256)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_resolveSet_withVars() {
		var s = session();
		var in = new LinkedHashSet<>(Arrays.asList("$E{a}", "$E{b}"));
		var out = (Set<?>) s.resolve((Object) in);
		assertTrue(out.contains("[a]"));
		assertTrue(out.contains("[b]"));
	}

	@Test void e02_resolveSet_noVars_returnsSame() {
		// containsVars(Collection) false branch.
		var s = session();
		var in = new LinkedHashSet<>(Arrays.asList("abc", "def"));
		var out = s.resolve((Object) in);
		assertSame(in, out);
	}

	@Test void e03_resolveSet_customClass_preservesType() {
		// Custom Set with public no-arg ctor → reflection branch picks our class.
		var s = session();
		var in = new MySet<String>();
		in.add("$E{a}");
		var out = s.resolve((Object) in);
		assertTrue(out instanceof MySet);
		assertTrue(((Set<?>) out).contains("[a]"));
	}

	@Test void e04_resolveSet_noPublicCtor_fallsBackToLinkedHashSet() {
		var s = session();
		var in = new NoCtorSet<String>(4);
		in.add("$E{a}");
		var out = s.resolve((Object) in);
		assertTrue(out instanceof Set);
		assertTrue(((Set<?>) out).contains("[a]"));
	}

	@Test void e05_resolveSet_nonStringValues_noVars() {
		var s = session();
		var in = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
		var out = s.resolve((Object) in);
		assertSame(in, out);  // no CharSequence with $ → same instance
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(Object) - List (line 257-269)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_resolveList_withVars() {
		var s = session();
		var in = new ArrayList<>(Arrays.asList("$E{a}", "$E{b}"));
		var out = (List<?>) s.resolve((Object) in);
		assertEquals(Arrays.asList("[a]", "[b]"), out);
	}

	@Test void f02_resolveList_noVars_returnsSame() {
		var s = session();
		var in = new ArrayList<>(Arrays.asList("abc", "def"));
		var out = s.resolve((Object) in);
		assertSame(in, out);
	}

	@Test void f03_resolveList_customClass_preservesType() {
		var s = session();
		var in = new MyList<String>();
		in.add("$E{a}");
		var out = s.resolve((Object) in);
		assertTrue(out instanceof MyList);
		assertEquals("[a]", ((List<?>) out).get(0));
	}

	@Test void f04_resolveList_emptyList_returnsSame() {
		var s = session();
		var in = new ArrayList<String>();
		var out = s.resolve((Object) in);
		assertSame(in, out);  // no elements → no vars → same
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(Object) - Map (line 271-283)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_resolveMap_withVars() {
		var s = session();
		var in = new LinkedHashMap<String,String>();
		in.put("a", "$E{x}");
		in.put("b", "literal");
		var out = (Map<?,?>) s.resolve((Object) in);
		assertEquals("[x]", out.get("a"));
		assertEquals("literal", out.get("b"));
	}

	@Test void g02_resolveMap_noVars_returnsSame() {
		var s = session();
		var in = new LinkedHashMap<String,String>();
		in.put("a", "abc");
		var out = s.resolve((Object) in);
		assertSame(in, out);
	}

	@Test void g03_resolveMap_customClass_preservesType() {
		var s = session();
		var in = new MyMap<String,String>();
		in.put("k", "$E{v}");
		var out = s.resolve((Object) in);
		assertTrue(out instanceof MyMap);
		assertEquals("[v]", ((Map<?,?>) out).get("k"));
	}

	@Test void g04_resolveMap_nonStringValues_noVars() {
		var s = session();
		var in = new LinkedHashMap<String,Integer>();
		in.put("a", 1);
		var out = s.resolve((Object) in);
		assertSame(in, out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(Object) - non-supported type fallthrough
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_resolveObject_otherType_returnsSame() {
		// Integer is not handled → same instance returned (line 285).
		var s = session();
		var in = Integer.valueOf(42);
		var out = s.resolve((Object) in);
		assertSame(in, out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolveTo(String, Writer) - line 300-307
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_resolveTo_null() throws IOException {
		var s = session();
		var w = new StringWriter();
		var out = s.resolveTo(null, w);
		assertSame(w, out);
		assertEquals("", w.toString());
	}

	@Test void i02_resolveTo_empty() throws IOException {
		var s = session();
		var w = new StringWriter();
		var out = s.resolveTo("", w);
		assertSame(w, out);
		assertEquals("", w.toString());
	}

	@Test void i03_resolveTo_var() throws IOException {
		var s = session();
		var w = new StringWriter();
		s.resolveTo("$E{x}", w);
		assertEquals("[x]", w.toString());
	}

	@Test void i04_resolveTo_literal() throws IOException {
		var s = session();
		var w = new StringWriter();
		s.resolveTo("hello", w);
		assertEquals("hello", w.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// compile(String) and resolveSupplier(String) — minor coverage on the session-overload paths
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_compile() {
		var s = session();
		var t = s.compile("$E{x}");
		assertEquals("[x]", t.resolve(s));
	}

	@Test void j02_resolveSupplier() {
		var s = session();
		var sup = s.resolveSupplier("$E{x}");
		assertEquals("[x]", sup.get());
		// Supplier can be reused.
		assertEquals("[x]", sup.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// bean / getBean / context fallback
	//-----------------------------------------------------------------------------------------------------------------

	public static class MyBean {
		final String name;
		public MyBean(String name) { this.name = name; }
	}

	@Test void k01_bean_getBean_present() {
		var s = session();
		var b = new MyBean("session");
		s.bean(MyBean.class, b);
		assertSame(b, s.getBean(MyBean.class).orElse(null));
	}

	@Test void k02_getBean_absent() {
		var s = session();
		assertTrue(s.getBean(MyBean.class).isEmpty());
	}

	@Test void k03_getBean_falbackToContext() {
		// If session bean store doesn't have it, falls back to context's bean store.
		var ctxBean = new MyBean("ctx");
		var resolver = VarResolver.create().vars(EchoVar.class).bean(MyBean.class, ctxBean).build();
		var s = resolver.createSession();
		assertSame(ctxBean, s.getBean(MyBean.class).orElse(null));
	}

	@Test void k04_session_overrides_context() {
		// Session-level bean takes precedence over context-level.
		var ctxBean = new MyBean("ctx");
		var resolver = VarResolver.create().vars(EchoVar.class).bean(MyBean.class, ctxBean).build();
		var s = resolver.createSession();
		var sessionBean = new MyBean("session");
		s.bean(MyBean.class, sessionBean);
		assertSame(sessionBean, s.getBean(MyBean.class).orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toString() / properties() — line 309-321
	//-----------------------------------------------------------------------------------------------------------------

	@Test void l01_toString_includesVarMap() {
		var s = session();
		var t = s.toString();
		assertNotNull(t);
		// Should mention the registered E var name.
		assertTrue(t.contains("E"), "Expected toString to contain 'E', got: " + t);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getVar(String) — line 329-332 (called via context.getVarMap())
	//-----------------------------------------------------------------------------------------------------------------

	/** Subclass that exposes the protected {@link VarResolverSession#getVar(String)} method. */
	public static class ExposedSession extends VarResolverSession {
		public ExposedSession(VarResolver context) { super(context, null); }
		public Var exposeGetVar(String name) { return getVar(name); }
	}

	@Test void m01_getVar_present() {
		// EchoVar can resolve any session, so getVar should return it.
		var sub = new ExposedSession(vr());
		assertNotNull(sub.exposeGetVar("E"));
	}

	@Test void m02_getVar_absent() {
		var sub = new ExposedSession(vr());
		assertNull(sub.exposeGetVar("DOES_NOT_EXIST"));
	}
}
