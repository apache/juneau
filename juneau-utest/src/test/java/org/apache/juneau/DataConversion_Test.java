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
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

@SuppressWarnings("rawtypes")
class DataConversion_Test extends TestBase {

	@BeforeEach
	void beforeTest() {
		setLocale(Locale.US);
	}

	@AfterEach
	void afterTest() {
		unsetLocale();
	}

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() throws Exception {
		var m = new JsonMap();

		// *** Number ***
		m.put("x", 123);
		assertEquals(123, (int)m.getInt("x"));
		assertEquals(123, (long)m.getLong("x"));

		// *** Boolean ***
		m.put("x", true);
		assertEquals(true, (boolean)m.getBoolean("x"));

		// *** Null ***
		m.put("x", null);
		assertNull(m.getString("x"));
		assertNull(m.getInt("x"));
		assertNull(m.getLong("x"));
		assertNull(m.getBoolean("x"));
		assertNull(m.getMap("x"));
		assertNull(m.getList("x"));

		// *** Map ***
		m.put("x", new HashMap());
		assertEquals("{}", m.getString("x"));

		// *** JsonMap ***
		m.put("x", JsonMap.ofJson("{foo:123}"));
		assertEquals("{foo:123}", m.getString("x"));

		// *** Collection ***
		var s = new HashSet<Integer>();
		s.add(123);
		m.put("x", s);
		assertEquals("[123]", m.getString("x"));

		// *** JsonList ***
		m.put("x", JsonList.ofJson("[123]"));
		assertEquals("[123]", m.getString("x"));
		assertEquals(1, m.getList("x").size());

		// *** Array ***
		m.put("x", a(123));
		assertEquals("[123]", m.getString("x"));
		assertEquals(1, m.getList("x").size());

		// *** Enum ***
		m.put("x", TestEnum.ENUM2);
		assertEquals("ENUM2", m.getString("x"));
		assertFalse(m.getBoolean("x"));
		assertThrows(InvalidDataConversionException.class, ()->m.getMap("x"));

		// *** Not a bean ***
		m.put("x", new NotABean("foo"));
		assertEquals("foo", m.getString("x"));
		assertThrows(InvalidDataConversionException.class, ()->m.getInt("x"));
		assertThrows(InvalidDataConversionException.class, ()->m.getLong("x"));
		assertFalse(m.getBoolean("x"));
		assertThrows(InvalidDataConversionException.class, ()->m.getMap("x"));
	}

	public enum TestEnum {
		ENUM0, ENUM1, ENUM2
	}

	public class NotABean {
		private String arg;

		public NotABean(String arg) {
			this.arg = arg;
		}

		@Override /* Overridden from Object */
		public String toString() {
			return arg;
		}
	}

	//====================================================================================================
	// Data conversions with swaps.
	//====================================================================================================
	@Test void a02_objectSwaps() throws Exception {
		var s = "2001-12-21T12:34:56Z";
		var bc = BeanContext.DEFAULT;
		var c = bc.convertToType(s, GregorianCalendar.class);
		assertEquals(2001, c.get(Calendar.YEAR));
		var c2 = bc.convertToType(s, Calendar.class);
		assertEquals(2001, c2.get(Calendar.YEAR));
		s = bc.convertToType(c2, String.class);
		assertEquals("2001-12-21T12:34:56Z", s);
	}
}