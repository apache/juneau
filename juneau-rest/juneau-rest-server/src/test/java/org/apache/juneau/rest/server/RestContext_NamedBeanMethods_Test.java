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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.rest.server.util.*;
import org.junit.jupiter.api.*;

/**
 * Tests that several {@link Bean @Bean} methods of the same return type and different names on one {@link Rest @Rest}
 * class each register their own result, that overrides run once, that non-public methods are honored, and that
 * named framework-typed methods alias the framework's single per-context instance.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1144", // Unused private methods are the fixtures: they are invoked reflectively via @Bean.
	"resource", // getBeanStore() returns the context's own (already-owned) BeanStore; the test doesn't own it.
	"unused" // Private @Bean fixture methods and their injected parameters (e.g. u(Missing m)) are only invoked reflectively
})
class RestContext_NamedBeanMethods_Test extends org.apache.juneau.TestBase {

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	/** Invocation counts by fixture method label. */
	static final Map<String,Integer> CALLS = new HashMap<>();

	static void called(String label) { CALLS.merge(label, 1, Integer::sum); }

	static int calls(String label) { return CALLS.getOrDefault(label, 0); }

	@BeforeEach void reset() { CALLS.clear(); }

	public static class Widget {
		private final String name;
		public Widget(String name) { this.name = name; }
		public String getName() { return name; }
	}

	public static class Missing {}

	private static <T> T bean(RestContext ctx, Class<T> type, String name) {
		return ctx.getBeanStore().getBean(type, name).orElse(null);
	}

	private static List<String> keys(RestContext ctx, Class<?> type) {
		return new ArrayList<>(new TreeSet<>(ctx.getBeanStore().getBeansOfType(type).keySet()));
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - each named method registers its own result
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	public static class Fix_Named {
		@Bean(name="a") public Widget a() { called("a"); return new Widget("A"); }
		@Bean(name="b") public Widget b() { called("b"); return new Widget("B"); }
		@Bean(name="c") public Widget c() { called("c"); return new Widget("C"); }
		@Bean public Widget plain() { called("plain"); return new Widget("P"); }
		@Bean("v") public Widget v() { called("v"); return new Widget("V"); }
	}

	private static RestContext named() throws Exception {
		return new RestContext(argsOf(Fix_Named.class, Fix_Named::new));
	}

	@Test void a01_eachNameResolvesToItsOwnMethod() throws Exception {
		var ctx = named();
		assertBean(bean(ctx, Widget.class, "a"), "name", "A");
		assertBean(bean(ctx, Widget.class, "b"), "name", "B");
		assertBean(bean(ctx, Widget.class, "c"), "name", "C");
		assertBean(ctx.getBeanStore().getBean(Widget.class).orElse(null), "name", "P");
		assertList(keys(ctx, Widget.class), "", "a", "b", "c", "v");
	}

	@Test void a02_eachMethodInvokedOnce() throws Exception {
		named();
		assertEquals(List.of(1, 1, 1, 1, 1), List.of(calls("a"), calls("b"), calls("c"), calls("plain"), calls("v")));
	}

	@Test void a03_valueAliasNamesTheBean() throws Exception {
		assertBean(bean(named(), Widget.class, "v"), "name", "V");
	}

	//-----------------------------------------------------------------------------------------------------------
	// a04 - overrides are invoked once (child result)
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_OverrideParent {
		@Bean(name="a") public Widget a() { called("parent.a"); return new Widget("parent"); }
	}

	@Rest
	public static class Fix_OverrideChild extends Fix_OverrideParent {
		@Override @Bean(name="a") public Widget a() { called("child.a"); return new Widget("child"); }
	}

	@Test void a04_overriddenMethodInvokedOnceChildWins() throws Exception {
		var ctx = new RestContext(argsOf(Fix_OverrideChild.class, Fix_OverrideChild::new));
		assertBean(bean(ctx, Widget.class, "a"), "name", "child");
		assertEquals(List.of(1, 0), List.of(calls("child.a"), calls("parent.a")));
	}

	//-----------------------------------------------------------------------------------------------------------
	// a05 - first wins: child class before parent
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_FirstWinsParent {
		@Bean public Widget aParent() { called("aParent"); return new Widget("parent"); }
	}

	@Rest
	public static class Fix_FirstWinsChild extends Fix_FirstWinsParent {
		@Bean public Widget zChild() { called("zChild"); return new Widget("child"); }
	}

	@Test void a05_unnamedCollisionChildLevelWins() throws Exception {
		var ctx = new RestContext(argsOf(Fix_FirstWinsChild.class, Fix_FirstWinsChild::new));
		assertBean(ctx.getBeanStore().getBean(Widget.class).orElse(null), "name", "child");
	}

	//-----------------------------------------------------------------------------------------------------------
	// a06 - unresolvable parameter
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	public static class Fix_Unresolvable {
		@Bean(name="u")
		public Widget u(Missing m) { called("u"); return new Widget("U"); }
	}

	@Test void a06_unresolvableParameterLeavesBeanAbsent() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Unresolvable.class, Fix_Unresolvable::new));
		assertNull(bean(ctx, Widget.class, "u"));
		assertEquals(0, calls("u"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// a07/a08 - any visibility, static
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	public static class Fix_Visibility {
		@Bean(name="pkg") Widget pkg() { return new Widget("pkg"); }
		@Bean(name="prot") protected Widget prot() { return new Widget("prot"); }
		@Bean(name="priv") private Widget priv() { return new Widget("priv"); }
		@Bean(name="s") public static Widget stat() { return new Widget("static"); }
	}

	@Test void a07_nonPublicMethodsRegistered() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Visibility.class, Fix_Visibility::new));
		assertBean(bean(ctx, Widget.class, "pkg"), "name", "pkg");
		assertBean(bean(ctx, Widget.class, "prot"), "name", "prot");
		assertBean(bean(ctx, Widget.class, "priv"), "name", "priv");
	}

	@Test void a08_staticMethodRegistered() throws Exception {
		assertBean(bean(new RestContext(argsOf(Fix_Visibility.class, Fix_Visibility::new)), Widget.class, "s"), "name", "static");
	}

	//-----------------------------------------------------------------------------------------------------------
	// a09 - factory throws
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	public static class Fix_Throws {
		@Bean(name="boom") public Widget boom() { throw new IllegalStateException("kaboom"); }
	}

	@Test void a09_factoryFailureNamesTheMethod() {
		var e = assertThrows(Exception.class, () -> new RestContext(argsOf(Fix_Throws.class, Fix_Throws::new)));
		var sb = new StringBuilder();
		for (Throwable t = e; t != null; t = t.getCause())
			sb.append(t.getMessage()).append('|');
		assertContains("Failed to create bean of type", sb);
		assertContains("boom", sb);
	}

	//-----------------------------------------------------------------------------------------------------------
	// a10 - children see the same instance
	//-----------------------------------------------------------------------------------------------------------

	@Rest(path="/child")
	public static class Fix_Child {}

	@Rest(children={Fix_Child.class})
	public static class Fix_Host {
		@Bean(name="a") public Widget a() { return new Widget("A"); }
		@Bean(name="b") public Widget b() { return new Widget("B"); }
	}

	@Test void a10_childSeesSameInstancePerName() throws Exception {
		var host = new RestContext(argsOf(Fix_Host.class, Fix_Host::new));
		var child = host.getRestChildren().asMap().get("child");
		assertBean(bean(child, Widget.class, "b"), "name", "B");
		assertSame(bean(host, Widget.class, "a"), bean(child, Widget.class, "a"));
		assertSame(bean(host, Widget.class, "b"), bean(child, Widget.class, "b"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// a11 - @Bean("x") value alias on a framework named slot
	//-----------------------------------------------------------------------------------------------------------

	@Rest
	public static class Fix_ValueAliasFramework {
		@Bean("postInitMethods") public MethodList myPostInit() { return MethodList.of(List.of()); }
		@RestPostInit public void init() { /* default list would contain this method */ }
	}

	@Test void a11_valueAliasOnFrameworkNamedSlotResolved() throws Exception {
		var ctx = new RestContext(argsOf(Fix_ValueAliasFramework.class, Fix_ValueAliasFramework::new));
		assertTrue(ctx.getPostInitMethods().isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------
	// a12 - named framework type aliases the framework instance
	//-----------------------------------------------------------------------------------------------------------

	@Rest(path="/enc")
	public static class Fix_EncChild {}

	@Rest(children={Fix_EncChild.class})
	public static class Fix_NamedFramework {
		@Bean(name="encoders")
		public EncoderSet encoders() { called("encoders"); return EncoderSet.create(new BasicBeanStore()).build(); }
	}

	@Test void a12_namedFrameworkTypeInvokedOnceAndAliased() throws Exception {
		var ctx = new RestContext(argsOf(Fix_NamedFramework.class, Fix_NamedFramework::new));
		ctx.getEncoders();
		var child = ctx.getRestChildren().asMap().get("enc");
		child.getEncoders();
		assertEquals(1, calls("encoders"));
		assertSame(ctx.getBeanStore().getBean(EncoderSet.class).orElse(null), bean(ctx, EncoderSet.class, "encoders"));
		assertNull(bean(child, EncoderSet.class, "encoders"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// a13 - package-private framework-typed @Bean
	//-----------------------------------------------------------------------------------------------------------

	static final RichLogger CUSTOM_LOGGER = RichLogger.getLogger("RestContext_NamedBeanMethods_Test.custom");

	@Rest
	public static class Fix_PkgFramework {
		@Bean RichLogger logger() { return CUSTOM_LOGGER; }
	}

	@Test void a13_packagePrivateFrameworkBeanUsed() throws Exception {
		var ctx = new RestContext(argsOf(Fix_PkgFramework.class, Fix_PkgFramework::new));
		assertSame(CUSTOM_LOGGER, ctx.getLogger());
	}

	//-----------------------------------------------------------------------------------------------------------
	// a15 / R1 - covariant override registers under both the child and the parent return types
	//-----------------------------------------------------------------------------------------------------------

	public static class Base {}
	public static class Sub extends Base {}

	public static class Fix_CovParent {
		@Bean(name="x") public Base thing() { called("parent.thing"); return new Base(); }
	}

	@Rest
	public static class Fix_CovChild extends Fix_CovParent {
		@Override public Sub thing() { called("child.thing"); return new Sub(); }
	}

	@Test void a15_covariantOverrideInvokedOnceRegisteredUnderBothTypes() throws Exception {
		var ctx = new RestContext(argsOf(Fix_CovChild.class, Fix_CovChild::new));
		assertEquals(List.of(1, 0), List.of(calls("child.thing"), calls("parent.thing")));
		assertNotNull(bean(ctx, Sub.class, "x"));
		assertSame(bean(ctx, Sub.class, "x"), bean(ctx, Base.class, "x"));
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - review findings
	//-----------------------------------------------------------------------------------------------------------

	public static class Fix_HiddenParent {
		@Bean(name="p") private Widget make() { called("parent.make"); return new Widget("parent"); }
	}

	@Rest
	public static class Fix_HiddenChild extends Fix_HiddenParent {
		private Widget make() { called("child.make"); return new Widget("child"); }
	}

	@Test void b01_unannotatedPrivateHelperDoesNotHideParentBean() throws Exception {
		var ctx = new RestContext(argsOf(Fix_HiddenChild.class, Fix_HiddenChild::new));
		assertBean(bean(ctx, Widget.class, "p"), "name", "parent");
		assertEquals(List.of(0, 1), List.of(calls("child.make"), calls("parent.make")));
	}

	@Rest
	public static class Fix_FieldAndMethod {
		@Bean(name="f") public Widget field = new Widget("field");
		@Bean(name="f") public Widget method() { called("method"); return new Widget("method"); }
	}

	@Test void b02_fieldWinsOverMethodWithSameKey() throws Exception {
		var ctx = new RestContext(argsOf(Fix_FieldAndMethod.class, Fix_FieldAndMethod::new));
		assertBean(bean(ctx, Widget.class, "f"), "name", "field");
		assertEquals(0, calls("method"));
	}

	@Rest
	public static class Fix_MethodScoped {
		@Bean(name="scopedEnc", methodScope="x")
		public EncoderSet scoped() { return EncoderSet.create(new BasicBeanStore()).build(); }
	}

	@Test void b03_methodScopedFrameworkTypeNotAliasedIntoClassStore() throws Exception {
		var ctx = new RestContext(argsOf(Fix_MethodScoped.class, Fix_MethodScoped::new));
		assertNull(bean(ctx, EncoderSet.class, "scopedEnc"));
	}

	@Rest(children={Fix_Child.class})
	public static class Fix_UserNamedMethodList {
		@Bean(name="mine") public MethodList mine() { called("mine"); return MethodList.of(List.of()); }
	}

	@Test void b04_userNamedMethodListIsAUserTypeInvokedAndExported() throws Exception {
		var ctx = new RestContext(argsOf(Fix_UserNamedMethodList.class, Fix_UserNamedMethodList::new));
		var child = ctx.getRestChildren().asMap().get("child");
		assertEquals(1, calls("mine"));
		assertNotNull(bean(ctx, MethodList.class, "mine"));
		assertSame(bean(ctx, MethodList.class, "mine"), bean(child, MethodList.class, "mine"));
	}
}
