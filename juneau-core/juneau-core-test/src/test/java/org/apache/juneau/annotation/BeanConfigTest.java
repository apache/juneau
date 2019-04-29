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
import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.BeanTraverseContext.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @BeanConfig annotation.
 */
public class BeanConfigTest {

	private static void check(String expected, Object o) {
		if (o instanceof Set) {
			Set<String> s2 = new TreeSet<>();
			for (Object o2 : (Set<?>)o)
				s2.add(TO_STRING.apply(o2));
			String actual = s2
				.stream()
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else if (o instanceof List) {
			List<?> l = (List<?>)o;
			String actual = l
				.stream()
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else if (o instanceof Map) {
			Map<?,?> m = (Map<?,?>)o;
			String actual = m
				.entrySet()
				.stream()
				.map(TO_STRING)
				.collect(Collectors.joining(","));
			assertEquals(expected, actual);
		} else {
			assertEquals(expected, TO_STRING.apply(o));
		}
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@SuppressWarnings({ "rawtypes" })
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Map.Entry) {
				Map.Entry e = (Map.Entry)t;
				return apply(e.getKey()) + "=" + apply(e.getValue());
			}
			if (t instanceof A1)
				return "A1";
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			return t.toString();
		}
	};

	static StringResolver sr = new StringResolver() {
		@Override
		public String resolve(String input) {
			if (input != null && input.startsWith("$"))
				input = input.substring(1);
			return input;
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Bean(typeName="A1")
	public static class A1 {
		public int foo;
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
		beanClassVisibility="$PRIVATE",
		beanConstructorVisibility="$PRIVATE",
		beanDictionary={A1.class,A2.class},
		beanDictionary_replace={A1.class,A2.class,A3.class},
		beanDictionary_remove=A2.class,
		beanFieldVisibility="$PRIVATE",
		beanFilters={A1.class,A2.class},
		beanFilters_replace={A1.class,A2.class,A3.class},
		beanFilters_remove=A2.class,
		beanMapPutReturnsOldValue="$true",
		beanMethodVisibility="$PRIVATE",
		beansRequireDefaultConstructor="$true",
		beansRequireSerializable="$true",
		beansRequireSettersForGetters="$true",
		beansRequireSomeProperties="$true",
		beanTypePropertyName="$foo",
		debug="$true",
		detectRecursions="$true",
		examples=@CSEntry(key=A1.class,value="${}"),
		excludeProperties=@CSEntry(key=A1.class,value="$foo"),
		fluentSetters="$true",
		ignoreInvocationExceptionsOnGetters="$true",
		ignoreInvocationExceptionsOnSetters="$true",
		ignorePropertiesWithoutSetters="$true",
		ignoreRecursions="$true",
		ignoreUnknownBeanProperties="$true",
		ignoreUnknownNullBeanProperties="$true",
		implClasses=@CCEntry(key=A1.class,value=A1.class),
		includeProperties=@CSEntry(key=A1.class,value="$foo"),
		initialDepth="$1",
		locale="$en-US",
		maxDepth="$1",
		mediaType="$text/foo",
		notBeanClasses={A1.class,A2.class},
		notBeanClasses_replace={A1.class,A2.class,A3.class},
		notBeanClasses_remove=A2.class,
		notBeanPackages={"$foo1","$foo2"},
		notBeanPackages_replace={"$foo1","$foo2","$foo3"},
		notBeanPackages_remove={"$foo2"},
		pojoSwaps={AB1.class,AB2.class},
		pojoSwaps_replace={AB1.class,AB2.class,AB3.class},
		pojoSwaps_remove=AB2.class,
		propertyNamer=PropertyNamerULC.class,
		sortProperties="$true",
		timeZone="$z",
		useEnumNames="$true",
		useInterfaceProxies="$true",
		useJavaBeanIntrospector="$true"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basic() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		JsonSerializer bc = JsonSerializer.create().applyAnnotations(m, sr).build();
		check("PRIVATE", bc.getProperty(BEAN_beanClassVisibility));
		check("PRIVATE", bc.getProperty(BEAN_beanConstructorVisibility));
		check("A1,A3", bc.getProperty(BEAN_beanDictionary));
		check(null, bc.getProperty(BEAN_beanDictionary_add));
		check(null, bc.getProperty(BEAN_beanDictionary_remove));
		check("PRIVATE", bc.getProperty(BEAN_beanFieldVisibility));
		check("A1,A3", bc.getProperty(BEAN_beanFilters));
		check(null, bc.getProperty(BEAN_beanFilters_add));
		check(null, bc.getProperty(BEAN_beanFilters_remove));
		check("true", bc.getProperty(BEAN_beanMapPutReturnsOldValue));
		check("PRIVATE", bc.getProperty(BEAN_beanMethodVisibility));
		check("true", bc.getProperty(BEAN_beansRequireDefaultConstructor));
		check("true", bc.getProperty(BEAN_beansRequireSerializable));
		check("true", bc.getProperty(BEAN_beansRequireSettersForGetters));
		check("true", bc.getProperty(BEAN_beansRequireSomeProperties));
		check("foo", bc.getProperty(BEAN_beanTypePropertyName));
		check("true", bc.getProperty(BEAN_debug));
		check("true", bc.getProperty(BEANTRAVERSE_detectRecursions));
		check("org.apache.juneau.annotation.BeanConfigTest$A1=A1", bc.getProperty(BEAN_examples));
		check("org.apache.juneau.annotation.BeanConfigTest$A1=foo", bc.getProperty(BEAN_excludeProperties));
		check("true", bc.getProperty(BEAN_fluentSetters));
		check("true", bc.getProperty(BEAN_ignoreInvocationExceptionsOnGetters));
		check("true", bc.getProperty(BEAN_ignoreInvocationExceptionsOnSetters));
		check("true", bc.getProperty(BEAN_ignorePropertiesWithoutSetters));
		check("true", bc.getProperty(BEANTRAVERSE_ignoreRecursions));
		check("true", bc.getProperty(BEAN_ignoreUnknownBeanProperties));
		check("true", bc.getProperty(BEAN_ignoreUnknownNullBeanProperties));
		check("org.apache.juneau.annotation.BeanConfigTest$A1=A1", bc.getProperty(BEAN_implClasses));
		check("org.apache.juneau.annotation.BeanConfigTest$A1=foo", bc.getProperty(BEAN_includeProperties));
		check("1", bc.getProperty(BEANTRAVERSE_initialDepth));
		check("en-US", bc.getProperty(BEAN_locale));
		check("1", bc.getProperty(BEANTRAVERSE_maxDepth));
		check("text/foo", bc.getProperty(BEAN_mediaType));
		check("A1,A3", bc.getProperty(BEAN_notBeanClasses));
		check(null, bc.getProperty(BEAN_notBeanClasses_add));
		check(null, bc.getProperty(BEAN_notBeanClasses_remove));
		check("foo1,foo3", bc.getProperty(BEAN_notBeanPackages));
		check(null, bc.getProperty(BEAN_notBeanPackages_add));
		check(null, bc.getProperty(BEAN_notBeanPackages_remove));
		check("AB1,AB3", bc.getProperty(BEAN_pojoSwaps));
		check(null, bc.getProperty(BEAN_pojoSwaps_add));
		check(null, bc.getProperty(BEAN_pojoSwaps_remove));
		check("PropertyNamerULC", bc.getProperty(BEAN_propertyNamer));
		check("true", bc.getProperty(BEAN_sortProperties));
		check("GMT", bc.getProperty(BEAN_timeZone));
		check("true", bc.getProperty(BEAN_useEnumNames));
		check("true", bc.getProperty(BEAN_useInterfaceProxies));
		check("true", bc.getProperty(BEAN_useJavaBeanIntrospector));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@BeanConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValues() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		JsonSerializer bc = JsonSerializer.create().applyAnnotations(m, sr).build();
		check(null, bc.getProperty(BEAN_beanClassVisibility));
		check(null, bc.getProperty(BEAN_beanConstructorVisibility));
		check(null, bc.getProperty(BEAN_beanDictionary));
		check(null, bc.getProperty(BEAN_beanDictionary_add));
		check(null, bc.getProperty(BEAN_beanDictionary_remove));
		check(null, bc.getProperty(BEAN_beanFieldVisibility));
		check(null, bc.getProperty(BEAN_beanFilters));
		check(null, bc.getProperty(BEAN_beanFilters_add));
		check(null, bc.getProperty(BEAN_beanFilters_remove));
		check(null, bc.getProperty(BEAN_beanMapPutReturnsOldValue));
		check(null, bc.getProperty(BEAN_beanMethodVisibility));
		check(null, bc.getProperty(BEAN_beansRequireDefaultConstructor));
		check(null, bc.getProperty(BEAN_beansRequireSerializable));
		check(null, bc.getProperty(BEAN_beansRequireSettersForGetters));
		check(null, bc.getProperty(BEAN_beansRequireSomeProperties));
		check(null, bc.getProperty(BEAN_beanTypePropertyName));
		check(null, bc.getProperty(BEAN_debug));
		check(null, bc.getProperty(BEANTRAVERSE_detectRecursions));
		check(null, bc.getProperty(BEAN_examples));
		check(null, bc.getProperty(BEAN_excludeProperties));
		check(null, bc.getProperty(BEAN_fluentSetters));
		check(null, bc.getProperty(BEAN_ignoreInvocationExceptionsOnGetters));
		check(null, bc.getProperty(BEAN_ignoreInvocationExceptionsOnSetters));
		check(null, bc.getProperty(BEAN_ignorePropertiesWithoutSetters));
		check(null, bc.getProperty(BEANTRAVERSE_ignoreRecursions));
		check(null, bc.getProperty(BEAN_ignoreUnknownBeanProperties));
		check(null, bc.getProperty(BEAN_ignoreUnknownNullBeanProperties));
		check(null, bc.getProperty(BEAN_implClasses));
		check(null, bc.getProperty(BEAN_includeProperties));
		check(null, bc.getProperty(BEANTRAVERSE_initialDepth));
		check(null, bc.getProperty(BEAN_locale));
		check(null, bc.getProperty(BEANTRAVERSE_maxDepth));
		check(null, bc.getProperty(BEAN_mediaType));
		check(null, bc.getProperty(BEAN_notBeanClasses));
		check(null, bc.getProperty(BEAN_notBeanClasses_add));
		check(null, bc.getProperty(BEAN_notBeanClasses_remove));
		check(null, bc.getProperty(BEAN_notBeanPackages));
		check(null, bc.getProperty(BEAN_notBeanPackages_add));
		check(null, bc.getProperty(BEAN_notBeanPackages_remove));
		check(null, bc.getProperty(BEAN_pojoSwaps));
		check(null, bc.getProperty(BEAN_pojoSwaps_add));
		check(null, bc.getProperty(BEAN_pojoSwaps_remove));
		check(null, bc.getProperty(BEAN_propertyNamer));
		check(null, bc.getProperty(BEAN_sortProperties));
		check(null, bc.getProperty(BEAN_timeZone));
		check(null, bc.getProperty(BEAN_useEnumNames));
		check(null, bc.getProperty(BEAN_useInterfaceProxies));
		check(null, bc.getProperty(BEAN_useJavaBeanIntrospector));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotation() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		JsonSerializer bc = JsonSerializer.create().applyAnnotations(m, sr).build();
		check(null, bc.getProperty(BEAN_beanClassVisibility));
	}
}
