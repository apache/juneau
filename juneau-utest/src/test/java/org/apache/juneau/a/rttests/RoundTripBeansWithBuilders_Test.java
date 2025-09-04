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

import static java.util.Collections.*;
import static org.apache.juneau.TestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class RoundTripBeansWithBuilders_Test extends SimpleTestBase {

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

	static RoundTripTester[] testers() {
		return TESTERS;
	}

	protected static RoundTripTester.Builder tester(String label) {
		return RoundTripTester.create(label).annotatedClasses(AcConfig.class);
	}

	//====================================================================================================
	// simple
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_simple(RoundTripTester t) throws Exception {
		var x = A.create().f1(1).build();
		x = t.roundTrip(x, A.class);
		assertBean(x, "f1", "1");
	}

	public static class A {
		private final int f1;

		public A(Builder b) {
			this.f1 = b.f1;
		}

		public static Builder create() {
			return new Builder();
		}

		@Bean(findFluentSetters=true)
		public static class Builder {

			private int f1;
			public Builder f1(int v) { f1 = v; return this; }

			public A build() {
				return new A(this);
			}
		}

		public int getF1() { return f1; }
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_simple_usingConfig(RoundTripTester t) throws Exception {
		var x = Ac.create().f1(1).build();
		x = t.roundTrip(x, Ac.class);
		assertBean(x, "f1", "1");
	}

	@Bean(on="Dummy1", findFluentSetters=true)
	@Bean(on="Builder", findFluentSetters=true)
	@Bean(on="Dummy2", findFluentSetters=true)
	private static class AcConfig {}

	public static class Ac {
		private final int f1;

		public Ac(Builder b) {
			this.f1 = b.f1;
		}

		public static Builder create() {
			return new Builder();
		}

		public static class Builder {

			private int f1;
			public Builder f1(int v) { f1 = v; return this; }

			public Ac build() {
				return new Ac(this);
			}
		}

		public int getF1() { return f1; }
	}

	//====================================================================================================
	// Bean property builder, simple
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_beanPropertyBuilder_simple(RoundTripTester t) throws Exception {
		var x = A2.create().f1(A.create().f1(1).build()).build();
		x = t.roundTrip(x, A2.class);
		assertBean(x, "f1{f1}", "{1}");
	}

	public static class A2 {
		private final A f1;

		public A2(Builder b) {
			this.f1 = b.f1;
		}

		public static Builder create() {
			return new Builder();
		}

		@Bean(findFluentSetters=true)
		public static class Builder {

			private A f1;
			public Builder f1(A v) { f1 = v; return this; }

			public A2 build() {
				return new A2(this);
			}
		}

		public A getF1() { return f1; }
	}

	//====================================================================================================
	// Bean property builder, collections
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a04_beanPropertyBuilder_collections(RoundTripTester t) throws Exception {
		// It's simply not possible to allow for expanded parameters with a builder-based approach
		// since the value on the builder can only be set once.
		if (t.label.equals("UrlEncoding - expanded params"))
			return;
		var x = A3.create()
			.f1(new A[]{A.create().f1(1).build()})
			.f2(singletonList(A.create().f1(2).build()))
			.f3(singletonList(singletonList(A.create().f1(3).build())))
			.f4(singletonList(new A[]{A.create().f1(4).build()}))
			.f5(singletonList(singletonList(new A[]{A.create().f1(5).build()})))
			.f6(singletonMap("foo", A.create().f1(6).build()))
			.f7(singletonMap("foo", singletonMap("bar", A.create().f1(7).build())))
			.f8(singletonMap("foo", new A[]{A.create().f1(8).build()}))
			.f9(singletonMap("foo", singletonList(new A[]{A.create().f1(9).build()})))
			.build();
		x = t.roundTrip(x, A3.class);
		assertJson(x, "{f1:[{f1:1}],f2:[{f1:2}],f3:[[{f1:3}]],f4:[[{f1:4}]],f5:[[[{f1:5}]]],f6:{foo:{f1:6}},f7:{foo:{bar:{f1:7}}},f8:{foo:[{f1:8}]},f9:{foo:[[{f1:9}]]}}");
	}

	@Bean(sort=true)
	public static class A3 {
		private final A[] f1;

		private final List<A> f2;
		private final List<List<A>> f3;
		private final List<A[]> f4;
		private final List<List<A[]>> f5;

		private final Map<String,A> f6;
		private final Map<String,Map<String,A>> f7;
		private final Map<String,A[]> f8;
		private final Map<String,List<A[]>> f9;

		public A3(Builder b) {
			this.f1 = b.f1;
			this.f2 = b.f2;
			this.f3 = b.f3;
			this.f4 = b.f4;
			this.f5 = b.f5;
			this.f6 = b.f6;
			this.f7 = b.f7;
			this.f8 = b.f8;
			this.f9 = b.f9;
		}

		public static Builder create() {
			return new Builder();
		}

		@Bean(findFluentSetters=true)
		public static class Builder {

			private A[] f1;
			public Builder f1(A[] v) { f1 = v; return this; }

			private List<A> f2;
			public Builder f2(List<A> v) { f2 = v; return this; }

			private List<List<A>> f3;
			public Builder f3(List<List<A>> v) { f3 = v; return this; }

			private List<A[]> f4;
			public Builder f4(List<A[]> v) { f4 = v; return this; }

			private List<List<A[]>> f5;
			public Builder f5(List<List<A[]>> v) { f5 = v; return this; }

			private Map<String,A> f6;
			public Builder f6(Map<String,A> v) { f6 = v; return this; }

			private Map<String,Map<String,A>> f7;
			public Builder f7(Map<String,Map<String,A>> v) { f7 = v; return this; }

			private Map<String,A[]> f8;
			public Builder f8(Map<String,A[]> v) { f8 = v; return this; }

			private Map<String,List<A[]>> f9;
			public Builder f9(Map<String,List<A[]>> v) { f9 = v; return this; }

			public A3 build() {
				return new A3(this);
			}
		}

		public A[] getF1() { return f1; }
		public List<A> getF2() { return f2; }
		public List<List<A>> getF3() { return f3; }
		public List<A[]> getF4() { return f4; }
		public List<List<A[]>> getF5() { return f5; }
		public Map<String,A> getF6() { return f6; }
		public Map<String,Map<String,A>> getF7() { return f7; }
		public Map<String,A[]> getF8() { return f8; }
		public Map<String,List<A[]>> getF9() { return f9; }
	}
}