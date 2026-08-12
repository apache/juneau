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
package org.apache.juneau.rest.common.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HttpUtils}.
 */
class HttpUtils_Test extends TestBase {

	private static Method m(String name) {
		for (var meth : Fixtures.class.getMethods())
			if (meth.getName().equals(name))
				return meth;
		throw new AssertionError("Method not found: " + name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// detectHttpMethod(Method, boolean, String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_detectHttpMethod_detectDisabled_returnsDefault() {
		assertEquals("FOO", HttpUtils.detectHttpMethod(m("doGet"), false, "FOO"));
	}

	@Test void a02_detectHttpMethod_doPrefixed_matchesUcMethod() {
		assertEquals("GET", HttpUtils.detectHttpMethod(m("doGet"), true, null));
	}

	@Test void a03_detectHttpMethod_doPrefixed_noUcMatch_andNoLcMatch_returnsDefault() {
		// "dot" starts with "do" (len > 2) but "T" matches no UC_METHODS entry, and "dot" itself matches no
		// LC_METHODS prefix either, so it falls all the way through to the default.
		assertEquals("FOO", HttpUtils.detectHttpMethod(m("dot"), true, "FOO"));
	}

	@Test void a04_detectHttpMethod_doPrefixed_noExactUcMatch_returnsDefault() {
		// "doGetSomething" starts with "do", but the whole remainder "GETSOMETHING" doesn't exactly equal any
		// UC_METHODS entry (only "GET" does), and the full name doesn't start with any LC_METHODS prefix either
		// (it starts with "do", not "get"), so it falls all the way through to the default.
		assertNull(HttpUtils.detectHttpMethod(m("doGetSomething"), true, null));
	}

	@Test void a05_detectHttpMethod_lcPrefixed_exactMatch() {
		assertEquals("GET", HttpUtils.detectHttpMethod(m("get"), true, null));
	}

	@Test void a06_detectHttpMethod_lcPrefixed_camelCaseMatch() {
		assertEquals("POST", HttpUtils.detectHttpMethod(m("postWidget"), true, null));
	}

	@Test void a07_detectHttpMethod_lcPrefixed_notFollowedByUppercase_noMatch() {
		// "getx" starts with "get" but the next char is lowercase, so it's not a recognized method-name boundary.
		assertEquals("FOO", HttpUtils.detectHttpMethod(m("getx"), true, "FOO"));
	}

	@Test void a08_detectHttpMethod_noPrefixMatch_returnsDefault() {
		assertNull(HttpUtils.detectHttpMethod(m("widget"), true, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// detectHttpPath(Method, String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_detectHttpPath_methodNull_doPrefixed_matchesUcMethod() {
		assertEquals("/", HttpUtils.detectHttpPath(m("doGet"), null));
	}

	@Test void b02_detectHttpPath_methodNull_doPrefixed_noUcMatch_andNoLcMatch_fallsBack() {
		assertEquals("/dot", HttpUtils.detectHttpPath(m("dot"), null));
	}

	@Test void b03_detectHttpPath_methodNull_lcPrefixed_decapitalizesRemainder() {
		assertEquals("/widget", HttpUtils.detectHttpPath(m("getWidget"), null));
	}

	@Test void b04_detectHttpPath_methodNull_noMatch_fallsBackToSlashPlusName() {
		assertEquals("/widget", HttpUtils.detectHttpPath(m("widget"), null));
	}

	@Test void b04b_detectHttpPath_methodNull_lcPrefixed_exactMatch_returnsRoot() {
		assertEquals("/", HttpUtils.detectHttpPath(m("get"), null));
	}

	@Test void b04c_detectHttpPath_methodNull_lcPrefixed_notFollowedByUppercase_fallsBack() {
		assertEquals("/getx", HttpUtils.detectHttpPath(m("getx"), null));
	}

	@Test void b05_detectHttpPath_methodGiven_nameEqualsIgnoreCase_returnsRoot() {
		assertEquals("/", HttpUtils.detectHttpPath(m("get"), "GET"));
	}

	@Test void b07_detectHttpPath_methodGiven_startsWithMethod_decapitalizesRemainder() {
		assertEquals("/widget", HttpUtils.detectHttpPath(m("getWidget"), "get"));
	}

	@Test void b08_detectHttpPath_methodGiven_startsWithMethod_notFollowedByUppercase_fallsBack() {
		// "getx" starts with "get" but the next char is lowercase, so it's not a recognized boundary; falls back
		// to '/' + the full method name.
		assertEquals("/getx", HttpUtils.detectHttpPath(m("getx"), "get"));
	}

	@Test void b09_detectHttpPath_methodGiven_noMatch_fallsBackToSlashPlusName() {
		assertEquals("/widget", HttpUtils.detectHttpPath(m("widget"), "post"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test fixtures
	//------------------------------------------------------------------------------------------------------------------

	public interface Fixtures {
		void doGet();
		void dot();
		void doGetSomething();
		void get();
		void postWidget();
		void getx();
		void widget();
		void getWidget();
	}
}
