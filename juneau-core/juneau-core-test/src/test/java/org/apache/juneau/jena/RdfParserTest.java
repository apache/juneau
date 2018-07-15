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
package org.apache.juneau.jena;

import static org.apache.juneau.jena.RdfCommon.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.transforms.*;
import org.junit.*;

public class RdfParserTest {

	@Before
	public void beforeTest() {
		TestUtils.setLocale(Locale.US);
	}

	@After
	public void afterTest() {
		TestUtils.unsetLocale();
	}

	@Test
	public void testParseIntoGenericPojos() throws Exception {
		A a = new A().init();

		// Create a new serializer with readable output.
		RdfSerializer s = RdfSerializer.create().xmlabbrev()
			.set(RDF_rdfxml_tab, 3)
			.sq()
			.addRootProperty()
			.build();

		String expected =
			"<rdf:RDF a='http://ns/' a1='http://ns2/' j='http://www.apache.org/juneau/' jp='http://www.apache.org/juneaubp/' rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"
			+ "\n   <rdf:Description about='http://test/a'>"
			+ "\n      <a:f1>1</a:f1>"
			+ "\n      <a:f2>f2</a:f2>"
			+ "\n      <a:f4a rdf:resource='http://test/a'/>"
			+ "\n      <a:f4b rdf:resource='http://test/external'/>"
			+ "\n      <a:f5>1999-01-01T00:00:00Z</a:f5>"
			+ "\n      <a:f6>"
			+ "\n         <rdf:Seq>"
			+ "\n            <rdf:li>"
			+ "\n               <rdf:Description about='http://test/a/a1'>"
			+ "\n                  <a1:f1>1</a1:f1>"
			+ "\n                  <a1:f2>f2</a1:f2>"
			+ "\n                  <a1:f4a rdf:resource='http://test/a'/>"
			+ "\n                  <a1:f4b rdf:resource='http://test/external'/>"
			+ "\n                  <a1:f5>1999-01-01T00:00:00Z</a1:f5>"
			+ "\n               </rdf:Description>"
			+ "\n            </rdf:li>"
			+ "\n         </rdf:Seq>"
			+ "\n      </a:f6>"
			+ "\n      <j:root>true</j:root>"
			+ "\n   </rdf:Description>"
			+ "\n</rdf:RDF>";

		String rdfXml = s.serialize(a);
		assertXmlEquals(expected, rdfXml);

		A a2 = RdfXmlParser.DEFAULT.parse(rdfXml, A.class);

		assertEqualObjects(a, a2);

		ObjectMap m = RdfXmlParser.DEFAULT.parse(rdfXml, ObjectMap.class);
		String json = SimpleJsonSerializer.DEFAULT_READABLE.serialize(m);

		String e = ""
			+ "{\n"
			+ "	uri: 'http://test/a',\n"
			+ "	f6: [\n"
			+ "		{\n"
			+ "			uri: 'http://test/a/a1',\n"
			+ "			f5: '1999-01-01T00:00:00Z',\n"
			+ "			f4b: 'http://test/external',\n"
			+ "			f4a: 'http://test/a',\n"
			+ "			f2: 'f2',\n"
			+ "			f1: '1'\n"
			+ "		}\n"
			+ "	],\n"
			+ "	f5: '1999-01-01T00:00:00Z',\n"
			+ "	f4b: 'http://test/external',\n"
			+ "	f4a: 'http://test/a',\n"
			+ "	f2: 'f2',\n"
			+ "	f1: '1',\n"
			+ "	root: 'true'\n"
			+ "}";
		assertEquals(e, json.replace("\r", ""));

	}

	@Rdf(prefix="a", namespace="http://ns/")
	public static class A {
		public int f1;
		public String f2;
		@Rdf(beanUri=true) public URI f3;
		public URI f4a, f4b;
		@Swap(CalendarSwap.ISO8601DTZ.class) public Calendar f5;
		public LinkedList<A1> f6 = new LinkedList<>();

		public A init() throws Exception {
			f1 = 1;
			f2 = "f2";
			f3 = new URI("http://test/a"); // Bean URI.
			f4a = new URI("http://test/a"); // Points to itself.
			f4b = new URI("http://test/external");
			f5 = new GregorianCalendar();
			DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			f5.setTime(df.parse("Jan 1, 1999"));
			f6 = new LinkedList<>();
			f6.add(new A1().init());
			return this;
		}
	}

	@Rdf(prefix="a1", namespace="http://ns2/")
	public static class A1 {
		public int f1;
		public String f2;
		@Rdf(beanUri=true) public URI f3;
		public URI f4a, f4b;
		@Swap(CalendarSwap.ISO8601DTZ.class) public Calendar f5;

		public A1 init() throws Exception {
			f1 = 1;
			f2 = "f2";
			f3 = new URI("http://test/a/a1");
			f4a = new URI("http://test/a");
			f4b = new URI("http://test/external");
			f5 = new GregorianCalendar();
			DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
			df.setTimeZone(TimeZone.getTimeZone("GMT"));
			f5.setTime(df.parse("Jan 1, 1999"));
			return this;
		}
	}
}
