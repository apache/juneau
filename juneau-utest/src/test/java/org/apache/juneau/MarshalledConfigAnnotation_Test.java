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
package org.apache.juneau;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.swap.*;
import org.junit.jupiter.api.*;

/**
 * Tests the marshalling-only attributes of the @MarshalledConfig annotation.
 *
 * <p>
 * The bean-modeling attributes were moved to {@code @BeanConfig} in Phase 3 of the bean-layer split.
 * See {@code BeanConfigAnnotation_Test} for tests on those attributes.
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class MarshalledConfigAnnotation_Test extends TestBase {

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
	public static class AB1 extends ObjectSwap<String,Integer> {
	}
	public static class AB2 extends ObjectSwap<String,Integer> {
	}
	public static class AB3 extends ObjectSwap<String,Integer> {
	}

	@MarshalledConfig(
		binaryFormat=BinaryFormat.HEX,
		calendarFormat=CalendarFormat.ISO_INSTANT,
		dateFormat=DateFormat.ISO_INSTANT,
		dictionary={A1.class,A2.class},
		dictionary_replace={A1.class,A2.class,A3.class},
		durationFormat=DurationFormat.MILLIS,
		enumFormat=EnumFormat.NAME,
		typePropertyName="$X{foo}",
		debug="$X{true}",
		localeFormat=LocaleFormat.UNDERSCORE,
		locale="$X{en-US}",
		mediaType="$X{text/foo}",
		periodFormat=PeriodFormat.DAYS,
		swaps={AB1.class,AB2.class},
		swaps_replace={AB1.class,AB2.class,AB3.class},
		temporalFormat=TemporalFormat.ISO_INSTANT,
		timeZoneFormat=TimeZoneFormat.OFFSET,
		timeZone="$X{z}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test void a01_basic() {
		var al = AnnotationWorkList.of(sr, rstream(a.getAnnotations()));
		var js = JsonSerializer.create().apply(al).build();
		var bs = js.getSession();
		var bc = js.getMarshallingContext();

		check("A1,A2,A3", bs.getBeanDictionary());
		check("foo", bs.getBeanTypePropertyName());
		check("true", bs.isDebug());
		check("en_US", bs.getLocale());
		check("text/foo", bs.getMediaType());
		check("AB1<String,Integer>,AB2<String,Integer>,AB3<String,Integer>", bs.getSwaps());
		check("GMT", bs.getTimeZone());
		check("5000", bs.serialize(Duration.ofSeconds(5)));
		check("\"3\"", bs.serialize(Period.ofDays(3)));
		check("HEX", bc.getBinaryFormat());
		check("ISO_INSTANT", bc.getCalendarFormat());
		check("ISO_INSTANT", bc.getDateFormat());
		check("NAME", bc.getEnumFormat());
		check("ISO_INSTANT", bc.getTemporalFormat());
		check("OFFSET", bc.getTimeZoneFormat());
		check("UNDERSCORE", bc.getLocaleFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@MarshalledConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test void b01_noValues() {
		var al = AnnotationWorkList.of(sr, rstream(b.getAnnotations()));
		var js = JsonSerializer.create().apply(al).build();
		var bc = js.getMarshallingContext();
		check("", bc.getBeanDictionary());
		check("_type", bc.getBeanTypePropertyName());
		check("false", js.isDebug());
		check("false", js.isDetectRecursions());
		check("false", js.isIgnoreRecursions());
		check("0", js.getInitialDepth());
		check(Locale.getDefault().toString(), bc.getDefaultLocale());
		check("100", js.getMaxDepth());
		check(null, bc.getDefaultMediaType());
		check("", bc.getSwaps());
		check(null, bc.getDefaultTimeZone());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test void c01_noAnnotation() {
		var al = AnnotationWorkList.of(sr, rstream(c.getAnnotations()));
		var js = JsonSerializer.create().apply(al).build();
		var bc = js.getMarshallingContext();
		check("", bc.getBeanDictionary());
		check("_type", bc.getBeanTypePropertyName());
		check("false", js.isDebug());
		check("false", js.isDetectRecursions());
		check("false", js.isIgnoreRecursions());
		check("0", js.getInitialDepth());
		check(Locale.getDefault().toString(), bc.getDefaultLocale());
		check("100", js.getMaxDepth());
		check(null, bc.getDefaultMediaType());
		check("", bc.getSwaps());
		check(null, bc.getDefaultTimeZone());
	}
}
