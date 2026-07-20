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
package org.apache.juneau.rest.server.arg;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Validates {@link CookieList}.
 *
 * <p>
 * Regression: the constructor did {@code super(l(values))}, and {@code l(null)} returns {@code null}, so
 * {@code new CookieList(null)} threw a {@link NullPointerException}.  {@code HttpServletRequest.getCookies()}
 * returns {@code null} for a cookie-less request (servlet spec), so any {@code @RestOp} method declaring a
 * {@code CookieList} parameter NPE'd on a request with no cookies.
 */
class CookieList_Test extends TestBase {

	@Test void a01_nullCookiesYieldsEmptyList() {
		assertTrue(CookieList.of(null).isEmpty());
		assertTrue(new CookieList(null).isEmpty());
	}

	@Test void a02_populatedCookiesPreserved() {
		var l = CookieList.of(new Cookie[]{new Cookie("a", "1"), new Cookie("b", "2")});
		assertEquals(2, l.size());
		assertEquals("a", l.get(0).getName());
		assertEquals("b", l.get(1).getName());
	}
}
