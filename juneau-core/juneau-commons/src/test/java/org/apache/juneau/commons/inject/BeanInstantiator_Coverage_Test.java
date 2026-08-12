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

import static org.apache.juneau.commons.TestAssertions.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage-gap tests for {@link BeanInstantiator}, supplementing {@code BeanInstantiator_Test} and
 * {@code BeanInstantiator_OptionD_Test}.
 *
 * <p>
 * Each nested group below is named for the specific branch/line gap (per {@code scripts/coverage.py}) it closes:
 * <ul>
 * 	<li class='note'>A — {@code findBeanImpl()}: loose-builder runtime-subtype acceptance, the
 * 		{@code Builder.anything()} fallback's {@code @Inject} ternary, and the explicit-builder/fallback path.
 * 	<li class='note'>B — {@code findBeanSubTypes()}: multi-level hierarchy traversal and the defensive "should
 * 		never happen" throw (see b02 below, which pins a suspected latent bug).
 * 	<li class='note'>C — {@code findBuilderType()}: the supertype-only ("weak") builder <b>winning</b> case
 * 		(Priority 3a/3b/3c) when no usable direct constructor exists, plus {@code isStrictBuilderType()}'s
 * 		descendant-return-type clause and {@code hasUsableDirectConstructor()}'s private-constructor exclusion.
 * 	<li class='note'>D — {@code inject()}: the {@code @ConfigProperties} short-circuit for a non-instance
 * 		target (builder injected via {@code injectBuilder()}).
 * 	<li class='note'>E — {@code autoWireBuilder()}: unwrap-disabled, wildcard-generic / unregistered-type
 * 		skip, and best-effort exception swallowing.
 * </ul>
 */
@SuppressWarnings({
	"java:S2094", // Intentionally empty helper beans.
	"resource"    // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class BeanInstantiator_Coverage_Test extends TestBase {

	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore(null);
	}

	private <T> BeanInstantiator.Builder<T> bc(Class<T> c) {
		return BeanInstantiator.of(c, beanStore);
	}

	//=================================================================================================================
	// A - findBeanImpl(): loose-builder acceptance, Builder.anything() ternary, explicit-builder fallback.
	//=================================================================================================================

	// Builder.build() declares the PARENT return type but constructs and returns the exact beanSubType at
	// runtime (the legacy "parent builder produces a preconfigured subclass" pattern documented on findBeanImpl()).
	public static class A01_Parent {
		protected final String value;
		public A01_Parent(String value) { this.value = value; }
		public String getValue() { return value; }
	}

	public static class A01_Child extends A01_Parent {
		public A01_Child(String value) { super(value); }
	}

	public static class A01_Builder {
		private String value = "default";
		// @formatter:off
		public A01_Builder value(String value) { this.value = value; return this;}
		// @formatter:on
		public A01_Parent build() { return new A01_Child(value); }
		public static A01_Builder create() { return new A01_Builder(); }
	}

	@Test
	void a01_looseBuilderAcceptsRuntimeSubtypeInstance() {
		var bean = bc(A01_Parent.class).type(A01_Child.class).builder(A01_Builder.class).run();
		assertInstanceOf(A01_Child.class, bean);
		assertEquals("default", bean.getValue());
	}

	// Builder.build() declares the wrong (non-ancestor-exact) return type AND returns null at runtime; the
	// "returned null but expected" log-message branch (builtBean == null ternary) must be exercised, and bean
	// creation must fall through to the direct constructor.
	public static class A02_Base {
		protected final String v;
		public A02_Base(String v) { this.v = v; }
		public String getV() { return v; }
	}

	public static class A02_Sub extends A02_Base {
		public A02_Sub() { super("ctor-fallback"); }
	}

	public static class A02_Builder {
		public A02_Base build() { return null; }
		public static A02_Builder create() { return new A02_Builder(); }
	}

	@Test
	void a02_looseBuilderNullResultFallsThroughToConstructor() {
		var creator = bc(A02_Base.class).type(A02_Sub.class).builder(A02_Builder.class).debug();
		var bean = creator.run();
		assertInstanceOf(A02_Sub.class, bean);
		assertEquals("ctor-fallback", bean.getV());
		assertContains("returned null but expected", creator.getDebugLog().toString());
	}

	// Builder.anything() fallback: one candidate has a parameter and no @Inject (filtered out via the
	// parameterCount==0 check), another has @Inject with a resolvable parameter (selected via the ternary's
	// true branch).
	public static class A03_Bean {
		protected final String v;
		public A03_Bean(String v) { this.v = v; }
		public String getV() { return v; }

		public static class Builder {
			public A03_Bean withParamNoInject(String extra) { return new A03_Bean("no-inject:" + extra); }
			@Inject
			public A03_Bean withInject(String dep) { return new A03_Bean("inject:" + dep); }
			public static Builder create() { return new Builder(); }
		}
	}

	@Test
	void a03_anyMethodFallbackPrefersInjectAnnotatedCandidate() {
		beanStore.add(String.class, "dep-value");
		var bean = bc(A03_Bean.class).run();
		assertEquals("inject:dep-value", bean.getV());
	}

	// Builder.anything() fallback selects a candidate that returns null at runtime; bean stays null and
	// falls through to hasAnyMethodWithRightReturnType/hasBuildMethodWithRightReturnType logic, which then
	// falls through further to the direct constructor.
	public static class A04_Bean {
		protected final String v;
		public A04_Bean() { this.v = "ctor"; }
		public String getV() { return v; }

		public static class Builder {
			public A04_Bean makeIt() { return null; }
			public static Builder create() { return new Builder(); }
		}
	}

	@Test
	void a04_anyMethodNullResultFallsThroughToConstructor() {
		var bean = bc(A04_Bean.class).run();
		assertEquals("ctor", bean.getV());
	}

	// Builder has a build() method matching by name+return-type (hasBuildMethodWithRightReturnType == true,
	// checked independently of parameter resolvability) but its only parameter is unresolvable, so the
	// stricter `buildMethod` search (which DOES require 0-args-or-resolvable) never finds/invokes it --
	// builderAttempted stays false. With a fallback supplier configured, control reaches the
	// hasBuildMethodWithRightReturnType branch's fallbackSupplier check instead of throwing (contrast with
	// BeanInstantiator_Test's d27, which hits the same branch WITHOUT a fallback and throws instead).
	public static class A05_Unresolvable {
		public A05_Unresolvable() { /* not registered in any store -- intentionally unresolvable */ }
	}

	public static class A05_Bean {
		public static class Builder {
			@SuppressWarnings({
				"unused" // Parameter required so the build() method is unresolvable, per the scenario under test.
			})
			public A05_Bean build(A05_Unresolvable unresolvable) { return new A05_Bean(); }
			public static Builder create() { return new Builder(); }
		}
	}

	@Test
	void a05_unresolvableBuildMethodUsesFallbackInsteadOfThrowing() {
		var fallbackBean = new A05_Bean();
		var creator = bc(A05_Bean.class).fallback(() -> fallbackBean);
		var bean = creator.run();
		assertSame(fallbackBean, bean);
	}

	//=================================================================================================================
	// B - findBeanSubTypes(): multi-level traversal and the defensive throw.
	//=================================================================================================================

	public static class B01_GrandParent { /* empty */ }
	public static class B01_Parent extends B01_GrandParent { /* empty */ }
	public static class B01_Child extends B01_Parent { /* empty */ }

	@Test
	void b01_findBeanSubTypesTraversesMultiLevelHierarchy() {
		var creator = bc(B01_GrandParent.class).type(B01_Child.class);
		var subTypes = creator.getBeanSubTypes();
		assertEquals(3, subTypes.size());
		assertEquals(B01_Child.class.getName(), subTypes.get(0).getName());
		assertEquals(B01_Parent.class.getName(), subTypes.get(1).getName());
		assertEquals(B01_GrandParent.class.getName(), subTypes.get(2).getName());
	}

	public interface B02_IFace { /* empty */ }
	public static class B02_Impl implements B02_IFace { /* empty */ }

	/**
	 * LATENT-BUG PIN: {@code findBeanSubTypes()}'s final defensive check is commented
	 * "This should never happen if beanSubType validation is correct" — but it CAN happen. {@code ClassInfo.getParents()}
	 * only walks the <b>superclass</b> chain (it deliberately excludes interfaces), while {@code type(Class)}'s
	 * guard only requires {@code beanType.isAssignableFrom(beanSubType)} (satisfied by interface implementation
	 * too). So requesting an interface {@code beanType} with a concrete implementing {@code beanSubType} passes
	 * {@code type()}'s validation but then throws here, because the interface never appears in the superclass-only
	 * parent list. This test pins the CURRENT (buggy) throwing behavior; it does not assert this is desired.
	 */
	@Test
	void b02_findBeanSubTypesThrowsWhenBeanTypeIsInterfaceNotInSuperclassChain() {
		var creator = bc(B02_IFace.class).type(B02_Impl.class);
		var ex = assertThrows(IllegalArgumentException.class, creator::getBeanSubTypes);
		assertContains("was not found in the parent hierarchy", ex.getMessage());
	}

	//=================================================================================================================
	// C - findBuilderType(): weak-builder-wins gating, isStrictBuilderType()'s descendant clause, and
	// hasUsableDirectConstructor()'s private-constructor exclusion.
	//=================================================================================================================

	// Priority 3a (static factory method): a supertype-only ("weak") builder must WIN when the requested
	// subtype has no usable direct constructor (here, because it's abstract).
	public abstract static class C01_Base {
		public static C01_Builder builder() { return new C01_Builder(); }
		public static class C01_Builder {
			public C01_Base build() { return null; }
		}
	}

	public abstract static class C01_Sub extends C01_Base { /* empty; abstract -> no usable direct ctor */ }

	@Test
	void c01_weakStaticBuilderWinsWhenSubtypeHasNoUsableConstructor() {
		var creator = bc(C01_Base.class).type(C01_Sub.class);
		var builderType = creator.getBuilderType();
		assertEquals(C01_Base.C01_Builder.class.getName(), builderType.getName());
	}

	// Priority 3b (own inner Builder class): weak builder WINS when the subtype is abstract.
	public static class C02_Base { /* empty */ }
	public abstract static class C02_Sub extends C02_Base {
		public static class Builder {
			public C02_Base build() { return new C02_Base(); }
		}
	}

	@Test
	void c02_weakInnerBuilderWinsWhenSubtypeHasNoUsableConstructor() {
		var creator = bc(C02_Base.class).type(C02_Sub.class);
		var builderType = creator.getBuilderType();
		assertEquals(C02_Sub.Builder.class.getName(), builderType.getName());
	}

	// Priority 3b: weak builder LOSES to a direct constructor when the subtype is concrete and has a usable ctor.
	public static class C03_Base { /* empty */ }
	public static class C03_Sub extends C03_Base {
		public boolean viaCtor;
		public C03_Sub() { this.viaCtor = true; }
		public static class Builder {
			public C03_Base build() { return new C03_Base(); }
		}
	}

	@Test
	void c03_weakInnerBuilderLosesToDirectConstructor() {
		var creator = bc(C03_Base.class).type(C03_Sub.class);
		var bean = creator.run();
		assertNull(creator.getBuilderType(), "Weak inner builder must not be selected when a direct constructor exists.");
		assertInstanceOf(C03_Sub.class, bean);
		assertTrue(((C03_Sub) bean).viaCtor);
	}

	// Priority 3c (parent's inner Builder class): weak builder WINS when the subtype is abstract.
	public static class C04_GrandBase { /* empty */ }
	public abstract static class C04_MidParent extends C04_GrandBase {
		public static class Builder {
			public C04_GrandBase build() { return new C04_GrandBase(); }
		}
	}

	public abstract static class C04_Sub extends C04_MidParent { /* empty; no own inner class */ }

	@Test
	void c04_weakParentInnerBuilderWinsWhenSubtypeHasNoUsableConstructor() {
		var creator = bc(C04_GrandBase.class).type(C04_Sub.class);
		var builderType = creator.getBuilderType();
		assertEquals(C04_MidParent.Builder.class.getName(), builderType.getName());
	}

	// isStrictBuilderType()'s second OR clause (beanSubType.isParentOf(rt)) requires a DIFFERENT build-method
	// overload than the one that satisfies isValidBuilderType() -- one overload returns an ancestor (satisfies
	// validity), a second overload (different build-method name, e.g. "create") returns a proper DESCENDANT of
	// beanSubType (satisfies strictness). Both are on the same builder class.
	public static class C05_Base { /* empty */ }
	public static class C05_Sub extends C05_Base {
		public static class Builder {
			public C05_Base build() { return new C05_Base(); }
			public C05_MoreSpecific create() { return new C05_MoreSpecific(); }
		}
	}

	public static class C05_MoreSpecific extends C05_Sub { /* empty */ }

	@Test
	void c05_isStrictBuilderTypeDescendantReturnTypeClause() {
		var creator = bc(C05_Sub.class);
		var builderType = creator.getBuilderType();
		assertEquals(C05_Sub.Builder.class.getName(), builderType.getName());
	}

	// hasUsableDirectConstructor()'s NOT_PRIVATE filter: a PRIVATE-only constructor must not count as "usable",
	// so a weak builder wins even though the subtype is concrete (not abstract).
	public static class C06_Base { /* empty */ }
	public static class C06_Sub extends C06_Base {
		@SuppressWarnings("unused")
		private C06_Sub() { /* private-only: no accessible direct constructor */ }
		public static class Builder {
			public C06_Base build() { return new C06_Base(); }
		}
	}

	@Test
	void c06_weakBuilderWinsWhenOnlyConstructorIsPrivate() {
		var creator = bc(C06_Sub.class);
		var builderType = creator.getBuilderType();
		assertEquals(C06_Sub.Builder.class.getName(), builderType.getName());
	}

	// Priority 3c (parent's inner Builder class), isStrictBuilderType() TRUE case: the parent's inner Builder's
	// build() method returns the exact concrete beanSubType (not just an ancestor), so the strict branch of the
	// `isStrictBuilderType(builderClass) ? builderClass : gateWeakBuilder(builderClass)` ternary is taken
	// directly -- gateWeakBuilder() (and thus hasUsableDirectConstructor()) is never consulted, even though
	// beanSubType here is concrete with a usable default constructor.
	public static class C07_GrandBase { /* empty */ }
	public static class C07_MidParent extends C07_GrandBase {
		public static class Builder {
			public C07_Sub build() { return new C07_Sub(); }
		}
	}

	public static class C07_Sub extends C07_MidParent { /* concrete; has a usable default ctor */ }

	@Test
	void c07_strictParentInnerBuilderWinsWithoutConsultingConstructorGate() {
		var creator = bc(C07_GrandBase.class).type(C07_Sub.class);
		var builderType = creator.getBuilderType();
		assertEquals(C07_MidParent.Builder.class.getName(), builderType.getName());
	}

	// isStrictBuilderType()'s @Inject-annotation disjunct: "build" only satisfies VALIDITY (0-arg, ancestor
	// return type -- filtered out of isStrictBuilderType's stricter same-or-descendant return-type filter), so
	// the ONLY candidate reaching isStrictBuilderType's final anyMatch is "create", which has a parameter (so
	// parameterCount()==0 is false) but IS @Inject-annotated -- forcing the match to happen via the annotation
	// disjunct rather than the parameterCount()==0 disjunct exercised by c05/c07.
	public static class C08_Base { /* empty */ }
	public static class C08_Sub extends C08_Base {
		public static class Builder {
			public C08_Base build() { return new C08_Base(); } // ancestor return -> validity only
			@Inject
			public C08_MoreSpecific create(String dep) { return new C08_MoreSpecific(dep); } // descendant return + @Inject -> strictness via inject clause
		}
	}

	public static class C08_MoreSpecific extends C08_Sub {
		public final String dep;
		public C08_MoreSpecific(String dep) { this.dep = dep; }
	}

	@Test
	void c08_isStrictBuilderTypeInjectAnnotationClause() {
		var creator = bc(C08_Base.class).type(C08_Sub.class);
		var builderType = creator.getBuilderType();
		assertEquals(C08_Sub.Builder.class.getName(), builderType.getName());
	}

	//=================================================================================================================
	// D - inject(): @ConfigProperties short-circuit for a non-instance target (injectBuilder() on the builder).
	//=================================================================================================================

	@ConfigProperties(prefix = "cov345")
	public static class D01_ConfigBean {
		public final String value;
		D01_ConfigBean(Builder b) { this.value = b.value; }
		public static class Builder {
			String value = "builder-default";
			public D01_ConfigBean build() { return new D01_ConfigBean(this); }
		}
	}

	@Test
	void d01_injectBuilderOnConfigPropertiesBeanSkipsBindingForNonInstanceBuilder() {
		// beanType carries @ConfigProperties, but injectBuilder() ALSO calls inject() on the BUILDER
		// instance -- which is not an instance of the annotated bean type -- driving the
		// `! beanType.inner().isInstance(bean)` short-circuit at the top of inject().
		var bean = bc(D01_ConfigBean.class).injectBuilder().run();
		assertEquals("builder-default", bean.value);
	}

	//=================================================================================================================
	// E - autoWireBuilder(): unwrap-disabled, wildcard-generic / unregistered-type skip, exception swallowing.
	//=================================================================================================================

	static class E01_Service { /* empty */ }

	public static class E01_Bean {
		public final String wired;
		E01_Bean(Builder b) { this.wired = b.svcSupplier == null ? "no-supplier" : "has-supplier"; }
		public static class Builder {
			Supplier<E01_Service> svcSupplier;
			public void setSvcSupplier(Supplier<E01_Service> s) { this.svcSupplier = s; }
			public E01_Bean build() { return new E01_Bean(this); }
		}
	}

	@Test
	void e01_autoWireUnwrapSuppliersDisabledSkipsSupplierWrapping() {
		beanStore.addBean(E01_Service.class, new E01_Service());
		var creator = bc(E01_Bean.class).builder(E01_Bean.Builder.class).autoWireBuilder();
		// Package-private test hook: autoWireUnwrapSuppliers has no public setter.
		creator.autoWireUnwrapSuppliers = false;
		var bean = creator.run();
		assertEquals("no-supplier", bean.wired,
			"With unwrap disabled, the raw Supplier type itself is looked up in the store (not present), so the setter is skipped.");
	}

	static class E02_ServiceA { /* empty */ }
	static class E02_ServiceB { /* empty */ }

	public static class E02_Bean {
		public final boolean wired1;
		public final boolean wired2;
		E02_Bean(Builder b) { this.wired1 = b.s1 != null; this.wired2 = b.s2 != null; }
		public static class Builder {
			Supplier<? extends E02_ServiceA> s1;
			Supplier<E02_ServiceB> s2;
			public void setS1(Supplier<? extends E02_ServiceA> v) { this.s1 = v; }
			public void setS2(Supplier<E02_ServiceB> v) { this.s2 = v; }
			public E02_Bean build() { return new E02_Bean(this); }
		}
	}

	@Test
	void e02_autoWireSupplierSkipsWildcardGenericAndUnregisteredType() {
		// s1's Supplier<? extends E02_ServiceA> generic argument is a WildcardType, not a Class -- drives the
		// `args[0] instanceof Class<?>` false branch. s2's Supplier<E02_ServiceB> is well-formed but no
		// E02_ServiceB bean is registered -- drives the `wrappedBean.isPresent()` false branch. Both setters
		// must be silently skipped (best-effort auto-wire).
		var bean = bc(E02_Bean.class).builder(E02_Bean.Builder.class).autoWireBuilder().run();
		assertFalse(bean.wired1);
		assertFalse(bean.wired2);
	}

	public static class E03_Bean {
		public final String value;
		E03_Bean(Builder b) { this.value = b.value; }
		public static class Builder {
			String value = "unchanged";
			@SuppressWarnings({
				"unused" // Parameter required to match the setter signature invoked via reflection during auto-wire.
			})
			public void setValue(String v) { throw rex("boom - setter intentionally fails to exercise autoWireBuilder()'s best-effort exception swallow"); }
			public E03_Bean build() { return new E03_Bean(this); }
		}
	}

	@Test
	void e03_autoWireSwallowsSetterInvocationException() {
		beanStore.add(String.class, "wired-value");
		var bean = bc(E03_Bean.class).builder(E03_Bean.Builder.class).autoWireBuilder().run();
		assertEquals("unchanged", bean.value,
			"Setter threw during auto-wire invocation; the exception must be swallowed and the field left at its default.");
	}

	@SuppressWarnings({
		"rawtypes" // Supplier field/setter intentionally use a raw type to drive the non-ParameterizedType branch.
	})
	public static class E04_Bean {
		public final boolean wired;
		E04_Bean(Builder b) { this.wired = b.raw != null; }
		public static class Builder {
			Supplier raw;
			public void setRaw(Supplier s) { this.raw = s; }
			public E04_Bean build() { return new E04_Bean(this); }
		}
	}

	@Test
	void e04_autoWireSupplierSkipsRawNonParameterizedGenericType() {
		// setRaw's parameter is a raw `Supplier` with no generic signature at all -- getGenericParameterTypes()
		// returns the raw Class (Supplier.class) rather than a ParameterizedType, driving the
		// `generic instanceof ParameterizedType` false branch. The setter must be silently skipped.
		var bean = bc(E04_Bean.class).builder(E04_Bean.Builder.class).autoWireBuilder().run();
		assertFalse(bean.wired);
	}

	public interface E05_MultiSupplier<X, Y> extends Supplier<X> {
		// Second type parameter is used here (rather than left decorative) solely so the setter's parameter
		// type carries two actual generic type arguments, per the scenario under test.
		Y other();
	}
	static class E05_ServiceX { /* empty */ }
	static class E05_ServiceY { /* empty */ }

	public static class E05_Bean {
		public final boolean wired;
		E05_Bean(Builder b) { this.wired = b.multi != null; }
		public static class Builder {
			E05_MultiSupplier<E05_ServiceX, E05_ServiceY> multi;
			public void setMulti(E05_MultiSupplier<E05_ServiceX, E05_ServiceY> v) { this.multi = v; }
			public E05_Bean build() { return new E05_Bean(this); }
		}
	}

	@Test
	void e05_autoWireSupplierSkipsGenericTypeWithMultipleTypeArguments() {
		beanStore.addBean(E05_ServiceX.class, new E05_ServiceX());
		// setMulti's parameter type has TWO actual type arguments (X, Y), unlike the single argument
		// Supplier<T> normally has -- driving the `args.length == 1` false branch. The setter must be
		// silently skipped.
		var bean = bc(E05_Bean.class).builder(E05_Bean.Builder.class).autoWireBuilder().run();
		assertFalse(bean.wired);
	}

	//=================================================================================================================
	// F - log(): the silent() + debug() combination.
	//=================================================================================================================

	public static class F01_Bean { /* empty; default-constructible */ }

	@Test
	void f01_logStillRecordsToDebugLogEvenWhenSilent() {
		// log()'s early-return guard is `silent && !debug.isPresent()`. No existing test combines silent()
		// (suppresses the java.util.logging output) WITH debug() (enables the in-memory debug log) -- this
		// drives the `!debug.isPresent()` operand's FALSE outcome, proving messages still get recorded to the
		// debug log even while silent.
		var creator = bc(F01_Bean.class).silent().debug();
		creator.run();
		assertFalse(creator.getDebugLog().isEmpty());
	}
}
