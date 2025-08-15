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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@Disabled
@SuppressWarnings({"serial"})
class RoundTripLargeObjects_Test extends SimpleTestBase {

	private static final int NUM_RUNS = 10;
	private static final int SIZE_PARAM = 20000;

	private static RoundTripTester[] TESTERS = {
		tester("Json DEFAULT")
			.serializer(JsonSerializer.create().keepNullProperties())
			.parser(JsonParser.create())
			.build(),
		tester("Json5 DEFAULT")
			.serializer(Json5Serializer.create().keepNullProperties())
			.parser(Json5Parser.create())
			.build(),
		tester("Json DEFAULT_SQ")
			.serializer(JsonSerializer.create().json5().keepNullProperties())
			.parser(JsonParser.create())
			.build(),
		tester("Xml DEFAULT w/namespaces,validation")
			.serializer(XmlSerializer.create().sq().ns().keepNullProperties().addNamespaceUrisToRoot().useWhitespace())
			.parser(XmlParser.create())
			.validateXml()
			.validateXmlWhitespace()
			.build(),
		tester("Xml DEFAULT wo/namespaces,validation")
			.serializer(XmlSerializer.create().sq().keepNullProperties())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Html")
			.serializer(HtmlSerializer.create().keepNullProperties())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("UrlEncoding")
			.serializer(UrlEncodingSerializer.create().keepNullProperties())
			.parser(UrlEncodingParser.create())
			.build(),
		tester("Uon")
			.serializer(UonSerializer.create().keepNullProperties())
			.parser(UonParser.create())
			.build(),
		tester("MsgPack")
			.serializer(MsgPackSerializer.create().keepNullProperties())
			.parser(MsgPackParser.create())
			.build()
	};

	static Stream<Arguments> testers() {
		return Stream.of(TESTERS).map(x -> args(x));
	}

	protected static RoundTripTester.Builder tester(String label) {
		return RoundTripTester.create(label);
	}

	//====================================================================================================
	// test
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_largeMap(RoundTripTester t) throws Exception {
		long startTime;
		var numRuns = NUM_RUNS;

		var a = A.create();
		var s = t.getSerializer();
		var p = t.getParser();
		System.err.println("\n---Speed test on " + t.label + "---"); // NOT DEBUG
		Object r = "";

		// Initialization run.
		r = s.serialize(a);
		System.err.println(format("Serialized size: {0,number} ", (r instanceof String ? r.toString().length() : ((byte[])r).length))); // NOT DEBUG
		p.parse(r, A.class);

		startTime = System.currentTimeMillis();
		for (int i = 0; i < numRuns; i++)
			r = s.serialize(a);
		System.err.println(format("Average serialize time: {0,number}ms", (System.currentTimeMillis()-startTime)/numRuns)); // NOT DEBUG
		startTime = System.currentTimeMillis();
		for (int i = 0; i < numRuns; i++)
			p.parse(r, A.class);
		System.err.println(format("Average parsed time: {0,number}ms", (System.currentTimeMillis()-startTime)/numRuns)); // NOT DEBUG
	}

	public static class A {
		public A1Map a1Map;
		public A1List a1List;
		public A1[] a1Array;

		static A create() {
			var a = new A();
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