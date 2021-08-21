// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.annotation;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.transform.*;
import org.junit.*;

/**
 * Tests the @BeanConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class BeanConfigAnnotation_Test {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@SuppressWarnings({ "rawtypes" })
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof List)
				return ((List<?>)t)
					.stream()
					.map(TO_STRING)
					.collect(Collectors.joining(","));
			if (t.getClass().isArray())
				return apply(ArrayUtils.toList(t, Object.class));
			if (t instanceof OMap)
				return ((OMap)t).toString();
			if (t instanceof Map)
				return ((Map<?,?>)t)
					.entrySet()
					.stream()
					.map(TO_STRING)
					.collect(Collectors.joining(","));
			if (t instanceof Map.Entry) {
				Map.Entry e = (Map.Entry)t;
				return apply(e.getKey()) + "=" + apply(e.getValue());
			}
			if (t instanceof BeanFilter)
				return ((BeanFilter)t).getBeanClass().getSimpleName();
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getSimpleName();
			if (t instanceof PropertyNamer)
				return ((PropertyNamer)t).getClass().getSimpleName();
			if (t instanceof TimeZone)
				return ((TimeZone)t).getID();
			return t.toString();
		}
	};

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Bean(typeName="A1")
	public static class A1 {
		public int foo;
		@Override
		public String toString() {return SimpleJson.DEFAULT.toString(this);}
	}
	@Bean(typeName="A2")
	public static class A2 {
		public int foo;
	}
	@Bean(typeName="A3")
	public static class A3 {
		public int foo;
	}
	public static class AB1 extends PojoSwap<String,Integer> {
	}
	public static class AB2 extends PojoSwap<String,Integer> {
	}
	public static class AB3 extends PojoSwap<String,Integer> {
	}

	@BeanConfig(
		beanClassVisibility="$X{PRIVATE}",
		beanConstructorVisibility="$X{PRIVATE}",
		dictionary={A1.class,A2.class},
		dictionary_replace={A1.class,A2.class,A3.class},
		beanFieldVisibility="$X{PRIVATE}",
		beanMapPutReturnsOldValue="$X{true}",
		beanMethodVisibility="$X{PRIVATE}",
		beansRequireDefaultConstructor="$X{true}",
		beansRequireSerializable="$X{true}",
		beansRequireSettersForGetters="$X{true}",
		disableBeansRequireSomeProperties="$X{true}",
		typePropertyName="$X{foo}",
		debug="$X{true}",
		disableIgnoreUnknownNullBeanProperties="$X{true}",
		disableIgnoreMissingSetters="$X{true}",
		disableInterfaceProxies="$X{true}",
		findFluentSetters="$X{true}",
		ignoreInvocationExceptionsOnGetters="$X{true}",
		ignoreInvocationExceptionsOnSetters="$X{true}",
		ignoreUnknownBeanProperties="$X{true}",
		locale="$X{en-US}",
		mediaType="$X{text/foo}",
		notBeanClasses={A1.class,A2.class},
		notBeanClasses_replace={A1.class,A2.class,A3.class},
		notBeanPackages={"$X{foo1}","$X{foo2}"},
		notBeanPackages_replace={"$X{foo1}","$X{foo2}","$X{foo3}"},
		swaps={AB1.class,AB2.class},
		swaps_replace={AB1.class,AB2.class,AB3.class},
		propertyNamer=PropertyNamerULC.class,
		sortProperties="$X{true}",
		timeZone="$X{z}",
		useEnumNames="$X{true}",
		useJavaBeanIntrospector="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void a01_basic() throws Exception {
		AnnotationList al = a.getAnnotationList();
		BeanSession bc = JsonSerializer.create().applyAnnotations(al, sr).build().createSession();

		check("PRIVATE", bc.getBeanClassVisibility());
		check("PRIVATE", bc.getBeanConstructorVisibility());
		check("A1,A2,A3", bc.getBeanDictionaryClasses());
		check("PRIVATE", bc.getBeanFieldVisibility());
		check("true", bc.isBeanMapPutReturnsOldValue());
		check("PRIVATE", bc.getBeanMethodVisibility());
		check("true", bc.isBeansRequireDefaultConstructor());
		check("true", bc.isBeansRequireSerializable());
		check("true", bc.isBeansRequireSettersForGetters());
		check("false", bc.isBeansRequireSomeProperties());
		check("foo", bc.getBeanTypePropertyName());
		check("true", bc.isDebug());
		check("true", bc.isFindFluentSetters());
		check("true", bc.isIgnoreInvocationExceptionsOnGetters());
		check("true", bc.isIgnoreInvocationExceptionsOnSetters());
		check("false", bc.isIgnoreMissingSetters());
		check("true", bc.isIgnoreUnknownBeanProperties());
		check("false", bc.isIgnoreUnknownNullBeanProperties());
		check("en_US", bc.getLocale());
		check("application/json", bc.getMediaType());
		check("A1,A2,A3", bc.getNotBeanClasses());
		check("foo1,foo2,foo3", bc.getNotBeanPackagesNames());
		check("AB1<String,Integer>,AB2<String,Integer>,AB3<String,Integer>", bc.getSwaps());
		check("PropertyNamerULC", bc.getPropertyNamer());
		check("true", bc.isSortProperties());
		check("GMT", bc.getTimeZone());
		check("true", bc.isUseEnumNames());
		check("false", bc.isUseInterfaceProxies());
		check("true", bc.isUseJavaBeanIntrospector());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@BeanConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void b01_noValues() throws Exception {
		AnnotationList al = b.getAnnotationList();
		JsonSerializer js = JsonSerializer.create().applyAnnotations(al, sr).build();
		BeanContext bc = js.getBeanContext();
		check("PUBLIC", bc.getBeanClassVisibility());
		check("PUBLIC", bc.getBeanConstructorVisibility());
		check("", bc.getBeanDictionaryClasses());
		check("PUBLIC", bc.getBeanFieldVisibility());
		check("false", bc.isBeanMapPutReturnsOldValue());
		check("PUBLIC", bc.getBeanMethodVisibility());
		check("false", bc.isBeansRequireDefaultConstructor());
		check("false", bc.isBeansRequireSerializable());
		check("false", bc.isBeansRequireSettersForGetters());
		check("true", bc.isBeansRequireSomeProperties());
		check("_type", bc.getBeanTypePropertyName());
		check("false", js.isDebug());
		check("false", js.isDetectRecursions());
		check("false", bc.isFindFluentSetters());
		check("false", bc.isIgnoreInvocationExceptionsOnGetters());
		check("false", bc.isIgnoreInvocationExceptionsOnSetters());
		check("true", bc.isIgnoreMissingSetters());
		check("false", js.isIgnoreRecursions());
		check("false", bc.isIgnoreUnknownBeanProperties());
		check("true", bc.isIgnoreUnknownNullBeanProperties());
		check("0", js.getInitialDepth());
		check(Locale.getDefault().toString(), bc.getDefaultLocale());
		check("100", js.getMaxDepth());
		check(null, bc.getDefaultMediaType());
		check("java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net", bc.getNotBeanPackagesNames());
		check("", bc.getSwaps());
		check("BasicPropertyNamer", bc.getPropertyNamer());
		check("false", bc.isSortProperties());
		check(null, bc.getDefaultTimeZone());
		check("false", bc.isUseEnumNames());
		check("true", bc.isUseInterfaceProxies());
		check("false", bc.isUseJavaBeanIntrospector());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void c01_noAnnotation() throws Exception {
		AnnotationList al = c.getAnnotationList();
		JsonSerializer js = JsonSerializer.create().applyAnnotations(al, sr).build();
		BeanContext bc = js.getBeanContext();
		check("PUBLIC", bc.getBeanClassVisibility());
		check("PUBLIC", bc.getBeanConstructorVisibility());
		check("", bc.getBeanDictionaryClasses());
		check("PUBLIC", bc.getBeanFieldVisibility());
		check("false", bc.isBeanMapPutReturnsOldValue());
		check("PUBLIC", bc.getBeanMethodVisibility());
		check("false", bc.isBeansRequireDefaultConstructor());
		check("false", bc.isBeansRequireSerializable());
		check("false", bc.isBeansRequireSettersForGetters());
		check("true", bc.isBeansRequireSomeProperties());
		check("_type", bc.getBeanTypePropertyName());
		check("false", js.isDebug());
		check("false", js.isDetectRecursions());
		check("false", bc.isFindFluentSetters());
		check("false", bc.isIgnoreInvocationExceptionsOnGetters());
		check("false", bc.isIgnoreInvocationExceptionsOnSetters());
		check("true", bc.isIgnoreMissingSetters());
		check("false", js.isIgnoreRecursions());
		check("false", bc.isIgnoreUnknownBeanProperties());
		check("true", bc.isIgnoreUnknownNullBeanProperties());
		check("0", js.getInitialDepth());
		check(Locale.getDefault().toString(), bc.getDefaultLocale());
		check("100", js.getMaxDepth());
		check(null, bc.getDefaultMediaType());
		check("java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net", bc.getNotBeanPackagesNames());
		check("", bc.getSwaps());
		check("BasicPropertyNamer", bc.getPropertyNamer());
		check("false", bc.isSortProperties());
		check(null, bc.getDefaultTimeZone());
		check("false", bc.isUseEnumNames());
		check("true", bc.isUseInterfaceProxies());
		check("false", bc.isUseJavaBeanIntrospector());
	}

}
