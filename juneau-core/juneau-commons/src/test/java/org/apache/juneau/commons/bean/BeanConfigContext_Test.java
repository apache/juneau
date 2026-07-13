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
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"java:S5961" // Comprehensive single-feature coverage test exceeds the per-method assertion threshold.
})
class BeanConfigContext_Test extends TestBase {

	//====================================================================================================
	// Defaults / DEFAULT singleton
	//====================================================================================================

	@Test
	void a01_default_visibilityIsPublic() {
		var ctx = BeanConfigContext.DEFAULT;
		assertEquals(Visibility.PUBLIC, ctx.getBeanClassVisibility());
		assertEquals(Visibility.PUBLIC, ctx.getBeanConstructorVisibility());
		assertEquals(Visibility.PUBLIC, ctx.getBeanFieldVisibility());
		assertEquals(Visibility.PUBLIC, ctx.getBeanMethodVisibility());
	}

	@Test
	void a02_default_booleanToggles() {
		var ctx = BeanConfigContext.DEFAULT;
		assertFalse(ctx.isBeanMapPutReturnsOldValue());
		assertFalse(ctx.isBeansRequireDefaultConstructor());
		assertFalse(ctx.isBeansRequireSerializable());
		assertFalse(ctx.isBeansRequireSettersForGetters());
		assertTrue(ctx.isBeansRequireSomeProperties());
		assertFalse(ctx.isFindFluentSetters());
		assertFalse(ctx.isIgnoreInvocationExceptionsOnGetters());
		assertFalse(ctx.isIgnoreInvocationExceptionsOnSetters());
		assertTrue(ctx.isIgnoreMissingSetters());
		assertTrue(ctx.isIgnoreTransientFields());
		assertFalse(ctx.isIgnoreUnknownBeanProperties());
		assertTrue(ctx.isIgnoreUnknownNullBeanProperties());
		assertFalse(ctx.isUnsortedProperties());
		assertTrue(ctx.isUseInterfaceProxies());
		assertFalse(ctx.isUseJavaBeanIntrospector());
	}

	@Test
	void a03_default_namingAndCollections() {
		var ctx = BeanConfigContext.DEFAULT;
		assertNotNull(ctx.getPropertyNamer());
		assertInstanceOf(BasicPropertyNamer.class, ctx.getPropertyNamer());
		assertEquals("_type", ctx.getBeanTypePropertyName());
		assertTrue(ctx.getNotBeanPackageNames().isEmpty());
		assertTrue(ctx.getNotBeanPackagePrefixes().isEmpty());
		assertTrue(ctx.getNotBeanClasses().isEmpty());
	}

	@Test
	void a04_default_storeAndAnnotationProvider() {
		var ctx = BeanConfigContext.DEFAULT;
		assertNull(ctx.getBeanStore());
		assertNotNull(ctx.getAnnotationProvider());
		assertSame(AnnotationProvider.INSTANCE, ctx.getAnnotationProvider());
	}

	//====================================================================================================
	// Builder setters
	//====================================================================================================

	@Test
	void b01_builder_visibilitySetters() {
		var ctx = BeanConfigContext.create()
			.beanClassVisibility(Visibility.PROTECTED)
			.beanConstructorVisibility(Visibility.PRIVATE)
			.beanFieldVisibility(Visibility.DEFAULT)
			.beanMethodVisibility(Visibility.PROTECTED)
			.build();
		assertEquals(Visibility.PROTECTED, ctx.getBeanClassVisibility());
		assertEquals(Visibility.PRIVATE, ctx.getBeanConstructorVisibility());
		assertEquals(Visibility.DEFAULT, ctx.getBeanFieldVisibility());
		assertEquals(Visibility.PROTECTED, ctx.getBeanMethodVisibility());
	}

	@Test
	void b02_builder_booleanToggles() {
		var ctx = BeanConfigContext.create()
			.beanMapPutReturnsOldValue(true)
			.beansRequireDefaultConstructor(true)
			.beansRequireSerializable(true)
			.beansRequireSettersForGetters(true)
			.beansRequireSomeProperties(false)
			.findFluentSetters(true)
			.ignoreInvocationExceptionsOnGetters(true)
			.ignoreInvocationExceptionsOnSetters(true)
			.ignoreMissingSetters(false)
			.ignoreTransientFields(false)
			.ignoreUnknownBeanProperties(true)
			.ignoreUnknownNullBeanProperties(false)
			.unsortedProperties(true)
			.useInterfaceProxies(false)
			.useJavaBeanIntrospector(true)
			.build();
		assertTrue(ctx.isBeanMapPutReturnsOldValue());
		assertTrue(ctx.isBeansRequireDefaultConstructor());
		assertTrue(ctx.isBeansRequireSerializable());
		assertTrue(ctx.isBeansRequireSettersForGetters());
		assertFalse(ctx.isBeansRequireSomeProperties());
		assertTrue(ctx.isFindFluentSetters());
		assertTrue(ctx.isIgnoreInvocationExceptionsOnGetters());
		assertTrue(ctx.isIgnoreInvocationExceptionsOnSetters());
		assertFalse(ctx.isIgnoreMissingSetters());
		assertFalse(ctx.isIgnoreTransientFields());
		assertTrue(ctx.isIgnoreUnknownBeanProperties());
		assertFalse(ctx.isIgnoreUnknownNullBeanProperties());
		assertTrue(ctx.isUnsortedProperties());
		assertFalse(ctx.isUseInterfaceProxies());
		assertTrue(ctx.isUseJavaBeanIntrospector());
	}

	@Test
	void b03_builder_propertyNamerAndTypeName() {
		var dlc = new PropertyNamerDLC();
		var ctx = BeanConfigContext.create()
			.propertyNamer(dlc)
			.beanTypePropertyName("kind")
			.build();
		assertSame(dlc, ctx.getPropertyNamer());
		assertEquals("kind", ctx.getBeanTypePropertyName());
	}

	@Test
	void b04_builder_notBeanVarargs() {
		var ctx = BeanConfigContext.create()
			.notBeanPackageNames("pkg.a", "pkg.b")
			.notBeanPackagePrefixes("pkg.c.", "pkg.d.")
			.notBeanClasses(String.class, Integer.class)
			.build();
		assertEquals(Set.of("pkg.a", "pkg.b"), ctx.getNotBeanPackageNames());
		assertEquals(Set.of("pkg.c.", "pkg.d."), ctx.getNotBeanPackagePrefixes());
		assertEquals(Set.of(String.class, Integer.class), ctx.getNotBeanClasses());
	}

	@Test
	void b05_builder_notBeanCollectionReplacers() {
		var ctx = BeanConfigContext.create()
			.notBeanPackageNames("seed.a")
			.notBeanPackageNames(List.of("only.a", "only.b"))
			.notBeanPackagePrefixes(List.of("pre."))
			.notBeanClasses(List.of(String.class, Long.class))
			.build();
		assertEquals(Set.of("only.a", "only.b"), ctx.getNotBeanPackageNames());
		assertEquals(Set.of("pre."), ctx.getNotBeanPackagePrefixes());
		assertEquals(Set.of(String.class, Long.class), ctx.getNotBeanClasses());
	}

	@Test
	void b06_builder_notBeanCollectionReplacers_nullClearsAll() {
		var ctx = BeanConfigContext.create()
			.notBeanPackageNames("seed.a")
			.notBeanPackagePrefixes("seed.b.")
			.notBeanClasses(String.class)
			.notBeanPackageNames((Collection<String>)null)
			.notBeanPackagePrefixes((Collection<String>)null)
			.notBeanClasses((Collection<? extends Class<?>>)null)
			.build();
		assertTrue(ctx.getNotBeanPackageNames().isEmpty());
		assertTrue(ctx.getNotBeanPackagePrefixes().isEmpty());
		assertTrue(ctx.getNotBeanClasses().isEmpty());
	}

	@Test
	void b07_builder_storeAndAnnotationProvider() {
		var store = new BasicBeanStore(null);
		var ap = AnnotationProvider.create().build();
		var ctx = BeanConfigContext.create()
			.beanStore(store)
			.annotationProvider(ap)
			.build();
		assertSame(store, ctx.getBeanStore());
		assertSame(ap, ctx.getAnnotationProvider());
	}

	@Test
	void b08_builder_beanStore_acceptsNull() {
		var ctx = BeanConfigContext.create().beanStore(null).build();
		assertNull(ctx.getBeanStore());
	}

	//====================================================================================================
	// Setter null-arg validation
	//====================================================================================================

	@Test
	void c01_setters_rejectNullVisibility() {
		var b = BeanConfigContext.create();
		assertThrows(IllegalArgumentException.class, () -> b.beanClassVisibility(null));
		assertThrows(IllegalArgumentException.class, () -> b.beanConstructorVisibility(null));
		assertThrows(IllegalArgumentException.class, () -> b.beanFieldVisibility(null));
		assertThrows(IllegalArgumentException.class, () -> b.beanMethodVisibility(null));
	}

	@Test
	void c02_setters_rejectNullPropertyNamer() {
		var b = BeanConfigContext.create();
		assertThrows(IllegalArgumentException.class, () -> b.propertyNamer(null));
	}

	@Test
	void c03_setters_rejectNullTypeName() {
		var b = BeanConfigContext.create();
		assertThrows(IllegalArgumentException.class, () -> b.beanTypePropertyName(null));
	}

	@Test
	void c04_setters_rejectNullVarargArrays() {
		var b = BeanConfigContext.create();
		assertThrows(IllegalArgumentException.class, () -> b.notBeanPackageNames((String[])null));
		assertThrows(IllegalArgumentException.class, () -> b.notBeanPackagePrefixes((String[])null));
		assertThrows(IllegalArgumentException.class, () -> b.notBeanClasses((Class<?>[])null));
	}

	@Test
	void c05_setters_rejectNullAnnotationProvider() {
		var b = BeanConfigContext.create();
		assertThrows(IllegalArgumentException.class, () -> b.annotationProvider(null));
	}

	//====================================================================================================
	// copy()
	//====================================================================================================

	@SuppressWarnings({
		"java:S5961" // Comprehensive single-feature coverage: verifies copy() preserves every configuration field.
	})
	@Test
	void d01_copy_preservesAllValues() {
		var store = new BasicBeanStore(null);
		var ap = AnnotationProvider.create().build();
		var dlc = new PropertyNamerDLC();
		var src = BeanConfigContext.create()
			.beanClassVisibility(Visibility.PROTECTED)
			.beanConstructorVisibility(Visibility.PRIVATE)
			.beanFieldVisibility(Visibility.DEFAULT)
			.beanMethodVisibility(Visibility.PROTECTED)
			.beanMapPutReturnsOldValue(true)
			.beansRequireDefaultConstructor(true)
			.beansRequireSerializable(true)
			.beansRequireSettersForGetters(true)
			.beansRequireSomeProperties(false)
			.findFluentSetters(true)
			.ignoreInvocationExceptionsOnGetters(true)
			.ignoreInvocationExceptionsOnSetters(true)
			.ignoreMissingSetters(false)
			.ignoreTransientFields(false)
			.ignoreUnknownBeanProperties(true)
			.ignoreUnknownNullBeanProperties(false)
			.unsortedProperties(true)
			.useInterfaceProxies(false)
			.useJavaBeanIntrospector(true)
			.propertyNamer(dlc)
			.beanTypePropertyName("kind")
			.notBeanPackageNames("pkg.a")
			.notBeanPackagePrefixes("pkg.b.")
			.notBeanClasses(String.class)
			.beanStore(store)
			.annotationProvider(ap)
			.notABeanPredicate(ci -> false)
			.build();

		var copy = src.copy().build();

		assertEquals(src.getBeanClassVisibility(), copy.getBeanClassVisibility());
		assertEquals(src.getBeanConstructorVisibility(), copy.getBeanConstructorVisibility());
		assertEquals(src.getBeanFieldVisibility(), copy.getBeanFieldVisibility());
		assertEquals(src.getBeanMethodVisibility(), copy.getBeanMethodVisibility());
		assertEquals(src.isBeanMapPutReturnsOldValue(), copy.isBeanMapPutReturnsOldValue());
		assertEquals(src.isBeansRequireDefaultConstructor(), copy.isBeansRequireDefaultConstructor());
		assertEquals(src.isBeansRequireSerializable(), copy.isBeansRequireSerializable());
		assertEquals(src.isBeansRequireSettersForGetters(), copy.isBeansRequireSettersForGetters());
		assertEquals(src.isBeansRequireSomeProperties(), copy.isBeansRequireSomeProperties());
		assertEquals(src.isFindFluentSetters(), copy.isFindFluentSetters());
		assertEquals(src.isIgnoreInvocationExceptionsOnGetters(), copy.isIgnoreInvocationExceptionsOnGetters());
		assertEquals(src.isIgnoreInvocationExceptionsOnSetters(), copy.isIgnoreInvocationExceptionsOnSetters());
		assertEquals(src.isIgnoreMissingSetters(), copy.isIgnoreMissingSetters());
		assertEquals(src.isIgnoreTransientFields(), copy.isIgnoreTransientFields());
		assertEquals(src.isIgnoreUnknownBeanProperties(), copy.isIgnoreUnknownBeanProperties());
		assertEquals(src.isIgnoreUnknownNullBeanProperties(), copy.isIgnoreUnknownNullBeanProperties());
		assertEquals(src.isUnsortedProperties(), copy.isUnsortedProperties());
		assertEquals(src.isUseInterfaceProxies(), copy.isUseInterfaceProxies());
		assertEquals(src.isUseJavaBeanIntrospector(), copy.isUseJavaBeanIntrospector());
		assertSame(src.getPropertyNamer(), copy.getPropertyNamer());
		assertEquals(src.getBeanTypePropertyName(), copy.getBeanTypePropertyName());
		assertEquals(src.getNotBeanPackageNames(), copy.getNotBeanPackageNames());
		assertEquals(src.getNotBeanPackagePrefixes(), copy.getNotBeanPackagePrefixes());
		assertEquals(src.getNotBeanClasses(), copy.getNotBeanClasses());
		assertSame(src.getBeanStore(), copy.getBeanStore());
		assertSame(src.getAnnotationProvider(), copy.getAnnotationProvider());
		// custom predicate path delegates to user predicate => false for any input
		assertFalse(copy.isNotABean(info(String.class)));
	}

	@Test
	void d02_copy_isIndependent() {
		var src = BeanConfigContext.create().findFluentSetters(true).build();
		var copy = src.copy().findFluentSetters(false).build();
		assertTrue(src.isFindFluentSetters());
		assertFalse(copy.isFindFluentSetters());
	}

	@Test
	void d03_copy_collectionsDoNotShareInstance() {
		var src = BeanConfigContext.create().notBeanPackageNames("pkg.a").build();
		var copy = src.copy().notBeanPackageNames("pkg.b").build();
		assertEquals(Set.of("pkg.a"), src.getNotBeanPackageNames());
		assertEquals(Set.of("pkg.a", "pkg.b"), copy.getNotBeanPackageNames());
	}

	//====================================================================================================
	// Returned collections are unmodifiable
	//====================================================================================================

	@Test
	void e01_collectionsAreUnmodifiable() {
		var ctx = BeanConfigContext.create()
			.notBeanPackageNames("pkg.a")
			.notBeanPackagePrefixes("pkg.b.")
			.notBeanClasses(String.class)
			.build();
		assertThrows(UnsupportedOperationException.class, () -> ctx.getNotBeanPackageNames().add("x"));
		assertThrows(UnsupportedOperationException.class, () -> ctx.getNotBeanPackagePrefixes().add("y"));
		assertThrows(UnsupportedOperationException.class, () -> ctx.getNotBeanClasses().add(Object.class));
	}

	//====================================================================================================
	// isNotABean default behavior
	//====================================================================================================

	@Test
	void f01_isNotABean_rejectsNonClassKinds() {
		var ctx = BeanConfigContext.DEFAULT;
		assertTrue(ctx.isNotABean(info(int.class)));
		assertTrue(ctx.isNotABean(info(int[].class)));
		assertTrue(ctx.isNotABean(info(java.lang.annotation.Retention.class)));
		assertTrue(ctx.isNotABean(info(java.time.DayOfWeek.class)));
	}

	@Test
	void f02_isNotABean_acceptsRegularClasses() {
		var ctx = BeanConfigContext.DEFAULT;
		assertFalse(ctx.isNotABean(info(BasicBeanStore.class)));
	}

	@Test
	void f03_isNotABean_packageName_excludes() {
		var ctx = BeanConfigContext.create()
			.notBeanPackageNames(BasicBeanStore.class.getPackage().getName())
			.build();
		assertTrue(ctx.isNotABean(info(BasicBeanStore.class)));
	}

	@Test
	void f04_isNotABean_packagePrefix_excludes() {
		var ctx = BeanConfigContext.create()
			.notBeanPackagePrefixes("org.apache.juneau.commons.")
			.build();
		assertTrue(ctx.isNotABean(info(BasicBeanStore.class)));
	}

	@Test
	void f05_isNotABean_classExclude() {
		var ctx = BeanConfigContext.create().notBeanClasses(BasicBeanStore.class).build();
		assertTrue(ctx.isNotABean(info(BasicBeanStore.class)));
	}

	@Test
	void f06_isNotABean_classExclude_includesSubtypes() {
		var ctx = BeanConfigContext.create().notBeanClasses(CharSequence.class).build();
		assertTrue(ctx.isNotABean(info(String.class)));
	}

	@Test
	void f07_isNotABean_customPredicateOverridesDefault() {
		var ctx = BeanConfigContext.create()
			.notABeanPredicate(ci -> ci.is(String.class))
			.build();
		assertTrue(ctx.isNotABean(info(String.class)));
		assertFalse(ctx.isNotABean(info(int.class))); // default would say true; predicate overrides
	}

	@Test
	void f08_isNotABean_customPredicateClearedToNullRevertsToDefault() {
		var ctx = BeanConfigContext.create()
			.notABeanPredicate(ci -> false)
			.notABeanPredicate(null)
			.build();
		assertTrue(ctx.isNotABean(info(int.class)));
	}

	@Test
	void f09_isNotABean_rejectsNullArg() {
		var ctx = BeanConfigContext.DEFAULT;
		assertThrows(IllegalArgumentException.class, () -> ctx.isNotABean(null));
	}

	@Test
	void f10_isNotABean_packageName_nonMatchingEntryIsSkipped() {
		var ctx = BeanConfigContext.create()
			.notBeanPackageNames("never.matches.this")
			.build();
		assertFalse(ctx.isNotABean(info(BasicBeanStore.class)));
	}

	@Test
	void f11_isNotABean_packagePrefix_nonMatchingEntryIsSkipped() {
		var ctx = BeanConfigContext.create()
			.notBeanPackagePrefixes("never.matches.")
			.build();
		assertFalse(ctx.isNotABean(info(BasicBeanStore.class)));
	}

	@Test
	void f12_isNotABean_classExclude_nonMatchingEntryIsSkipped() {
		var ctx = BeanConfigContext.create()
			.notBeanClasses(UUID.class)
			.build();
		assertFalse(ctx.isNotABean(info(BasicBeanStore.class)));
	}
}
