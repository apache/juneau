/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.transforms;

import static org.junit.Assert.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@SuppressWarnings({"deprecation","javadoc"})
public class DateFilterTest {

	private static TimeZone tz;

	@BeforeClass
	public static void beforeClass() {
		tz = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT-5"));
	}

	@AfterClass
	public static void afterClass() {
		TimeZone.setDefault(tz);
	}

	private Date testDate = new Date(1, 2, 3, 4, 5, 6);
	private String tz1 = new SimpleDateFormat("zzz").format(testDate);

	//====================================================================================================
	// testString - DEFAULT_STRING
	//====================================================================================================
	@Test
	public void testString() throws Exception {
		Class<?> f = DateSwap.ToString.class;
		WriterSerializer s = new JsonSerializer.Simple().addTransforms(f);
		ReaderParser p = new JsonParser().addTransforms(f);
		doTest(s, p, "'Sun Mar 03 04:05:06 "+tz1+" 1901'");
	}

	//====================================================================================================
	// testISO8601DTZ - DEFAULT_ISO8601DTZ
	//====================================================================================================
	@Test
	public void testISO8601DTZ() throws Exception {
		Class<?> f = DateSwap.ISO8601DTZ.class;
		WriterSerializer s = new JsonSerializer.Simple().addTransforms(f);
		ReaderParser p = new JsonParser().addTransforms(f);
		doTest(s, p, "'1901-03-03T09:05:06Z'");
	}

	//====================================================================================================
	// testRFC2822DT - DEFAULT_RFC2822DT
	//====================================================================================================
	@Test
	public void testRFC2822DT() throws Exception {
		Class<?> f = DateSwap.RFC2822DT.class;
		WriterSerializer s = new JsonSerializer.Simple().addTransforms(f);
		ReaderParser p = new JsonParser().addTransforms(f);
		doTest(s, p, "'Sun, 03 Mar 1901 04:05:06 "+tz1+"'");
	}

	//====================================================================================================
	// testLong - DEFAULT_LONG
	//====================================================================================================
	@Test
	public void testLong() throws Exception {
		Class<?> f = DateLongSwap.class;
		WriterSerializer s = new JsonSerializer.Simple().addTransforms(f);
		ReaderParser p = new JsonParser().addTransforms(f);
		doTest(s, p, "-2172149694000");
	}

	//====================================================================================================
	// testMap - DEFAULT_MAP
	//====================================================================================================
	@Test
	public void testMap() throws Exception {
		Class<?> f = DateMapSwap.class;
		WriterSerializer s = new JsonSerializer.Simple().addTransforms(f);
		ReaderParser p = new JsonParser().addTransforms(f);
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

		final String jsonData = new JsonSerializer().addTransforms(
			DateSwap.ISO8601DT.class).serialize(testBeanA);
		final ObjectMap data = new JsonParser().addTransforms(
			DateSwap.ISO8601DT.class).parse(jsonData, ObjectMap.class);

		final DateSwap.ISO8601DT dateSwap = new DateSwap.ISO8601DT();
		// this works
		final String sValue = data.getString("birthday"); //$NON-NLS-1$
		dateSwap.unswap(sValue, data.getBeanContext().getClassMeta(Date.class));
		// this does not work
		data.get(dateSwap, "birthday"); //$NON-NLS-1$
	}

	public static class A {
		public Date birthday;

		public A init() {
			birthday = new Date();
			return this;
		}
	}
}