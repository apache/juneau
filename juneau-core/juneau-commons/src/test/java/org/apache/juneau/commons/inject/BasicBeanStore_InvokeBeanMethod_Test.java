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
package org.apache.juneau.commons.inject;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BeanStore#invokeBeanMethod(Class, MethodInfo, Object, Object...)},
 * {@link BeanAnnotation#find(MethodInfo)} and the second (non-public, {@code @Bean}-annotated) pass of
 * {@link BasicBeanStore#createBeanFromMethod(Class, Object, java.util.function.Predicate, Object...)}.
 */
@SuppressWarnings({
	"java:S114", // Nested fixture classes are named for the scenario they exercise (Pass2PkgOnly, FindBase, ...), not to a type-naming convention
	"java:S1144", // Unused private fixture methods are invoked reflectively
	"java:S1172", // Unused fixture parameters
	"java:S2094", // Fixture classes such as ChildOfPrivate and CovChild are intentionally empty subclasses that exist only to drive inheritance lookups
	"resource", // The BasicBeanStore instances created per test are in-memory and never need closing
	"unused" // Fixture methods declare parameters/locals that are never read because they are only invoked reflectively via invokeBeanMethod
})
class BasicBeanStore_InvokeBeanMethod_Test extends TestBase {

	static class Widget {
		final String tag;
		Widget(String tag) { this.tag = tag; }
		String getTag() { return tag; }
	}

	static class SubWidget extends Widget {
		SubWidget(String tag) { super(tag); }
	}

	private static MethodInfo m(Class<?> c, String name) {
		return ClassInfo.of(c).getDeclaredMethods().stream().filter(x -> x.getNameSimple().equals(name)).findFirst().orElseThrow();
	}

	private static Widget[] tags(Widget...w) { return w; }

	//-----------------------------------------------------------------------------------------------------------------
	// invokeBeanMethod
	//-----------------------------------------------------------------------------------------------------------------

	public static class Fixture {
		@Bean public Widget pub() { return new Widget("pub"); }
		@Bean protected Widget prot() { return new Widget("prot"); }
		@Bean Widget pkg() { return new Widget("pkg"); }
		@Bean private Widget priv() { return new Widget("priv"); }
		@Bean public static Widget stat() { return new Widget("stat"); }
		@Deprecated @Bean public Widget dep() { return new Widget("dep"); }
		public void voidMethod() { /* no-op */ }
		public Widget needsString(String s) { return new Widget(s); }
		public Widget needsMissing(Integer i) { return new Widget("x"); }
		public Widget make() { throw new IllegalStateException("boom"); }
		public String notAWidget() { return "s"; }
		public Widget bridgeMe() { return new Widget("bridge"); }
	}

	@Test void a01_public() {
		var r = new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "pub"), new Fixture());
		assertEquals("pub", r.get().tag);
	}

	@Test void a02_protected() {
		var r = new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "prot"), new Fixture());
		assertEquals("prot", r.get().tag);
	}

	@Test void a03_packagePrivate() {
		var r = new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "pkg"), new Fixture());
		assertEquals("pkg", r.get().tag);
	}

	@Test void a04_private() {
		var r = new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "priv"), new Fixture());
		assertEquals("priv", r.get().tag);
	}

	@Test void a05_staticWithClassTarget() {
		var r = new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "stat"), Fixture.class);
		assertEquals("stat", r.get().tag);
	}

	@Test void a06_instanceWithClassTarget_empty() {
		assertTrue(new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "pub"), Fixture.class).isEmpty());
	}

	@Test void a07_unresolvableParam_empty() {
		assertTrue(new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "needsMissing"), new Fixture()).isEmpty());
	}

	@Test void a08_paramFromStore() {
		var store = new BasicBeanStore(null);
		store.addBean(String.class, "fromStore");
		assertEquals("fromStore", store.invokeBeanMethod(Widget.class, m(Fixture.class, "needsString"), new Fixture()).get().tag);
	}

	@Test void a09_paramFromExtraBeans() {
		assertEquals("extra", new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "needsString"), new Fixture(), "extra").get().tag);
	}

	@Test void a10_deprecatedIsInvoked() {
		assertEquals("dep", new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(Fixture.class, "dep"), new Fixture()).get().tag);
	}

	@Test void a11_voidReturn_empty() {
		assertTrue(new BasicBeanStore(null).invokeBeanMethod(Object.class, m(Fixture.class, "voidMethod"), new Fixture()).isEmpty());
	}

	@Test void a12_beanTypeNotAssignable_iae() {
		var store = new BasicBeanStore(null);
		var mi = m(Fixture.class, "pub");
		var fixture = new Fixture();
		assertThrows(IllegalArgumentException.class, () -> store.invokeBeanMethod(String.class, mi, fixture));
		assertThrows(IllegalArgumentException.class, () -> store.invokeBeanMethod(SubWidget.class, mi, fixture));
	}

	@Test void a13_supertypeBeanTypeAllowed() {
		assertEquals("pub", new BasicBeanStore(null).invokeBeanMethod(Object.class, m(Fixture.class, "pub"), new Fixture()).map(x -> ((Widget)x).tag).get());
	}

	@Test void a14_foreignDeclaringClass_iae() {
		var store = new BasicBeanStore(null);
		var mi = m(Fixture.class, "pub");
		assertThrows(IllegalArgumentException.class, () -> store.invokeBeanMethod(Widget.class, mi, "notAFixture"));
		assertThrows(IllegalArgumentException.class, () -> store.invokeBeanMethod(Widget.class, mi, String.class));
	}

	@Test void a15_nulls_iae() {
		var store = new BasicBeanStore(null);
		var mi = m(Fixture.class, "pub");
		var fixture = new Fixture();
		assertThrows(IllegalArgumentException.class, () -> store.invokeBeanMethod(null, mi, fixture));
		assertThrows(IllegalArgumentException.class, () -> store.invokeBeanMethod(Widget.class, null, fixture));
		assertThrows(IllegalArgumentException.class, () -> store.invokeBeanMethod(Widget.class, mi, null));
	}

	@Test void a16_throwingMethod_wrapped() {
		var store = new BasicBeanStore(null);
		var mi = m(Fixture.class, "make");
		var o = new Fixture();
		var e = assertThrowsWithMessage(BeanCreationException.class, "via method [Fixture.make", () -> store.invokeBeanMethod(Widget.class, mi, o));
		assertEquals("Failed to create bean of type [Widget] via method [Fixture.make]", e.getMessage());
	}

	public static class ParentWithPrivate {
		@Bean private Widget hidden() { return new Widget("hidden"); }
	}

	public static class ChildOfPrivate extends ParentWithPrivate {}

	@Test void a17_privateInheritedMethodOnSubclassInstance() {
		var r = new BasicBeanStore(null).invokeBeanMethod(Widget.class, m(ParentWithPrivate.class, "hidden"), new ChildOfPrivate());
		assertEquals("hidden", r.get().tag);
	}

	public static class BeanCreationThrower {
		public Widget make() { throw new BeanCreationException("inner"); }
	}

	@Test void a18_beanCreationExceptionNotDoubleWrapped() {
		var store = new BasicBeanStore(null);
		var mi = m(BeanCreationThrower.class, "make");
		var o = new BeanCreationThrower();
		var e = assertThrows(BeanCreationException.class, () -> store.invokeBeanMethod(Widget.class, mi, o));
		assertEquals("inner", e.getMessage());
	}

	public static class Covariant extends Fixture {
		@Override public Widget bridgeMe() { return new SubWidget("cov"); }
	}

	public static class CovBase {
		public Widget make() { return new Widget("base"); }
	}
	public static class CovChild extends CovBase {
		@Override public SubWidget make() { return new SubWidget("child"); }
	}

	@Test void a19_bridgeMethod_empty() {
		var bridge = ClassInfo.of(CovChild.class).getDeclaredMethods().stream()
			.filter(x -> eq(x.getNameSimple(), "make") && x.isBridge()).findFirst().orElseThrow();
		assertTrue(new BasicBeanStore(null).invokeBeanMethod(Widget.class, bridge, new CovChild()).isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanAnnotation.find
	//-----------------------------------------------------------------------------------------------------------------

	public static class FindBase {
		@Bean(name="base") public Widget foo() { return null; }
		@Bean(name="priv") private Widget hid() { return null; }
		@Bean(name="stat") public static Widget sta() { return null; }
		@Bean(name="pkgParent") Widget pkgMethod() { return null; }
	}

	public static class FindChild extends FindBase {
		@Override public Widget foo() { return null; }
		public Widget hid() { return null; }
		@SuppressWarnings({
			"java:S9149" // Test fixture: deliberately hides FindBase.sta() to exercise static-method annotation lookup.
		})
		public static Widget sta() { return null; }
		@Bean(name="own") public Widget own() { return null; }
		@Override Widget pkgMethod() { return null; }
	}

	@Test void b01_ownAnnotation() {
		assertEquals("own", BeanAnnotation.name(BeanAnnotation.find(m(FindChild.class, "own")).get()));
	}

	@Test void b02_inheritedThroughTrueOverride() {
		assertEquals("base", BeanAnnotation.name(BeanAnnotation.find(m(FindChild.class, "foo")).get()));
	}

	@Test void b03_notInheritedFromPrivateParent() {
		assertTrue(BeanAnnotation.find(m(FindChild.class, "hid")).isEmpty());
	}

	@Test void b04_notInheritedFromStaticParent() {
		assertTrue(BeanAnnotation.find(m(FindChild.class, "sta")).isEmpty());
	}

	@Test void b05_bridgeIsEmpty() {
		var bridge = ClassInfo.of(CovChild.class).getDeclaredMethods().stream()
			.filter(x -> eq(x.getNameSimple(), "make") && x.isBridge()).findFirst().orElseThrow();
		assertTrue(BeanAnnotation.find(bridge).isEmpty());
	}

	@Test void b06_packagePrivateOverrideInSamePackage() {
		assertEquals("pkgParent", BeanAnnotation.name(BeanAnnotation.find(m(FindChild.class, "pkgMethod")).get()));
	}

	@Test void b07_parentOwn() {
		assertEquals("priv", BeanAnnotation.name(BeanAnnotation.find(m(FindBase.class, "hid")).get()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// createBeanFromMethod second pass
	//-----------------------------------------------------------------------------------------------------------------

	public static class Pass2PkgOnly {
		@Bean Widget pkg() { return new Widget("pkg"); }
	}

	@Test void c01_packagePrivateBeanFound() {
		assertEquals("pkg", new BasicBeanStore(null).createBeanFromMethod(Widget.class, new Pass2PkgOnly()).get().tag);
	}

	public static class Pass2PublicWins {
		@Bean Widget nonPublic() { return new Widget("nonPublic"); }
		public Widget pub() { return new Widget("public"); }
	}

	@Test void c02_publicWins() {
		assertEquals("public", new BasicBeanStore(null).createBeanFromMethod(Widget.class, new Pass2PublicWins()).get().tag);
	}

	public static class Pass2NoAnnotation {
		Widget helper() { return new Widget("helper"); }
		private Widget helper2() { return new Widget("helper2"); }
	}

	@Test void c03_nonPublicWithoutBeanNeverFound() {
		assertTrue(new BasicBeanStore(null).createBeanFromMethod(Widget.class, new Pass2NoAnnotation(), null).isEmpty());
	}

	public static class Pass2Filter {
		@Bean(name="a") Widget a() { return new Widget("a"); }
		@Bean(name="b") Widget b() { return new Widget("b"); }
	}

	@Test void c04_filterAppliesInPass2() {
		var r = new BasicBeanStore(null).createBeanFromMethod(Widget.class, new Pass2Filter(), mi -> eq(mi.getNameSimple(), "b"));
		assertEquals("b", r.get().tag);
		assertTrue(new BasicBeanStore(null).createBeanFromMethod(Widget.class, new Pass2Filter(), mi -> false).isEmpty());
	}

	public static class Pass2Parent {
		@Bean Widget parentOne() { return new Widget("parent"); }
	}
	public static class Pass2Child extends Pass2Parent {
		@Bean Widget childOne() { return new Widget("child"); }
	}

	@Test void c05_childFirst() {
		assertEquals("child", new BasicBeanStore(null).createBeanFromMethod(Widget.class, new Pass2Child()).get().tag);
	}

	public static class Pass2Override extends Pass2Parent {
		@Override Widget parentOne() { return new Widget("overridden"); }
	}

	@Test void c06_overriddenParentNotInvokedTwice() {
		assertEquals("overridden", new BasicBeanStore(null).createBeanFromMethod(Widget.class, new Pass2Override()).get().tag);
	}

	public static class Pass2Throws {
		@Bean Widget boom() { throw new IllegalStateException("x"); }
	}

	@Test void c07_pass2Failure_wrapped() {
		var store = new BasicBeanStore(null);
		var o = new Pass2Throws();
		assertThrowsWithMessage(BeanCreationException.class, "via method [Pass2Throws.boom", () -> store.createBeanFromMethod(Widget.class, o));
	}

	public interface StaticIface {
		@Bean static Widget sm() { return new Widget("iface"); }
	}
	public static class ImplIface implements StaticIface {}

	@Test void c08_interfaceStaticExcluded() {
		assertTrue(new BasicBeanStore(null).createBeanFromMethod(Widget.class, ImplIface.class).isEmpty());
	}

	public static class Pass2Static {
		@Bean static Widget sm() { return new Widget("static"); }
	}

	@Test void c09_staticNonPublicWithClassTarget() {
		assertEquals("static", new BasicBeanStore(null).createBeanFromMethod(Widget.class, Pass2Static.class).get().tag);
	}

	public static class Pass2Deprecated {
		@Deprecated @Bean Widget old() { return new Widget("old"); }
	}

	@Test void c10_deprecatedSkipped() {
		assertTrue(new BasicBeanStore(null).createBeanFromMethod(Widget.class, new Pass2Deprecated()).isEmpty());
	}
}
