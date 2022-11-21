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
package org.apache.juneau.rest.annotation;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.http.header.ContentType.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.apache.juneau.urlencoding.annotation.UrlEncoding;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Content_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @Body on parameter
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static class A {
		@RestPut(path="/String")
		public String a(@Content String b) {
			return b;
		}
		@RestPut(path="/Integer")
		public Integer b(@Content Integer b) {
			return b;
		}
		@RestPut(path="/int")
		public Integer c(@Content int b) {
			return b;
		}
		@RestPut(path="/Boolean")
		public Boolean d(@Content Boolean b) {
			return b;
		}
		@RestPut(path="/boolean")
		public Boolean e(@Content boolean b) {
			return b;
		}
		@RestPut(path="/float")
		public float f(@Content float f) {
			return f;
		}
		@RestPut(path="/Float")
		public Float g(@Content Float f) {
			return f;
		}
		@RestPut(path="/Map")
		public TreeMap<String,Integer> h(@Content TreeMap<String,Integer> m) {
			return m;
		}
		@RestPut(path="/enum")
		public TestEnum i(@Content TestEnum e) {
			return e;
		}
		public static class A11 {
			public String f1;
		}
		@RestPut(path="/Bean")
		public A11 j(@Content A11 b) {
			return b;
		}
		@RestPut(path="/InputStream")
		public String k(@Content InputStream b) throws Exception {
			return read(b);
		}
		@RestPut(path="/Reader")
		public String l(@Content Reader b) throws Exception {
			return read(b);
		}
		@RestPut(path="/InputStreamTransform")
		public A14 m(@Content A14 b) throws Exception {
			return b;
		}
		public static class A14 {
			String s;
			public A14(InputStream in) throws Exception { this.s = read(in); }
			@Override public String toString() { return s; }
		}
		@RestPut(path="/ReaderTransform")
		public A15 n(@Content A15 b) throws Exception {
			return b;
		}
		public static class A15 {
			private String s;
			public A15(Reader in) throws Exception { this.s = read(in); }
			@Override public String toString() { return s; }
		}
		@RestPut(path="/StringTransform")
		public A16 o(@Content A16 b) throws Exception { return b; }
		public static class A16 {
			private String s;
			public A16(String s) throws Exception { this.s = s; }
			@Override public String toString() { return s; }
		}
	}

	@Test
	public void a01_onParameters() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);

		a.put("/String", "foo")
			.json()
			.run()
			.assertContent("'foo'");
		// If no Content-Type specified, should be treated as plain-text.
		a.put("/String", "'foo'")
			.run()
			.assertContent("'\\'foo\\''");
		// If Content-Type not matched, should be treated as plain-text.
		a.put("/String", "'foo'").contentType("")
			.run()
			.assertContent("'\\'foo\\''");
		a.put("/String", "'foo'").contentType("text/plain")
			.run()
			.assertContent("'\\'foo\\''");
		a.put("/String?content=foo", null)
			.run()
			.assertContent("'foo'");
		a.put("/String?content=null", null)
			.run()
			.assertContent("null");
		a.put("/String?content=", null)
			.run()
			.assertContent("''");

		a.put("/Integer", "123").json()
			.run()
			.assertContent("123");
		// Integer takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Integer", "123")
			.run()
			.assertContent("123");
		a.put("/Integer?content=123", null)
			.run()
			.assertContent("123");
		a.put("/Integer?content=-123", null)
			.run()
			.assertContent("-123");
		a.put("/Integer?content=null", null)
			.run()
			.assertContent("null");
		a.put("/Integer?content=", null)
			.run()
			.assertContent("null");
		a.put("/Integer?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		a.put("/int", "123").json()
			.run()
			.assertContent("123");
		a.put("/int", "123")
			.run()
			.assertContent("123"); // Uses part parser.
		a.put("/int?content=123", null)
			.run()
			.assertContent("123");
		a.put("/int?content=-123", null)
			.run()
			.assertContent("-123");
		a.put("/int?content=null", null)
			.run()
			.assertContent("0");
		a.put("/int?content=", null)
			.run()
			.assertContent("0");
		a.put("/int?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		a.put("/Boolean", "true").json()
			.run()
			.assertContent("true");
		// Boolean takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Boolean", "true")
			.run()
			.assertContent("true");
		a.put("/Boolean?content=true", null)
			.run()
			.assertContent("true");
		a.put("/Boolean?content=false", null)
			.run()
			.assertContent("false");
		a.put("/Boolean?content=null", null)
			.run()
			.assertContent("null");
		a.put("/Boolean?content=", null)
			.run()
			.assertContent("null");
		a.put("/Boolean?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		a.put("/boolean", "true").json()
			.run()
			.assertContent("true");
		a.put("/boolean", "true")
			.run()
			.assertContent("true"); // Uses part parser.
		a.put("/boolean?content=true", null)
			.run()
			.assertContent("true");
		a.put("/boolean?content=false", null)
			.run()
			.assertContent("false");
		a.put("/boolean?content=null", null)
			.run()
			.assertContent("false");
		a.put("/boolean?content=", null)
			.run()
			.assertContent("false");
		a.put("/boolean?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		a.put("/float", "1.23").json()
			.run()
			.assertContent("1.23");
		a.put("/float", "1.23")
			.run()
			.assertContent("1.23");  // Uses part parser.
		a.put("/float?content=1.23", null)
			.run()
			.assertContent("1.23");
		a.put("/float?content=-1.23", null)
			.run()
			.assertContent("-1.23");
		a.put("/float?content=null", null)
			.run()
			.assertContent("0.0");
		a.put("/float?content=", null)
			.run()
			.assertContent("0.0");
		a.put("/float?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		a.put("/Float", "1.23").json()
			.run()
			.assertContent("1.23");
		// Float takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Float", "1.23")
			.run()
			.assertContent("1.23");
		a.put("/Float?content=1.23", null)
			.run()
			.assertContent("1.23");
		a.put("/Float?content=-1.23", null)
			.run()
			.assertContent("-1.23");
		a.put("/Float?content=null", null)
			.run()
			.assertContent("null");
		a.put("/Float?content=", null)
			.run()
			.assertContent("null");
		a.put("/Float?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		a.put("/Map", "{foo:123}", APPLICATION_JSON)
			.run()
			.assertContent("{foo:123}");
		a.put("/Map", "(foo=123)", TEXT_OPENAPI)
			.run()
			.assertStatus(415);
		a.put("/Map?content=(foo=123)", null)
			.run()
			.assertContent("{foo:123}");
		a.put("/Map?content=()", null)
			.run()
			.assertContent("{}");
		a.put("/Map?content=null", null)
			.run()
			.assertContent("null");
		a.put("/Map?content=", null)
			.run()
			.assertContent("null");
		a.put("/Map?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		a.put("/enum", "'ONE'", APPLICATION_JSON)
			.run()
			.assertContent("'ONE'");
		a.put("/enum", "ONE")
			.run()
			.assertContent("'ONE'");
		a.put("/enum?content=ONE", null)
			.run()
			.assertContent("'ONE'");
		a.put("/enum?content=TWO", null)
			.run()
			.assertContent("'TWO'");
		a.put("/enum?content=null", null)
			.run()
			.assertContent("null");
		a.put("/enum?content=", null)
			.run()
			.assertContent("null");
		a.put("/enum?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		a.put("/Bean", "{f1:'a'}", APPLICATION_JSON)
			.run()
			.assertContent("{f1:'a'}");
		a.put("/Bean", "(f1=a)", TEXT_OPENAPI)
			.run()
			.assertStatus(415);
		a.put("/Bean?content=(f1=a)", null)
			.run()
			.assertContent("{f1:'a'}");
		a.put("/Bean?content=()", null)
			.run()
			.assertContent("{}");
		a.put("/Bean?content=null", null)
			.run()
			.assertContent("null");
		a.put("/Bean?content=", null)
			.run()
			.assertContent("null");
		a.put("/Bean?content=bad&noTrace=true", null)
			.run()
			.assertStatus(400);

		// Content-Type should always be ignored.
		a.put("/InputStream", "'a'", APPLICATION_JSON)
			.run()
			.assertContent("'\\'a\\''");
		a.put("/InputStream", "'a'")
			.run()
			.assertContent("'\\'a\\''");
		a.put("/InputStream?content=a", null)
			.run()
			.assertContent("'a'");
		a.put("/InputStream?content=null", null)
			.run()
			.assertContent("'null'");
		a.put("/InputStream?content=", null)
			.run()
			.assertContent("''");

		// Content-Type should always be ignored.
		a.put("/Reader", "'a'", APPLICATION_JSON)
			.run()
			.assertContent("'\\'a\\''");
		a.put("/Reader", "'a'")
			.run()
			.assertContent("'\\'a\\''");
		a.put("/Reader?content=a", null)
			.run()
			.assertContent("'a'");
		a.put("/Reader?content=null", null)
			.run()
			.assertContent("'null'");
		a.put("/Reader?content=", null)
			.run()
			.assertContent("''");

		// It's not currently possible to pass in a &body parameter for InputStream/Reader transforms.

		// Input stream transform requests must not specify Content-Type or else gets resolved as POJO.
		a.put("/InputStreamTransform?noTrace=true", "'a'", APPLICATION_JSON)
			.run()
			.assertContent().isContains("Bad Request");
		a.put("/InputStreamTransform", "'a'")
			.run()
			.assertContent("'\\'a\\''");

		// Reader transform requests must not specify Content-Type or else gets resolved as POJO.
		a.put("/ReaderTransform?noTrace=true", "'a'", APPLICATION_JSON)
			.run()
			.assertContent().isContains("Bad Request");
		a.put("/ReaderTransform", "'a'")
			.run()
			.assertContent("'\\'a\\''");

		// When Content-Type specified and matched, treated as a parsed POJO.
		a.put("/StringTransform", "'a'", APPLICATION_JSON)
			.run()
			.assertContent("'a'");
		// When Content-Type not matched, treated as plain text.
		a.put("/StringTransform", "'a'")
			.run()
			.assertContent("'\\'a\\''");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Body on POJO
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class B {
		@RestPut(path="/StringTransform")
		public B1 a(B1 b) {
			return b;
		}
		@Content
		public static class B1 {
			private String val;
			public B1(String val) { this.val = val; }
			@Override public String toString() { return val; }
		}
		@RestPut(path="/Bean")
		public B2 b(B2 b) {
			return b;
		}
		@Content
		public static class B2 {
			public String f1;
		}
		@RestPut(path="/BeanList")
		public B3 c(B3 b) {
			return b;
		}
		@SuppressWarnings("serial")
		@Content
		public static class B3 extends LinkedList<B2> {}
		@RestPut(path="/InputStreamTransform")
		public B4 d(B4 b) throws Exception {
			return b;
		}
		@Content
		public static class B4 {
			String s;
			public B4(InputStream in) throws Exception { this.s = read(in); }
			@Override public String toString() { return s; }
		}
		@RestPut(path="/ReaderTransform")
		public B5 e(B5 b) throws Exception {
			return b;
		}
		@Content
		public static class B5 {
			private String s;
			public B5(Reader in) throws Exception { this.s = read(in); }
			@Override public String toString() { return s; }
		}
	}

	@Test
	public void b01_onPojos() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.put("/StringTransform", "'foo'", APPLICATION_JSON)
			.run()
			.assertContent("'foo'");
		// When Content-Type not matched, treated as plain text.
		b.put("/StringTransform", "'foo'")
			.run()
			.assertContent("'\\'foo\\''");
		b.put("/Bean", "{f1:'a'}", APPLICATION_JSON)
			.run()
			.assertContent("{f1:'a'}");
		b.put("/Bean", "(f1=a)", TEXT_OPENAPI)
			.run()
			.assertStatus(415);
		b.put("/BeanList", "[{f1:'a'}]", APPLICATION_JSON)
			.run()
			.assertContent("[{f1:'a'}]");
		b.put("/BeanList", "(f1=a)", TEXT_OPENAPI)
			.run()
			.assertStatus(415);
		b.put("/InputStreamTransform", "a")
			.run()
			.assertContent("'a'");
		// When Content-Type matched, treated as parsed POJO.
		b.put("/InputStreamTransform?noTrace=true", "a", APPLICATION_JSON)
			.run()
			.assertContent().isContains("Bad Request");
		b.put("/ReaderTransform", "a")
			.run()
			.assertContent("'a'");
		// When Content-Type matched, treated as parsed POJO.
		b.put("/ReaderTransform?noTrace=true", "a", APPLICATION_JSON)
			.run()
			.assertContent().isContains("Bad Request");
	}

	//------------------------------------------------------------------------------------------------------------------
	// No serializers or parsers needed when using only streams and readers.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestPut(path="/String")
		public Reader a(@Content Reader b) throws Exception {
			return b;
		}
		@RestPut(path="/InputStream")
		public InputStream b(@Content InputStream b) throws Exception {
			return b;
		}
		@RestPut(path="/Reader")
		public Reader c(@Content Reader b) throws Exception {
			return b;
		}
		@RestPut(path="/StringTransform")
		public Reader d(@Content D1 b) throws Exception {
			return reader(b.toString());
		}
		public static class D1 {
			private String s;
			public D1(String in) throws Exception { this.s = in; }
			@Override public String toString() { return s; }
		}
		@RestPut(path="/InputStreamTransform")
		public Reader e(@Content D2 b) throws Exception {
			return reader(b.toString());
		}
		public static class D2 {
			String s;
			public D2(InputStream in) throws Exception { this.s = read(in); }
			@Override public String toString() { return s; }
		}
		@RestPut(path="/ReaderTransform")
		public Reader f(@Content D3 b) throws Exception {
			return reader(b.toString());
		}
		public static class D3 {
			private String s;
			public D3(Reader in) throws Exception{ this.s = read(in); }
			@Override public String toString() { return s; }
		}
		@RestPut(path="/StringTransformBodyOnPojo")
		public Reader g(D4 b) throws Exception {
			return reader(b.toString());
		}
		@Content
		public static class D4 {
			private String s;
			public D4(String in) throws Exception { this.s = in; }
			@Override public String toString() { return s; }
		}
		@RestPut(path="/InputStreamTransformBodyOnPojo")
		public Reader h(D5 b) throws Exception {
			return reader(b.toString());
		}
		@Content
		public static class D5 {
			String s;
			public D5(InputStream in) throws Exception { this.s = read(in); }
			@Override public String toString() { return s; }
		}

		@RestPut(path="/ReaderTransformBodyOnPojo")
		public Reader i(D6 b) throws Exception {
			return reader(b.toString());
		}
		@Content
		public static class D6 {
			private String s;
			public D6(Reader in) throws Exception{ this.s = read(in); }
			@Override public String toString() { return s; }
		}
	}

	@Test
	public void d01_noMediaTypesOnStreams() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		d.put("/String", "a")
			.run()
			.assertContent("a");
		d.put("/String", "a", APPLICATION_JSON)
			.run()
			.assertContent("a");
		d.put("/InputStream", "a")
			.run()
			.assertContent("a");
		d.put("/InputStream", "a", APPLICATION_JSON)
			.run()
			.assertContent("a");
		d.put("/Reader", "a")
			.run()
			.assertContent("a");
		d.put("/Reader", "a", APPLICATION_JSON)
			.run()
			.assertContent("a");
		d.put("/StringTransform", "a")
			.run()
			.assertContent("a");
		d.put("/StringTransform?noTrace=true", "a", APPLICATION_JSON)
			.run()
			.assertStatus(415);
		d.put("/InputStreamTransform", "a")
			.run()
			.assertContent("a");
		d.put("/InputStreamTransform", "a", APPLICATION_JSON)
			.run()
			.assertContent("a");
		d.put("/ReaderTransform", "a")
			.run()
			.assertContent("a");
		d.put("/ReaderTransform", "a", APPLICATION_JSON)
			.run()
			.assertContent("a");
		d.put("/StringTransformBodyOnPojo", "a")
			.run()
			.assertContent("a");
		d.put("/StringTransformBodyOnPojo?noTrace=true", "a", APPLICATION_JSON)
			.run()
			.assertStatus(415);
		d.put("/InputStreamTransformBodyOnPojo", "a")
			.run()
			.assertContent("a");
		d.put("/InputStreamTransformBodyOnPojo", "a", APPLICATION_JSON)
			.run()
			.assertContent("a");
		d.put("/ReaderTransformBodyOnPojo", "a")
			.run()
			.assertContent("a");
		d.put("/ReaderTransformBodyOnPojo", "a", APPLICATION_JSON)
			.run()
			.assertContent("a");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Complex POJOs
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class E {
		@RestPut(path="/B")
		public XBeans.XB a(@Content XBeans.XB b) {
			return b;
		}
		@RestPut(path="/C")
		public XBeans.XC b(@Content XBeans.XC c) {
			return c;
		}
	}

	@Test
	public void e01_complexPojos() throws Exception {
		RestClient e = MockRestClient.build(E.class);
		String expected;

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/B", Json5Serializer.DEFAULT.toString(XBeans.XB.INSTANCE), APPLICATION_JSON)
			.run()
			.assertContent(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/B?content=" + UonSerializer.DEFAULT.serialize(XBeans.XB.INSTANCE), "a")
			.run()
			.assertContent(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/C", Json5Serializer.DEFAULT.toString(XBeans.XB.INSTANCE), APPLICATION_JSON)
			.run()
			.assertContent(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/C?content=" + UonSerializer.DEFAULT.serialize(XBeans.XB.INSTANCE), "a")
			.run()
			.assertContent(expected);
	}

	@Rest(serializers=Json5Serializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	@Bean(on="A,B,C",sort=true)
	@UrlEncoding(on="C",expandedParams=true)
	public static class E2 {
		@RestPut(path="/B")
		public XBeans.XE a(@Content XBeans.XE b) {
			return b;
		}
		@RestPut(path="/C")
		public XBeans.XF b(@Content XBeans.XF c) {
			return c;
		}
	}

	@Test
	public void e02_complexPojos() throws Exception {
		RestClient e2 = MockRestClient.build(E2.class);
		String expected;

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/B", Json5Serializer.DEFAULT.copy().applyAnnotations(XBeans.Annotations.class).build().toString(XBeans.XE.INSTANCE), APPLICATION_JSON)
			.run()
			.assertContent(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/B?content=" + UonSerializer.DEFAULT.copy().applyAnnotations(XBeans.Annotations.class).build().serialize(XBeans.XE.INSTANCE), "a")
			.run()
			.assertContent(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/C", Json5Serializer.DEFAULT.copy().applyAnnotations(XBeans.Annotations.class).build().toString(XBeans.XE.INSTANCE), APPLICATION_JSON)
			.run()
			.assertContent(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/C?content=" + UonSerializer.DEFAULT.copy().applyAnnotations(XBeans.Annotations.class).build().serialize(XBeans.XE.INSTANCE), "a")
			.run()
			.assertContent(expected);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Form POSTS with @Body parameter
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class,parsers=JsonParser.class)
	public static class F {
		@RestPost(path="/*")
		public Reader a(
				@Content F1 bean,
				@HasQuery("p1") boolean hqp1, @HasQuery("p2") boolean hqp2,
				@Query("p1") String qp1, @Query("p2") int qp2) throws Exception {
			return reader("bean=["+Json5Serializer.DEFAULT.toString(bean)+"],qp1=["+qp1+"],qp2=["+qp2+"],hqp1=["+hqp1+"],hqp2=["+hqp2+"]");
		}
		public static class F1 {
			public String p1;
			public int p2;
		}
	}

	@Test
	public void f01_formPostAsContent() throws Exception {
		RestClient f = MockRestClient.build(F.class);
		f.post("/", "{p1:'p1',p2:2}", APPLICATION_JSON)
			.run()
			.assertContent("bean=[{p1:'p1',p2:2}],qp1=[null],qp2=[0],hqp1=[false],hqp2=[false]");
		f.post("/", "{}", APPLICATION_JSON)
			.run()
			.assertContent("bean=[{p2:0}],qp1=[null],qp2=[0],hqp1=[false],hqp2=[false]");
		f.post("?p1=p3&p2=4", "{p1:'p1',p2:2}", APPLICATION_JSON)
			.run()
			.assertContent("bean=[{p1:'p1',p2:2}],qp1=[p3],qp2=[4],hqp1=[true],hqp2=[true]");
		f.post("?p1=p3&p2=4", "{}", APPLICATION_JSON)
			.run()
			.assertContent("bean=[{p2:0}],qp1=[p3],qp2=[4],hqp1=[true],hqp2=[true]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using @UrlEncoding(expandedParams=true) annotation on bean.
	// A simple round-trip test to verify that both serializing and parsing works.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=UrlEncodingSerializer.class,parsers=UrlEncodingParser.class)
	public static class G {
		@RestPost(path="/")
		public XBeans.XC a(@Content XBeans.XC content) throws Exception {
			return content;
		}
	}

	@Test
	public void g01_multiPartParameterKeysOnCollections() throws Exception {
		RestClient g = MockRestClient.build(G.class);
		String in = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=@(e,f)&f05=@(g,h)"
			+ "&f06=@(i,j)&f06=@(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=b,b=2,c=false)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=b,b=2,c=false)"
			+ "&f09=@((a=a,b=1,c=true))&f09=@((a=b,b=2,c=false))"
			+ "&f10=@((a=a,b=1,c=true))&f10=@((a=b,b=2,c=false))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=@(e,f)&f15=@(g,h)"
			+ "&f16=@(i,j)&f16=@(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=b,b=2,c=false)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=b,b=2,c=false)"
			+ "&f19=@((a=a,b=1,c=true))&f19=@((a=b,b=2,c=false))"
			+ "&f20=@((a=a,b=1,c=true))&f20=@((a=b,b=2,c=false))";
		g.post("/", in, APPLICATION_FORM_URLENCODED)
			.run()
			.assertContent(in);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using URLENC_expandedParams property.
	// A simple round-trip test to verify that both serializing and parsing works.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=UrlEncodingSerializer.class,parsers=UrlEncodingParser.class)
	public static class H {
		@RestPost(path="/")
		@UrlEncodingConfig(expandedParams="true")
		public XBeans.XB a(@Content XBeans.XB content) throws Exception {
			return content;
		}
	}

	@Test
	public void h01_multiPartParameterKeysOnCollections_usingExpandedParams() throws Exception {
		RestClient h = MockRestClient.build(H.class);
		String in = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=@(e,f)&f05=@(g,h)"
			+ "&f06=@(i,j)&f06=@(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=b,b=2,c=false)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=b,b=2,c=false)"
			+ "&f09=@((a=a,b=1,c=true))&f09=@((a=b,b=2,c=false))"
			+ "&f10=@((a=a,b=1,c=true))&f10=@((a=b,b=2,c=false))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=@(e,f)&f15=@(g,h)"
			+ "&f16=@(i,j)&f16=@(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=b,b=2,c=false)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=b,b=2,c=false)"
			+ "&f19=@((a=a,b=1,c=true))&f19=@((a=b,b=2,c=false))"
			+ "&f20=@((a=a,b=1,c=true))&f20=@((a=b,b=2,c=false))";
		h.post("/", in, APPLICATION_FORM_URLENCODED)
			.run()
			.assertContent(in);
	}

	@Rest(serializers=UrlEncodingSerializer.class,parsers=UrlEncodingParser.class)
	@Bean(on="A,B,C",sort=true)
	@UrlEncoding(on="C",expandedParams=true)
	public static class H2 {
		@RestPost(path="/")
		@UrlEncodingConfig(expandedParams="true")
		public XBeans.XE a(@Content XBeans.XE content) throws Exception {
			return content;
		}
	}

	@Test
	public void h02_multiPartParameterKeysOnCollections_usingExpandedParams() throws Exception {
		RestClient h2 = MockRestClient.build(H2.class);
		String in = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=@(e,f)&f05=@(g,h)"
			+ "&f06=@(i,j)&f06=@(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=b,b=2,c=false)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=b,b=2,c=false)"
			+ "&f09=@((a=a,b=1,c=true))&f09=@((a=b,b=2,c=false))"
			+ "&f10=@((a=a,b=1,c=true))&f10=@((a=b,b=2,c=false))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=@(e,f)&f15=@(g,h)"
			+ "&f16=@(i,j)&f16=@(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=b,b=2,c=false)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=b,b=2,c=false)"
			+ "&f19=@((a=a,b=1,c=true))&f19=@((a=b,b=2,c=false))"
			+ "&f20=@((a=a,b=1,c=true))&f20=@((a=b,b=2,c=false))";
		h2.post("/", in, APPLICATION_FORM_URLENCODED)
			.run()
			.assertContent(in);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test behavior of @Body(required=true).
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class,parsers=JsonParser.class)
	public static class I {
		@RestPost
		public XBeans.XB a(@Content @Schema(r=true) XBeans.XB content) throws Exception {
			return content;
		}
		@RestPost
		@Bean(on="A,B,C",sort=true)
		@UrlEncoding(on="C",expandedParams=true)
		public XBeans.XE b(@Content @Schema(r=true) XBeans.XE content) throws Exception {
			return content;
		}
	}

	@Test
	public void i01_required() throws Exception {
		RestClient i = MockRestClient.buildLax(I.class);

		i.post("/a", "", APPLICATION_JSON)
			.run()
			.assertStatus(400)
			.assertContent().isContains("Required value not provided.");
		i.post("/a", "{}", APPLICATION_JSON)
			.run()
			.assertStatus(200);

		i.post("/b", "", APPLICATION_JSON)
			.run()
			.assertStatus(400)
			.assertContent().isContains("Required value not provided.");
		i.post("/b", "{}", APPLICATION_JSON)
			.run()
			.assertStatus(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional body parameter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class,parsers=JsonParser.class)
	public static class J {
		@RestPost
		public Object a(@Content Optional<Integer> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestPost
		public Object b(@Content Optional<ABean> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestPost
		public Object c(@Content Optional<List<ABean>> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestPost
		public Object d(@Content List<Optional<ABean>> body) throws Exception {
			return body;
		}
	}

	@Test
	public void j01_optionalParams() throws Exception {
		RestClient j = MockRestClient.buildJson(J.class);
		j.post("/a", 123)
			.run()
			.assertStatus(200)
			.assertContent("123");
		j.post("/a", null)
			.run()
			.assertStatus(200)
			.assertContent("null");

		j.post("/b", ABean.get())
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		j.post("/b", null)
			.run()
			.assertStatus(200)
			.assertContent("null");

		String body1 = Json5.of(list(ABean.get()));
		j.post("/c", body1, APPLICATION_JSON)
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		j.post("/c", null)
			.run()
			.assertStatus(200)
			.assertContent("null");

		String body2 = Json5.of(list(optional(ABean.get())));
		j.post("/d", body2, APPLICATION_JSON)
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		j.post("/d", null)
			.run()
			.assertStatus(200)
			.assertContent("null");
	}
}
