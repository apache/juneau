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

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.BeanTestFakes.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link BeanPropertyMeta.Builder#validate} — specifically the marshalling-side branches
 * (non-<jk>null</jk> {@link BeanTypeResolver}) that are otherwise unreachable from {@code juneau-commons} since
 * {@link BeanMeta}'s constructor always invokes {@code validate} with a <jk>null</jk> resolver in this module (the
 * marshalling-side resolver is only ever wired up from {@code juneau-marshall}).
 *
 * <p>
 * Drives {@link BeanPropertyMeta.Builder#validate} directly (rather than through {@link BeanMeta} discovery) with a
 * {@link BeanTestFakes.FakeBeanTypeResolver} (or a purpose-built {@link BeanTypeResolver} for the handful of cases
 * that need a resolver returning a type unrelated to the field/getter/setter it was asked to resolve).
 */
@SuppressWarnings({
	"unused"  // Test POJO members are read reflectively, not directly.
})
class BeanPropertyMeta_Validate_Coverage_Test extends TestBase {

	//====================================================================================================
	// Test POJOs
	//====================================================================================================

	public static class VFields {
		public String str;
		public Map<String,Object> mapField;
	}

	public static class VGetters {
		public String getStr() { return null; }
		public Integer getInt() { return null; }
		public Map<String,Object> getMap() { return null; }
		public Map<String,Object> getMapByKey(String key) { return null; }
		public String getBadDynaGetter(int x) { return null; }
		public String getBadDynaGetterNoArgs() { return null; }
	}

	public static class VSetters {
		public void setStr(String v) {/* no-op - reflection target only */}
		public void setBadType(Integer v) {/* no-op - reflection target only */}
		public void setTwoArgs(String a, String b) {/* no-op - reflection target only, non-dyna arity mismatch */}
		public void setDynaOk(String key, Object value) {/* no-op - reflection target only */}
		public void setDynaBad(String key) {/* no-op - reflection target only */}
		public void setDynaBadKeyType(Integer key, Object value) {/* no-op - reflection target only */}
	}

	public static class VAnnotatedField {
		@BeanProp(ro = "true")
		public String name;
	}

	public static class VAnnotatedGetter {
		@BeanProp(wo = "true")
		public String getName() { return null; }
	}

	public static class VAnnotatedSetter {
		@BeanProp(ro = "true")
		public void setName(String v) {/* no-op - reflection target only */}
	}

	public static class VAnnotatedGetterRo {
		@BeanProp(ro = "true")
		public String getName() { return null; }
	}

	public static class VAnnotatedSetterWo {
		@BeanProp(wo = "true")
		public void setName(String v) {/* no-op - reflection target only */}
	}

	public static class VInnerFieldAnnotated {
		@BeanProp(ro = "true")
		private String hidden;
		public String getHidden() { return null; }
	}

	public static class VInnerFieldPlain {
		private String hidden;
		public String getHidden() { return null; }
	}

	//====================================================================================================
	// Helpers
	//====================================================================================================

	private static BeanPropertyMeta.Builder builder(String name) {
		return BeanPropertyMeta.builder(BeanMeta.of(Object.class), name);
	}

	/** A resolver whose {@code resolveType()} always returns a fixed type, ignoring what was actually requested. */
	private static BeanTypeResolver fixedTypeResolver(BeanInfo<?> fixed, BeanInfo<?> objectType) {
		return new BeanTypeResolver() {
			@Override public BeanInfo<?> resolveType(AnnotationInfo<BeanProp> lastBeanProp, ClassInfo type, TypeVariables typeVarImpls) { return fixed; }
			@Override public BeanInfo<?> objectType() { return objectType; }
			@Override public AnnotationProvider getAnnotationProvider() { return BeanConfigContext.DEFAULT.getAnnotationProvider(); }
		};
	}

	/** A resolver that fails to resolve any type at all (simulates a marshalling context that can't classify the type). */
	private static BeanTypeResolver nonResolvingResolver() {
		return fixedTypeResolver(null, new FakeBeanInfo<>(Object.class));
	}

	//====================================================================================================
	// validate() - early-exit guards
	//====================================================================================================

	@Test
	void a01_validate_noAccessors_returnsFalse() throws Exception {
		var b = builder("x");
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void a02_validate_getterOnly_beansRequireSettersForGetters_returnsFalse() throws Exception {
		var beanMeta = BeanMeta.of(Object.class, BeanConfigContext.create().beansRequireSettersForGetters(true).build());
		var b = BeanPropertyMeta.builder(beanMeta, "str").setGetter(info(VGetters.class.getMethod("getStr"))).canRead();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void a03_validate_getterOnly_constructorArg_ignoresRequireSettersForGetters() throws Exception {
		var beanMeta = BeanMeta.of(Object.class, BeanConfigContext.create().beansRequireSettersForGetters(true).build());
		var b = BeanPropertyMeta.builder(beanMeta, "str").setGetter(info(VGetters.class.getMethod("getStr")))
			.setAsConstructorArg().canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void a04_validate_bcNonNull_typeUnresolvable_returnsFalse() throws Exception {
		var b = builder("str").setGetter(info(VGetters.class.getMethod("getStr"))).canRead();
		assertFalse(b.validate(nonResolvingResolver(), null, Set.of(), Set.of()));
	}

	//====================================================================================================
	// validate() - happy path / field, getter, setter type resolution
	//====================================================================================================

	@Test
	void b01_validate_fieldOnly_resolvesTypeFromFieldType_returnsTrue() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		var pm = b.build();
		assertFalse(pm.isDyna());
		assertTrue(pm.canRead());
		assertTrue(pm.canWrite());
	}

	@Test
	void b02_validate_annotatedField_readOnlyFromAnnotation() throws Exception {
		var b = builder("name").setField(info(VAnnotatedField.class.getField("name"))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertTrue(b.build().isReadOnly());
	}

	@Test
	void b03_validate_annotatedGetter_writeOnlyFromAnnotation() throws Exception {
		var b = builder("name").setGetter(info(VAnnotatedGetter.class.getMethod("getName"))).canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertTrue(b.build().isWriteOnly());
	}

	@Test
	void b04_validate_annotatedSetter_readOnlyFromAnnotation() throws Exception {
		var b = builder("name").setSetter(info(VAnnotatedSetter.class.getMethod("setName", String.class))).canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertTrue(b.build().isReadOnly());
	}

	@Test
	void b05_validate_getterOnly_resolvesTypeFromReturnType() throws Exception {
		var b = builder("str").setGetter(info(VGetters.class.getMethod("getStr"))).canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void b06_validate_setterOnly_resolvesTypeFromParamType() throws Exception {
		var b = builder("str").setSetter(info(VSetters.class.getMethod("setStr", String.class))).canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void b07_validate_bproReadOnlySet_marksReadOnly() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of("str"), Set.of()));
		assertTrue(b.build().isReadOnly());
	}

	@Test
	void b08_validate_bpwoWildcard_marksWriteOnly() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of("*")));
		assertTrue(b.build().isWriteOnly());
	}

	//====================================================================================================
	// validate() - non-dyna getter/setter/field type-compatibility checks
	//====================================================================================================

	@Test
	void c01_validate_nonDyna_getterReturnTypeMismatch_returnsFalse() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str")))
			.setGetter(info(VGetters.class.getMethod("getInt"))).canRead().canWrite();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void c02_validate_nonDyna_getterReturnTypeCompatible_returnsTrue() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str")))
			.setGetter(info(VGetters.class.getMethod("getStr"))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void c03_validate_nonDyna_setterParamTypeMismatch_returnsFalse() throws Exception {
		var b = builder("str").setGetter(info(VGetters.class.getMethod("getStr")))
			.setSetter(info(VSetters.class.getMethod("setBadType", Integer.class))).canRead().canWrite();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void c04_validate_nonDyna_setterParamTypeCompatible_returnsTrue() throws Exception {
		var b = builder("str").setGetter(info(VGetters.class.getMethod("getStr")))
			.setSetter(info(VSetters.class.getMethod("setStr", String.class))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void c04b_validate_nonDyna_setterArityMismatch_returnsFalse() throws Exception {
		// pt.size() != 1 (two params here) short-circuits the type-compatibility check entirely - distinct from
		// c03's "one param, wrong type" case.
		var b = builder("str").setGetter(info(VGetters.class.getMethod("getStr")))
			.setSetter(info(VSetters.class.getMethod("setTwoArgs", String.class, String.class))).canRead().canWrite();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void c05_validate_nonDyna_fieldTypeMismatch_returnsFalse() throws Exception {
		// Resolver returns a fixed Integer type regardless of what's actually requested, so the declared field
		// type (Map) can never match it - exercises the "resolved type disagrees with the field" branch, which
		// is unreachable via ordinary field-driven resolution (the field IS the source of the resolved type there).
		var resolver = fixedTypeResolver(new FakeBeanInfo<>(Integer.class), new FakeBeanInfo<>(Object.class));
		var b = builder("mapField").setField(info(VFields.class.getField("mapField"))).canRead().canWrite();
		assertFalse(b.validate(resolver, null, Set.of(), Set.of()));
	}

	//====================================================================================================
	// validate() - dyna ("*") property variants
	//====================================================================================================

	@Test
	void d01_validate_dyna_getterReturnsMapNoArgs_setsIsDynaGetterMap() throws Exception {
		var b = builder("*").setGetter(info(VGetters.class.getMethod("getMap"))).canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertTrue(b.build().isDyna());
	}

	@Test
	void d02_validate_dyna_getterWithStringParam_ok() throws Exception {
		var b = builder("*").setGetter(info(VGetters.class.getMethod("getMapByKey", String.class))).canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void d03_validate_dyna_getterInvalidSignature_returnsFalse() throws Exception {
		var b = builder("*").setGetter(info(VGetters.class.getMethod("getBadDynaGetter", int.class))).canRead();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void d03b_validate_dyna_getterZeroArgsNonMap_returnsFalse() throws Exception {
		// pt.size() != 1 (zero args here) short-circuits the "single String-arg" check entirely - a distinct
		// combo from d03's "one non-String arg" case.
		var b = builder("*").setGetter(info(VGetters.class.getMethod("getBadDynaGetterNoArgs"))).canRead();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void d04_validate_dyna_setterTwoArgsStringFirst_ok() throws Exception {
		var b = builder("*").setSetter(info(VSetters.class.getMethod("setDynaOk", String.class, Object.class))).canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void d05_validate_dyna_setterInvalidSignature_returnsFalse() throws Exception {
		var b = builder("*").setSetter(info(VSetters.class.getMethod("setDynaBad", String.class))).canWrite();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void d05b_validate_dyna_setterTwoArgsFirstNotString_returnsFalse() throws Exception {
		// pt.size() == 2 (satisfies the arity half) but the first param isn't String - distinct from d05's
		// "wrong arity" case, which short-circuits before the type check is ever evaluated.
		var b = builder("*").setSetter(info(VSetters.class.getMethod("setDynaBadKeyType", Integer.class, Object.class))).canWrite();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void d06_validate_dyna_fieldNotMap_returnsFalse() throws Exception {
		var b = builder("*").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		assertFalse(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	@Test
	void d07_validate_dyna_fieldIsMap_valueTypeNull_fallsBackToObjectType() throws Exception {
		// FakeBeanInfo's default valueType is null; the resolver's objectType() is non-null, so the
		// post-dyna-reassignment null-refill (rawTypeMeta = bc.objectType()) is exercised.
		var b = builder("*").setField(info(VFields.class.getField("mapField"))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertTrue(b.build().isDyna());
	}

	@Test
	void d08_validate_dyna_fieldIsMap_valueTypeNonNull_keepsResolvedValueType() throws Exception {
		var fixed = new FakeBeanInfo<>(Map.class).valueType(new FakeBeanInfo<>(Object.class));
		var resolver = fixedTypeResolver(fixed, new FakeBeanInfo<>(Object.class));
		var b = builder("*").setField(info(VFields.class.getField("mapField"))).canRead().canWrite();
		assertTrue(b.validate(resolver, null, Set.of(), Set.of()));
	}

	@Test
	void d09_validate_dyna_valueTypeNull_objectTypeAlsoNull_typeMetaStaysNull() throws Exception {
		// Both the dyna value-type refill (line ~482) and the typeMeta refill (line ~487) fall through to a
		// null bc.objectType() - closes out the otherwise-dead-looking "still null after fallback" branches.
		var resolver = fixedTypeResolver(new FakeBeanInfo<>(Map.class), null);
		var b = builder("*").setField(info(VFields.class.getField("mapField"))).canRead().canWrite();
		assertTrue(b.validate(resolver, null, Set.of(), Set.of()));
		assertTrue(b.build().isDyna());
	}

	@Test
	void d10_validate_typeMetaPreset_skipsRefillEntirely() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		b.typeMeta = new FakeBeanInfo<>(Object.class);
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	//====================================================================================================
	// validate() - annotation ro/wo combinations on getter/setter (complements b03/b04)
	//====================================================================================================

	@Test
	void e01_validate_annotatedGetter_readOnlyFromAnnotation() throws Exception {
		var b = builder("name").setGetter(info(VAnnotatedGetterRo.class.getMethod("getName"))).canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertTrue(b.build().isReadOnly());
	}

	@Test
	void e02_validate_annotatedSetter_writeOnlyFromAnnotation() throws Exception {
		var b = builder("name").setSetter(info(VAnnotatedSetterWo.class.getMethod("setName", String.class))).canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertTrue(b.build().isWriteOnly());
	}

	@Test
	void e03_validate_bproWildcard_marksReadOnly() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of("*"), Set.of()));
		assertTrue(b.build().isReadOnly());
	}

	@Test
	void e04_validate_bpwoExactName_marksWriteOnly() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of("str")));
		assertTrue(b.build().isWriteOnly());
	}

	//====================================================================================================
	// validate() - inner-field-only (field == null, innerField != null) branches at line ~386
	//====================================================================================================

	@Test
	void f01_validate_innerFieldAnnotated_fieldNull_resolvesTypeFromAnnotatedInnerField() throws Exception {
		var b = builder("hidden")
			.setInnerField(info(VInnerFieldAnnotated.class.getDeclaredField("hidden")))
			.setGetter(info(VInnerFieldAnnotated.class.getMethod("getHidden"))).canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertTrue(b.build().isReadOnly());
	}

	@Test
	void f02_validate_innerFieldPlain_fieldNull_noAnnotation_skipsFieldResolution() throws Exception {
		var b = builder("hidden")
			.setInnerField(info(VInnerFieldPlain.class.getDeclaredField("hidden")))
			.setGetter(info(VInnerFieldPlain.class.getMethod("getHidden"))).canRead();
		// rawTypeMeta ends up resolved via the getter block instead (field block is skipped: field is null and
		// the inner field carries no @BeanProp annotation).
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
	}

	//====================================================================================================
	// Builder methods - delegateFor() / readTransform() / writeTransform() / rawMetaType(Class)
	//====================================================================================================

	@Test
	void g01_delegateFor_setsDelegate() throws Exception {
		var original = builder("orig").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		assertTrue(original.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		var originalPm = original.build();

		var b = builder("str").setField(info(VFields.class.getField("str"))).delegateFor(originalPm).canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		assertSame(originalPm, b.build().getDelegateFor());
	}

	@Test
	void g02_readTransform_appliesToGetResult() throws Exception {
		var beanMeta = BeanMeta.create(new FakeBeanInfo<>(VFields.class), null).beanMeta();
		var b = BeanPropertyMeta.builder(beanMeta, "str").setField(info(VFields.class.getField("str")))
			.readTransform((session, v) -> "transformed").canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		var pm = b.build();
		var bean = new VFields();
		bean.str = "original";
		var bMap = BeanMap.of(bean, beanMeta);
		bMap.setBeanSession(new FakeBeanSession());
		assertEquals("transformed", pm.get(bMap, "str"));
	}

	@Test
	void g03_writeTransform_appliesToSetValue() throws Exception {
		var bean = new VFields();
		var beanMeta = BeanMeta.create(new FakeBeanInfo<>(VFields.class), null).beanMeta();
		var b = BeanPropertyMeta.builder(beanMeta, "str").setField(info(VFields.class.getField("str")))
			.writeTransform((session, v) -> "transformed").canRead().canWrite();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		var bMap = BeanMap.of(bean, beanMeta);
		bMap.setBeanSession(new FakeBeanSession());
		b.build().set(bMap, "str", "original");
		assertEquals("transformed", bean.str);
	}

	@Test
	void g04_rawMetaTypeFromClass_bcNonNull_resolvesViaResolver() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		b.bc = new FakeBeanTypeResolver();
		b.rawMetaType(String.class);
		assertNotNull(b.rawTypeMeta);
		assertNotNull(b.typeMeta);
	}

	@Test
	void g05_rawMetaTypeFromClass_bcNull_leavesTypeMetaNull() throws Exception {
		var b = builder("str").setField(info(VFields.class.getField("str"))).canRead().canWrite();
		b.rawMetaType(String.class);
		assertNull(b.rawTypeMeta);
	}

	//====================================================================================================
	// getDynaMap() - marshalling-path branches
	//====================================================================================================

	public static class DynaGetterBean {
		private final Map<String,Object> extra = new LinkedHashMap<>();
		public Map<String,Object> getExtra() { return extra; }
	}

	public static class DynaNoAccessorsBean {
		public String unrelated;  // Gives BeanMeta.create() a property to discover so it doesn't reject the class as "not a bean".
	}

	public static class DynaExtraKeysBean {
		public String unrelated;  // Gives BeanMeta.create() a property to discover so it doesn't reject the class as "not a bean".
		private final Map<String,Object> data = new LinkedHashMap<>();
		public Collection<String> extraKeys() { return data.keySet(); }
		public Object getExtra(String key) { return data.get(key); }
	}

	@Test
	void h01_getDynaMap_extraKeysWithPerKeyGetter_enumeratesViaExtraKeys() throws Exception {
		var beanMeta = BeanMeta.create(new FakeBeanInfo<>(DynaExtraKeysBean.class), null).beanMeta();
		var b = BeanPropertyMeta.builder(beanMeta, "*")
			.setGetter(info(DynaExtraKeysBean.class.getMethod("getExtra", String.class)))
			.setExtraKeys(info(DynaExtraKeysBean.class.getMethod("extraKeys")))
			.canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		var pm = b.build();
		var bean = new DynaExtraKeysBean();
		bean.data.put("a", 1);
		bean.data.put("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), pm.getDynaMap(bean));
	}

	@Test
	void h02_getDynaMap_extraKeysSetButGetterIsDynaGetterMap_getterTakesPrecedence() throws Exception {
		// extraKeys is (unusually, but legally) configured alongside a no-arg Map-returning dyna getter - the
		// "!isDynaGetterMap" guard means the getter's own Map is returned directly rather than going through
		// extraKeys' per-key enumeration.
		var beanMeta = BeanMeta.create(new FakeBeanInfo<>(DynaGetterBean.class), null).beanMeta();
		var b = BeanPropertyMeta.builder(beanMeta, "*")
			.setGetter(info(DynaGetterBean.class.getMethod("getExtra")))
			.setExtraKeys(info(DynaExtraKeysBean.class.getMethod("extraKeys")))
			.canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		var pm = b.build();
		var bean = new DynaGetterBean();
		bean.extra.put("k", "v");
		assertEquals(Map.of("k", "v"), pm.getDynaMap(bean));
	}

	public static class BeanPropBean {
		public NestedBean nested;
	}

	public static class NestedBean {
		public String name;
	}

	@Test
	void h03_getDynaMap_extraKeysSetButNoGetter_readsFromFieldInstead() throws Exception {
		var beanMeta = BeanMeta.create(new FakeBeanInfo<>(BeanPropBean.class), null).beanMeta();
		var b = BeanPropertyMeta.builder(beanMeta, "*")
			.setField(info(BeanPropBean.class.getField("nested")))
			.setExtraKeys(info(DynaExtraKeysBean.class.getMethod("extraKeys")))
			.canRead().canWrite();
		b.isDyna = true;
		var pm = b.build();
		var bean = new BeanPropBean();
		bean.nested = null;
		assertNull(pm.getDynaMap(bean));
	}

	@Test
	void h04_getDynaMap_dynaGetterWithKeyParam_noExtraKeys_throws() throws Exception {
		// A per-key dyna getter (single String param) without an extraKeys accessor has no way to enumerate all
		// entries, so getDynaMap() falls through every accessor check and throws.
		var beanMeta = BeanMeta.create(new FakeBeanInfo<>(VGetters.class), null).beanMeta();
		var b = BeanPropertyMeta.builder(beanMeta, "*")
			.setGetter(info(VGetters.class.getMethod("getMapByKey", String.class))).canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		var pm = b.build();
		assertThrowsWithMessage(BeanRuntimeException.class, "Getter or public field not defined", () -> pm.getDynaMap(new VGetters()));
	}

	@Test
	void e01_getDynaMap_dynaGetterMap_returnsGetterResult() throws Exception {
		var beanMeta = BeanMeta.create(new FakeBeanInfo<>(DynaGetterBean.class), null).beanMeta();
		var b = BeanPropertyMeta.builder(beanMeta, "*")
			.setGetter(info(DynaGetterBean.class.getMethod("getExtra"))).canRead();
		assertTrue(b.validate(new FakeBeanTypeResolver(), null, Set.of(), Set.of()));
		var pm = b.build();
		var bean = new DynaGetterBean();
		bean.extra.put("k", "v");
		assertEquals(Map.of("k", "v"), pm.getDynaMap(bean));
	}

	@Test
	void e02_getDynaMap_noGetterOrFieldOrExtraKeys_throws() throws Exception {
		// isDyna can only be set true through validate() (private field), and validate() rejects a dyna property
		// that ends up with no getter/field/extraKeys before returning true - so the "no accessor" throw in
		// getDynaMap() can only be observed by relaxing isDyna to package-private for direct test construction.
		var beanMeta = BeanMeta.create(new FakeBeanInfo<>(DynaNoAccessorsBean.class), null).beanMeta();
		var b = BeanPropertyMeta.builder(beanMeta, "*").canRead();
		b.isDyna = true;
		var pm = b.build();
		assertThrowsWithMessage(BeanRuntimeException.class, "Getter or public field not defined", () -> pm.getDynaMap(new DynaNoAccessorsBean()));
	}
}
