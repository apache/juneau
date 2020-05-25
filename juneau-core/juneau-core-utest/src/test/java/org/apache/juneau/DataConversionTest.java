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

import org.apache.juneau.collections.*;
import org.apache.juneau.testutils.*;
import org.junit.*;

@SuppressWarnings({"unchecked","rawtypes"})
@FixMethodOrder(NAME_ASCENDING)
public class DataConversionTest {

	@Before
	public void beforeTest() {
		TestUtils.setLocale(Locale.US);
	}

	@After
	public void afterTest() {
		TestUtils.unsetLocale();
	}

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		OMap m = new OMap();

		// *** Number ***
		m.put("x", 123);
		assertEquals((int)m.getInt("x"), 123);
		assertEquals((long)m.getLong("x"), 123);

		// *** Boolean ***
		m.put("x", true);
		assertEquals((boolean)m.getBoolean("x"), true);

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
		assertEquals(m.getString("x"), "{}");

		// *** OMap ***
		m.put("x", OMap.ofJson("{foo:123}"));
		assertEquals(m.getString("x"), "{foo:123}");

		// *** Collection ***
		Set s = new HashSet();
		s.add(123);
		m.put("x", s);
		assertEquals(m.getString("x"), "[123]");

		// *** OList ***
		m.put("x", OList.ofJson("[123]"));
		assertEquals(m.getString("x"), "[123]");
		assertEquals(m.getList("x").size(), 1);

		// *** Array ***
		m.put("x", new Integer[]{123});
		assertEquals(m.getString("x"), "[123]");
		assertEquals(m.getList("x").size(), 1);

		// *** Enum ***
		m.put("x", TestEnum.ENUM2);
		assertEquals(m.getString("x"), "ENUM2");
		assertFalse(m.getBoolean("x"));
		try {
			m.getMap("x");
			fail("Invalid conversion from Enum to OMap");
		} catch (InvalidDataConversionException e) {}

		// *** Not a bean ***
		m.put("x", new NotABean("foo"));
		assertEquals(m.getString("x"), "foo");
		try {
			m.getInt("x");
			fail("Invalid conversion from NotABean to Integer");
		} catch (InvalidDataConversionException e) {}
		try {
			m.getLong("x");
			fail("Invalid conversion from NotABean to Long");
		} catch (InvalidDataConversionException e) {}
		assertFalse(m.getBoolean("x"));
		try {
			m.getMap("x");
			fail("Invalid conversion from NotABean to OMap");
		} catch (InvalidDataConversionException e) {}

	}

	public enum TestEnum {
		ENUM0, ENUM1, ENUM2
	}

	public class NotABean {
		private String arg;

		public NotABean(String arg) {
			this.arg = arg;
		}

		@Override /* Object */
		public String toString() {
			return arg;
		}
	}

	//====================================================================================================
	// Data conversions with swaps.
	//====================================================================================================
	@Test
	public void testObjectSwaps() throws Exception {
		String s = "2001-12-21T12:34:56Z";
		BeanSession session = BeanContext.create().build().createSession();
		Calendar c = session.convertToType(s, GregorianCalendar.class);
		assertEquals(2001, c.get(Calendar.YEAR));
		c = session.convertToType(s, Calendar.class);
		assertEquals(2001, c.get(Calendar.YEAR));
		s = session.convertToType(c, String.class);
		assertEquals("2001-12-21T12:34:56Z", s);
	}
}
