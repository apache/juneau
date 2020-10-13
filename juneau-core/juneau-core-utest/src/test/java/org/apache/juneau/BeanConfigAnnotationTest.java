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
import static org.junit.runners.MethodSorters.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
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
		typePropertyName="$X{foo}",
		bpi="A1:$X{foo}",
		bpx="A1:$X{bar}",
		bpro="A1:$X{baz}",
		bpwo="A1:$X{qux}",
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
		swaps={AB1.class,AB2.class},
		swaps_replace={AB1.class,AB2.class,AB3.class},
		swaps_remove=AB2.class,
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
	public void a01_basic() throws Exception {
		AnnotationList al = a.getAnnotationList();
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
		check("AB1<String,Integer>,AB3<String,Integer>", bc.getSwaps());
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
	public void b01_noValues() throws Exception {
		AnnotationList al = b.getAnnotationList();
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
		check(Locale.getDefault().toString(), bc.getDefaultLocale());
		check("100", bc.getMaxDepth());
		check(null, bc.getDefaultMediaType());
		check("java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net", bc.getNotBeanPackagesNames());
		check("", bc.getSwaps());
		check("PropertyNamerDefault", bc.getPropertyNamer());
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
		check(Locale.getDefault().toString(), bc.getDefaultLocale());
		check("100", bc.getMaxDepth());
		check(null, bc.getDefaultMediaType());
		check("java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net", bc.getNotBeanPackagesNames());
		check("", bc.getSwaps());
		check("PropertyNamerDefault", bc.getPropertyNamer());
		check("false", bc.isSortProperties());
		check(null, bc.getDefaultTimeZone());
		check("false", bc.isUseEnumNames());
		check("true", bc.isUseInterfaceProxies());
		check("false", bc.isUseJavaBeanIntrospector());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpi/bpx) should override @Bean(bpi/bpx)
	//-----------------------------------------------------------------------------------------------------------------

	@BeanConfig(bpi="D:b,c,d", bpx="D:c")
	@Bean(bpi="a,b,c", bpx="b")
	static class D {
		public int a, b, c, d;

		public static D create() {
			D d = new D();
			d.a = 1;
			d.b = 2;
			d.c = 3;
			d.d = 4;
			return d;
		}
	}

	private static ClassInfo d = ClassInfo.of(D.class);

	@Test
	public void d01_beanBpiBpxCombined_noBeanConfig() throws Exception {
		String json = SimpleJson.DEFAULT.toString(D.create());
		assertEquals("{a:1,c:3}", json);
		D d = SimpleJson.DEFAULT.read(json, D.class);
		json = SimpleJson.DEFAULT.toString(d);
		assertEquals("{a:1,c:3}", json);
	}

	@Test
	public void d02_beanBpiBpxCombined_beanConfigOverride() throws Exception {
		AnnotationList al = d.getAnnotationList();
		JsonSerializer js = JsonSerializer.create().simple().applyAnnotations(al, sr).build();
		JsonParser jp = JsonParser.create().applyAnnotations(al, sr).build();

		String json = js.serialize(D.create());
		assertEquals("{b:2,d:4}", json);
		D d = jp.parse(json, D.class);
		json = js.serialize(d);
		assertEquals("{b:2,d:4}", json);
	}

	@Test
	public void d03_beanBpiBpxCombined_beanContextBuilderOverride() throws Exception {
		Bean ba = new BeanAnnotation("D").bpi("b,c,d").bpx("c");
		JsonSerializer js = JsonSerializer.create().simple().annotations(ba).build();
		JsonParser jp = JsonParser.create().annotations(ba).build();

		String json = js.serialize(D.create());
		assertEquals("{b:2,d:4}", json);
		D d = jp.parse(json, D.class);
		json = js.serialize(d);
		assertEquals("{b:2,d:4}", json);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpi/bpx) should override @Bean(bpi/bpx)
	//-----------------------------------------------------------------------------------------------------------------

	@Bean(bpi="a,b,c")
	static class E1 {
		public int a, b, c, d;
	}

	@BeanConfig(bpi="E:b,c,d", bpx="E:c")
	@Bean(bpx="b")
	static class E extends E1 {

		public static E create() {
			E e = new E();
			e.a = 1;
			e.b = 2;
			e.c = 3;
			e.d = 4;
			return e;
		}
	}

	private static ClassInfo e = ClassInfo.of(E.class);

	@Test
	public void e01_beanBpiBpxCombined_multipleBeanAnnotations_noBeanConfig() throws Exception {
		String json = SimpleJson.DEFAULT.toString(E.create());
		assertEquals("{a:1,c:3}", json);
		E e = SimpleJson.DEFAULT.read(json, E.class);
		json = SimpleJson.DEFAULT.toString(e);
		assertEquals("{a:1,c:3}", json);
	}

	@Test
	public void e02_beanBpiBpxCombined_multipleBeanAnnotations_beanConfigOverride() throws Exception {
		AnnotationList al = e.getAnnotationList();
		JsonSerializer js = JsonSerializer.create().simple().applyAnnotations(al, sr).build();
		JsonParser jp = JsonParser.create().applyAnnotations(al, sr).build();

		String json = js.serialize(E.create());
		assertEquals("{b:2,d:4}", json);
		E e = jp.parse(json, E.class);
		json = js.serialize(e);
		assertEquals("{b:2,d:4}", json);
	}

	@Test
	public void e03_beanBpiBpxCombined_multipleBeanAnnotations_beanContextBuilderOverride() throws Exception {
		Bean ba = new BeanAnnotation("E").bpi("b,c,d").bpx("c");
		JsonSerializer js = JsonSerializer.create().simple().annotations(ba).build();
		JsonParser jp = JsonParser.create().annotations(ba).build();

		String json = js.serialize(E.create());
		assertEquals("{b:2,d:4}", json);
		E e = jp.parse(json, E.class);
		json = js.serialize(e);
		assertEquals("{b:2,d:4}", json);
	}
}
