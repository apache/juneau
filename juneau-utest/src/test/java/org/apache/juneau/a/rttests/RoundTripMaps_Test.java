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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swaps.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"deprecation"})
class RoundTripMaps_Test extends SimpleTestBase {

	private static RoundTripTester[] TESTERS = {
		tester("Json - default")
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester("Json - lax")
			.serializer(JsonSerializer.create().json5().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester("Json - lax, readable")
			.serializer(JsonSerializer.create().json5().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester("Xml - namespaces, validation, readable")
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		tester("Xml - no namespaces, validation")
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Html - default")
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Html - readable")
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Html - with key/value headers")
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Uon - default")
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester("Uon - readable")
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester("Uon - encoded")
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create().decoding())
			.build(),
		tester("UrlEncoding - default")
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester("UrlEncoding - readable")
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester("UrlEncoding - expanded params")
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create().expandedParams())
			.build(),
		tester("MsgPack")
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(MsgPackParser.create())
			.build(),
		tester("Json schema")
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.returnOriginalObject()
			.build(),
	};

	static Stream<Arguments> testers() {
		return Stream.of(TESTERS).map(x -> args(x));
	}

	protected static RoundTripTester.Builder tester(String label) {
		return RoundTripTester.create(label).pojoSwaps(ByteArraySwap.Base64.class);
	}

	//====================================================================================================
	// Map<Integer,String> test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_mapIntegerString(RoundTripTester t) throws Exception {
		var x = new TreeMap<Integer,String>();
		x.put(1, "a");
		x.put(2, null);
		x = t.roundTrip(x, TreeMap.class, Integer.class, String.class);
		assertEquals("a", x.get(1));
		assertNull(null, x.get(2));

		var x2 = new HashMap<Integer,String>();
		x2.put(1, "a");
		x2.put(2, null);
		x2.put(null, "b");
		x2 = t.roundTrip(x2, HashMap.class, Integer.class, String.class);
		assertEquals("a", x2.get(1));
		assertNull(x2.get(2));
		assertEquals("b", x2.get(null));
	}

	//====================================================================================================
	// Map<Boolean,String> test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_mapBooleanString(RoundTripTester t) throws Exception {
		var x = new TreeMap<Boolean,String>();
		x.put(true, "a");
		x.put(false, null);
		x = t.roundTrip(x, TreeMap.class, Boolean.class, String.class);
		assertEquals("a", x.get(true));
		assertNull(null, x.get(false));

		var x2 = new HashMap<Boolean,String>();
		x2.put(true, "a");
		x2.put(false, null);
		x2.put(null, "b");
		x2 = t.roundTrip(x2, HashMap.class, Boolean.class, String.class);
		assertEquals("a", x2.get(true));
		assertNull(x2.get(false));
		assertEquals("b", x2.get(null));
	}

	//====================================================================================================
	// Map<byte[],String> test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_mapByteArrayString(RoundTripTester t) throws Exception {

		// Note, you cannot really test maps with byte[] keys since byte[] does not test for equality.
		// So just test serialization.
		String e;
		Object r;

		var x = new LinkedHashMap<byte[],String>();
		x.put(new byte[]{1,2,3}, "a");
		x.put(new byte[]{4,5,6}, null);
		x.put(null, "b");

		var s = (Serializer)JsonSerializer.create().json5().swaps(ByteArraySwap.Base64.class).keepNullProperties().build();
		e = "{AQID:'a',BAUG:null,null:'b'}";
		r = s.serialize(x);
		assertEquals(e, r);

		s = XmlSerializer.create().ns().sq().swaps(ByteArraySwap.Base64.class).keepNullProperties().build();
		e = "<object><AQID>a</AQID><BAUG _type='null'/><_x0000_>b</_x0000_></object>";
		r = s.serialize(x);
		assertEquals(e, r);

		s = HtmlSerializer.create().sq().swaps(ByteArraySwap.Base64.class).keepNullProperties().addKeyValueTableHeaders().build();
		e = "<table><tr><th>key</th><th>value</th></tr><tr><td>AQID</td><td>a</td></tr><tr><td>BAUG</td><td><null/></td></tr><tr><td><null/></td><td>b</td></tr></table>";
		r = s.serialize(x);
		assertEquals(e, r);

		s = UonSerializer.create().encoding().swaps(ByteArraySwap.Base64.class).keepNullProperties().build();
		e = "(AQID=a,BAUG=null,null=b)";
		r = s.serialize(x);
		assertEquals(e, r);

		s = UrlEncodingSerializer.create().swaps(ByteArraySwap.Base64.class).keepNullProperties().build();
		e = "AQID=a&BAUG=null&null=b";
		r = s.serialize(x);
		assertEquals(e, r);
	}

	//====================================================================================================
	// Map<Date,String> test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a04_mapDateString(RoundTripTester t) throws Exception {
		var xd1 = new Date(1,2,3,4,5,6);
		var xd2 = new Date(2,3,4,5,6,7);

		var x = new TreeMap<Date,String>();
		x.put(xd1, "a");
		x.put(xd2, null);
		x = t.roundTrip(x, TreeMap.class, Date.class, String.class);
		assertEquals("a", x.get(xd1));
		assertNull(null, x.get(xd2));

		var x2 = new HashMap<Date,String>();
		x2.put(xd1, "a");
		x2.put(xd2, null);
		x2.put(null, "b");
		x2 = t.roundTrip(x2, HashMap.class, Date.class, String.class);
		assertEquals("a", x2.get(xd1));
		assertNull(x2.get(xd2));
		assertEquals("b", x2.get(null));
	}

	//====================================================================================================
	// Map<Calendar,String> test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a05_mapCalendarString(RoundTripTester t) throws Exception {
		var xc1 = GregorianCalendar.from(ZonedDateTime.parse("2012-12-21T12:34:56Z"));
		var xc2 = GregorianCalendar.from(ZonedDateTime.parse("2012-12-21T12:34:57Z"));

		var x = new TreeMap<Calendar,String>();
		x.put(xc1, "a");
		x.put(xc2, null);
		x = t.roundTrip(x, TreeMap.class, GregorianCalendar.class, String.class);
		assertEquals("a", x.get(xc1));
		assertNull(null, x.get(xc2));

		var x2 = new HashMap<Calendar,String>();
		x2.put(xc1, "a");
		x2.put(xc2, null);
		x2.put(null, "b");
		x2 = t.roundTrip(x2, HashMap.class, GregorianCalendar.class, String.class);

		assertEquals("a", x2.get(xc1));
		assertNull(x2.get(xc2));
		assertEquals("b", x2.get(null));
	}

	//====================================================================================================
	// Map<Enum,String> test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a06_mapEnumString(RoundTripTester t) throws Exception {

		var x = new TreeMap<TestEnum,String>();
		x.put(TestEnum.FOO, "a");
		x.put(TestEnum.BAR, null);
		x = t.roundTrip(x, TreeMap.class, TestEnum.class, String.class);
		assertEquals("a", x.get(TestEnum.FOO));
		assertNull(null, x.get(TestEnum.BAR));

		var x2 = new HashMap<TestEnum,String>();  // NOSONAR
		x2.put(TestEnum.FOO, "a");
		x2.put(TestEnum.BAR, null);
		x2.put(null, "b");
		x2 = t.roundTrip(x2, HashMap.class, TestEnum.class, String.class);
		assertEquals("a", x2.get(TestEnum.FOO));
		assertNull(x2.get(TestEnum.BAR));
		assertEquals("b", x2.get(null));
	}

	public enum TestEnum {
		FOO,BAR,BAZ
	}
}