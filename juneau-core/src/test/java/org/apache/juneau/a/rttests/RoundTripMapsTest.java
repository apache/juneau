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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.serializer.SerializerContext.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;



/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"unchecked","deprecation","javadoc"})
public class RoundTripMapsTest extends RoundTripTest {

	public RoundTripMapsTest(String label, Serializer s, Parser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	@Override /* RoundTripTest */
	public Class<?>[] getTransforms() {
		return new Class<?>[]{
			ByteArrayBase64Swap.class,
			DateSwap.ISO8601DTZ.class,
			CalendarLongSwap.class,
		};
	}

	//====================================================================================================
	// Map<Integer,String> test
	//====================================================================================================
	@Test
	public void testMapIntegerString() throws Exception {
		Map<Integer,String> t = new TreeMap<Integer,String>();
		t.put(1, "a");
		t.put(2, null);
		t = roundTripMap(t, TreeMap.class, Integer.class, String.class);
		assertEquals("a", t.get(1));
		assertNull(null, t.get(2));

		t = new HashMap<Integer,String>();
		t.put(1, "a");
		t.put(2, null);
		t.put(null, "b");
		t = roundTripMap(t, HashMap.class, Integer.class, String.class);
		assertEquals("a", t.get(1));
		assertNull(t.get(2));
		assertEquals("b", t.get(null));
	}

	//====================================================================================================
	// Map<Boolean,String> test
	//====================================================================================================
	@Test
	public void testMapBooleanString() throws Exception {
		Map<Boolean,String> t = new TreeMap<Boolean,String>();
		t.put(true, "a");
		t.put(false, null);
		t = roundTripMap(t, TreeMap.class, Boolean.class, String.class);
		assertEquals("a", t.get(true));
		assertNull(null, t.get(false));

		t = new HashMap<Boolean,String>();
		t.put(true, "a");
		t.put(false, null);
		t.put(null, "b");
		t = roundTripMap(t, HashMap.class, Boolean.class, String.class);
		assertEquals("a", t.get(true));
		assertNull(t.get(false));
		assertEquals("b", t.get(null));
	}

	//====================================================================================================
	// Map<byte[],String> test
	//====================================================================================================
	@Test
	public void testMapByteArrayString() throws Exception {

		// Note, you cannot really test maps with byte[] keys since byte[] does not test for equality.
		// So just test serialization.
		String e;
		Object r;

		Map<byte[],String> t = new LinkedHashMap<byte[],String>();
		t.put(new byte[]{1,2,3}, "a");
		t.put(new byte[]{4,5,6}, null);
		t.put(null, "b");

		s = new JsonSerializer.Simple().addTransforms(getTransforms()).setProperty(SERIALIZER_trimNullProperties, false);
		e = "{AQID:'a',BAUG:null,null:'b'}";
		r = s.serialize(t);
		assertEquals(e, r);

		s = new XmlSerializer.SimpleXmlJsonSq().addTransforms(getTransforms()).setProperty(SERIALIZER_trimNullProperties, false);
		e = "<object><AQID>a</AQID><BAUG nil='true'/><_x0000_>b</_x0000_></object>";
		r = s.serialize(t);
		assertEquals(e, r);

		s = new HtmlSerializer.Sq().addTransforms(getTransforms()).setProperty(SERIALIZER_trimNullProperties, false);
		e = "<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>AQID</string></td><td><string>a</string></td></tr><tr><td><string>BAUG</string></td><td><null/></td></tr><tr><td><null/></td><td><string>b</string></td></tr></table>";
		r = s.serialize(t);
		assertEquals(e, r);

		s = new UonSerializer.Encoding().addTransforms(getTransforms()).setProperty(SERIALIZER_trimNullProperties, false);
		e = "$o(AQID=a,BAUG=%00,%00=b)";
		r = s.serialize(t);
		assertEquals(e, r);

		s = new UrlEncodingSerializer().addTransforms(getTransforms()).setProperty(SERIALIZER_trimNullProperties, false);
		e = "AQID=a&BAUG=%00&%00=b";
		r = s.serialize(t);
		assertEquals(e, r);
	}

	//====================================================================================================
	// Map<Date,String> test
	//====================================================================================================
	@Test
	public void testMapDateString() throws Exception {
		Date td1 = new Date(1,2,3,4,5,6);
		Date td2 = new Date(2,3,4,5,6,7);

		Map<Date,String> t = new TreeMap<Date,String>();
		t.put(td1, "a");
		t.put(td2, null);
		t = roundTripMap(t, TreeMap.class, Date.class, String.class);
		assertEquals("a", t.get(td1));
		assertNull(null, t.get(td2));

		t = new HashMap<Date,String>();
		t.put(td1, "a");
		t.put(td2, null);
		t.put(null, "b");
		t = roundTripMap(t, HashMap.class, Date.class, String.class);
		assertEquals("a", t.get(td1));
		assertNull(t.get(td2));
		assertEquals("b", t.get(null));
	}

	//====================================================================================================
	// Map<Calendar,String> test
	//====================================================================================================
	@Test
	public void testMapCalendarString() throws Exception {
		Calendar td1 = new GregorianCalendar();
		td1.setTime(new Date(1,2,3,4,5,6));
		Calendar td2 = new GregorianCalendar();
		td2.setTime(new Date(2,3,4,5,6,7));

		Map<Calendar,String> t = new TreeMap<Calendar,String>();
		t.put(td1, "a");
		t.put(td2, null);
		t = roundTripMap(t, TreeMap.class, GregorianCalendar.class, String.class);
		assertEquals("a", t.get(td1));
		assertNull(null, t.get(td2));

		t = new HashMap<Calendar,String>();
		t.put(td1, "a");
		t.put(td2, null);
		t.put(null, "b");
		t = roundTripMap(t, HashMap.class, GregorianCalendar.class, String.class);
		assertEquals("a", t.get(td1));
		assertNull(t.get(td2));
		assertEquals("b", t.get(null));
	}

	//====================================================================================================
	// Map<Enum,String> test
	//====================================================================================================
	@Test
	public void testMapEnumString() throws Exception {

		Map<TestEnum,String> t = new TreeMap<TestEnum,String>();
		t.put(TestEnum.FOO, "a");
		t.put(TestEnum.BAR, null);
		t = roundTripMap(t, TreeMap.class, TestEnum.class, String.class);
		assertEquals("a", t.get(TestEnum.FOO));
		assertNull(null, t.get(TestEnum.BAR));

		t = new HashMap<TestEnum,String>();
		t.put(TestEnum.FOO, "a");
		t.put(TestEnum.BAR, null);
		t.put(null, "b");
		t = roundTripMap(t, HashMap.class, TestEnum.class, String.class);
		assertEquals("a", t.get(TestEnum.FOO));
		assertNull(t.get(TestEnum.BAR));
		assertEquals("b", t.get(null));
	}

	public enum TestEnum {
		FOO,BAR,BAZ
	}
}
