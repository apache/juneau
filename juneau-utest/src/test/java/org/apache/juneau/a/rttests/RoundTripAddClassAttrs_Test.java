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

import static org.apache.juneau.common.internal.Utils.*;
import static org.junit.jupiter.api.Assertions.*;
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
class RoundTripAddClassAttrs_Test extends SimpleTestBase {

	private static RoundTrip_Tester[] TESTERS = {
		tester(1, "JsonSerializer.DEFAULT/JsonParser.DEFAULT")
			.serializer(JsonSerializer.create().addBeanTypes().addRootType())
			.parser(JsonParser.create().disableInterfaceProxies())
			.build(),
		tester(2, "JsonSerializer.DEFAULT_SIMPLE/JsonParser.DEFAULT")
			.serializer(JsonSerializer.create().json5().addBeanTypes().addRootType())
			.parser(JsonParser.create().disableInterfaceProxies())
			.build(),
		tester(3, "JsonSerializer.DEFAULT_SQ/JsonParser.DEFAULT")
			.serializer(JsonSerializer.create().json5().addBeanTypes().addRootType())
			.parser(JsonParser.create().disableInterfaceProxies())
			.build(),
		tester(4, "XmlSerializer.DEFAULT/XmlParser.DEFAULT")
			.serializer(XmlSerializer.create().addBeanTypes().addRootType())
			.parser(XmlParser.create().disableInterfaceProxies())
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		tester(5, "HtmlSerializer.DEFAULT/HtmlParser.DEFAULT")
			.serializer(HtmlSerializer.create().addBeanTypes().addRootType())
			.parser(HtmlParser.create().disableInterfaceProxies())
			.validateXmlWhitespace()
			.build(),
		tester(6, "UonSerializer.DEFAULT_ENCODING/UonParser.DEFAULT_DECODING")
			.serializer(UonSerializer.create().encoding().addBeanTypes().addRootType())
			.parser(UonParser.create().decoding().disableInterfaceProxies())
			.build(),
		tester(7, "UonSerializer.DEFAULT/UonParser.DEFAULT")
			.serializer(UonSerializer.create().addBeanTypes().addRootType())
			.parser(UonParser.create().disableInterfaceProxies())
			.build(),
		tester(8, "UrlEncodingSerializer.DEFAULT/UrlEncodingParser.DEFAULT")
			.serializer(UrlEncodingSerializer.create().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create().disableInterfaceProxies())
			.build(),
		tester(9, "MsgPackSerializer.DEFAULT/MsgPackParser.DEFAULT")
			.serializer(MsgPackSerializer.create().addBeanTypes().addRootType())
			.parser(MsgPackParser.create().disableInterfaceProxies())
			.build()
	};

	static RoundTrip_Tester[]  testers() {
		return TESTERS;
	}

	protected static RoundTrip_Tester.Builder tester(int index, String label) {
		return RoundTrip_Tester.create(index, label).dictionary(A.class, B.class, C.class, D.class, E.class, F.class);
	}

	//====================================================================================================
	// testBean
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_bean(RoundTrip_Tester t) throws Exception {
		var x = new A("foo");

		x = t.roundTrip(x, A.class);
		assertEquals("foo", x.getF1());

		var xa = t.roundTrip(x, AA.class);
		assertEquals("foo", xa.getF1());

		var xi = t.roundTrip(x, IA.class);
		assertEquals("foo", xi.getF1());

		x = t.roundTrip(x, Object.class);
		assertEquals("foo", x.getF1());
	}

	public interface IA {
		String getF1();
		void setF1(String f1);
	}

	public abstract static class AA implements IA {}

	@Bean(typeName="A")
	public static class A extends AA {
		private String f1;

		@Override /* AA */ public String getF1() { return f1; }
		@Override /* AA */ public void setF1(String v) { f1 = v; }

		public A() {}
		public A(String f1) {
			this.f1 = f1;
		}
	}

	//====================================================================================================
	// testBeanArray
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_beanArray(RoundTrip_Tester t) throws Exception {
		var x = a(new A("foo"));

		x = t.roundTrip(x, A[].class);
		assertEquals("foo", x[0].getF1());

		var xa = (AA[])t.roundTrip(x, AA[].class);
		assertEquals("foo", xa[0].getF1());

		var xi = (IA[])t.roundTrip(x, IA[].class);
		assertEquals("foo", xi[0].getF1());
	}

	//====================================================================================================
	// testBeanWithBeanProps
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_beanWithBeanProps(RoundTrip_Tester t) throws Exception {
		var x = new B("foo");
		x = t.roundTrip(x, B.class);
		assertBean(x, "f2a{f1},f2b{f1},f2c{f1},f2d{f1}", "{foo},{foo},{foo},{foo}");

		x = t.roundTrip(x, Object.class);
		assertBean(x, "f2a{f1},f2b{f1},f2c{f1},f2d{f1}", "{foo},{foo},{foo},{foo}");
	}

	@Bean(typeName="B")
	public static class B {
		public A f2a;
		public AA f2b;
		public IA f2c;
		public Object f2d;
		public B() {}
		public B(String f1) {
			f2d = f2c = f2b = f2a = new A(f1);
		}
	}

	//====================================================================================================
	// testMapsWithTypeParams - Maps with type parameters should not have class attributes on entries.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a04_mapsWithTypeParams(RoundTrip_Tester t) throws Exception {
		var x = new C("foo");
		x = t.roundTrip(x, C.class);
		assertBean(x, "f3a{foo{f1}},f3b{foo{f1}},f3c{foo{f1}},f3d{foo{f1}}", "{{foo}},{{foo}},{{foo}},{{foo}}");

		x = t.roundTrip(x, Object.class);
		assertBean(x, "f3a{foo{f1}},f3b{foo{f1}},f3c{foo{f1}},f3d{foo{f1}}", "{{foo}},{{foo}},{{foo}},{{foo}}");
	}

	@Bean(typeName="C")
	public static class C {
		public Map<String,A> f3a = new HashMap<>();
		public Map<String,A> f3b = new HashMap<>();
		public Map<String,A> f3c = new HashMap<>();
		public Map<String,A> f3d = new HashMap<>();

		public C(){}
		public C(String f1) {
			var b = new A(f1);
			f3a.put("foo", b);
			f3b.put("foo", b);
			f3c.put("foo", b);
			f3d.put("foo", b);
		}
	}

	//====================================================================================================
	// testMapsWithoutTypeParams - Maps without type parameters should have class attributes on entries.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a05_mapsWithoutTypeParams(RoundTrip_Tester t) throws Exception {
		var x = new D("foo");
		x = t.roundTrip(x, D.class);
		assertBean(x, "f4a{0{f1}},f4b{0{f1}},f4c{0{f1}},f4d{0{f1}}", "{{foo}},{{foo}},{{foo}},{{foo}}");

		x = t.roundTrip(x, Object.class);
		assertBean(x, "f4a{0{f1}},f4b{0{f1}},f4c{0{f1}},f4d{0{f1}}", "{{foo}},{{foo}},{{foo}},{{foo}}");
	}

	@Bean(typeName="D")
	public static class D {
		public A[] f4a;
		public AA[] f4b;
		public IA[] f4c;
		public Object[] f4d;

		public D(){}
		public D(String f1) {
			var b = new A(f1);
			f4a = new A[]{b};
			f4b = new AA[]{b};
			f4c = new IA[]{b};
			f4d = new Object[]{b};
		}
	}

	//====================================================================================================
	// testBeanWithListProps
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a06_beanWithListProps(RoundTrip_Tester t) throws Exception {
		var x = new E("foo");
		x = t.roundTrip(x, E.class);
		assertBean(x, "f5a{0{f1}},f5b{0{f1}},f5c{0{f1}},f5d{0{f1}}", "{{foo}},{{foo}},{{foo}},{{foo}}");

		x = t.roundTrip(x, Object.class);
		assertBean(x, "f5a{0{f1}},f5b{0{f1}},f5c{0{f1}},f5d{0{f1}}", "{{foo}},{{foo}},{{foo}},{{foo}}");
	}

	@Bean(typeName="E")
	public static class E {
		public List<A> f5a = new LinkedList<>();
		public List<AA> f5b = new LinkedList<>();
		public List<IA> f5c = new LinkedList<>();
		public List<Object> f5d = new LinkedList<>();

		public E(){}
		public E(String f1) {
			var b = new A(f1);
			f5a.add(b);
			f5b.add(b);
			f5c.add(b);
			f5d.add(b);
		}
	}

	//====================================================================================================
	// testBeanWithListOfArraysProps
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a07_beanWithListOfArraysProps(RoundTrip_Tester t) throws Exception {
		var x = new F("foo");
		x = t.roundTrip(x, F.class);
		assertBean(x, "f6a{0{0{f1}}},f6b{0{0{f1}}},f6c{0{0{f1}}},f6d{0{0{f1}}}", "{{{foo}}},{{{foo}}},{{{foo}}},{{{foo}}}");

		x = t.roundTrip(x, Object.class);
		assertBean(x, "f6a{0{0{f1}}},f6b{0{0{f1}}},f6c{0{0{f1}}},f6d{0{0{f1}}}", "{{{foo}}},{{{foo}}},{{{foo}}},{{{foo}}}");
	}

	@Bean(typeName="F")
	public static class F {
		public List<A[]> f6a = new LinkedList<>();
		public List<AA[]> f6b = new LinkedList<>();
		public List<IA[]> f6c = new LinkedList<>();
		public List<Object[]> f6d = new LinkedList<>();

		public F(){}
		public F(String f1) {
			var b = a(new A(f1));
			f6a.add(b);
			f6b.add(b);
			f6c.add(b);
			f6d.add(b);
		}
	}
}