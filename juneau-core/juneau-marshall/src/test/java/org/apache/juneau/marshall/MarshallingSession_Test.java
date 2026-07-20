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
package org.apache.juneau.marshall;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link MarshallingSession#toBeanMap(Object, PropertyNamer)}.
 */
class MarshallingSession_Test extends TestBase {

	MarshallingContext bc = MarshallingContext.DEFAULT;
	MarshallingSession bs = MarshallingContext.DEFAULT_SESSION;

	public static class A {
		public String fooBar = "x";
		public String qux = "y";
	}

	//====================================================================================================
	// toBeanMap(Object, PropertyNamer) honors the supplied namer for property names.
	//====================================================================================================

	@Test void a01_dashedLowerCaseNamer() {
		var m = bs.toBeanMap(new A(), PropertyNamerDLC.INSTANCE);
		assertList(m.keySet(), "foo-bar", "qux");
		assertEquals("x", m.get("foo-bar"));
		assertEquals("y", m.get("qux"));
	}

	@Test void a02_dashedUpperCaseStartNamer() {
		var m = bs.toBeanMap(new A(), PropertyNamerDUCS.INSTANCE);
		assertList(m.keySet(), "Foo-Bar", "Qux");
		assertEquals("x", m.get("Foo-Bar"));
		assertEquals("y", m.get("Qux"));
	}

	public static class A03_UpperNamer implements PropertyNamer {
		@Override /* Overridden from PropertyNamer */
		public String getPropertyName(String name) {
			return name == null ? null : name.toUpperCase();
		}
	}

	@Test void a03_customAllCapsNamer() {
		var m = bs.toBeanMap(new A(), new A03_UpperNamer());
		assertList(m.keySet(), "FOOBAR", "QUX");
		assertEquals("x", m.get("FOOBAR"));
	}

	//====================================================================================================
	// The null / default-namer path is unchanged (uses the shared, Class-keyed BeanMeta cache).
	//====================================================================================================

	@Test void b01_nullNamerMatchesDefault() {
		var m = bs.toBeanMap(new A(), (PropertyNamer)null);
		var expected = bs.toBeanMap(new A());
		assertList(m.keySet(), "fooBar", "qux");
		assertEquals(expected.keySet(), m.keySet());
	}

	@Test void b02_defaultNamerUsesCachedBeanMeta() {
		// Passing the session's own default namer must reuse the cached BeanMeta (same instance as no-namer path).
		var m = bs.toBeanMap(new A(), bs.getPropertyNamer());
		var cached = bc.getClassMeta(A.class).getBeanMeta();
		assertSame(cached, m.getMeta());
		assertList(m.keySet(), "fooBar", "qux");
	}

	@Test void b03_overrideNamerDoesNotPolluteCache() {
		// Building an alternate-namer BeanMap must not change the shared cached BeanMeta's property names.
		var before = bc.getClassMeta(A.class).getBeanMeta();
		bs.toBeanMap(new A(), PropertyNamerDLC.INSTANCE);
		var after = bc.getClassMeta(A.class).getBeanMeta();
		assertSame(before, after);
		assertList(after.getProperties().keySet(), "fooBar", "qux");
	}
}
