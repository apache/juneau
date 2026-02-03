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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.TimeZone;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.settings.Settings;
import org.apache.juneau.json.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

class BeanContext_Test extends TestBase {

	BeanContext bc = BeanContext.DEFAULT;
	BeanSession bs = BeanContext.DEFAULT_SESSION;

	@AfterEach
	void tearDown() {
		Settings.get().clearLocal();
	}

	public interface A1 {
		int getF1();
		void setF1(int f1);
	}

	@Test void a01_normalCachableBean() throws ExecutableException {
		var cm1 = bc.getClassMeta(A1.class);
		var cm2 = bc.getClassMeta(A1.class);
		assertSame(cm1, cm2);
	}

	interface A2 {
		void foo(int x);
	}

	@Test void a02_lambdaExpressionsNotCached() throws ExecutableException {
		var bc2 = BeanContext.DEFAULT;
		var fi = (A2)System.out::println;
		var cm1 = bc2.getClassMeta(fi.getClass());
		var cm2 = bc2.getClassMeta(fi.getClass());
		assertNotSame(cm1, cm2);
	}

	@Test void a03_proxiesNotCached() throws ExecutableException {
		var a1 = bs.getBeanMeta(A1.class).newBean(null);
		var cm1 = bc.getClassMeta(a1.getClass());
		var cm2 = bc.getClassMeta(a1.getClass());
		assertNotSame(cm1, cm2);
	}

	@Test void b01_ignoreUnknownEnumValues() {
		var p1 = JsonParser.DEFAULT;
		assertThrowsWithMessage(Exception.class, "Could not resolve enum value 'UNKNOWN' on class 'org.apache.juneau.testutils.pojos.TestEnum'", () -> p1.parse("'UNKNOWN'", TestEnum.class));

		var p2 = JsonParser.create().ignoreUnknownEnumValues().build();
		assertNull(p2.parse("'UNKNOWN'", TestEnum.class));
	}

	//====================================================================================================
	// BeanContext.Builder locale, mediaType, and timeZone from Settings
	//====================================================================================================

	@Test void c01_locale_fromSettings() {
		Settings.get().setLocal("BeanContext.locale", "fr-CA");
		try {
			var bc = BeanContext.create().build();
			assertEquals(Locale.forLanguageTag("fr-CA"), bc.getLocale());
		} finally {
			Settings.get().clearLocal();
		}
	}

	@Test void c02_locale_defaultWhenNotSet() {
		var bc = BeanContext.create().build();
		assertEquals(Locale.getDefault(), bc.getLocale());
	}

	@Test void c03_mediaType_fromSettings() {
		Settings.get().setLocal("BeanContext.mediaType", "application/json");
		try {
			var bc = BeanContext.create().build();
			assertEquals(MediaType.of("application/json"), bc.getMediaType());
		} finally {
			Settings.get().clearLocal();
		}
	}

	@Test void c04_mediaType_nullWhenNotSet() {
		var bc = BeanContext.create().build();
		assertNull(bc.getMediaType());
	}

	@Test void c05_timeZone_fromSettings() {
		Settings.get().setLocal("BeanContext.timeZone", "America/New_York");
		try {
			var bc = BeanContext.create().build();
			assertEquals(TimeZone.getTimeZone("America/New_York"), bc.getTimeZone());
		} finally {
			Settings.get().clearLocal();
		}
	}

	@Test void c06_timeZone_nullWhenNotSet() {
		var bc = BeanContext.create().build();
		assertNull(bc.getTimeZone());
	}

	//====================================================================================================
	// BeanContext.copy() - Copy constructor coverage
	//====================================================================================================

	@Test void d01_copy() {
		// Create a BeanContext with some properties set to exercise the copy constructor
		var original = BeanContext.create()
			.sortProperties()
			.locale(Locale.CANADA)
			.mediaType(MediaType.JSON)
			.timeZone(TimeZone.getTimeZone("America/New_York"))
			.build();

		// Call copy() which uses the copy constructor Builder(BeanContext copyFrom)
		// This exercises lines 273-277 which convert boolean fields from BeanContext to Builder disable flags
		var builder = original.copy();

		// Build a new context from the copied builder
		var copied = builder.build();

		// Verify the copied context has the same values
		assertEquals(original.getBeanClassVisibility(), copied.getBeanClassVisibility());
		assertEquals(original.getBeanConstructorVisibility(), copied.getBeanConstructorVisibility());
		assertEquals(original.getBeanFieldVisibility(), copied.getBeanFieldVisibility());
		assertEquals(original.getBeanMethodVisibility(), copied.getBeanMethodVisibility());
		assertEquals(original.getBeanDictionary(), copied.getBeanDictionary());
		assertEquals(original.getLocale(), copied.getLocale());
		assertEquals(original.getMediaType(), copied.getMediaType());
		assertEquals(original.getTimeZone(), copied.getTimeZone());
	}

	//====================================================================================================
	// BeanContext.Builder.impl() - Line 2372 coverage
	//====================================================================================================

	@Test
	void e01_impl() {
		var impl = BeanContext.create()
			.sortProperties()
			.locale(Locale.CANADA)
			.build();

		var builder = BeanContext.create()
			.impl(impl);

		// Build should return the pre-instantiated context
		var result = builder.build();
		assertSame(impl, result);
	}
}