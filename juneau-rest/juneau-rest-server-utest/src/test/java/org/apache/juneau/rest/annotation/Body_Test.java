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

import static org.apache.juneau.http.HttpMethod.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.HasQuery;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.apache.juneau.urlencoding.annotation.UrlEncoding;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Body_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @Body on parameter
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static class A {
		@RestMethod(name=PUT, path="/String")
		public String a(@Body String b) {
			return b;
		}
		@RestMethod(name=PUT, path="/Integer")
		public Integer b(@Body Integer b) {
			return b;
		}
		@RestMethod(name=PUT, path="/int")
		public Integer c(@Body int b) {
			return b;
		}
		@RestMethod(name=PUT, path="/Boolean")
		public Boolean d(@Body Boolean b) {
			return b;
		}
		@RestMethod(name=PUT, path="/boolean")
		public Boolean e(@Body boolean b) {
			return b;
		}
		@RestMethod(name=PUT, path="/float")
		public float f(@Body float f) {
			return f;
		}
		@RestMethod(name=PUT, path="/Float")
		public Float g(@Body Float f) {
			return f;
		}
		@RestMethod(name=PUT, path="/Map")
		public TreeMap<String,Integer> h(@Body TreeMap<String,Integer> m) {
			return m;
		}
		@RestMethod(name=PUT, path="/enum")
		public TestEnum i(@Body TestEnum e) {
			return e;
		}
		public static class A11 {
			public String f1;
		}
		@RestMethod(name=PUT, path="/Bean")
		public A11 j(@Body A11 b) {
			return b;
		}
		@RestMethod(name=PUT, path="/InputStream")
		public String k(@Body InputStream b) throws Exception {
			return IOUtils.read(b);
		}
		@RestMethod(name=PUT, path="/Reader")
		public String l(@Body Reader b) throws Exception {
			return IOUtils.read(b);
		}
		@RestMethod(name=PUT, path="/InputStreamTransform")
		public A14 m(@Body A14 b) throws Exception {
			return b;
		}
		public static class A14 {
			String s;
			public A14(InputStream in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/ReaderTransform")
		public A15 n(@Body A15 b) throws Exception {
			return b;
		}
		public static class A15 {
			private String s;
			public A15(Reader in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/StringTransform")
		public A16 o(@Body A16 b) throws Exception { return b; }
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
			.assertBody().is("'foo'");
		// If no Content-Type specified, should be treated as plain-text.
		a.put("/String", "'foo'")
			.run()
			.assertBody().is("'\\'foo\\''");
		// If Content-Type not matched, should be treated as plain-text.
		a.put("/String", "'foo'").contentType("")
			.run()
			.assertBody().is("'\\'foo\\''");
		a.put("/String", "'foo'").contentType("text/plain")
			.run()
			.assertBody().is("'\\'foo\\''");
		a.put("/String?body=foo", null)
			.run()
			.assertBody().is("'foo'");
		a.put("/String?body=null", null)
			.run()
			.assertBody().is("null");
		a.put("/String?body=", null)
			.run()
			.assertBody().is("''");

		a.put("/Integer", "123").json()
			.run()
			.assertBody().is("123");
		// Integer takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Integer", "123")
			.run()
			.assertBody().is("123");
		a.put("/Integer?body=123", null)
			.run()
			.assertBody().is("123");
		a.put("/Integer?body=-123", null)
			.run()
			.assertBody().is("-123");
		a.put("/Integer?body=null", null)
			.run()
			.assertBody().is("null");
		a.put("/Integer?body=", null)
			.run()
			.assertBody().is("null");
		a.put("/Integer?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		a.put("/int", "123").json()
			.run()
			.assertBody().is("123");
		a.put("/int", "123")
			.run()
			.assertBody().is("123"); // Uses part parser.
		a.put("/int?body=123", null)
			.run()
			.assertBody().is("123");
		a.put("/int?body=-123", null)
			.run()
			.assertBody().is("-123");
		a.put("/int?body=null", null)
			.run()
			.assertBody().is("0");
		a.put("/int?body=", null)
			.run()
			.assertBody().is("0");
		a.put("/int?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		a.put("/Boolean", "true").json()
			.run()
			.assertBody().is("true");
		// Boolean takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Boolean", "true")
			.run()
			.assertBody().is("true");
		a.put("/Boolean?body=true", null)
			.run()
			.assertBody().is("true");
		a.put("/Boolean?body=false", null)
			.run()
			.assertBody().is("false");
		a.put("/Boolean?body=null", null)
			.run()
			.assertBody().is("null");
		a.put("/Boolean?body=", null)
			.run()
			.assertBody().is("null");
		a.put("/Boolean?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		a.put("/boolean", "true").json()
			.run()
			.assertBody().is("true");
		a.put("/boolean", "true")
			.run()
			.assertBody().is("true"); // Uses part parser.
		a.put("/boolean?body=true", null)
			.run()
			.assertBody().is("true");
		a.put("/boolean?body=false", null)
			.run()
			.assertBody().is("false");
		a.put("/boolean?body=null", null)
			.run()
			.assertBody().is("false");
		a.put("/boolean?body=", null)
			.run()
			.assertBody().is("false");
		a.put("/boolean?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		a.put("/float", "1.23").json()
			.run()
			.assertBody().is("1.23");
		a.put("/float", "1.23")
			.run()
			.assertBody().is("1.23");  // Uses part parser.
		a.put("/float?body=1.23", null)
			.run()
			.assertBody().is("1.23");
		a.put("/float?body=-1.23", null)
			.run()
			.assertBody().is("-1.23");
		a.put("/float?body=null", null)
			.run()
			.assertBody().is("0.0");
		a.put("/float?body=", null)
			.run()
			.assertBody().is("0.0");
		a.put("/float?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		a.put("/Float", "1.23").json()
			.run()
			.assertBody().is("1.23");
		// Float takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Float", "1.23")
			.run()
			.assertBody().is("1.23");
		a.put("/Float?body=1.23", null)
			.run()
			.assertBody().is("1.23");
		a.put("/Float?body=-1.23", null)
			.run()
			.assertBody().is("-1.23");
		a.put("/Float?body=null", null)
			.run()
			.assertBody().is("null");
		a.put("/Float?body=", null)
			.run()
			.assertBody().is("null");
		a.put("/Float?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		a.put("/Map", "{foo:123}", "application/json")
			.run()
			.assertBody().is("{foo:123}");
		a.put("/Map", "(foo=123)", "text/openapi")
			.run()
			.assertCode().is(415);
		a.put("/Map?body=(foo=123)", null)
			.run()
			.assertBody().is("{foo:123}");
		a.put("/Map?body=()", null)
			.run()
			.assertBody().is("{}");
		a.put("/Map?body=null", null)
			.run()
			.assertBody().is("null");
		a.put("/Map?body=", null)
			.run()
			.assertBody().is("null");
		a.put("/Map?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		a.put("/enum", "'ONE'", "application/json")
			.run()
			.assertBody().is("'ONE'");
		a.put("/enum", "ONE")
			.run()
			.assertBody().is("'ONE'");
		a.put("/enum?body=ONE", null)
			.run()
			.assertBody().is("'ONE'");
		a.put("/enum?body=TWO", null)
			.run()
			.assertBody().is("'TWO'");
		a.put("/enum?body=null", null)
			.run()
			.assertBody().is("null");
		a.put("/enum?body=", null)
			.run()
			.assertBody().is("null");
		a.put("/enum?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		a.put("/Bean", "{f1:'a'}", "application/json")
			.run()
			.assertBody().is("{f1:'a'}");
		a.put("/Bean", "(f1=a)", "text/openapi")
			.run()
			.assertCode().is(415);
		a.put("/Bean?body=(f1=a)", null)
			.run()
			.assertBody().is("{f1:'a'}");
		a.put("/Bean?body=()", null)
			.run()
			.assertBody().is("{}");
		a.put("/Bean?body=null", null)
			.run()
			.assertBody().is("null");
		a.put("/Bean?body=", null)
			.run()
			.assertBody().is("null");
		a.put("/Bean?body=bad&noTrace=true", null)
			.run()
			.assertCode().is(400);

		// Content-Type should always be ignored.
		a.put("/InputStream", "'a'", "application/json")
			.run()
			.assertBody().is("'\\'a\\''");
		a.put("/InputStream", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");
		a.put("/InputStream?body=a", null)
			.run()
			.assertBody().is("'a'");
		a.put("/InputStream?body=null", null)
			.run()
			.assertBody().is("'null'");
		a.put("/InputStream?body=", null)
			.run()
			.assertBody().is("''");

		// Content-Type should always be ignored.
		a.put("/Reader", "'a'", "application/json")
			.run()
			.assertBody().is("'\\'a\\''");
		a.put("/Reader", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");
		a.put("/Reader?body=a", null)
			.run()
			.assertBody().is("'a'");
		a.put("/Reader?body=null", null)
			.run()
			.assertBody().is("'null'");
		a.put("/Reader?body=", null)
			.run()
			.assertBody().is("''");

		// It's not currently possible to pass in a &body parameter for InputStream/Reader transforms.

		// Input stream transform requests must not specify Content-Type or else gets resolved as POJO.
		a.put("/InputStreamTransform?noTrace=true", "'a'", "application/json")
			.run()
			.assertBody().contains("Bad Request");
		a.put("/InputStreamTransform", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");

		// Reader transform requests must not specify Content-Type or else gets resolved as POJO.
		a.put("/ReaderTransform?noTrace=true", "'a'", "application/json")
			.run()
			.assertBody().contains("Bad Request");
		a.put("/ReaderTransform", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");

		// When Content-Type specified and matched, treated as a parsed POJO.
		a.put("/StringTransform", "'a'", "application/json")
			.run()
			.assertBody().is("'a'");
		// When Content-Type not matched, treated as plain text.
		a.put("/StringTransform", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Body on POJO
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class B {
		@RestMethod(name=PUT, path="/StringTransform")
		public B1 a(B1 b) {
			return b;
		}
		@Body
		public static class B1 {
			private String val;
			public B1(String val) { this.val = val; }
			@Override public String toString() { return val; }
		}
		@RestMethod(name=PUT, path="/Bean")
		public B2 b(B2 b) {
			return b;
		}
		@Body
		public static class B2 {
			public String f1;
		}
		@RestMethod(name=PUT, path="/BeanList")
		public B3 c(B3 b) {
			return b;
		}
		@SuppressWarnings("serial")
		@Body
		public static class B3 extends LinkedList<B2> {}
		@RestMethod(name=PUT, path="/InputStreamTransform")
		public B4 d(B4 b) throws Exception {
			return b;
		}
		@Body
		public static class B4 {
			String s;
			public B4(InputStream in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/ReaderTransform")
		public B5 e(B5 b) throws Exception {
			return b;
		}
		@Body
		public static class B5 {
			private String s;
			public B5(Reader in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
	}

	@Test
	public void b01_onPojos() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.put("/StringTransform", "'foo'", "application/json")
			.run()
			.assertBody().is("'foo'");
		// When Content-Type not matched, treated as plain text.
		b.put("/StringTransform", "'foo'")
			.run()
			.assertBody().is("'\\'foo\\''");
		b.put("/Bean", "{f1:'a'}", "application/json")
			.run()
			.assertBody().is("{f1:'a'}");
		b.put("/Bean", "(f1=a)", "text/openapi")
			.run()
			.assertCode().is(415);
		b.put("/BeanList", "[{f1:'a'}]", "application/json")
			.run()
			.assertBody().is("[{f1:'a'}]");
		b.put("/BeanList", "(f1=a)", "text/openapi")
			.run()
			.assertCode().is(415);
		b.put("/InputStreamTransform", "a")
			.run()
			.assertBody().is("'a'");
		// When Content-Type matched, treated as parsed POJO.
		b.put("/InputStreamTransform?noTrace=true", "a", "application/json")
			.run()
			.assertBody().contains("Bad Request");
		b.put("/ReaderTransform", "a")
			.run()
			.assertBody().is("'a'");
		// When Content-Type matched, treated as parsed POJO.
		b.put("/ReaderTransform?noTrace=true", "a", "application/json")
			.run()
			.assertBody().contains("Bad Request");
	}

	//------------------------------------------------------------------------------------------------------------------
	// No serializers or parsers needed when using only streams and readers.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestMethod(name=PUT, path="/String")
		public Reader a(@Body Reader b) throws Exception {
			return b;
		}
		@RestMethod(name=PUT, path="/InputStream")
		public InputStream b(@Body InputStream b) throws Exception {
			return b;
		}
		@RestMethod(name=PUT, path="/Reader")
		public Reader c(@Body Reader b) throws Exception {
			return b;
		}
		@RestMethod(name=PUT, path="/StringTransform")
		public Reader d(@Body D1 b) throws Exception {
			return new StringReader(b.toString());
		}
		public static class D1 {
			private String s;
			public D1(String in) throws Exception { this.s = in; }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/InputStreamTransform")
		public Reader e(@Body D2 b) throws Exception {
			return new StringReader(b.toString());
		}
		public static class D2 {
			String s;
			public D2(InputStream in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/ReaderTransform")
		public Reader f(@Body D3 b) throws Exception {
			return new StringReader(b.toString());
		}
		public static class D3 {
			private String s;
			public D3(Reader in) throws Exception{ this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/StringTransformBodyOnPojo")
		public Reader g(D4 b) throws Exception {
			return new StringReader(b.toString());
		}
		@Body
		public static class D4 {
			private String s;
			public D4(String in) throws Exception { this.s = in; }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/InputStreamTransformBodyOnPojo")
		public Reader h(D5 b) throws Exception {
			return new StringReader(b.toString());
		}
		@Body
		public static class D5 {
			String s;
			public D5(InputStream in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}

		@RestMethod(name=PUT, path="/ReaderTransformBodyOnPojo")
		public Reader i(D6 b) throws Exception {
			return new StringReader(b.toString());
		}
		@Body
		public static class D6 {
			private String s;
			public D6(Reader in) throws Exception{ this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
	}

	@Test
	public void d01_noMediaTypesOnStreams() throws Exception {
		RestClient d = MockRestClient.buildLax(D.class);
		d.put("/String", "a")
			.run()
			.assertBody().is("a");
		d.put("/String", "a", "application/json")
			.run()
			.assertBody().is("a");
		d.put("/InputStream", "a")
			.run()
			.assertBody().is("a");
		d.put("/InputStream", "a", "application/json")
			.run()
			.assertBody().is("a");
		d.put("/Reader", "a")
			.run()
			.assertBody().is("a");
		d.put("/Reader", "a", "application/json")
			.run()
			.assertBody().is("a");
		d.put("/StringTransform", "a")
			.run()
			.assertBody().is("a");
		d.put("/StringTransform?noTrace=true", "a", "application/json")
			.run()
			.assertCode().is(415);
		d.put("/InputStreamTransform", "a")
			.run()
			.assertBody().is("a");
		d.put("/InputStreamTransform", "a", "application/json")
			.run()
			.assertBody().is("a");
		d.put("/ReaderTransform", "a")
			.run()
			.assertBody().is("a");
		d.put("/ReaderTransform", "a", "application/json")
			.run()
			.assertBody().is("a");
		d.put("/StringTransformBodyOnPojo", "a")
			.run()
			.assertBody().is("a");
		d.put("/StringTransformBodyOnPojo?noTrace=true", "a", "application/json")
			.run()
			.assertCode().is(415);
		d.put("/InputStreamTransformBodyOnPojo", "a")
			.run()
			.assertBody().is("a");
		d.put("/InputStreamTransformBodyOnPojo", "a", "application/json")
			.run()
			.assertBody().is("a");
		d.put("/ReaderTransformBodyOnPojo", "a")
			.run()
			.assertBody().is("a");
		d.put("/ReaderTransformBodyOnPojo", "a", "application/json")
			.run()
			.assertBody().is("a");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Complex POJOs
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class E {
		@RestMethod(name=PUT, path="/B")
		public XBeans.XB a(@Body XBeans.XB b) {
			return b;
		}
		@RestMethod(name=PUT, path="/C")
		public XBeans.XC b(@Body XBeans.XC c) {
			return c;
		}
	}

	@Test
	public void e01_complexPojos() throws Exception {
		RestClient e = MockRestClient.build(E.class);
		String expected;

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/B", SimpleJsonSerializer.DEFAULT.toString(XBeans.XB.INSTANCE), "application/json")
			.run()
			.assertBody().is(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/B?body=" + UonSerializer.DEFAULT.serialize(XBeans.XB.INSTANCE), "a")
			.run()
			.assertBody().is(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/C", SimpleJsonSerializer.DEFAULT.toString(XBeans.XB.INSTANCE), "application/json")
			.run()
			.assertBody().is(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/C?body=" + UonSerializer.DEFAULT.serialize(XBeans.XB.INSTANCE), "a")
			.run()
			.assertBody().is(expected);
	}

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	@BeanConfig(applyBean={@Bean(on="A,B,C",sort=true)})
	@UrlEncodingConfig(applyUrlEncoding={@UrlEncoding(on="C",expandedParams=true)})
	public static class E2 {
		@RestMethod(name=PUT, path="/B")
		public XBeans.XE a(@Body XBeans.XE b) {
			return b;
		}
		@RestMethod(name=PUT, path="/C")
		public XBeans.XF b(@Body XBeans.XF c) {
			return c;
		}
	}

	@Test
	public void e02_complexPojos() throws Exception {
		RestClient e2 = MockRestClient.build(E2.class);
		String expected;

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/B", SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(XBeans.Annotations.class).build().toString(XBeans.XE.INSTANCE), "application/json")
			.run()
			.assertBody().is(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/B?body=" + UonSerializer.DEFAULT.builder().applyAnnotations(XBeans.Annotations.class).build().serialize(XBeans.XE.INSTANCE), "a")
			.run()
			.assertBody().is(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/C", SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(XBeans.Annotations.class).build().toString(XBeans.XE.INSTANCE), "application/json")
			.run()
			.assertBody().is(expected);

		expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/C?body=" + UonSerializer.DEFAULT.builder().applyAnnotations(XBeans.Annotations.class).build().serialize(XBeans.XE.INSTANCE), "a")
			.run()
			.assertBody().is(expected);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Form POSTS with @Body parameter
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class,parsers=JsonParser.class)
	public static class F {
		@RestMethod(name=POST, path="/*")
		public Reader a(
				@Body F1 bean,
				@HasQuery("p1") boolean hqp1, @HasQuery("p2") boolean hqp2,
				@Query("p1") String qp1, @Query("p2") int qp2) throws Exception {
			return new StringReader("bean=["+SimpleJsonSerializer.DEFAULT.toString(bean)+"],qp1=["+qp1+"],qp2=["+qp2+"],hqp1=["+hqp1+"],hqp2=["+hqp2+"]");
		}
		public static class F1 {
			public String p1;
			public int p2;
		}
	}

	@Test
	public void f01_formPostAsContent() throws Exception {
		RestClient f = MockRestClient.build(F.class);
		f.post("/", "{p1:'p1',p2:2}", "application/json")
			.run()
			.assertBody().is("bean=[{p1:'p1',p2:2}],qp1=[null],qp2=[0],hqp1=[false],hqp2=[false]");
		f.post("/", "{}", "application/json")
			.run()
			.assertBody().is("bean=[{p2:0}],qp1=[null],qp2=[0],hqp1=[false],hqp2=[false]");
		f.post("?p1=p3&p2=4", "{p1:'p1',p2:2}", "application/json")
			.run()
			.assertBody().is("bean=[{p1:'p1',p2:2}],qp1=[p3],qp2=[4],hqp1=[true],hqp2=[true]");
		f.post("?p1=p3&p2=4", "{}", "application/json")
			.run()
			.assertBody().is("bean=[{p2:0}],qp1=[p3],qp2=[4],hqp1=[true],hqp2=[true]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using @UrlEncoding(expandedParams=true) annotation on bean.
	// A simple round-trip test to verify that both serializing and parsing works.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=UrlEncodingSerializer.class,parsers=UrlEncodingParser.class)
	public static class G {
		@RestMethod(name=POST,path="/")
		public XBeans.XC a(@Body XBeans.XC content) throws Exception {
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
		g.post("/", in, "application/x-www-form-urlencoded")
			.run()
			.assertBody().is(in);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using URLENC_expandedParams property.
	// A simple round-trip test to verify that both serializing and parsing works.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=UrlEncodingSerializer.class,parsers=UrlEncodingParser.class)
	public static class H {
		@RestMethod(name=POST,path="/",
			properties={
				@Property(name=UrlEncodingSerializer.URLENC_expandedParams, value="true"),
				@Property(name=UrlEncodingParser.URLENC_expandedParams, value="true")
			}
		)
		public XBeans.XB a(@Body XBeans.XB content) throws Exception {
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
		h.post("/", in, "application/x-www-form-urlencoded")
			.run()
			.assertBody().is(in);
	}

	@Rest(serializers=UrlEncodingSerializer.class,parsers=UrlEncodingParser.class)
	@BeanConfig(applyBean={@Bean(on="A,B,C",sort=true)})
	@UrlEncodingConfig(applyUrlEncoding={@UrlEncoding(on="C",expandedParams=true)})
	public static class H2 {
		@RestMethod(name=POST,path="/",
			properties={
				@Property(name=UrlEncodingSerializer.URLENC_expandedParams, value="true"),
				@Property(name=UrlEncodingParser.URLENC_expandedParams, value="true")
			}
		)
		public XBeans.XE a(@Body XBeans.XE content) throws Exception {
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
		h2.post("/", in, "application/x-www-form-urlencoded")
			.run()
			.assertBody().is(in);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test behavior of @Body(required=true).
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class,parsers=JsonParser.class)
	public static class I {
		@RestMethod(name=POST)
		public XBeans.XB a(@Body(r=true) XBeans.XB content) throws Exception {
			return content;
		}
		@RestMethod(name=POST)
		@BeanConfig(applyBean={@Bean(on="A,B,C",sort=true)})
		@UrlEncodingConfig(applyUrlEncoding={@UrlEncoding(on="C",expandedParams=true)})
		public XBeans.XE b(@Body(r=true) XBeans.XE content) throws Exception {
			return content;
		}
	}

	@Test
	public void i01_required() throws Exception {
		RestClient i = MockRestClient.buildLax(I.class);

		i.post("/a", "", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains("Required value not provided.");
		i.post("/a", "{}", "application/json")
			.run()
			.assertCode().is(200);

		i.post("/b", "", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains("Required value not provided.");
		i.post("/b", "{}", "application/json")
			.run()
			.assertCode().is(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional body parameter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=SimpleJsonSerializer.class,parsers=JsonParser.class)
	public static class J {
		@RestMethod(name=POST)
		public Object a(@Body Optional<Integer> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestMethod(name=POST)
		public Object b(@Body Optional<ABean> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestMethod(name=POST)
		public Object c(@Body Optional<List<ABean>> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestMethod(name=POST)
		public Object d(@Body List<Optional<ABean>> body) throws Exception {
			return body;
		}
	}

	@Test
	public void j01_optionalParams() throws Exception {
		RestClient j = MockRestClient.buildJson(J.class);
		j.post("/a", 123)
			.run()
			.assertCode().is(200)
			.assertBody().is("123");
		j.post("/a", null)
			.run()
			.assertCode().is(200)
			.assertBody().is("null");

		j.post("/b", ABean.get())
			.run()
			.assertCode().is(200)
			.assertBody().is("{a:1,b:'foo'}");
		j.post("/b", null)
			.run()
			.assertCode().is(200)
			.assertBody().is("null");

		String body1 = SimpleJson.DEFAULT.toString(AList.of(ABean.get()));
		j.post("/c", body1, "application/json")
			.run()
			.assertCode().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		j.post("/c", null)
			.run()
			.assertCode().is(200)
			.assertBody().is("null");

		String body2 = SimpleJson.DEFAULT.toString(AList.of(Optional.of(ABean.get())));
		j.post("/d", body2, "application/json")
			.run()
			.assertCode().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		j.post("/d", null)
			.run()
			.assertCode().is(200)
			.assertBody().is("null");
	}
}
