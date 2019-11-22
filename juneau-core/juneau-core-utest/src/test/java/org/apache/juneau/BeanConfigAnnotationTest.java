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
package org.apache.juneau;

import static org.junit.Assert.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
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
public class BeanConfigAnnotationTest {

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
			if (t instanceof ObjectMap)
				return ((ObjectMap)t).toString();
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
		dictionary_remove=A2.class,
		beanFieldVisibility="$X{PRIVATE}",
		beanFilters={A1.class,A2.class},
		beanFilters_replace={A1.class,A2.class,A3.class},
		beanFilters_remove=A2.class,
		beanMapPutReturnsOldValue="$X{true}",
		beanMethodVisibility="$X{PRIVATE}",
		beansRequireDefaultConstructor="$X{true}",
		beansRequireSerializable="$X{true}",
		beansRequireSettersForGetters="$X{true}",
		beansRequireSomeProperties="$X{true}",
		beanTypePropertyName="$X{foo}",
		bpiMap=@CS(k=A1.class,v="$X{foo}"),
		bpxMap=@CS(k=A1.class,v="$X{foo}"),
		bproMap=@CS(k=A1.class,v="$X{foo}"),
		bpwoMap=@CS(k=A1.class,v="$X{foo}"),
		debug="$X{true}",
		detectRecursions="$X{true}",
		examples="$X{A1}: {foo:1}",
		fluentSetters="$X{true}",
		ignoreInvocationExceptionsOnGetters="$X{true}",
		ignoreInvocationExceptionsOnSetters="$X{true}",
		ignorePropertiesWithoutSetters="$X{true}",
		ignoreRecursions="$X{true}",
		ignoreUnknownBeanProperties="$X{true}",
		ignoreUnknownNullBeanProperties="$X{true}",
		implClasses=@CC(k=A1.class,v=A1.class),
		initialDepth="$X{1}",
		locale="$X{en-US}",
		maxDepth="$X{1}",
		mediaType="$X{text/foo}",
		notBeanClasses={A1.class,A2.class},
		notBeanClasses_replace={A1.class,A2.class,A3.class},
		notBeanClasses_remove=A2.class,
		notBeanPackages={"$X{foo1}","$X{foo2}"},
		notBeanPackages_replace={"$X{foo1}","$X{foo2}","$X{foo3}"},
		notBeanPackages_remove={"$X{foo2}"},
		pojoSwaps={AB1.class,AB2.class},
		pojoSwaps_replace={AB1.class,AB2.class,AB3.class},
		pojoSwaps_remove=AB2.class,
		propertyNamer=PropertyNamerULC.class,
		sortProperties="$X{true}",
		timeZone="$X{z}",
		useEnumNames="$X{true}",
		useInterfaceProxies="$X{true}",
		useJavaBeanIntrospector="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basic() throws Exception {
		AnnotationList al = a.getAnnotationList(null);
		BeanTraverseSession bc = JsonSerializer.create().applyAnnotations(al, sr).build().createSession();

		check("PRIVATE", bc.getBeanClassVisibility());
		check("PRIVATE", bc.getBeanConstructorVisibility());
		check("A1,A3", bc.getBeanDictionaryClasses());
		check("PRIVATE", bc.getBeanFieldVisibility());
		check("A1,A3", bc.getBeanFilters());
		check("true", bc.isBeanMapPutReturnsOldValue());
		check("PRIVATE", bc.getBeanMethodVisibility());
		check("true", bc.isBeansRequireDefaultConstructor());
		check("true", bc.isBeansRequireSerializable());
		check("true", bc.isBeansRequireSettersForGetters());
		check("true", bc.isBeansRequireSomeProperties());
		check("foo", bc.getBeanTypePropertyName());
		check("org.apache.juneau.BeanConfigAnnotationTest$A1=foo", bc.getBpi());
		check("org.apache.juneau.BeanConfigAnnotationTest$A1=foo", bc.getBpx());
		check("org.apache.juneau.BeanConfigAnnotationTest$A1=foo", bc.getBpro());
		check("org.apache.juneau.BeanConfigAnnotationTest$A1=foo", bc.getBpwo());
		check("true", bc.isDebug());
		check("true", bc.isDetectRecursions());
		check("A1={foo:1}", bc.getExamples());
		check("true", bc.isFluentSetters());
		check("true", bc.isIgnoreInvocationExceptionsOnGetters());
		check("true", bc.isIgnoreInvocationExceptionsOnSetters());
		check("true", bc.isIgnorePropertiesWithoutSetters());
		check("true", bc.isIgnoreRecursions());
		check("true", bc.isIgnoreUnknownBeanProperties());
		check("true", bc.isIgnoreUnknownNullBeanProperties());
		check("org.apache.juneau.BeanConfigAnnotationTest$A1=A1", bc.getImplClasses());
		check("1", bc.getInitialDepth());
		check("en_US", bc.getLocale());
		check("1", bc.getMaxDepth());
		check("application/json", bc.getMediaType());
		check("A1,A3", bc.getNotBeanClasses());
		check("foo1,foo3", bc.getNotBeanPackagesNames());
		check("AB1<String,Integer>,AB3<String,Integer>", bc.getPojoSwaps());
		check("PropertyNamerULC", bc.getPropertyNamer());
		check("true", bc.isSortProperties());
		check("GMT", bc.getTimeZone());
		check("true", bc.isUseEnumNames());
		check("true", bc.isUseInterfaceProxies());
		check("true", bc.isUseJavaBeanIntrospector());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@BeanConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValues() throws Exception {
		AnnotationList al = b.getAnnotationList(null);
		JsonSerializer bc = JsonSerializer.create().applyAnnotations(al, sr).build();
		check("PUBLIC", bc.getBeanClassVisibility());
		check("PUBLIC", bc.getBeanConstructorVisibility());
		check("", bc.getBeanDictionaryClasses());
		check("PUBLIC", bc.getBeanFieldVisibility());
		check("", bc.getBeanFilters());
		check("false", bc.isBeanMapPutReturnsOldValue());
		check("PUBLIC", bc.getBeanMethodVisibility());
		check("false", bc.isBeansRequireDefaultConstructor());
		check("false", bc.isBeansRequireSerializable());
		check("false", bc.isBeansRequireSettersForGetters());
		check("true", bc.isBeansRequireSomeProperties());
		check("_type", bc.getBeanTypePropertyName());
		check("", bc.getBpi());
		check("", bc.getBpx());
		check("", bc.getBpro());
		check("", bc.getBpwo());
		check("false", bc.isDebug());
		check("false", bc.isDetectRecursions());
		check("", bc.getExamples());
		check("false", bc.isFluentSetters());
		check("false", bc.isIgnoreInvocationExceptionsOnGetters());
		check("false", bc.isIgnoreInvocationExceptionsOnSetters());
		check("true", bc.isIgnorePropertiesWithoutSetters());
		check("false", bc.isIgnoreRecursions());
		check("false", bc.isIgnoreUnknownBeanProperties());
		check("true", bc.isIgnoreUnknownNullBeanProperties());
		check("", bc.getImplClasses());
		check("0", bc.getInitialDepth());
		check(Locale.getDefault().toString(), bc.getLocale());
		check("100", bc.getMaxDepth());
		check(null, bc.getMediaType());
		check("java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net", bc.getNotBeanPackagesNames());
		check("", bc.getPojoSwaps());
		check("PropertyNamerDefault", bc.getPropertyNamer());
		check("false", bc.isSortProperties());
		check(null, bc.getTimeZone());
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
	public void noAnnotation() throws Exception {
		AnnotationList al = c.getAnnotationList(null);
		JsonSerializer bc = JsonSerializer.create().applyAnnotations(al, sr).build();
		check("PUBLIC", bc.getBeanClassVisibility());
		check("PUBLIC", bc.getBeanConstructorVisibility());
		check("", bc.getBeanDictionaryClasses());
		check("PUBLIC", bc.getBeanFieldVisibility());
		check("", bc.getBeanFilters());
		check("false", bc.isBeanMapPutReturnsOldValue());
		check("PUBLIC", bc.getBeanMethodVisibility());
		check("false", bc.isBeansRequireDefaultConstructor());
		check("false", bc.isBeansRequireSerializable());
		check("false", bc.isBeansRequireSettersForGetters());
		check("true", bc.isBeansRequireSomeProperties());
		check("_type", bc.getBeanTypePropertyName());
		check("", bc.getBpi());
		check("", bc.getBpx());
		check("", bc.getBpro());
		check("", bc.getBpwo());
		check("false", bc.isDebug());
		check("false", bc.isDetectRecursions());
		check("", bc.getExamples());
		check("false", bc.isFluentSetters());
		check("false", bc.isIgnoreInvocationExceptionsOnGetters());
		check("false", bc.isIgnoreInvocationExceptionsOnSetters());
		check("true", bc.isIgnorePropertiesWithoutSetters());
		check("false", bc.isIgnoreRecursions());
		check("false", bc.isIgnoreUnknownBeanProperties());
		check("true", bc.isIgnoreUnknownNullBeanProperties());
		check("", bc.getImplClasses());
		check("0", bc.getInitialDepth());
		check(Locale.getDefault().toString(), bc.getLocale());
		check("100", bc.getMaxDepth());
		check(null, bc.getMediaType());
		check("java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net", bc.getNotBeanPackagesNames());
		check("", bc.getPojoSwaps());
		check("PropertyNamerDefault", bc.getPropertyNamer());
		check("false", bc.isSortProperties());
		check(null, bc.getTimeZone());
		check("false", bc.isUseEnumNames());
		check("true", bc.isUseInterfaceProxies());
		check("false", bc.isUseJavaBeanIntrospector());
	}
}
