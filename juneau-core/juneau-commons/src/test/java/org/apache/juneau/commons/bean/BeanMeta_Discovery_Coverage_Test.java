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

import java.io.*;
import java.util.*;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.BeanTestFakes.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link BeanMeta} paths that are unreachable via the commons-only {@link BeanMeta#of(Class)}
 * entry point used by {@code BeanMeta_Coverage_Test}:
 *
 * <ul>
 * 	<li>The marshalling-side {@link BeanMeta#create(BeanInfo, ClassInfo)} factory's sanity-check guards
 * 		(not-a-bean predicates, serializable requirement, {@code @BeanIgnore}, visibility) and its
 * 		{@code catch (RuntimeException)} fallback — none of these run on the {@link BeanMeta#of} path.
 * 	<li>{@link BeanFilter}-driven construction (fixed/excluded/read-only/write-only property lists, fluent
 * 		setters, stop/interface class, property namer overrides) — {@code of()} never builds a
 * 		{@link BeanFilter} ({@link BeanMetaInitializer#NOOP} always returns <jk>null</jk>), so these branches
 * 		need a fake {@link BeanMetaInitializer} that supplies one (see {@link BeanTestFakes#initializerWithFilter}).
 * 	<li>A non-<jk>null</jk> {@code marshallingContext} (per-property {@code BeanRegistry} construction, the
 * 		bean-level "_type" registry mapping, and the {@link BeanPropertyPostProcessor} hook) — {@code of()}
 * 		always leaves it <jk>null</jk>.
 * 	<li>{@code newBean}'s {@code @BeanType(factory=...)} path, non-static-inner-class instantiation, and the
 * 		{@link org.apache.juneau.commons.inject.BeanStore}-backed factory lookup.
 * 	<li>A few method/field-discovery edge cases in {@code findBeanMethods}/{@code findBeanFields} (dyna methods,
 * 		{@code java.beans.Transient}-annotated accessors, non-public record canonical constructors).
 * </ul>
 */
@SuppressWarnings({
	"unused"  // Test POJO fields/methods are read reflectively through BeanMeta, not directly.
})
class BeanMeta_Discovery_Coverage_Test extends TestBase {

	//====================================================================================================
	// Test POJOs - create() factory guards
	//====================================================================================================

	public static class PlainA {
		public String x;
	}

	public static class PlainSerializable implements Serializable {
		private static final long serialVersionUID = 1L;
		public String x;
	}

	@BeanType
	public static class RegisteredNotSerializable {
		public String x;
	}

	@BeanIgnore
	public static class IgnoredForCreate {
		public String x;
	}

	static class PackagePrivateBean {
		public String x;
	}

	@BeanType
	static class PackagePrivateRegisteredBean {
		public String x;
	}

	public static class TwoBeanCtorsForCreate {
		private final String a;
		@BeanCtor(properties = "a")
		public TwoBeanCtorsForCreate(String a) { this.a = a; }
		@BeanCtor(properties = "a")
		public TwoBeanCtorsForCreate(String a, int unused) { this.a = a; }
		public String getA() { return a; }
	}

	public static class NoArgPrivateForCreate {
		private String hidden;
		private NoArgPrivateForCreate() {}
		public String getHidden() { return hidden; }
		public void setHidden(String v) { hidden = v; }
	}

	//====================================================================================================
	// create() factory - sanity-check guards
	//====================================================================================================

	@Test
	void a01_create_notABean_excludeClassList_returnsReason() {
		var cfg = BeanConfigContext.create().notBeanClasses(PlainA.class).build();
		var r = BeanMeta.create(new FakeBeanInfo<>(PlainA.class, cfg), null);
		assertNull(r.beanMeta());
		assertEquals("Class matches exclude-class list", r.notABeanReason());
		assertTrue(r.optBeanMeta().isEmpty());
	}

	@Test
	void a02_create_requireSerializable_notSerializable_returnsReason() {
		var cfg = BeanConfigContext.create().beansRequireSerializable(true).build();
		var r = BeanMeta.create(new FakeBeanInfo<>(PlainA.class, cfg), null);
		assertNull(r.beanMeta());
		assertEquals("Class is not serializable", r.notABeanReason());
	}

	@Test
	void a03_create_requireSerializable_serializableClass_succeeds() {
		var cfg = BeanConfigContext.create().beansRequireSerializable(true).build();
		var r = BeanMeta.create(new FakeBeanInfo<>(PlainSerializable.class, cfg), null);
		assertNotNull(r.beanMeta());
		assertNull(r.notABeanReason());
	}

	@Test
	void a04_create_requireSerializable_beanRegistrationAnnotationBypasses() {
		// @BeanType on the class satisfies BeanMetaInitializer.NOOP.hasBeanRegistrationAnnotation(), which
		// short-circuits the serializable requirement even though the class isn't Serializable.
		var cfg = BeanConfigContext.create().beansRequireSerializable(true).build();
		var r = BeanMeta.create(new FakeBeanInfo<>(RegisteredNotSerializable.class, cfg), null);
		assertNotNull(r.beanMeta());
		assertNull(r.notABeanReason());
	}

	@Test
	void a05_create_beanIgnoreClass_returnsReason() {
		var r = BeanMeta.create(new FakeBeanInfo<>(IgnoredForCreate.class), null);
		assertNull(r.beanMeta());
		assertEquals("Class is annotated with @BeanIgnore", r.notABeanReason());
	}

	@Test
	void a06_create_notPublicClass_returnsReason() {
		var r = BeanMeta.create(new FakeBeanInfo<>(PackagePrivateBean.class), null);
		assertNull(r.beanMeta());
		assertEquals("Class is not public", r.notABeanReason());
	}

	@Test
	void a07_create_notPublicClass_beanRegistrationAnnotationBypasses() {
		var r = BeanMeta.create(new FakeBeanInfo<>(PackagePrivateRegisteredBean.class), null);
		assertNotNull(r.beanMeta());
		assertNull(r.notABeanReason());
	}

	@Test
	void a08_create_anonymousClass_returnsReason() {
		var anon = new Runnable() { @Override public void run() {} };
		var r = BeanMeta.create(new FakeBeanInfo<>(anon.getClass()), null);
		assertNull(r.beanMeta());
		assertEquals("Class is not public", r.notABeanReason());
	}

	// With Visibility.PRIVATE, isVisible(modifiers) is always true (even for an anonymous class's
	// package-private modifiers), so the "! isVisible" disjunct is false and short-circuit evaluation
	// falls through to actually evaluate cm.isAnonymousClass() (as opposed to a08 above, where the default
	// PUBLIC visibility already makes "! isVisible" true for the anonymous class, so isAnonymousClass() is
	// never reached at all) - this is the only way to exercise that disjunct's "true" outcome.
	@Test
	void a08b_create_anonymousClass_permissiveVisibility_stillReturnsReason() {
		var cfg = BeanConfigContext.create().beanClassVisibility(org.apache.juneau.commons.reflect.Visibility.PRIVATE).build();
		var anon = new Runnable() { @Override public void run() {} };
		var r = BeanMeta.create(new FakeBeanInfo<>(anon.getClass(), cfg), null);
		assertNull(r.beanMeta());
		assertEquals("Class is not public", r.notABeanReason());
	}

	@Test
	void a09_create_success_returnsBeanMetaValue() {
		var r = BeanMeta.create(new FakeBeanInfo<>(PlainA.class), null);
		assertNotNull(r.beanMeta());
		assertNull(r.notABeanReason());
		assertTrue(r.optBeanMeta().isPresent());
	}

	@Test
	void a10_create_multipleBeanCtor_exceptionCapturedAsReason() {
		var r = BeanMeta.create(new FakeBeanInfo<>(TwoBeanCtorsForCreate.class), null);
		assertNull(r.beanMeta());
		assertNotNull(r.notABeanReason());
		// BeanMetaValue.optNotABeanReason() is package-private; exercise it directly here.
		assertTrue(r.optNotABeanReason().isPresent());
		assertTrue(r.optBeanMeta().isEmpty());
	}

	@Test
	void a11_create_noArgConstructorRequired_missing_returnsReason() {
		var cfg = BeanConfigContext.create().beansRequireDefaultConstructor(true).build();
		var r = BeanMeta.create(new FakeBeanInfo<>(NoArgPrivateForCreate.class, cfg), null);
		assertNull(r.beanMeta());
		assertEquals("Class does not have the required no-arg constructor", r.notABeanReason());
	}

	@Test
	void a12_create_noArgConstructorRequired_recordExempt_succeeds() {
		var cfg = BeanConfigContext.create().beansRequireDefaultConstructor(true).build();
		var r = BeanMeta.create(new FakeBeanInfo<>(BeanMeta_Coverage_Test.SimpleRecord.class, cfg), null);
		assertNotNull(r.beanMeta());
		assertNull(r.notABeanReason());
	}

	// A package-private record has no *public* canonical constructor (findBeanConstructor's record branch only
	// matches public ones - see e04 below), so beanConstructor.constructor() is empty here.  Combined with
	// beansRequireDefaultConstructor(true), this drives the "! ci.isRecord()" guard to its false outcome (as
	// opposed to a11, where isRecord() is also false, and a12, where the constructor IS present so the whole
	// condition short-circuits before isRecord() is even evaluated) - proving records are exempt even when no
	// constructor could be found at all.  Uses the package-private constructor directly (bypassing
	// BeanMeta.create()'s own "class is not public" guard, which would otherwise reject this class before
	// ever reaching the constructor-required check) - see BeanMeta.notABeanReason's package-private relaxation.
	@Test
	void a13_create_noArgConstructorRequired_recordWithNoPublicCtor_stillExempt() {
		var cfg = BeanConfigContext.create().beansRequireDefaultConstructor(true).build();
		var bm = new BeanMeta<>(new FakeBeanInfo<>(PackagePrivateRecord.class, cfg), null, null, null);
		assertNull(bm.notABeanReason);
		assertFalse(bm.hasConstructor());
	}

	//====================================================================================================
	// BeanFilter-driven construction (bf != null) via a fake BeanMetaInitializer
	//====================================================================================================

	public static class FilterableBean {
		public String x;
		public int y;
		public String z;
	}

	@Test
	void c01_beanFilter_fixedPropertyOrder_isHonored() {
		var filter = new FakeBeanFilter().properties("y", "x", "z");
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new FakeBeanInfo<>(FilterableBean.class, cfg), null).beanMeta();
		assertNotNull(bm);
		assertEquals(List.of("y", "x", "z"), new ArrayList<>(bm.getProperties().keySet()));
	}

	@Test
	void c02_beanFilter_missingFixedProperty_throws() {
		var filter = new FakeBeanFilter().properties("x", "doesNotExist");
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var r = BeanMeta.create(new FakeBeanInfo<>(FilterableBean.class, cfg), null);
		assertNull(r.beanMeta());
		assertNotNull(r.notABeanReason());
		assertTrue(r.notABeanReason().contains("doesNotExist"));
	}

	@Test
	void c03_beanFilter_excludeProperties_movesToHidden() {
		var filter = new FakeBeanFilter().excludeProperties("y");
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new FakeBeanInfo<>(FilterableBean.class, cfg), null).beanMeta();
		assertNotNull(bm);
		assertFalse(bm.getProperties().containsKey("y"));
		assertTrue(bm.getHiddenProperties().containsKey("y"));
	}

	@Test
	void c04_beanFilter_includeProperties_hidesOthers() {
		var filter = new FakeBeanFilter().properties("x");
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new FakeBeanInfo<>(FilterableBean.class, cfg), null).beanMeta();
		assertNotNull(bm);
		assertEquals(Set.of("x"), bm.getProperties().keySet());
		assertTrue(bm.getHiddenProperties().containsKey("y"));
		assertTrue(bm.getHiddenProperties().containsKey("z"));
	}

	@Test
	void c05_beanFilter_unsortedProperties_true_flowsThrough() {
		var filter = new FakeBeanFilter().unsortedProperties(true);
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new FakeBeanInfo<>(FilterableBean.class, cfg), null).beanMeta();
		assertNotNull(bm);
		// isUnsortedProperties() is protected, which also grants same-package access.
		assertTrue(bm.isUnsortedProperties());
	}

	@Test
	void c06_beanFilter_fluentSetters_true_flowsThrough() {
		var filter = new FakeBeanFilter().fluentSetters(true);
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new FakeBeanInfo<>(BeanMeta_Coverage_Test.FluentBean.class, cfg), null).beanMeta();
		assertNotNull(bm);
		var pm = bm.getPropertyMeta("n");
		assertNotNull(pm);
		assertNotNull(pm.getSetter());
	}

	@Test
	void c07_beanFilter_propertyNamerOverride_isUsed() {
		var namer = new PropertyNamer() {
			@Override public String getPropertyName(String name) { return name.toUpperCase(); }
		};
		var filter = new FakeBeanFilter().propertyNamer(namer);
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new FakeBeanInfo<>(FilterableBean.class, cfg), null).beanMeta();
		assertNotNull(bm);
		assertTrue(bm.getProperties().containsKey("X"));
	}

	@Test
	void c08_beanFilter_getBeanFilter_returnsFilterOnMarshallingPath() {
		var filter = new FakeBeanFilter();
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new FakeBeanInfo<>(FilterableBean.class, cfg), null).beanMeta();
		assertNotNull(bm);
		assertSame(filter, bm.getBeanFilter());
	}

	//====================================================================================================
	// marshallingContext != null (per-property registry wiring + post-processor hook)
	//====================================================================================================

	public static class MarshallingContextBean {
		public String x;
		public int y;
	}

	@Test
	void d01_marshallingContextNonNull_constructionSucceeds_andPostProcessorInvoked() {
		var calls = new ArrayList<String>();
		var postProcessor = (BeanPropertyPostProcessor) (mc, builder) -> calls.add(builder.name);
		var cfg = BeanConfigContext.create().beanPropertyPostProcessor(postProcessor).build();
		var cm = new FakeBeanInfo<>(MarshallingContextBean.class, cfg).marshallingContext(new FakeBeanTypeResolver(cfg));
		var bm = BeanMeta.create(cm, null).beanMeta();
		assertNotNull(bm);
		// One call per discovered property (x, y) - proves the marshallingContext != null branch that invokes
		// the BeanPropertyPostProcessor hook (never exercised via BeanMeta.of(), which always leaves it null).
		assertTrue(calls.contains("x"));
		assertTrue(calls.contains("y"));
	}

	@Test
	void d01b_marshallingContextNonNull_postProcessorThrows_wrappedAsBeanRuntimeException() {
		// validateAndRegisterProperty()'s try block covers both p.validate() AND the post-processor hook
		// (~line 722) - a RuntimeException thrown by the latter must be caught and re-wrapped by the same
		// generic "catch (Exception e) { throw brex(...); }" clause as a validate()-thrown exception would be.
		// BeanMeta.create()'s own outer "catch (RuntimeException e)" then catches THAT BeanRuntimeException
		// and surfaces it as notABeanReason rather than propagating it to the caller.
		var postProcessor = (BeanPropertyPostProcessor) (mc, builder) -> { throw new RuntimeException("boom-postprocess"); };
		var cfg = BeanConfigContext.create().beanPropertyPostProcessor(postProcessor).build();
		var cm = new FakeBeanInfo<>(MarshallingContextBean.class, cfg).marshallingContext(new FakeBeanTypeResolver(cfg));
		var r = BeanMeta.create(cm, null);
		assertNull(r.beanMeta());
		assertNotNull(r.notABeanReason());
		assertTrue(r.notABeanReason().contains("boom-postprocess"));
	}

	@Test
	void d01c_marshallingContextNonNull_dictionaryClassesPopulated_buildsPropertyBeanRegistry() {
		// dictionaryClasses is normally populated by the marshalling-side MarshalledPropertyPostProcessor
		// (from @MarshalledProp(dictionary={})); the commons-side BeanPropertyPostProcessor SPI hook lets this
		// test set it directly (package-private field, same package) to exercise the "nn(marshallingContext)
		// && nn(v.dictionaryClasses)" branch that builds the per-property BeanRegistry side-map entry - never
		// exercised by d02 below, whose dictionaryClasses stays at validate()'s default emptyList().
		var postProcessor = (BeanPropertyPostProcessor) (mc, builder) -> {
			if ("x".equals(builder.name))
				builder.dictionaryClasses = List.of(info(String.class));
		};
		var cfg = BeanConfigContext.create().beanPropertyPostProcessor(postProcessor).build();
		var cm = new FakeBeanInfo<>(MarshallingContextBean.class, cfg).marshallingContext(new FakeBeanTypeResolver(cfg));
		var bm = BeanMeta.create(cm, null).beanMeta();
		assertNotNull(bm);
		var pm = bm.getPropertyMeta("x");
		assertNotNull(pm);
		// BeanMetaInitializer.NOOP.buildPropertyBeanRegistry always returns null, so the registry itself is
		// still null - this test just proves the branch executes (and thus calls buildPropertyBeanRegistry)
		// without error, mirroring d02's "prove it runs" style below for the bean-level registry mapping.
		assertNull(bm.getPropertyBeanRegistry(pm));
	}

	@Test
	void d02_marshallingContextNonNull_typeProperty_registryMapped_noThrow() {
		// Exercises the "nn(marshallingContext)" branch that maps the synthetic "_type" property into the
		// per-property BeanRegistry side-map.  BeanMetaInitializer.NOOP.buildBeanRegistry always returns null,
		// so the outcome is still a null registry - this test just proves the branch executes without error.
		var cm = new FakeBeanInfo<>(MarshallingContextBean.class).marshallingContext(new FakeBeanTypeResolver());
		var bm = BeanMeta.create(cm, null).beanMeta();
		assertNotNull(bm);
		assertNull(bm.getPropertyBeanRegistry(bm.getTypeProperty()));
	}

	@Test
	void d02b_findDictionaryName_parentRegistryLookup_typeNameFound_returnsIt() {
		// findTypeNameInParents() found a match here (as opposed to d02 above, whose NOOP-backed registry
		// always returns null) - exercises findDictionaryName()'s "n != null" early-return branch, distinct
		// from the "@Marshalled annotation" fallback (findMarshalledTypeName) exercised elsewhere.
		var cfg = BeanConfigContext.create().beanMetaInitializer(BeanTestFakes.initializerWithParentTypeName("ParentType")).build();
		var cm = new FakeBeanInfo<>(MarshallingContextBean.class, cfg).marshallingContext(new FakeBeanTypeResolver(cfg));
		var bm = BeanMeta.create(cm, null).beanMeta();
		assertNotNull(bm);
		assertEquals("ParentType", bm.getDictionaryName());
	}

	//====================================================================================================
	// newBean(): @BeanType(factory=...), non-static inner class, BeanStore-backed factory lookup
	//====================================================================================================

	@BeanType(factory = WidgetHolderFactory.class)
	public static class WidgetHolder {
		public String x;
	}

	public static class WidgetHolderFactory implements BeanFactory<WidgetHolder> {
		@Override public WidgetHolder create() {
			var w = new WidgetHolder();
			w.x = "from-factory";
			return w;
		}
	}

	@Test
	void e01_newBean_factoryClass_usesBeanInstantiatorFallback() throws Exception {
		var bm = BeanMeta.of(WidgetHolder.class);
		var w = bm.newBean(null);
		assertNotNull(w);
		assertEquals("from-factory", w.x);
	}

	@Test
	void e02_newBean_factoryClass_usesBeanStoreWhenAvailable() throws Exception {
		var factoryInstance = new WidgetHolderFactory();
		var cfg = BeanConfigContext.create().beanStore(new FakeBeanStore(factoryInstance)).build();
		var bm = BeanMeta.of(WidgetHolder.class, cfg);
		var w = bm.newBean(null);
		assertNotNull(w);
		assertEquals("from-factory", w.x);
	}

	public class NonStaticInner {
		public String x;
	}

	@Test
	void e03_newBean_nonStaticInnerClass_usesOuterInstance() throws Exception {
		var bm = BeanMeta.of(NonStaticInner.class);
		assertTrue(bm.hasConstructor());
		var inner = bm.newBean(this);
		assertNotNull(inner);
	}

	// A record's canonical constructor can never be more restrictive than the record class itself (JLS
	// 8.10.4.2), so the only way to get a non-public canonical constructor is a non-public record: its
	// (package-private) canonical constructor is legal, but findBeanConstructor's record branch specifically
	// searches for a *public* constructor match, so it finds nothing here and falls through to "no constructor".
	record PackagePrivateRecord(String a) {}

	@Test
	void e04_record_nonPublicCanonicalConstructor_noPublicMatch_hasNoConstructor() {
		var bm = BeanMeta.of(PackagePrivateRecord.class);
		assertFalse(bm.hasConstructor());
	}

	//====================================================================================================
	// findBeanMethods(): dyna ("*") discovery through full BeanMeta construction
	//====================================================================================================

	public static class DynaMethodBean {
		private final Map<String,Object> extras = new LinkedHashMap<>();
		public String name;
		@BeanProp(name = "*")
		public Map<String,Object> getExtras() { return extras; }
		@BeanProp(name = "*")
		public void setExtras(String key, Object value) { extras.put(key, value); }
	}

	@Test
	void f01_dynaMethods_discoveredThroughFullConstruction() {
		var bm = BeanMeta.of(DynaMethodBean.class);
		var dyna = bm.getDynaProperty();
		assertNotNull(dyna);
		assertEquals("*", dyna.getName());
		assertNotNull(dyna.getGetter());
		assertNotNull(dyna.getSetter());
	}

	public static class ExtraKeysMethodBean {
		public String name;
		private final Map<String,Object> data = new LinkedHashMap<>(Map.of("a", 1, "b", 2));
		@BeanProp(name = "*")
		public Object getExtras(String key) { return data.get(key); }
		@BeanProp(name = "*")
		public Set<String> extraKeys() { return data.keySet(); }
	}

	@Test
	void f02_dynaExtraKeysMethod_discoveredAndEnumeratesViaPerKeyGetter() throws Exception {
		var bm = BeanMeta.of(ExtraKeysMethodBean.class);
		var dyna = bm.getDynaProperty();
		assertNotNull(dyna);
		var bean = new ExtraKeysMethodBean();
		var m = dyna.getDynaMap(bean);
		assertEquals(Map.of("a", 1, "b", 2), m);
	}

	public static class TransientGetterAnnotatedBean {
		public String name;
		private String temp;
		// java.beans.Transient is @Target(METHOD)-only (it cannot be placed on the field itself); this is the
		// standard JavaBeans idiom for a transient property (see java.beans.Transient javadoc example).
		@java.beans.Transient
		public String getTemp() { return temp; }
		public void setTemp(String v) { temp = v; }
	}

	@Test
	void f03_transientAnnotation_onGetter_excludesGetter_setterStillRegistersProperty() {
		// Method-level java.beans.Transient is checked per-method, independent of ignoreTransientFields (that
		// config flag only governs the `transient` keyword / field-level check, both of which are field-only
		// concerns).  Only the getter carries @Transient here, so the property still exists (registered by the
		// unmarked setter) but with no getter.
		var bm = BeanMeta.of(TransientGetterAnnotatedBean.class);
		assertTrue(bm.getProperties().containsKey("name"));
		var pm = bm.getPropertyMeta("temp");
		assertNotNull(pm);
		assertNull(pm.getGetter());
		assertNotNull(pm.getSetter());
	}

	//====================================================================================================
	// Record constructor-arg remapping when a component's accessor is renamed via @BeanProp
	//====================================================================================================

	// @BeanProp written at the record-component position (JLS 9.7.4) propagates to the generated field, the
	// generated accessor, and the canonical constructor parameter alike (its @Target includes all three), so
	// both field- and method-based discovery agree on the renamed property "renamedA".  findBeanConstructor's
	// record branch still derives constructor args from the *raw* component name ("a") though, so the remap
	// loop in the BeanMeta constructor must translate "a" -> "renamedA" by matching the property's field/getter
	// name (both still physically named "a").
	public record RenamedRecord(@BeanProp(name = "renamedA") String a) {}

	// A record whose @BeanCtor-annotated (non-canonical) constructor declares MORE properties than the
	// record actually has components - the remap loop's `idx < components.size() ? ... : componentName`
	// ternary (BeanMeta's constructor, ~line 604) must fall back to the raw arg name once idx runs past the
	// component list, rather than indexing out of bounds.
	public record ExtraCtorArgRecord(String a) {
		@BeanCtor(properties = "a,extra")
		public ExtraCtorArgRecord(String a, String extra) { this(a); }
	}

	@Test
	void g00_recordConstructorArgs_beanCtorArgCountExceedsComponentCount_fallsBackToRawArgName() {
		// "extra" isn't a real property (no matching component/field/getter), so the remap search's
		// orElse(componentName) fallback kicks in, and the subsequent constructor-arg-registration pass
		// (which requires every @BeanCtor arg to resolve to an actual discovered property) throws.
		var ex = assertThrows(org.apache.juneau.commons.reflect.BeanRuntimeException.class, () -> BeanMeta.of(ExtraCtorArgRecord.class));
		assertTrue(ex.getMessage().contains("extra"));
	}

	@Test
	void g01_recordConstructorArgs_remappedToRenamedPropertyName() {
		var bm = BeanMeta.of(RenamedRecord.class);
		assertTrue(bm.getProperties().containsKey("renamedA"));
		assertFalse(bm.getProperties().containsKey("a"));
		assertEquals(List.of("renamedA"), bm.getConstructorArgs());
	}

	// A record with an UNrenamed component ("a") preceding a renamed one ("b" -> "renamedB") - the remap
	// search now has to walk past the "a" property's entry (whose field/getter are physically named "a", so
	// both the field- and getter-name checks against rcName="b" come back false) before landing on the
	// "renamedB" entry, exercising the search filter's false outcomes for both the field-name and
	// getter-name checks (as opposed to g01 above, where the single-property record's lone entry matches on
	// the very first field-name check, never even evaluating the getter-name check).
	public record MultiRenamedRecord(String a, @BeanProp(name = "renamedB") String b) {}

	@Test
	void g01b_recordConstructorArgs_unrenamedComponentPrecedesRenamed_searchSkipsNonMatchingEntry() {
		var bm = BeanMeta.of(MultiRenamedRecord.class);
		assertTrue(bm.getProperties().containsKey("a"));
		assertTrue(bm.getProperties().containsKey("renamedB"));
		assertFalse(bm.getProperties().containsKey("b"));
		assertEquals(List.of("a", "renamedB"), bm.getConstructorArgs());
	}

	// Like ExtraCtorArgRecord/g00 above (a @BeanCtor arg, "extra", beyond the component list, so rcName falls
	// back to the raw arg name and can never match anything), PLUS an extra setter-only property ("bar": no
	// backing field, no getter method - only a bare setter). Because "extra" matches NO property at all, the
	// remap search's filter().findFirst() can't short-circuit on a match - it must walk every normalProps
	// entry to conclude there is none, which (regardless of unspecified HashMap iteration order) guarantees
	// the setter-only "bar" entry is actually evaluated. That entry's builder has both field==null and
	// getter==null, driving BOTH of the search filter's still-missing null-guard outcomes in one pass:
	// "field != null" false (BeanMeta.java:606) and "getter != null" false (BeanMeta.java:607) - as opposed
	// to g00/g01b, whose entries all have non-null field AND getter.
	public record ExtraCtorArgWithSetterOnlyPropertyRecord(String a) {
		@BeanCtor(properties = "a,extra")
		public ExtraCtorArgWithSetterOnlyPropertyRecord(String a, String extra) { this(a); }
		public void setBar(String v) { /* no-op - setter-only extra property: no field, no getter */ }
	}

	@Test
	void g01c_recordConstructorArgs_extraSetterOnlyProperty_hasNullFieldAndGetter_searchSkipsWithoutMatching() {
		var ex = assertThrows(org.apache.juneau.commons.reflect.BeanRuntimeException.class,
			() -> BeanMeta.of(ExtraCtorArgWithSetterOnlyPropertyRecord.class));
		assertTrue(ex.getMessage().contains("extra"));
	}

	//====================================================================================================
	// @BeanCtor(properties=X) referencing a property that doesn't exist on the class
	//====================================================================================================

	public static class BeanCtorMissingProp {
		public String a;
		@BeanCtor(properties = "a,missing")
		public BeanCtorMissingProp(String a, String missing) { this.a = a; }
	}

	@Test
	void g02_beanCtorProperties_referencesMissingProperty_throws() {
		var ex = assertThrows(org.apache.juneau.commons.reflect.BeanRuntimeException.class, () -> BeanMeta.of(BeanCtorMissingProp.class));
		assertTrue(ex.getMessage().contains("missing"));
		assertTrue(ex.getMessage().contains("@BeanCtor"));
	}

	//====================================================================================================
	// @BeanCtor(properties=X) whose arg count doesn't match the constructor's parameter count
	//====================================================================================================

	public static class BeanCtorTooManyProps {
		public String a;
		@BeanCtor(properties = "a,extra")
		public BeanCtorTooManyProps(String a) { this.a = a; }
	}

	@Test
	void g03_beanCtorProperties_countMismatch_nonBlank_throws() {
		var ex = assertThrows(org.apache.juneau.commons.reflect.BeanRuntimeException.class, () -> BeanMeta.of(BeanCtorTooManyProps.class));
		assertTrue(ex.getMessage().contains("does not match number of parameters"));
	}

	public static class BeanCtorNoPropsSpecified {
		public String a;
		@BeanCtor
		public BeanCtorNoPropsSpecified(String a) { this.a = a; }
	}

	@Test
	void g04_beanCtorProperties_blank_fallsBackToParamNames_thenUnmatchedNameThrows() {
		// @BeanCtor with no properties=X and an arg-count mismatch (0 from the blank annotation vs. 1 actual
		// parameter) falls back to reading constructor parameter names via reflection (test compilation resolves
		// real parameter names here, so this succeeds rather than hitting the "could not find name" guard) - it
		// resolves the synthesized name "arg0", which doesn't match the actual property name "a", so the
		// subsequent constructor-arg-registration pass throws instead.
		var ex = assertThrows(org.apache.juneau.commons.reflect.BeanRuntimeException.class, () -> BeanMeta.of(BeanCtorNoPropsSpecified.class));
		assertTrue(ex.getMessage().contains("was not found on the class definition"));
	}

	//====================================================================================================
	// Explicit pNames constructor argument (protected BeanMeta(BeanInfo, BeanFilter, String[], ClassInfo))
	//====================================================================================================

	@Test
	void g05_explicitPNames_reordersAndExcludesUnlistedProperties() {
		// pNames rebuilds the property map by iterating pNames itself (not the discovered property map), so a
		// discovered property that's simply omitted from pNames (here, "y") is dropped outright - unlike the
		// BeanFilter-driven include/exclude lists (c03/c04 above), it's never routed into hiddenProperties.
		var bm = new BeanMeta<>(new FakeBeanInfo<>(FilterableBean.class), null, new String[] {"z", "x"}, null);
		assertEquals(List.of("z", "x"), new ArrayList<>(bm.getProperties().keySet()));
		assertFalse(bm.getHiddenProperties().containsKey("y"));
	}

	@Test
	void g06_explicitPNames_unknownName_registeredAsNullHiddenProperty() {
		var bm = new BeanMeta<>(new FakeBeanInfo<>(FilterableBean.class), null, new String[] {"x", "doesNotExist"}, null);
		assertEquals(List.of("x"), new ArrayList<>(bm.getProperties().keySet()));
		assertTrue(bm.getHiddenProperties().containsKey("doesNotExist"));
		assertNull(bm.getHiddenProperties().get("doesNotExist"));
	}

	//====================================================================================================
	// useJavaBeanIntrospector: BeanFilter#getInterfaceClass() override, and a bad stop class surfacing as
	// the constructor's generic "catch (Exception)" fallback (Introspector.getBeanInfo declares a checked
	// IntrospectionException, which isn't a BeanRuntimeException and so isn't rethrown as-is).
	//====================================================================================================

	// java.beans.Introspector only recognizes JavaBean-style accessor *methods*, not bare public fields, so this
	// needs its own getter-bearing POJO (FilterableBean's plain public fields would introspect to zero
	// properties and mask the "does the filter's interfaceClass get consulted at all" question this test asks).
	public static class IntrospectableBean {
		private String x;
		public String getX() { return x; }
		public void setX(String v) { x = v; }
	}

	@Test
	void g07_useJavaBeanIntrospector_filterInterfaceClassOverride_isUsed() {
		var filter = new FakeBeanFilter().interfaceClass(info(IntrospectableBean.class));
		var cfg = BeanConfigContext.create().useJavaBeanIntrospector(true).beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var bm = BeanMeta.create(new FakeBeanInfo<>(IntrospectableBean.class, cfg), null).beanMeta();
		assertNotNull(bm);
		assertTrue(bm.getProperties().containsKey("x"));
	}

	@Test
	void g08_useJavaBeanIntrospector_stopClassNotSuperclass_capturedAsGenericException() {
		// String isn't a superclass of IntrospectableBean, so Introspector.getBeanInfo(IntrospectableBean.class,
		// String.class) throws a checked IntrospectionException - caught by the constructor's trailing
		// "catch (Exception e)" (the BeanRuntimeException-specific catch above it only rethrows, it doesn't
		// apply here), landing in notABeanReason rather than propagating.
		var filter = new FakeBeanFilter().stopClass(info(String.class));
		var cfg = BeanConfigContext.create().useJavaBeanIntrospector(true).beanMetaInitializer(BeanTestFakes.initializerWithFilter(filter)).build();
		var r = BeanMeta.create(new FakeBeanInfo<>(IntrospectableBean.class, cfg), null);
		assertNull(r.beanMeta());
		assertNotNull(r.notABeanReason());
		assertTrue(r.notABeanReason().startsWith("Exception:"));
	}

	// A setter with no matching getter: Introspector still creates a PropertyDescriptor for it, but with a
	// null readMethod - exercises mergeJavaBeanPropertyDescriptorsIntoNormalProps()'s "pd.getReadMethod() ==
	// null" branch, as opposed to every other useJavaBeanIntrospector fixture in this file (which all pair
	// a getter with a setter).
	public static class WriteOnlyIntrospectableBean {
		private String value;
		public void setValue(String v) { value = v; }
	}

	@Test
	void g09b_useJavaBeanIntrospector_writeOnlyProperty_readMethodNull_setterStillRegistered() {
		var cfg = BeanConfigContext.create().useJavaBeanIntrospector(true).build();
		var bm = BeanMeta.of(WriteOnlyIntrospectableBean.class, cfg);
		var pm = bm.getPropertyMeta("value");
		assertNotNull(pm);
		assertNotNull(pm.getSetter());
		assertNull(pm.getGetter());
	}

	// An interface bean type: exercises the c2.isInterface() branch's call to
	// Introspector.getBeanInfo(c2, null) (a null stop class, as opposed to the concrete-class branch above,
	// which always passes a real ancestor stopClass).
	public interface ClassPropIntrospectableIface {
		String getName();
	}

	@Test
	void g09c_useJavaBeanIntrospector_interfaceType_propertiesDiscovered() {
		var cfg = BeanConfigContext.create().useJavaBeanIntrospector(true).build();
		var bm = BeanMeta.of(ClassPropIntrospectableIface.class, cfg);
		assertTrue(bm.getProperties().containsKey("name"));
	}

	public static class IgnoreAccessorsIntrospectableBean {
		@BeanIgnore(ignoreAccessors = true)
		private String hidden;
		public String getHidden() { return hidden; }
		public void setHidden(String v) { hidden = v; }

		// Plain @BeanIgnore (ignoreAccessors defaults to false) only suppresses field-based discovery - it does
		// NOT suppress the JavaBeans-introspector-discovered accessor property, exercising
		// fieldBeanIgnoreIgnoresAccessors()'s "no matching ignoreAccessors=true annotation" false-return path.
		@BeanIgnore
		private String ignoredFieldOnly;
		public String getIgnoredFieldOnly() { return ignoredFieldOnly; }
		public void setIgnoredFieldOnly(String v) { ignoredFieldOnly = v; }

		private String visible;
		public String getVisible() { return visible; }
		public void setVisible(String v) { visible = v; }

		// @BeanIgnore(ignoreAccessors=true) PLUS an unrelated RUNTIME-retained annotation: exercises
		// findSuppressedPropertyNamesFromIgnoredFields()'s name-resolution stream filter's "matches neither
		// @BeanProp nor @Name" branch, mirroring b07's coverage of the analogous filter in the normal
		// (non-ignoreAccessors) field-discovery pass.
		@BeanIgnore(ignoreAccessors = true)
		@Deprecated
		private String hiddenLegacy;
		public String getHiddenLegacy() { return hiddenLegacy; }
		public void setHiddenLegacy(String v) { hiddenLegacy = v; }
	}

	@Test
	void g09_useJavaBeanIntrospector_beanIgnoreIgnoreAccessors_suppressesAccessorProperty() {
		var cfg = BeanConfigContext.create().useJavaBeanIntrospector(true).build();
		var bm = BeanMeta.of(IgnoreAccessorsIntrospectableBean.class, cfg);
		assertFalse(bm.getProperties().containsKey("hidden"));
		assertTrue(bm.getProperties().containsKey("ignoredFieldOnly"));
		assertTrue(bm.getProperties().containsKey("visible"));
		assertFalse(bm.getProperties().containsKey("hiddenLegacy"));
	}

	//====================================================================================================
	// newBean(): factory exception wrapping, member-class-with-no-constructor, BeanStore-present-but-empty
	//====================================================================================================

	public static class ThrowingFactory implements BeanFactory<WidgetHolder> {
		@Override public WidgetHolder create() { throw new IllegalStateException("boom"); }
	}

	@BeanType(factory = ThrowingFactory.class)
	public static class ThrowingFactoryBean {
		public String x;
	}

	@Test
	void e05_newBean_factoryThrowsRuntimeException_wrappedAsExecutableException() {
		var bm = BeanMeta.of(ThrowingFactoryBean.class);
		var ex = assertThrows(ExecutableException.class, () -> bm.newBean(null));
		assertInstanceOf(IllegalStateException.class, ex.getCause());
	}

	public class NonStaticInnerPrivateCtor {
		private NonStaticInnerPrivateCtor() {}
	}

	@Test
	void e06_newBean_nonStaticInnerClass_noDiscoverableConstructor_returnsNull() throws Exception {
		var bm = BeanMeta.of(NonStaticInnerPrivateCtor.class);
		assertFalse(bm.hasConstructor());
		assertNull(bm.newBean(this));
	}

	@Test
	void e07_newBean_factoryClass_beanStorePresentButEmpty_fallsBackToInstantiator() throws Exception {
		var cfg = BeanConfigContext.create().beanStore(new FakeBeanStore("unrelated-bean")).build();
		var bm = BeanMeta.of(WidgetHolder.class, cfg);
		var w = bm.newBean(null);
		assertNotNull(w);
		assertEquals("from-factory", w.x);
	}

	public static class ExecutableExceptionThrowingFactory implements BeanFactory<WidgetHolder> {
		@Override public WidgetHolder create() { throw new ExecutableException("boom-direct"); }
	}

	@BeanType(factory = ExecutableExceptionThrowingFactory.class)
	public static class ExecutableExceptionFactoryBean {
		public String x;
	}

	@Test
	void e08_newBean_factoryThrowsExecutableExceptionDirectly_rethrownUnwrapped() {
		// Distinct from e05 above (which throws a plain RuntimeException, wrapped by the generic
		// "catch (Exception e)" clause): here the factory throws an ExecutableException directly, which
		// newBean()'s dedicated "catch (ExecutableException e) { throw e; }" clause must rethrow as-is
		// rather than double-wrapping it inside another ExecutableException.
		var bm = BeanMeta.of(ExecutableExceptionFactoryBean.class);
		var ex = assertThrows(ExecutableException.class, () -> bm.newBean(null));
		assertEquals("boom-direct", ex.getMessage());
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// findBeanFields(): a PropertyNamer that returns null for the field-name fallback - the field is
	// silently dropped rather than registered under a null key.
	//====================================================================================================

	public static class NullNamingPropertyNamer implements PropertyNamer {
		@Override public String getPropertyName(String name) { return "skipMe".equals(name) ? null : name; }
	}

	public static class SelectivelyDroppedFieldBean {
		public String kept = "x";
		public String skipMe = "y";
	}

	@Test
	void f01_findBeanFields_propertyNamerReturnsNull_fieldSilentlyDropped() {
		var cfg = BeanConfigContext.create().propertyNamer(new NullNamingPropertyNamer()).build();
		var bm = BeanMeta.of(SelectivelyDroppedFieldBean.class, cfg);
		assertTrue(bm.getProperties().containsKey("kept"));
		assertFalse(bm.getProperties().containsKey("skipMe"));
		assertEquals(1, bm.getProperties().size());
	}

	// Same null-property-namer scenario as f01 above, but for the parallel @BeanIgnore(ignoreAccessors=true)
	// name-resolution pass (findSuppressedPropertyNamesFromIgnoredFields) rather than the normal field pass -
	// covers that method's analogous "nn(name)" guard's false outcome.
	public static class SkipMeIgnoredField {
		@BeanIgnore(ignoreAccessors = true)
		public String skipMe = "y";
	}

	@Test
	void f01b_findSuppressedPropertyNamesFromIgnoredFields_propertyNamerReturnsNull_notAdded() {
		var cfg = BeanConfigContext.create().useJavaBeanIntrospector(true).propertyNamer(new NullNamingPropertyNamer()).build();
		// Doesn't throw despite the null-returning namer - proves the suppressed-names set silently skips
		// the null-named entry rather than adding a null to the set (which would NPE on later containment
		// checks against real (non-null) property names).
		assertDoesNotThrow(() -> BeanMeta.of(SkipMeIgnoredField.class, cfg));
	}

	//====================================================================================================
	// findInnerBeanField(): a transient private backing field, with ignoreTransientFields toggled - the
	// getter/setter-discovered property has no p.field (the private transient field fails the default
	// PUBLIC field-visibility check on top of being transient), so validateAndRegisterProperty()'s
	// backfill lookup is what's exercised here.
	//====================================================================================================

	public static class TransientBackedAccessorBean {
		private transient String val;
		public String getVal() { return val; }
		public void setVal(String v) { val = v; }
	}

	@Test
	void f02_findInnerBeanField_transientField_defaultIgnoreTransients_notBackfilled() {
		// Default ignoreTransientFields=true - the transient private field is skipped, so the property's
		// innerField backfill lookup comes up empty (isNotTransient()==false AND noIgnoreTransients==false).
		var bm = BeanMeta.of(TransientBackedAccessorBean.class);
		var pm = bm.getPropertyMeta("val");
		assertNotNull(pm);
		assertNull(pm.getInnerField());
	}

	@Test
	void f03_findInnerBeanField_transientField_ignoreTransientsDisabled_backfilled() {
		// ignoreTransientFields(false) flips noIgnoreTransients to true, so the transient field now passes
		// the isNotTransient()||noIgnoreTransients guard and gets backfilled as the property's innerField.
		var cfg = BeanConfigContext.create().ignoreTransientFields(false).build();
		var bm = BeanMeta.of(TransientBackedAccessorBean.class, cfg);
		var pm = bm.getPropertyMeta("val");
		assertNotNull(pm);
		assertNotNull(pm.getInnerField());
		assertEquals("val", pm.getInnerField().getName());
	}

	//====================================================================================================
	// findBeanMethods(): "with"-style fluent setter naming convention
	//====================================================================================================

	public static class WithSetterBean {
		private String name;
		public String getName() { return name; }
		public WithSetterBean withName(String v) { name = v; return this; }
	}

	@Test
	void h01_withPrefixedSetter_discoveredAsSetter() {
		var bm = BeanMeta.of(WithSetterBean.class);
		var pm = bm.getPropertyMeta("name");
		assertNotNull(pm);
		assertNotNull(pm.getSetter());
		assertEquals("withName", pm.getSetter().getNameSimple());
	}
}
