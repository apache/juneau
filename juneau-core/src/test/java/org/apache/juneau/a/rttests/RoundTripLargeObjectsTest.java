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

import static org.apache.juneau.a.rttests.RoundTripTest.Flags.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.apache.juneau.urlencoding.UonSerializerContext.*;
import static org.apache.juneau.xml.XmlSerializerContext.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@Ignore
@SuppressWarnings({"hiding","serial"})
public class RoundTripLargeObjectsTest extends RoundTripTest {

	private static final int NUM_RUNS = 10;
	private static final int SIZE_PARAM = 20000;

	public RoundTripLargeObjectsTest(String label, Serializer s, Parser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	@Parameterized.Parameters
	public static Collection<Object[]> getPairs() {
		return Arrays.asList(new Object[][] {
			// Full round-trip testing
			{ /* 0 */
				"Json DEFAULT",
				new JsonSerializer().setProperty(SERIALIZER_trimNullProperties, false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 1 */
				"Json DEFAULT_LAX",
				new JsonSerializer.Simple().setProperty(SERIALIZER_trimNullProperties, false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 2 */
				"Json DEFAULT_SQ",
				new JsonSerializer.Simple().setProperty(SERIALIZER_trimNullProperties, false),
				JsonParser.DEFAULT,
				0
			},
			{ /* 3 */
				"Xml DEFAULT w/namespaces,validation",
				new XmlSerializer.XmlJsonSq().setProperty(SERIALIZER_trimNullProperties, false).setProperty(XML_addNamespaceUrisToRoot, true).setProperty(SERIALIZER_useIndentation, true),
				XmlParser.DEFAULT,
				CHECK_XML_WHITESPACE | VALIDATE_XML
			},
			{ /* 4 */
				"Xml DEFAULT wo/namespaces,validation",
				new XmlSerializer.SimpleXmlJsonSq().setProperty(SERIALIZER_trimNullProperties, false),
				XmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 5 */
				"Html",
				new HtmlSerializer().setProperty(SERIALIZER_trimNullProperties, false),
				HtmlParser.DEFAULT,
				CHECK_XML_WHITESPACE
			},
			{ /* 6 */
				"UrlEncoding",
				new UrlEncodingSerializer().setProperty(SERIALIZER_trimNullProperties, false).setProperty(UON_simpleMode, false),
				UrlEncodingParser.DEFAULT,
				0
			},
			{ /* 7 */
				"Uon",
				new UonSerializer().setProperty(SERIALIZER_trimNullProperties, false).setProperty(UON_simpleMode, false),
				UonParser.DEFAULT,
				0
			},
			{ /* 8 */
				"MsgPack",
				new MsgPackSerializer().setProperty(SERIALIZER_trimNullProperties, false).setProperty(UON_simpleMode, false),
				MsgPackParser.DEFAULT,
				0
			},
//			{ /* 9 */
//				"Rdf.Xml",
//				new RdfSerializer.Xml().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
//				RdfParser.DEFAULT_XML,
//				0
//			},
//			{ /* 10 */
//				"Rdf.XmlAbbrev",
//				new RdfSerializer.XmlAbbrev().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
//				RdfParser.DEFAULT_XML,
//				0
//			},
//			{ /* 11 */
//				"Rdf.Turtle",
//				new RdfSerializer.Turtle().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
//				RdfParser.DEFAULT_TURTLE,
//				0
//			},
//			{ /* 12 */
//				"Rdf.NTriple",
//				new RdfSerializer.NTriple().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
//				RdfParser.DEFAULT_NTRIPLE,
//				0
//			},
//			{ /* 13 */
//				"Rdf.N3",
//				new RdfSerializer.N3().setProperty(SERIALIZER_trimNullProperties, false).setProperty(RDF_addLiteralTypes, true),
//				RdfParser.DEFAULT_N3,
//				0
//			},
		});
	}

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void testLargeMap() throws Exception {
		long startTime;
		int numRuns = NUM_RUNS;

		A a = A.create();
		Serializer s = getSerializer();
		Parser p = getParser();
		System.err.println("\n---Speed test on " + label + "---");
		Object r = "";

		// Initialization run.
		r = s.serialize(a);
		System.err.println(MessageFormat.format("Serialized size: {0,number} ", (r instanceof String ? r.toString().length() : ((byte[])r).length)));
		p.parse(r, A.class);

		startTime = System.currentTimeMillis();
		for (int i = 0; i < numRuns; i++)
			r = s.serialize(a);
		System.err.println(MessageFormat.format("Average serialize time: {0,number}ms", (System.currentTimeMillis()-startTime)/numRuns));
		startTime = System.currentTimeMillis();
		for (int i = 0; i < numRuns; i++)
			a = p.parse(r, A.class);
		System.err.println(MessageFormat.format("Average parsed time: {0,number}ms", (System.currentTimeMillis()-startTime)/numRuns));
	}

	public static class A {
		public A1Map a1Map;
		public A1List a1List;
		public A1[] a1Array;

		static A create() {
			A a = new A();
			a.a1Map = new A1Map();
			a.a1List = new A1List();
			for (int i = 0; i < SIZE_PARAM; i++) {
				a.a1Map.put(String.valueOf(i), new A1());
				a.a1List.add(new A1());
			}
			a.a1Array = a.a1List.toArray(new A1[0]);
			return a;
		}
	}

	public static class A1 {
		public String f1 = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";
	}

	public static class A1Map extends LinkedHashMap<String,A1> {}

	public static class A1List extends LinkedList<A1> {}
}
