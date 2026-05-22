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
package org.apache.juneau.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.bean.*;

/**
 * Tests the bean-modeling attributes of the @BeanConfig annotation.
 *
 * <p>
 * These tests were extracted from {@code MarshalledConfigAnnotation_Test} during Phase 3 of the
 * bean-layer split, when bean-modeling attributes moved off {@code @MarshalledConfig} onto
 * {@code @BeanConfig} in {@code juneau-commons}.
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class BeanConfigAnnotation_Test extends TestBase {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof List)
				return ((List<?>)t)
					.stream()
					.map(TO_STRING)
					.collect(Collectors.joining(","));
			if (t instanceof Set)
				return ((Set<?>)t)
					.stream()
					.map(TO_STRING)
					.collect(Collectors.joining(","));
			if (isArray(t))
				return apply(toList(t, Object.class));
			if (t instanceof JsonMap)
				return ((JsonMap)t).toString();
			if (t instanceof Map)
				return ((Map<?,?>)t)
					.entrySet()
					.stream()
					.map(TO_STRING)
					.collect(Collectors.joining(","));
			if (t instanceof Map.Entry e) {
				return apply(e.getKey()) + "=" + apply(e.getValue());
			}
			if (t instanceof MarshalledFilter)
				return ((MarshalledFilter)t).getBeanClass().getNameSimple();
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			if (t instanceof ClassInfo)
				return ((ClassInfo)t).getNameSimple();
			if (t instanceof PropertyNamer)
				return t.getClass().getSimpleName();
			if (t instanceof TimeZone)
				return ((TimeZone)t).getID();
			return t.toString();
		}
	};

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Marshalled(typeName="A1")
	public static class A1 {
		public int foo;
		@Override
		public String toString() {return Json5.of(this);}
	}
	@Marshalled(typeName="A2")
	public static class A2 {
		public int foo;
	}
	@Marshalled(typeName="A3")
	public static class A3 {
		public int foo;
	}

	@BeanConfig(
		beanClassVisibility="$X{PRIVATE}",
		beanConstructorVisibility="$X{PRIVATE}",
		beanFieldVisibility="$X{PRIVATE}",
		beanMapPutReturnsOldValue="$X{true}",
		beanMethodVisibility="$X{PRIVATE}",
		beansRequireDefaultConstructor="$X{true}",
		beansRequireSerializable="$X{true}",
		beansRequireSettersForGetters="$X{true}",
		disableBeansRequireSomeProperties="$X{true}",
		disableIgnoreUnknownNullBeanProperties="$X{true}",
		disableIgnoreMissingSetters="$X{true}",
		disableInterfaceProxies="$X{true}",
		findFluentSetters="$X{true}",
		ignoreInvocationExceptionsOnGetters="$X{true}",
		ignoreInvocationExceptionsOnSetters="$X{true}",
		ignoreUnknownBeanProperties="$X{true}",
		notBeanClasses={A1.class,A2.class},
		notBeanClasses_replace={A1.class,A2.class,A3.class},
		notBeanPackages={"$X{foo1}","$X{foo2}"},
		notBeanPackages_replace={"$X{foo1}","$X{foo2}","$X{foo3}"},
		propertyNamer=PropertyNamerULC.class,
		unsortedProperties="$X{false}",
		useJavaBeanIntrospector="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void a01_basic() {
		var al = AnnotationWorkList.of(sr, rstream(a.getAnnotations()));
		var bs = JsonSerializer.create().apply(al).build().getSession();

		check("PRIVATE", bs.getBeanClassVisibility());
		check("PRIVATE", bs.getBeanConstructorVisibility());
		check("PRIVATE", bs.getBeanFieldVisibility());
		check("true", bs.isBeanMapPutReturnsOldValue());
		check("PRIVATE", bs.getBeanMethodVisibility());
		check("true", bs.isBeansRequireDefaultConstructor());
		check("true", bs.isBeansRequireSerializable());
		check("true", bs.isBeansRequireSettersForGetters());
		check("false", bs.isBeansRequireSomeProperties());
		check("true", bs.isFindFluentSetters());
		check("true", bs.isIgnoreInvocationExceptionsOnGetters());
		check("true", bs.isIgnoreInvocationExceptionsOnSetters());
		check("false", bs.isIgnoreMissingSetters());
		check("true", bs.isIgnoreUnknownBeanProperties());
		check("false", bs.isIgnoreUnknownNullBeanProperties());
		check("A1,A2,A3,Map,Collection,Reader,Writer,InputStream,OutputStream,Throwable", bs.getNotBeanClasses());
		check("foo1,foo2,foo3,java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net", bs.getNotBeanPackagesNames());
		check("PropertyNamerULC", bs.getPropertyNamer());
		check("false", bs.isUnsortedProperties());
		check("false", bs.isUseInterfaceProxies());
		check("true", bs.isUseJavaBeanIntrospector());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@BeanConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void b01_noValues() {
		var al = AnnotationWorkList.of(sr, rstream(b.getAnnotations()));
		var js = JsonSerializer.create().apply(al).build();
		var bc = js.getMarshallingContext();
		check("PUBLIC", bc.getBeanClassVisibility());
		check("PUBLIC", bc.getBeanConstructorVisibility());
		check("PUBLIC", bc.getBeanFieldVisibility());
		check("false", bc.isBeanMapPutReturnsOldValue());
		check("PUBLIC", bc.getBeanMethodVisibility());
		check("false", bc.isBeansRequireDefaultConstructor());
		check("false", bc.isBeansRequireSerializable());
		check("false", bc.isBeansRequireSettersForGetters());
		check("true", bc.isBeansRequireSomeProperties());
		check("false", bc.isFindFluentSetters());
		check("false", bc.isIgnoreInvocationExceptionsOnGetters());
		check("false", bc.isIgnoreInvocationExceptionsOnSetters());
		check("true", bc.isIgnoreMissingSetters());
		check("false", bc.isIgnoreUnknownBeanProperties());
		check("true", bc.isIgnoreUnknownNullBeanProperties());
		check("java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net", bc.getNotBeanPackagesNames());
		check("BasicPropertyNamer", bc.getPropertyNamer());
		check("false", bc.isUnsortedProperties());
		check("true", bc.isUseInterfaceProxies());
		check("false", bc.isUseJavaBeanIntrospector());
	}
}
