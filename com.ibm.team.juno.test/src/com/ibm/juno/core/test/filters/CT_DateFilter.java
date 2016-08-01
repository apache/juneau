/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.filters;

import static org.junit.Assert.*;

import java.text.*;
import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;

@SuppressWarnings("deprecation")
public class CT_DateFilter {

	private Date testDate = new Date(1, 2, 3, 4, 5, 6);
	private String tz1 = new SimpleDateFormat("zzz").format(testDate);

	//====================================================================================================
	// testString - DEFAULT_STRING
	//====================================================================================================
	@Test
	public void testString() throws Exception {
		Class<?> f = DateFilter.ToString.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, "'Sun Mar 03 04:05:06 "+tz1+" 1901'");
	}

	//====================================================================================================
	// testISO8601DTZ - DEFAULT_ISO8601DTZ
	//====================================================================================================
	@Test
	public void testISO8601DTZ() throws Exception {
		Class<?> f = DateFilter.ISO8601DTZ.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, "'1901-03-03T09:05:06Z'");
	}

	//====================================================================================================
	// testRFC2822DT - DEFAULT_RFC2822DT
	//====================================================================================================
	@Test
	public void testRFC2822DT() throws Exception {
		Class<?> f = DateFilter.RFC2822DT.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, "'Sun, 03 Mar 1901 04:05:06 "+tz1+"'");
	}

	//====================================================================================================
	// testLong - DEFAULT_LONG
	//====================================================================================================
	@Test
	public void testLong() throws Exception {
		Class<?> f = DateLongFilter.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, "-2172149694000");
	}

	//====================================================================================================
	// testMap - DEFAULT_MAP
	//====================================================================================================
	@Test
	public void testMap() throws Exception {
		Class<?> f = DateMapFilter.class;
		WriterSerializer s = new JsonSerializer.Simple().addFilters(f);
		ReaderParser p = new JsonParser().addFilters(f);
		doTest(s, p, "{time:-2172149694000}");
	}

	public void doTest(WriterSerializer s, ReaderParser p, String expected) throws Exception {
		Date d;
		String actual;

		d = testDate;
		actual = s.serialize(d);
		assertEquals(expected, actual);
		d = p.parse(actual, Date.class);
		assertEquals(1, d.getYear());
		assertEquals(2, d.getMonth());
		assertEquals(3, d.getDate());
		assertEquals(4, d.getHours());
		assertEquals(5, d.getMinutes());
		assertEquals(6, d.getSeconds());

		d = new java.sql.Date(testDate.getTime());
		actual = s.serialize(d);
		assertEquals(expected, actual);
		d = p.parse(actual, java.sql.Date.class);
		assertEquals(1, d.getYear());
		assertEquals(2, d.getMonth());
		assertEquals(3, d.getDate());

		d = new java.sql.Time(testDate.getTime());
		actual = s.serialize(d);
		assertEquals(expected, actual);
		d = p.parse(actual, java.sql.Time.class);
		assertEquals(4, d.getHours());
		assertEquals(5, d.getMinutes());
		assertEquals(6, d.getSeconds());

		d = new java.sql.Timestamp(testDate.getTime());
		actual = s.serialize(d);
		assertEquals(expected, actual);
		d = p.parse(actual, java.sql.Timestamp.class);
		assertEquals(1, d.getYear());
		assertEquals(2, d.getMonth());
		assertEquals(3, d.getDate());
		assertEquals(4, d.getHours());
		assertEquals(5, d.getMinutes());
		assertEquals(6, d.getSeconds());
	}

	//====================================================================================================
	//====================================================================================================
	@Test
	public void testBeanWithDate() throws Exception {
		A testBeanA = new A().init();

		final String jsonData = new JsonSerializer().addFilters(
			DateFilter.ISO8601DT.class).serialize(testBeanA);
		final ObjectMap data = new JsonParser().addFilters(
			DateFilter.ISO8601DT.class).parse(jsonData, ObjectMap.class);

		final DateFilter.ISO8601DT dateFilter = new DateFilter.ISO8601DT();
		// this works
		final String sValue = data.getString("birthday"); //$NON-NLS-1$
		dateFilter.unfilter(sValue, data.getBeanContext().getClassMeta(Date.class));
		// this does not work
		data.get(dateFilter, "birthday"); //$NON-NLS-1$
	}

	public static class A {
		public Date birthday;

		public A init() {
			birthday = new Date();
			return this;
		}
	}
}