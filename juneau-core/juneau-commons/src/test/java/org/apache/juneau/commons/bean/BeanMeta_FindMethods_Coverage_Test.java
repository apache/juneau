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
package org.apache.juneau.commons.bean;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for the many method-signature-pattern branches inside {@link BeanMeta#findBeanMethods()}
 * (0/1/2-parameter dispatch, dyna "*" combinations, bare-{@code @BeanProp} name-derivation fallbacks).
 */
@SuppressWarnings({
	"unused"  // Test POJO fields/methods are read reflectively through BeanMeta, not directly.
})
class BeanMeta_FindMethods_Coverage_Test extends TestBase {

	//====================================================================================================
	// 0-param: bare @BeanProp name-derivation fallback (bpName.isEmpty() branch) - reached only when the
	// method's own name would otherwise match "get"/"is" but the return type disqualifies it (void), or the
	// name matches neither convention at all.
	//====================================================================================================

	public static class BareBeanPropZeroParamBean {
		@BeanProp
		public void getFoo() { /* no-op */ }
		@BeanProp
		public void isFoo() { /* no-op */ }
		@BeanProp
		public String fooBar() { return "x"; }
	}

	// SUSPECTED BUG (not fixed here): a normal, unannotated `getFoo()` (non-void)
	// resolves to property "foo" (lowercased by the PropertyNamer at line ~1436) - but this bare-@BeanProp
	// void-returning `getFoo()` resolves to "Foo" (capital F, unlowercased).  Root cause: the bpName-fallback
	// branch (~1391-1396) sets `bpName = n` using the *pre-namer* stripped name ("Foo"), and that raw value
	// then overrides the already-namered `n` again at line ~1442-1443 (`if (nn(bpName) && !bpName.isEmpty())
	// n = bpName;`), undoing the lowercasing that just happened one line earlier for every other bpName-driven
	// getter shape.  Pinning current (buggy) behavior here rather than silently fixing it.
	@Test
	void a01_bareBeanProp_voidGetPrefixed_stripsGetPrefix_butSkipsNamerLowercasing() {
		var bm = BeanMeta.of(BareBeanPropZeroParamBean.class);
		assertTrue(bm.getProperties().containsKey("Foo"));
		assertFalse(bm.getProperties().containsKey("foo"));
	}

	@Test
	void a02_bareBeanProp_voidIsPrefixed_stripsIsPrefix_butSkipsNamerLowercasing() {
		// isFoo() is void, so the dedicated "is" getter branch (which requires a boolean return type)
		// doesn't match; falls through to the bare-@BeanProp fallback, which strips "is" instead of "get".
		// Same casing quirk as a01 above - both isFoo() and getFoo() land on the SAME (capitalized)
		// property name "Foo"; BeanMeta just keeps whichever method is processed last as the registered getter.
		var bm = BeanMeta.of(BareBeanPropZeroParamBean.class);
		assertTrue(bm.getProperties().containsKey("Foo"));
	}

	@Test
	void a03_bareBeanProp_noConventionalPrefix_nameUnchanged() {
		var bm = BeanMeta.of(BareBeanPropZeroParamBean.class);
		assertTrue(bm.getProperties().containsKey("fooBar"));
	}

	//====================================================================================================
	// 0-param: "get" name-shaped method that returns void - disqualified from the getter branch by the
	// void-return guard, distinct from a01 above only in that this one carries NO @BeanProp at all, so it's
	// simply dropped (methodType stays UNKNOWN) rather than falling through to the bare-annotation fallback.
	//====================================================================================================

	public static class VoidGetterNoAnnotationBean {
		public String name;
		public void getFoo() { /* no-op - looks like a getter but returns void, so it's not one */ }
	}

	@Test
	void a04_voidReturningGetPrefixed_noAnnotation_notRegisteredAsProperty() {
		var bm = BeanMeta.of(VoidGetterNoAnnotationBean.class);
		assertTrue(bm.getProperties().containsKey("name"));
		assertFalse(bm.getProperties().containsKey("foo"));
	}

	//====================================================================================================
	// 0-param: "is" name-shaped method returning the *wrapper* Boolean (as opposed to primitive boolean) -
	// still recognized as a boolean getter per the `rt.is(Boolean.TYPE) || rt.is(Boolean.class)` check.
	//====================================================================================================

	public static class BoxedBooleanIsGetterBean {
		private Boolean flag = Boolean.TRUE;
		public Boolean isFlag() { return flag; }
		public void setFlag(Boolean v) { flag = v; }
	}

	@Test
	void a05_isPrefixed_boxedBooleanReturn_recognizedAsGetter() {
		var bm = BeanMeta.of(BoxedBooleanIsGetterBean.class);
		var pm = bm.getPropertyMeta("flag");
		assertNotNull(pm);
		assertNotNull(pm.getGetter());
		assertEquals("isFlag", pm.getGetter().getNameSimple());
	}

	//====================================================================================================
	// 1-param dyna "*": Map-typed single param -> SETTER (whole-map replacement, distinct from the
	// 2-param String-keyed setExtras(key, value) dyna setter already covered elsewhere).
	//====================================================================================================

	public static class DynaWholeMapSetterBean {
		private Map<String,Object> extras = new LinkedHashMap<>();
		public Map<String,Object> getExtras() { return extras; }
		@BeanProp(name = "*")
		public void setExtras(Map<String,Object> m) { extras = m; }
	}

	@Test
	void b01_dynaOneParamMapSetter_recognizedAsSetter() {
		var bm = BeanMeta.of(DynaWholeMapSetterBean.class);
		var dyna = bm.getDynaProperty();
		assertNotNull(dyna);
		assertNotNull(dyna.getSetter());
		assertEquals("setExtras", dyna.getSetter().getNameSimple());
	}

	//====================================================================================================
	// 1-param dyna "*": param type is neither Map nor String -> neither sub-branch matches, methodType stays
	// UNKNOWN, which throws (same outlet as b03 below, but reached via the params.size()==1 code path rather
	// than the params.isEmpty() one - the two throw sites share a message but are driven by different guards).
	//====================================================================================================

	public static class DynaOneParamNonMatchingBean {
		@BeanProp(name = "*")
		public void setExtras(int notMapOrString) { /* no-op - wrong param type for either dyna sub-branch */ }
	}

	@Test
	void b02_dynaOneParam_nonMapNonStringParam_throws() {
		var ex = assertThrows(BeanRuntimeException.class, () -> BeanMeta.of(DynaOneParamNonMatchingBean.class));
		assertTrue(ex.getMessage().contains("could not determine method type"));
	}

	//====================================================================================================
	// "*" bpName but methodType stays UNKNOWN for ALL matching methods on the class -> throws (the
	// dedicated exception at the end of the per-method processing loop, distinct from b02 above where
	// another method on the same class DID resolve to a valid dyna methodType).
	//====================================================================================================

	public static class DynaUnresolvableBean {
		public String name;
		@BeanProp(name = "*")
		public String notAValidDynaShape() { return "x"; }
	}

	@Test
	void b03_dynaBpName_methodTypeUnresolvable_throws() {
		var ex = assertThrows(BeanRuntimeException.class, () -> BeanMeta.of(DynaUnresolvableBean.class));
		assertTrue(ex.getMessage().contains("could not determine method type"));
	}

	//====================================================================================================
	// 2-param: "*" dyna shape that does NOT match the setter pattern (name doesn't start with "set") ->
	// falls through to the `else { methodType = GETTER; }` branch for 2-param methods.
	//====================================================================================================

	public static class DynaTwoParamNonSetterShapeBean {
		private final Map<String,Object> extras = new LinkedHashMap<>(Map.of("a", 1));
		@BeanProp(name = "*")
		public Object getExtras(String key) { return extras.get(key); }
		@BeanProp(name = "*")
		public Object oddTwoArgMethod(String a, String b) { return null; }
	}

	@Test
	void c01_dynaTwoParam_nonSetterShape_fallsThroughToGetterBranch() {
		// oddTwoArgMethod's 2-param condition (bpName=="*" && param0==String && name.startsWith("set") && ...)
		// is false purely because the method name doesn't start with "set" - exercising the `else` GETTER
		// fallback at the end of the params.size()==2 block.  The resulting BeanMeta still just reflects
		// whichever dyna getter method was processed last; this test only proves construction doesn't throw
		// and the dyna property remains discoverable.
		var bm = BeanMeta.of(DynaTwoParamNonSetterShapeBean.class);
		assertNotNull(bm.getDynaProperty());
	}

	//====================================================================================================
	// 1-param setter: "setX" name-shaped method whose return type disqualifies the direct setter branch
	// (not void, not assignable from the bean type) - mirrors the getter-side a01/a03 bare-@BeanProp
	// fallback, but for the SETTER half of the params.size()==1 block (~1416-1424).
	//====================================================================================================

	public static class BareBeanPropOneParamSetterBean {
		@BeanProp
		public String setFoo(String v) { return v; }
		@BeanProp
		public void fooBarSetter(String v) { /* no-op - name matches neither "set" nor "with" */ }
	}

	@Test
	void d01_bareBeanProp_setPrefixed_nonVoidNonBeanReturn_fallsThroughToBareFallback() {
		// setFoo(String) returns String (not void, not assignable from the bean class), so the dedicated
		// "set" branch's return-type guard fails; falls through to the bare-@BeanProp fallback, which still
		// recognizes it as a SETTER and strips "set".  Same casing quirk noted on a01 - this lands
		// on "Foo" (unlowercased), not "foo".
		var bm = BeanMeta.of(BareBeanPropOneParamSetterBean.class);
		var pm = bm.getPropertyMeta("Foo");
		assertNotNull(pm);
		assertNotNull(pm.getSetter());
	}

	@Test
	void d02_bareBeanProp_noConventionalSetterPrefix_nameUnchanged() {
		var bm = BeanMeta.of(BareBeanPropOneParamSetterBean.class);
		var pm = bm.getPropertyMeta("fooBarSetter");
		assertNotNull(pm);
		assertNotNull(pm.getSetter());
	}

	//====================================================================================================
	// 1-param setter: "setX" that returns the bean type itself (fluent, but spelled "setX" rather than
	// "withX") - exercises the `rt.isAssignableFrom(ci)` TRUE side of the OR (short-circuiting before the
	// `rt.is(Void.TYPE)` check), distinct from the ubiquitous void-returning "setX" shape used everywhere
	// else in this test suite.
	//====================================================================================================

	public static class SelfReturningSetterBean {
		private String name;
		public SelfReturningSetterBean setName(String v) { name = v; return this; }
	}

	@Test
	void d03_setPrefixed_selfReturningSetter_recognizedAsSetter() {
		var bm = BeanMeta.of(SelfReturningSetterBean.class);
		var pm = bm.getPropertyMeta("name");
		assertNotNull(pm);
		assertNotNull(pm.getSetter());
		assertEquals("setName", pm.getSetter().getNameSimple());
	}

	//====================================================================================================
	// newBean(): interface with useInterfaceProxies disabled has no constructor AND no proxy handler ->
	// falls all the way through to the final `return null;` (as opposed to l02/l03 in BeanMeta_Coverage_Test,
	// which only check getBeanProxyInvocationHandler() directly without exercising newBean() itself).
	//====================================================================================================

	public interface NoProxyIface {
		String getName();
	}

	@Test
	void e01_newBean_interfaceWithProxiesDisabled_returnsNull() throws Exception {
		var cfg = BeanConfigContext.create().useInterfaceProxies(false).build();
		var bm = BeanMeta.of(NoProxyIface.class, cfg);
		assertFalse(bm.hasConstructor());
		assertNull(bm.getBeanProxyInvocationHandler());
		assertNull(bm.newBean(null));
	}

	//====================================================================================================
	// 1-param setter: "with"-prefixed method whose return type ISN'T assignable from the bean class - the
	// dedicated "with" branch's return-type guard fails, and (with no annotation/fluentSetters) the method
	// is dropped entirely - distinct from h01 (WithSetterBean, in BeanMeta_Coverage_Test) where the return
	// type IS assignable from the bean class.
	//====================================================================================================

	public static class VoidWithPrefixedBean {
		public String name;
		public void withFoo(String v) { /* no-op - "with"-prefixed but void return, not fluent */ }
	}

	@Test
	void f01_withPrefixed_voidReturn_notRecognizedAsSetter() {
		var bm = BeanMeta.of(VoidWithPrefixedBean.class);
		assertTrue(bm.getProperties().containsKey("name"));
		assertFalse(bm.getProperties().containsKey("foo"));
		assertFalse(bm.getProperties().containsKey("Foo"));
	}

	//====================================================================================================
	// 1-param setter: explicit (non-empty) @BeanProp name on a method that doesn't match "set"/"with" at
	// all - exercises the `else { n = bpName; }` branch (bpName.isEmpty()==false) of the setter-side
	// bare/explicit-@BeanProp fallback, as opposed to d01/d02 above (which both use a BARE @BeanProp, so
	// bpName is empty rather than a real name).
	//====================================================================================================

	public static class ExplicitNamedOneParamSetterBean {
		@BeanProp(name = "customSet")
		public void applyValue(String v) { /* no-op */ }
	}

	@Test
	void f02_explicitBeanPropName_oneParamNonConventionalSetter_usesAnnotationName() {
		var bm = BeanMeta.of(ExplicitNamedOneParamSetterBean.class);
		var pm = bm.getPropertyMeta("customSet");
		assertNotNull(pm);
		assertNotNull(pm.getSetter());
	}

	//====================================================================================================
	// 2-param: "*" dyna shape that DOES match every conjunct of the setter pattern (String key, "set"-prefixed,
	// void return) - the only fixture in this file to drive the 2-param dyna condition's SETTER outcome, as
	// opposed to c01 above (name doesn't start with "set") and b02 (1-param, non-matching type).
	//====================================================================================================

	public static class DynaTwoParamSetterBean {
		private final Map<String,Object> extras = new LinkedHashMap<>();
		@BeanProp(name = "*")
		public void setExtras(String key, Object value) { extras.put(key, value); }
	}

	@Test
	void c02_dynaTwoParam_setterShape_recognizedAsSetter() {
		var bm = BeanMeta.of(DynaTwoParamSetterBean.class);
		var dyna = bm.getDynaProperty();
		assertNotNull(dyna);
		assertNotNull(dyna.getSetter());
	}

	//====================================================================================================
	// 2-param: "*" dyna shape whose first param ISN'T a String - the second conjunct is false (as opposed to
	// c02 above, where it's true), short-circuiting the rest and falling through to the `else` GETTER branch
	// even though the method name starts with "set".
	//====================================================================================================

	public static class DynaTwoParamNonStringKeyBean {
		@BeanProp(name = "*")
		public void setExtras(int notAString, Object value) { /* no-op - wrong first-param type for dyna setter shape */ }
	}

	@Test
	void c03_dynaTwoParam_nonStringFirstParam_fallsThroughToGetterBranch() {
		var bm = BeanMeta.of(DynaTwoParamNonStringKeyBean.class);
		assertNotNull(bm.getDynaProperty());
	}

	//====================================================================================================
	// 2-param: "*" dyna shape whose return type IS assignable to the declaring class (a fluent setter) -
	// the `rt.isAssignableFrom(ci)` disjunct is true here, short-circuiting `rt.is(Void.TYPE)` (as opposed
	// to c02 above, where the return type is void and only the second disjunct is true).
	//====================================================================================================

	public static class DynaTwoParamFluentSetterBean {
		private final Map<String,Object> extras = new LinkedHashMap<>();
		@BeanProp(name = "*")
		public DynaTwoParamFluentSetterBean setExtras(String key, Object value) { extras.put(key, value); return this; }
	}

	@Test
	void c04_dynaTwoParam_fluentSetterShape_returnTypeAssignableToBean_recognizedAsSetter() {
		var bm = BeanMeta.of(DynaTwoParamFluentSetterBean.class);
		var dyna = bm.getDynaProperty();
		assertNotNull(dyna);
		assertNotNull(dyna.getSetter());
	}

	//====================================================================================================
	// 2-param: "*" dyna shape, String key, "set"-prefixed, but return type is NEITHER void NOR assignable to
	// the declaring class (an unrelated return type) - both disjuncts of `rt.isAssignableFrom(ci) ||
	// rt.is(Void.TYPE)` must be explicitly evaluated to false here (as opposed to c02, where the second
	// disjunct short-circuits true via void, and c04, where the first disjunct short-circuits true via a
	// fluent return type), so the whole condition is false and this falls through to the `else` GETTER
	// branch despite matching every other conjunct.
	//====================================================================================================

	public static class DynaTwoParamUnrelatedReturnTypeSetterBean {
		private final Map<String,Object> extras = new LinkedHashMap<>();
		@BeanProp(name = "*")
		public String setExtras(String key, Object value) { extras.put(key, value); return "ignored"; }
	}

	@Test
	void c04b_dynaTwoParam_setterShape_unrelatedReturnType_fallsThroughToGetterBranch() {
		var bm = BeanMeta.of(DynaTwoParamUnrelatedReturnTypeSetterBean.class);
		assertNotNull(bm.getDynaProperty());
	}

	//====================================================================================================
	// 2-param: bpName present but NOT "*" - the outer `"*".equals(bpName)` conjunct is false (as opposed to
	// c01 above, where bpName IS "*" but a later conjunct in the same condition is what's false), so this
	// exercises a distinct branch outcome for that same guard even though both fall through to the same
	// `else { methodType = GETTER; }`.
	//====================================================================================================

	public static class ExplicitNamedTwoParamMethodBean {
		@BeanProp(name = "explicitTwoArg")
		public Object twoArgExplicit(String a, String b) { return null; }
	}

	@Test
	void g01_explicitBeanPropName_twoParamMethod_notStarBpName_fallsThroughToGetterBranch() {
		var bm = BeanMeta.of(ExplicitNamedTwoParamMethodBean.class);
		assertTrue(bm.getProperties().containsKey("explicitTwoArg"));
	}

	//====================================================================================================
	// 2-param: BARE @BeanProp (no name=X) on a non-dyna-shaped method - bpName() resolves to "" (present but
	// empty) rather than null. Unlike the 0-/1-param branches above (which special-case an empty bpName by
	// deriving one from the method name), the 2-param branch has no such fallback: bpName stays "" and gets
	// assigned straight into `n`, so the property is registered under the empty-string key. This documents
	// existing (arguably degenerate) behavior for this edge case; also exercises the `nn(bpName) &&
	// !bpName.isEmpty()` guard's false outcome below (empty-but-non-null bpName is NOT re-applied to `n`).
	//====================================================================================================

	public static class BareBeanPropTwoParamMethodBean {
		@BeanProp
		public Object twoArgBare(String a, String b) { return null; }
	}

	@Test
	void g02_bareBeanPropName_twoParamMethod_emptyBpName_registersEmptyNamedProperty() {
		var bm = BeanMeta.of(BareBeanPropTwoParamMethodBean.class);
		assertTrue(bm.getProperties().containsKey(""));
	}

	//====================================================================================================
	// findBeanMethods(): a method explicitly opting BACK IN via `@java.beans.Transient(false)` - the
	// Transient-skip guard's `.value()` must be evaluated (not just its presence), so an explicit `false`
	// must NOT suppress discovery, unlike the bare/default-true `@Transient` case already covered by
	// BeanMeta_Discovery_Coverage_Test's TransientGetterAnnotatedBean.
	//====================================================================================================

	public static class ExplicitFalseTransientGetterBean {
		private String foo;
		@java.beans.Transient(false)
		public String getFoo() { return foo; }
		public void setFoo(String v) { foo = v; }
	}

	@Test
	void h01_explicitFalseTransient_doesNotSuppressGetterDiscovery() {
		var bm = BeanMeta.of(ExplicitFalseTransientGetterBean.class);
		var pm = bm.getPropertyMeta("foo");
		assertNotNull(pm);
		assertNotNull(pm.getGetter());
	}

	//====================================================================================================
	// findBeanMethods(): two-getters tie-break, when the SUPERCLASS's getter is discovered before the
	// (overriding-name, not overriding-signature) child class's getter - classHierarchy.get() visits
	// superclasses before the class itself, so putting the "first" getter on the superclass and the
	// "second" on the child deterministically controls processing order.
	//====================================================================================================

	public static class AnnotatedGetterSuperclass {
		@BeanProp(name = "value")
		public String primary() { return "p"; }
	}

	// Unannotated getValue() resolves to the same "value" property via ordinary getX() naming - processed
	// AFTER the annotated superclass getter, so this exercises the "@BeanProp on existing getter takes
	// precedence" branch (existing bpm.getter is annotated, the incoming one here is not).
	public static class UnannotatedGetterChild extends AnnotatedGetterSuperclass {
		public String getValue() { return "c"; }
	}

	@Test
	void i01_twoGetters_existingAnnotatedGetterTakesPrecedenceOverUnannotatedOverride() {
		var bm = BeanMeta.of(UnannotatedGetterChild.class);
		var pm = bm.getPropertyMeta("value");
		assertNotNull(pm);
		assertEquals("primary", pm.getGetter().getNameSimple());
	}

	public static class GetterSuperclassGetX {
		public boolean getFlag() { return true; }
	}

	// isFlag() is processed AFTER the superclass's getFlag() (same reasoning as above) - exercises the
	// "getX() overrides isX()" branch's TRUE outcome via the (m.startsWith("is") && existing.startsWith("get"))
	// check, as opposed to the more common processing order (isX() first, getX() second) which reaches the
	// same final answer through the implicit "keep current" else-path instead.
	public static class GetterChildIsX extends GetterSuperclassGetX {
		public boolean isFlag() { return true; }
	}

	@Test
	void i02_twoGetters_getPrefixedSuperclassGetterTakesPrecedenceOverIsPrefixedOverride() {
		var bm = BeanMeta.of(GetterChildIsX.class);
		var pm = bm.getPropertyMeta("flag");
		assertNotNull(pm);
		assertEquals("getFlag", pm.getGetter().getNameSimple());
	}

	// Existing (superclass) getter is unannotated and "is"-prefixed (not "get"-prefixed) - neither getter is
	// @BeanProp-annotated, so the "@BeanProp takes precedence" disjunct is false, and the child's method name
	// DOES start with "is" but the existing getter's simple name does NOT start with "get" - so the
	// "getX() overrides isX()" disjunct's second conjunct is false too (as opposed to i02 above, where it's
	// true). Both disjuncts false means the override condition as a whole is false, so the code falls through
	// to the implicit "last processed wins" default instead of preferring the existing getter.
	public static class IsPrefixedSuperclassGetter {
		public boolean isActive() { return true; }
	}

	public static class AnnotatedIsPrefixedChildGetter extends IsPrefixedSuperclassGetter {
		@BeanProp(name = "active")
		public boolean isEnabled() { return false; }
	}

	@Test
	void i03_twoGetters_neitherPrecedenceDisjunctTrue_lastProcessedGetterWins() {
		var bm = BeanMeta.of(AnnotatedIsPrefixedChildGetter.class);
		var pm = bm.getPropertyMeta("active");
		assertNotNull(pm);
		assertEquals("isEnabled", pm.getGetter().getNameSimple());
	}
}
