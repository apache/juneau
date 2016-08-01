/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filters.*;

@SuppressWarnings({"unchecked","rawtypes"})
public class CT_DataConversionTest {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		ObjectMap m = new ObjectMap();

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
		assertNull(m.getObjectMap("x"));
		assertNull(m.getList("x"));
		assertNull(m.getObjectList("x"));

		// *** Map ***
		m.put("x", new HashMap());
		assertEquals(m.getString("x"), "{}");

		// *** ObjectMap ***
		m.put("x", new ObjectMap("{foo:123}"));
		assertEquals(m.getString("x"), "{foo:123}");

		// *** Collection ***
		Set s = new HashSet();
		s.add(123);
		m.put("x", s);
		assertEquals(m.getString("x"), "[123]");

		// *** ObjectList ***
		m.put("x", new ObjectList("[123]"));
		assertEquals(m.getString("x"), "[123]");
		assertEquals(m.getList("x").size(), 1);
		assertEquals(m.getObjectList("x").size(), 1);

		// *** Array ***
		m.put("x", new Integer[]{123});
		assertEquals(m.getString("x"), "[123]");
		assertEquals(m.getList("x").size(), 1);
		assertEquals(m.getObjectList("x").size(), 1);

		// *** Enum ***
		m.put("x", TestEnum.ENUM2);
		assertEquals(m.getString("x"), "ENUM2");
		assertFalse(m.getBoolean("x"));
		try {
			m.getMap("x");
			fail("Invalid conversion from Enum to Map");
		} catch (InvalidDataConversionException e) {}
		try {
			m.getObjectMap("x");
			fail("Invalid conversion from Enum to ObjectMap");
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
			fail("Invalid conversion from NotABean to Map");
		} catch (InvalidDataConversionException e) {}
		try {
			m.getObjectMap("x");
			fail("Invalid conversion from NotABean to ObjectMap");
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
	// Data conversions with object filters.
	//====================================================================================================
	@Test
	public void testObjectFilters() throws Exception {
		String s = "Jan 12, 2001";
		BeanContext bc = new BeanContextFactory().addFilters(CalendarFilter.Medium.class).getBeanContext();
		Calendar c = bc.convertToType(s, GregorianCalendar.class);
		assertEquals(2001, c.get(Calendar.YEAR));
		c = bc.convertToType(s, Calendar.class);
		assertEquals(2001, c.get(Calendar.YEAR));
		s = bc.convertToType(c, String.class);
		assertEquals("Jan 12, 2001", s);
	}
}
