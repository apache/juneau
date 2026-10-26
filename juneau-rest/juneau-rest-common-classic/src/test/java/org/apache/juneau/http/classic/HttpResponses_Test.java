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
package org.apache.juneau.http.classic;

import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.classic.response.*;
import org.junit.jupiter.api.*;

/**
 * Tests: {@link HttpResponses}
 */
class HttpResponses_Test extends TestBase {

	private static boolean isUnmodifiable(Object o) {
		return o instanceof BasicHttpResponse<?> r ? r.isUnmodifiable() : ((BasicHttpException)o).isUnmodifiable();
	}

	private static List<Field> constants() {
		return Arrays.stream(HttpResponses.class.getFields()).filter(f -> Modifier.isStatic(f.getModifiers())).toList();
	}

	@Test void a01_constants_areUnmodifiableSingletonsOfTheirType() throws Exception {
		var fields = constants();
		assertSize(() -> "Number of HttpResponses constants; update when one is added or removed.", 55, fields);
		for (var f : fields) {
			var v = f.get(null);
			assertInstanceOf(HttpResponse.class, v, f::getName);
			assertTrue(isUnmodifiable(v), f::getName);
			assertSame(f.getType().getField("INSTANCE").get(null), v, f::getName);
		}
	}

	@Test void a02_factories_matchConstantsAndReturnFreshModifiableInstances() throws Exception {
		// Factories are matched to constants by return type (names differ, e.g. continue_() and a misspelled constant).
		var byType = new HashMap<Class<?>,Method>();
		for (var m : HttpResponses.class.getMethods())
			if (Modifier.isStatic(m.getModifiers()))
				byType.put(m.getReturnType(), m);
		for (var f : constants()) {
			var m = byType.remove(f.getType());
			assertNotNull(m, () -> "No factory for " + f.getName());
			var args = m.getParameterCount() == 0 ? new Object[0] : new Object[]{"http://localhost/target"};
			var a = (HttpResponse)m.invoke(null, args);
			var b = (HttpResponse)m.invoke(null, args);
			assertEquals(f.getType(), a.getClass(), f::getName);
			assertNotSame(a, b, f::getName);
			assertFalse(isUnmodifiable(a), f::getName);
			assertEquals(((HttpResponse)f.get(null)).getStatusLine().getStatusCode(), a.getStatusLine().getStatusCode(), f::getName);
			if (args.length > 0)
				assertEquals("http://localhost/target", a.getFirstHeader("Location").getValue(), f::getName);
		}
		// The only factory without a constant is the generic exception.
		assertEquals(Set.of(BasicHttpException.class), byType.keySet());
		assertEquals(0, ((HttpResponse)byType.get(BasicHttpException.class).invoke(null)).getStatusLine().getStatusCode());
	}
}
