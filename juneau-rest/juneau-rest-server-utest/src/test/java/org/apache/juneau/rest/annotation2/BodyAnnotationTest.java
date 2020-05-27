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
package org.apache.juneau.rest.annotation2;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.HasQuery;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.apache.juneau.rest.testutils.DTOs;
import org.apache.juneau.rest.testutils.DTOs2;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.apache.juneau.urlencoding.annotation.UrlEncoding;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BodyAnnotationTest {

	//=================================================================================================================
	// @Body on parameter
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="text/json")
	public static class A {
		@RestMethod(name=PUT, path="/String")
		public String a01(@Body String b) {
			return b;
		}
		@RestMethod(name=PUT, path="/Integer")
		public Integer a02(@Body Integer b) {
			return b;
		}
		@RestMethod(name=PUT, path="/int")
		public Integer a03(@Body int b) {
			return b;
		}
		@RestMethod(name=PUT, path="/Boolean")
		public Boolean a04(@Body Boolean b) {
			return b;
		}
		@RestMethod(name=PUT, path="/boolean")
		public Boolean a05(@Body boolean b) {
			return b;
		}
		@RestMethod(name=PUT, path="/float")
		public float a06(@Body float f) {
			return f;
		}
		@RestMethod(name=PUT, path="/Float")
		public Float a07(@Body Float f) {
			return f;
		}
		@RestMethod(name=PUT, path="/Map")
		public TreeMap<String,Integer> a08(@Body TreeMap<String,Integer> m) {
			return m;
		}
		@RestMethod(name=PUT, path="/enum")
		public TestEnum a09(@Body TestEnum e) {
			return e;
		}
		public static class A11 {
			public String f1;
		}
		@RestMethod(name=PUT, path="/Bean")
		public A11 a11(@Body A11 b) {
			return b;
		}
		@RestMethod(name=PUT, path="/InputStream")
		public String a12(@Body InputStream b) throws Exception {
			return IOUtils.read(b);
		}
		@RestMethod(name=PUT, path="/Reader")
		public String a13(@Body Reader b) throws Exception {
			return IOUtils.read(b);
		}
		@RestMethod(name=PUT, path="/InputStreamTransform")
		public A14 a14(@Body A14 b) throws Exception {
			return b;
		}
		public static class A14 {
			String s;
			public A14(InputStream in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/ReaderTransform")
		public A15 a15(@Body A15 b) throws Exception {
			return b;
		}
		public static class A15 {
			private String s;
			public A15(Reader in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/StringTransform")
		public A16 a16(@Body A16 b) throws Exception { return b; }
		public static class A16 {
			private String s;
			public A16(String s) throws Exception { this.s = s; }
			@Override public String toString() { return s; }
		}
	}
	private static MockRest a = MockRest.build(A.class);

	@Test
	public void a01a_onParameter_String() throws Exception {
		a.put("/String", "'foo'").json()
			.run()
			.assertBody().is("'foo'");
	}
	@Test
	public void a01b_onParameter_String_noContentType() throws Exception {
		// If no Content-Type specified, should be treated as plain-text.
		a.put("/String", "'foo'")
			.run()
			.assertBody().is("'\\'foo\\''");
	}
	@Test
	public void a01c_onParameter_String_noContentType_other() throws Exception {
		// If Content-Type not matched, should be treated as plain-text.
		a.put("/String", "'foo'").contentType("")
			.run()
			.assertBody().is("'\\'foo\\''");
		a.put("/String", "'foo'").contentType("text/plain")
			.run()
			.assertBody().is("'\\'foo\\''");
	}
	@Test
	public void a02a_onParameter_Integer() throws Exception {
		a.put("/Integer", "123").json()
			.run()
			.assertBody().is("123");
	}
	@Test
	public void a02b_onParameter_Integer_noContentType() throws Exception {
		// Integer takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Integer", "123")
			.run()
			.assertBody().is("123");
	}
	@Test
	public void a03a_onParameter_int() throws Exception {
		a.put("/int", "123").json()
			.run()
			.assertBody().is("123");
	}
	@Test
	public void a03b_onParameter_int_noContentType() throws Exception {
		a.put("/int", "123")
			.run()
			.assertBody().is("123"); // Uses part parser.
	}
	@Test
	public void a04a_onParameter_Boolean() throws Exception {
		a.put("/Boolean", "true").json()
			.run()
			.assertBody().is("true");
	}
	@Test
	public void a04b_onParameter_Boolean_noContentType() throws Exception {
		// Boolean takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Boolean", "true")
			.run()
			.assertBody().is("true");
	}
	@Test
	public void a05a_onParameter_boolean() throws Exception {
		a.put("/boolean", "true").json()
			.run()
			.assertBody().is("true");
	}
	@Test
	public void a05b_onParameter_boolean_noContentType() throws Exception {
		a.put("/boolean", "true")
			.run()
			.assertBody().is("true"); // Uses part parser.
	}
	@Test
	public void a06a_onParameter_float() throws Exception {
		a.put("/float", "1.23").json()
			.run()
			.assertBody().is("1.23");
	}
	@Test
	public void a06b_onParameter_float_noContentType() throws Exception {
		a.put("/float", "1.23")
			.run()
			.assertBody().is("1.23");  // Uses part parser.
	}
	@Test
	public void a07a_onParameter_Float() throws Exception {
		a.put("/Float", "1.23").json()
			.run()
			.assertBody().is("1.23");
	}
	@Test
	public void a07b_onParameter_Float_noContentType() throws Exception {
		// Float takes in a String arg, so it can be parsed without Content-Type.
		a.put("/Float", "1.23")
			.run()
			.assertBody().is("1.23");
	}
	@Test
	public void a08a_onParameter_Map() throws Exception {
		a.put("/Map", "{foo:123}").json()
			.run()
			.assertBody().is("{foo:123}");
	}
	@Test
	public void a08b_onParameter_Map_noContentType() throws Exception {
		a.put("/Map", "(foo=123)")
			.run()
			.assertStatus().is(415);
	}
	@Test
	public void a09a_onParameter_enum() throws Exception {
		a.put("/enum", "'ONE'").json()
			.run()
			.assertBody().is("'ONE'");
	}
	@Test
	public void a09b_onParameter_enum_noContentType() throws Exception {
		a.put("/enum", "ONE")
			.run()
			.assertBody().is("'ONE'");
	}
	@Test
	public void a11a_onParameter_Bean() throws Exception {
		a.put("/Bean", "{f1:'a'}").json()
			.run()
			.assertBody().is("{f1:'a'}");
	}
	@Test
	public void a11b_onParameter_Bean_noContentType() throws Exception {
		a.put("/Bean", "(f1=a)")
			.run()
			.assertStatus().is(415);
	}
	@Test
	public void a12a_onParameter_InputStream() throws Exception {
		// Content-Type should always be ignored.
		a.put("/InputStream", "'a'").json()
			.run()
			.assertBody().is("'\\'a\\''");
	}
	@Test
	public void a12b_onParameter_InputStream_noContentType() throws Exception {
		a.put("/InputStream", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");
	}
	@Test
	public void a13a_onParameter_Reader() throws Exception {
		// Content-Type should always be ignored.
		a.put("/Reader", "'a'").json()
			.run()
			.assertBody().is("'\\'a\\''");
	}
	@Test
	public void a13b_onParameter_Reader_noContentType() throws Exception {
		a.put("/Reader", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");
	}
	@Test
	public void a14a_onParameter_InputStreamTransform() throws Exception {
		// Input stream transform requests must not specify Content-Type or else gets resolved as POJO.
		a.put("/InputStreamTransform?noTrace=true", "'a'").json()
			.run()
			.assertBody().contains("Bad Request");
	}
	@Test
	public void a14b_onParameter_InputStreamTransform_noContentType() throws Exception {
		a.put("/InputStreamTransform", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");
	}
	@Test
	public void a15a_onParameter_ReaderTransform() throws Exception {
		// Reader transform requests must not specify Content-Type or else gets resolved as POJO.
		a.put("/ReaderTransform?noTrace=true", "'a'").json()
			.run()
			.assertBody().contains("Bad Request");
	}
	@Test
	public void a15b_onParameter_ReaderTransform_noContentType() throws Exception {
		a.put("/ReaderTransform", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");
	}
	@Test
	public void a16a_onParameter_StringTransform() throws Exception {
		// When Content-Type specified and matched, treated as a parsed POJO.
		a.put("/StringTransform", "'a'").json()
			.run()
			.assertBody().is("'a'");
	}
	@Test
	public void a16b_onParameter_StringTransform_noContentType() throws Exception {
		// When Content-Type not matched, treated as plain text.
		a.put("/StringTransform", "'a'")
			.run()
			.assertBody().is("'\\'a\\''");
	}

	//=================================================================================================================
	// @Body on POJO
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class B {
		@RestMethod(name=PUT, path="/StringTransform")
		public B01 b01(B01 b) {
			return b;
		}
		@Body
		public static class B01 {
			private String val;
			public B01(String val) { this.val = val; }
			@Override public String toString() { return val; }
		}
		@RestMethod(name=PUT, path="/Bean")
		public B02 b02(B02 b) {
			return b;
		}
		@Body
		public static class B02 {
			public String f1;
		}
		@RestMethod(name=PUT, path="/BeanList")
		public B03 b03(B03 b) {
			return b;
		}
		@SuppressWarnings("serial")
		@Body
		public static class B03 extends LinkedList<B02> {}
		@RestMethod(name=PUT, path="/InputStreamTransform")
		public B04 b04(B04 b) throws Exception {
			return b;
		}
		@Body
		public static class B04 {
			String s;
			public B04(InputStream in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/ReaderTransform")
		public B05 b05(B05 b) throws Exception {
			return b;
		}
		@Body
		public static class B05 {
			private String s;
			public B05(Reader in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
	}
	private static MockRest b = MockRest.build(B.class);

	@Test
	public void b01a_onPojo_StringTransform() throws Exception {
		b.put("/StringTransform", "'foo'").json()
			.run()
			.assertBody().is("'foo'");
	}
	@Test
	public void b01b_onPojo_StringTransform_noContentType() throws Exception {
		// When Content-Type not matched, treated as plain text.
		b.put("/StringTransform", "'foo'")
			.run()
			.assertBody().is("'\\'foo\\''");
	}
	@Test
	public void b02a_onPojo_Bean() throws Exception {
		b.put("/Bean", "{f1:'a'}").json()
			.run()
			.assertBody().is("{f1:'a'}");
	}
	@Test
	public void b02b_onPojo_Bean_noContentType() throws Exception {
		b.put("/Bean", "(f1=a)")
			.run()
			.assertStatus().is(415);
	}
	@Test
	public void b03a_onPojo_BeanList() throws Exception {
		b.put("/BeanList", "[{f1:'a'}]").json()
			.run()
			.assertBody().is("[{f1:'a'}]");
	}
	@Test
	public void b03b_onPojo_BeanList_noContentType() throws Exception {
		b.put("/BeanList", "(f1=a)")
			.run()
			.assertStatus().is(415);
	}
	@Test
	public void b04a_onPojo_InputStreamTransform() throws Exception {
		b.put("/InputStreamTransform", "a")
			.run()
			.assertBody().is("'a'");
	}
	@Test
	public void b04b_onPojo_InputStreamTransform_withContentType() throws Exception {
		// When Content-Type matched, treated as parsed POJO.
		b.put("/InputStreamTransform?noTrace=true", "a").json()
			.run()
			.assertBody().contains("Bad Request");
	}
	@Test
	public void b05a_onPojo_ReaderTransform() throws Exception {
		b.put("/ReaderTransform", "a")
			.run()
			.assertBody().is("'a'");
	}
	@Test
	public void b05b_onPojo_ReaderTransform_withContentType() throws Exception {
		// When Content-Type matched, treated as parsed POJO.
		b.put("/ReaderTransform?noTrace=true", "a").json()
			.run()
			.assertBody().contains("Bad Request");
	}

	//=================================================================================================================
	// Basic tests using @Body parameter
	//=================================================================================================================

	public void c01_bodyParam_String() throws Exception {
		a.put("/String?body=foo", null)
			.run()
			.assertBody().is("'foo'");
		a.put("/String?body=null", null)
			.run()
			.assertBody().is("null");
		a.put("/String?body=", null)
			.run()
			.assertBody().is("''");
	}
	@Test
	public void c02_bodyParam_Integer() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c03_bodyParam_int() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c04_bodyParam_Boolean() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c05_bodyParam_boolean() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c06_bodyParam_Float() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c07_bodyParam_float() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c08_bodyParam_Map() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c09_bodyParam_enum() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c11_bodyParam_Bean() throws Exception {
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
			.assertStatus().is(400);
	}
	@Test
	public void c12_bodyParam_InputStream() throws Exception {
		a.put("/InputStream?body=a", null)
			.run()
			.assertBody().is("'a'");
		a.put("/InputStream?body=null", null)
			.run()
			.assertBody().is("'null'");
		a.put("/InputStream?body=", null)
			.run()
			.assertBody().is("''");
	}
	@Test
	public void c13_bodyParam_Reader() throws Exception {
		a.put("/Reader?body=a", null)
			.run()
			.assertBody().is("'a'");
		a.put("/Reader?body=null", null)
			.run()
			.assertBody().is("'null'");
		a.put("/Reader?body=", null)
			.run()
			.assertBody().is("''");
	}

	// It's not currently possible to pass in a &body parameter for InputStream/Reader transforms.

	//=================================================================================================================
	// No serializers or parsers needed when using only streams and readers.
	//=================================================================================================================

	@Rest
	public static class D {
		@RestMethod(name=PUT, path="/String")
		public Reader d01(@Body Reader b) throws Exception {
			return b;
		}
		@RestMethod(name=PUT, path="/InputStream")
		public InputStream d02(@Body InputStream b) throws Exception {
			return b;
		}
		@RestMethod(name=PUT, path="/Reader")
		public Reader d03(@Body Reader b) throws Exception {
			return b;
		}
		@RestMethod(name=PUT, path="/StringTransform")
		public Reader d04(@Body D04 b) throws Exception {
			return new StringReader(b.toString());
		}
		public static class D04 {
			private String s;
			public D04(String in) throws Exception { this.s = in; }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/InputStreamTransform")
		public Reader d05(@Body D05 b) throws Exception {
			return new StringReader(b.toString());
		}
		public static class D05 {
			String s;
			public D05(InputStream in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/ReaderTransform")
		public Reader d06(@Body D06 b) throws Exception {
			return new StringReader(b.toString());
		}
		public static class D06 {
			private String s;
			public D06(Reader in) throws Exception{ this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/StringTransformBodyOnPojo")
		public Reader d07(D07 b) throws Exception {
			return new StringReader(b.toString());
		}
		@Body
		public static class D07 {
			private String s;
			public D07(String in) throws Exception { this.s = in; }
			@Override public String toString() { return s; }
		}
		@RestMethod(name=PUT, path="/InputStreamTransformBodyOnPojo")
		public Reader d08(D08 b) throws Exception {
			return new StringReader(b.toString());
		}
		@Body
		public static class D08 {
			String s;
			public D08(InputStream in) throws Exception { this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}

		@RestMethod(name=PUT, path="/ReaderTransformBodyOnPojo")
		public Reader d09(D09 b) throws Exception {
			return new StringReader(b.toString());
		}
		@Body
		public static class D09 {
			private String s;
			public D09(Reader in) throws Exception{ this.s = IOUtils.read(in); }
			@Override public String toString() { return s; }
		}
	}
	private static MockRest d = MockRest.build(D.class);

	@Test
	public void d01a_noMediaTypes_String() throws Exception {
		d.put("/String", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d01b_noMediaTypes_String_withContentType() throws Exception {
		d.put("/String", "a").json()
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d02a_noMediaTypes_InputStream() throws Exception {
		d.put("/InputStream", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d02b_noMediaTypes_InputStream_withContentType() throws Exception {
		d.put("/InputStream", "a").json()
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d03a_noMediaTypes_Reader() throws Exception {
		d.put("/Reader", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d03b_noMediaTypes_Reader_withContentType() throws Exception {
		d.put("/Reader", "a").json()
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d04a_noMediaTypes_StringTransform() throws Exception {
		d.put("/StringTransform", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d04b_noMediaTypes_StringTransform_withContentType() throws Exception {
		d.put("/StringTransform?noTrace=true", "a").json()
			.run()
			.assertStatus().is(415);
	}
	@Test
	public void d05a_noMediaTypes_InputStreamTransform() throws Exception {
		d.put("/InputStreamTransform", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d05b_noMediaTypes_InputStreamTransform_withContentType() throws Exception {
		d.put("/InputStreamTransform", "a").json()
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d06a_noMediaTypes_ReaderTransform() throws Exception {
		d.put("/ReaderTransform", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d06b_noMediaTypes_ReaderTransform_withContentType() throws Exception {
		d.put("/ReaderTransform", "a").json()
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d07a_noMediaTypes_StringTransformBodyOnPojo() throws Exception {
		d.put("/StringTransformBodyOnPojo", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d07b_noMediaTypes_StringTransformBodyOnPojo_withContentType() throws Exception {
		d.put("/StringTransformBodyOnPojo?noTrace=true", "a").json()
			.run()
			.assertStatus().is(415);
	}
	@Test
	public void d08a_noMediaTypes_InputStreamTransformBodyOnPojo() throws Exception {
		d.put("/InputStreamTransformBodyOnPojo", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d08b_noMediaTypes_InputStreamTransformBodyOnPojo_withContentType() throws Exception {
		d.put("/InputStreamTransformBodyOnPojo", "a").json()
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d09a_noMediaTypes_ReaderTransformBodyOnPojo() throws Exception {
		d.put("/ReaderTransformBodyOnPojo", "a")
			.run()
			.assertBody().is("a");
	}
	@Test
	public void d09b_noMediaTypes_ReaderTransformBodyOnPojo_withContentType() throws Exception {
		d.put("/ReaderTransformBodyOnPojo", "a").json()
			.run()
			.assertBody().is("a");
	}

	//=================================================================================================================
	// Complex POJOs
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class E {
		@RestMethod(name=PUT, path="/B")
		public DTOs.B testPojo1(@Body DTOs.B b) {
			return b;
		}
		@RestMethod(name=PUT, path="/C")
		public DTOs.C testPojo2(@Body DTOs.C c) {
			return c;
		}
	}
	private static MockRest e = MockRest.build(E.class);

	@Test
	public void e01_complexPojos_B_body() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/B", SimpleJsonSerializer.DEFAULT.toString(DTOs.B.INSTANCE)).json()
			.run()
			.assertBody().is(expected);
	}
	@Test
	public void e02_complexPojos_B_bodyParam() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/B?body=" + UonSerializer.DEFAULT.serialize(DTOs.B.INSTANCE), "a")
			.run()
			.assertBody().is(expected);
	}
	@Test
	public void e03_complexPojos_C_body() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/C", SimpleJsonSerializer.DEFAULT.toString(DTOs.B.INSTANCE)).json()
			.run()
			.assertBody().is(expected);
	}
	@Test
	public void e04_complexPojos_C_bodyParam() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e.put("/C?body=" + UonSerializer.DEFAULT.serialize(DTOs.B.INSTANCE), "a")
			.run()
			.assertBody().is(expected);
	}

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	@BeanConfig(applyBean={@Bean(on="A,B,C",sort=true)})
	@UrlEncodingConfig(applyUrlEncoding={@UrlEncoding(on="C",expandedParams=true)})
	public static class E2 {
		@RestMethod(name=PUT, path="/B")
		public DTOs2.B testPojo1(@Body DTOs2.B b) {
			return b;
		}
		@RestMethod(name=PUT, path="/C")
		public DTOs2.C testPojo2(@Body DTOs2.C c) {
			return c;
		}
	}
	private static MockRest e2 = MockRest.build(E2.class);

	@Test
	public void e05_complexPojos_B_body() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/B", SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(DTOs2.Annotations.class).build().toString(DTOs2.B.INSTANCE)).json()
			.run()
			.assertBody().is(expected);
	}
	@Test
	public void e06_complexPojos_B_bodyParam() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/B?body=" + UonSerializer.DEFAULT.builder().applyAnnotations(DTOs2.Annotations.class).build().serialize(DTOs2.B.INSTANCE), "a")
			.run()
			.assertBody().is(expected);
	}
	@Test
	public void e07_complexPojos_C_body() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/C", SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(DTOs2.Annotations.class).build().toString(DTOs2.B.INSTANCE)).json()
			.run()
			.assertBody().is(expected);
	}
	@Test
	public void e08_complexPojos_C_bodyParam() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		e2.put("/C?body=" + UonSerializer.DEFAULT.builder().applyAnnotations(DTOs2.Annotations.class).build().serialize(DTOs2.B.INSTANCE), "a")
			.run()
			.assertBody().is(expected);
	}

	//=================================================================================================================
	// Form POSTS with @Body parameter
	//=================================================================================================================

	@Rest(serializers=JsonSerializer.class,parsers=JsonParser.class)
	public static class F {
		@RestMethod(name=POST, path="/*")
		public Reader formPostAsContent(
				@Body F01 bean,
				@HasQuery("p1") boolean hqp1, @HasQuery("p2") boolean hqp2,
				@Query("p1") String qp1, @Query("p2") int qp2) throws Exception {
			return new StringReader("bean=["+SimpleJsonSerializer.DEFAULT.toString(bean)+"],qp1=["+qp1+"],qp2=["+qp2+"],hqp1=["+hqp1+"],hqp2=["+hqp2+"]");
		}
		public static class F01 {
			public String p1;
			public int p2;
		}
	}
	static MockRest f = MockRest.build(F.class);

	@Test
	public void f01_formPostAsContent() throws Exception {
		f.post("/", "{p1:'p1',p2:2}").json()
			.run()
			.assertBody().is("bean=[{p1:'p1',p2:2}],qp1=[null],qp2=[0],hqp1=[false],hqp2=[false]");
		f.post("/", "{}").json()
			.run()
			.assertBody().is("bean=[{p2:0}],qp1=[null],qp2=[0],hqp1=[false],hqp2=[false]");
		f.post("?p1=p3&p2=4", "{p1:'p1',p2:2}").json()
			.run()
			.assertBody().is("bean=[{p1:'p1',p2:2}],qp1=[p3],qp2=[4],hqp1=[true],hqp2=[true]");
		f.post("?p1=p3&p2=4", "{}").json()
			.run()
			.assertBody().is("bean=[{p2:0}],qp1=[p3],qp2=[4],hqp1=[true],hqp2=[true]");
	}

	//=================================================================================================================
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using @UrlEncoding(expandedParams=true) annotation on bean.
	// A simple round-trip test to verify that both serializing and parsing works.
	//=================================================================================================================

	@Rest(serializers=UrlEncodingSerializer.class,parsers=UrlEncodingParser.class)
	public static class G {
		@RestMethod(name=POST,path="/")
		public DTOs.C g(@Body DTOs.C content) throws Exception {
			return content;
		}
	}
	static MockRest g = MockRest.build(G.class);

	@Test
	public void g01() throws Exception {
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
		g.post("/", in).urlEnc()
			.run()
			.assertBody().is(in);
	}

	//=================================================================================================================
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using URLENC_expandedParams property.
	// A simple round-trip test to verify that both serializing and parsing works.
	//=================================================================================================================

	@Rest(serializers=UrlEncodingSerializer.class,parsers=UrlEncodingParser.class)
	public static class H {
		@RestMethod(name=POST,path="/",
			properties={
				@Property(name=UrlEncodingSerializer.URLENC_expandedParams, value="true"),
				@Property(name=UrlEncodingParser.URLENC_expandedParams, value="true")
			}
		)
		public DTOs.B g(@Body DTOs.B content) throws Exception {
			return content;
		}
	}
	static MockRest h = MockRest.build(H.class);

	@Test
	public void h01() throws Exception {
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
		h.post("/", in).urlEnc()
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
		public DTOs2.B g(@Body DTOs2.B content) throws Exception {
			return content;
		}
	}
	static MockRest h2 = MockRest.build(H2.class);

	@Test
	public void h02() throws Exception {
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
		h2.post("/", in).urlEnc()
			.run()
			.assertBody().is(in);
	}

	//=================================================================================================================
	// Test behavior of @Body(required=true).
	//=================================================================================================================

	@Rest(serializers=JsonSerializer.class,parsers=JsonParser.class)
	public static class I {
		@RestMethod(name=POST,path="/")
		public DTOs.B g(@Body(r=true) DTOs.B content) throws Exception {
			return content;
		}
	}
	static MockRest i = MockRest.build(I.class);

	@Test
	public void i01() throws Exception {
		i.post("/", "").json()
			.run()
			.assertStatus().is(400)
			.assertBody().contains("Required value not provided.");
		i.post("/", "{}").json()
			.run()
			.assertStatus().is(200);
	}

	@Rest(serializers=JsonSerializer.class,parsers=JsonParser.class)
	public static class I2 {
		@RestMethod(name=POST,path="/")
		@BeanConfig(applyBean={@Bean(on="A,B,C",sort=true)})
		@UrlEncodingConfig(applyUrlEncoding={@UrlEncoding(on="C",expandedParams=true)})
		public DTOs2.B g(@Body(r=true) DTOs2.B content) throws Exception {
			return content;
		}
	}
	static MockRest i2 = MockRest.build(I2.class);

	@Test
	public void i02() throws Exception {
		i2.post("/", "").json()
			.run()
			.assertStatus().is(400)
			.assertBody().contains("Required value not provided.");
		i2.post("/", "{}").json()
			.run()
			.assertStatus().is(200);
	}

	//=================================================================================================================
	// Optional body parameter.
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class,parsers=JsonParser.class)
	public static class J {
		@RestMethod(name=POST,path="/a")
		public Object a(@Body Optional<Integer> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestMethod(name=POST,path="/b")
		public Object b(@Body Optional<ABean> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestMethod(name=POST,path="/c")
		public Object c(@Body Optional<List<ABean>> body) throws Exception {
			assertNotNull(body);
			return body;
		}
		@RestMethod(name=POST,path="/d")
		public Object d(@Body List<Optional<ABean>> body) throws Exception {
			return body;
		}
	}
	static MockRest j = MockRest.buildJson(J.class);

	@Test
	public void j01_optionalParam_integer() throws Exception {
		j.post("/a", "123")
			.run()
			.assertStatus().is(200)
			.assertBody().is("123");
		j.post("/a", "null")
			.run()
			.assertStatus().is(200)
			.assertBody().is("null");
	}

	@Test
	public void j02_optionalParam_bean() throws Exception {
		j.post("/b", new ABean().init())
			.run()
			.assertStatus().is(200)
			.assertBody().is("{a:1,b:'foo'}");
		j.post("/b", "null")
			.run()
			.assertStatus().is(200)
			.assertBody().is("null");
	}

	@Test
	public void j03_optionalParam_listOfBeans() throws Exception {
		String body = SimpleJson.DEFAULT.toString(AList.of(new ABean().init()));
		j.post("/c", body)
			.run()
			.assertStatus().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		j.post("/c", "null")
			.run()
			.assertStatus().is(200)
			.assertBody().is("null");
	}

	@Test
	public void j04_optionalParam_listOfOptionals() throws Exception {
		String body = SimpleJson.DEFAULT.toString(AList.of(Optional.of(new ABean().init())));
		j.post("/d", body)
			.run()
			.assertStatus().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		j.post("/d", "null")
			.run()
			.assertStatus().is(200)
			.assertBody().is("null");
	}

	//=================================================================================================================
	// Swagger - @Body on POJO
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SA {

		@Body(
			d={"a","b"},
			r=true,
			schema=@Schema(type="string"),
			ex=" 'a' ",
			exs="{foo:'bar'}"
		)
		public static class SA01 {
			public SA01(String x) {}
		}
		@RestMethod
		public void sa01(SA01 h) {}

		@Body({
			"description:'a\nb',",
			"required:true,",
			"schema:{type:'string'},",
			"x-example:'\\'a\\'',",
			"x-examples:{foo:'bar'}"
		})
		public static class SA02 {
			public SA02(String x) {}
		}
		@RestMethod
		public void sa02(SA02 h) {}

		@Body(
			value={
				"description:'a\nb',",
				"required:true,",
				"schema:{type:'string'},",
				"x-example:'\\'a\\'',",
				"x-examples:{foo:'bar'}"
			},
			d={"b","c"},
			schema=@Schema(type="string"),
			ex="'b'",
			exs="{foo:'baz'}"
		)
		public static class SA03 {
			public SA03(String x) {}
		}
		@RestMethod
		public void sa03(SA03 h) {}
	}

	static Swagger sa = getSwagger(SA.class);

	@Test
	public void sa01_Body_onPojo_basic() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa01","get","body",null);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'bar'}", x.getExamples());
	}
	@Test
	public void sa02_Body_onPojo_api() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa02","get","body",null);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertEquals("'a'", x.getExample());
		assertObjectEquals("{foo:'bar'}", x.getExamples());
	}
	@Test
	public void sa03_Body_onPojo_mixed() throws Exception {
		ParameterInfo x = sa.getParameterInfo("/sa03","get","body",null);
		assertEquals("b\nc", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertEquals("'b'", x.getExample());
		assertObjectEquals("{foo:'baz'}", x.getExamples());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SB {

		@Body(schema=@Schema(" type:'b' "))
		public static class SB01 {}
		@RestMethod
		public void sb01(SB01 h) {}

		@Body
		public static class SB02 {
			public String f1;
		}
		@RestMethod
		public void sb02(SB02 b) {}

		@Body
		public static class SB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void sb03(SB03 b) {}

		@Body
		public static class SB04 {}
		@RestMethod
		public void sb04(SB04 b) {}
	}

	static Swagger sb = getSwagger(SB.class);

	@Test
	public void sb01_Body_onPojo_schemaValue() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb01","get","body",null);
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void sb02_Body_onPojo_autoDetectBean() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb02","get","body",null);
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void sb03_Body_onPojo_autoDetectList() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb03","get","body",null);
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void sb04_Body_onPojo_autoDetectStringObject() throws Exception {
		ParameterInfo x = sb.getParameterInfo("/sb04","get","body",null);
		assertObjectEquals("{type:'string'}", x.getSchema());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class SC {

		@Body(ex=" {f1:'b'} ")
		public static class SC01 {
			public String f1;
		}
		@RestMethod
		public void sc01(SC01 h) {}

		@Body(exs={" foo:'bar' "})
		public static class SC02 {}
		@RestMethod
		public void sc02(SC02 h) {}
	}

	static Swagger sc = getSwagger(SC.class);

	@Test
	public void sc01_Body_onPojo_example() throws Exception {
		ParameterInfo x = sc.getParameterInfo("/sc01","get","body",null);
		assertEquals("{f1:'b'}", x.getExample());
	}
	@Test
	public void sc02_Body_onPojo_examples() throws Exception {
		ParameterInfo x = sc.getParameterInfo("/sc02","get","body",null);
		assertObjectEquals("{foo:'bar'}", x.getExamples());
	}

	//=================================================================================================================
	// @Body on parameter
	//=================================================================================================================

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TA {

		public static class TA01 {
			public TA01(String x) {}
		}

		@RestMethod
		public void ta01(
			@Body(
				d= {"a","b"},
				r=true,
				schema=@Schema(type="string"),
				ex="a",
				exs=" {foo:'bar'} "
			) TA01 b) {}

		public static class TA02 {
			public TA02(String x) {}
		}

		@RestMethod
		public void ta02(
			@Body({
				"description:'a\nb',",
				"required:true,",
				"schema:{type:'string'},",
				"x-example:'a',",
				"x-examples:{foo:'bar'}"
			}) TA02 b) {}

		public static class TA03 {
			public TA03(String x) {}
		}

		@RestMethod
		public void ta03(
			@Body(
				value= {
					"description:'a\nb',",
					"required:true,",
					"schema:{type:'string'},",
					"x-example:'a',",
					"x-examples:{foo:'bar'}"
				},
				d= {"b","c"},
				schema=@Schema(type="string"),
				ex="b",
				exs=" {foo:'baz'} "
			) TA03 b) {}
	}

	static Swagger ta = getSwagger(TA.class);

	@Test
	public void ta01_Body_onParameter_basic() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta01","get","body",null);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertEquals("a", x.getExample());
		assertObjectEquals("{foo:'bar'}", x.getExamples());
	}
	@Test
	public void ta02_Body_onParameter_api() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta02","get","body",null);
		assertEquals("a\nb", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertEquals("a", x.getExample());
		assertObjectEquals("{foo:'bar'}", x.getExamples());
	}
	@Test
	public void ta03_Body_onParameter_mixed() throws Exception {
		ParameterInfo x = ta.getParameterInfo("/ta03","get","body",null);
		assertEquals("b\nc", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("{type:'string'}", x.getSchema());
		assertEquals("b", x.getExample());
		assertObjectEquals("{foo:'baz'}", x.getExamples());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TB {

		public static class TB01 {}
		@RestMethod
		public void tb01(@Body(schema=@Schema(" { type:'b' } ")) TB01 b) {}

		public static class TB02 {
			public String f1;
		}
		@RestMethod
		public void tb02(@Body TB02 b) {}

		public static class TB03 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void tb03(@Body TB03 b) {}

		public static class TB04 {}
		@RestMethod
		public void tb04(@Body TB04 b) {}

		@RestMethod
		public void tb05(@Body Integer b) {}

		@RestMethod
		public void tb06(@Body Boolean b) {}
	}

	static Swagger tb = getSwagger(TB.class);

	@Test
	public void tb01_Body_onParameter_schemaValue() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb01","get","body",null);
		assertObjectEquals("{type:'b'}", x.getSchema());
	}
	@Test
	public void tb02_Body_onParameter_autoDetectBean() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb02","get","body",null);
		assertObjectEquals("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());
	}
	@Test
	public void tb03_Body_onParameter_autoDetectList() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb03","get","body",null);
		assertObjectEquals("{type:'array',items:{type:'string'}}", x.getSchema());
	}
	@Test
	public void tb04_Body_onParameter_autoDetectStringObject() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb04","get","body",null);
		assertObjectEquals("{type:'string'}", x.getSchema());
	}
	@Test
	public void tb05_Body_onParameter_autoDetectInteger() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb05","get","body",null);
		assertObjectEquals("{format:'int32',type:'integer'}", x.getSchema());
	}
	@Test
	public void tb06_Body_onParameter_autoDetectBoolean() throws Exception {
		ParameterInfo x = tb.getParameterInfo("/tb06","get","body",null);
		assertObjectEquals("{type:'boolean'}", x.getSchema());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class TC {

		public static class TC01 {
			public String f1;
		}
		@RestMethod
		public void tc01(@Body(ex="{f1:'b'}") TC01 b) {}

		public static class TC02 {}
		@RestMethod
		public void tc02(@Body(exs={" foo:'bar' "}) TC02 b) {}
	}

	static Swagger tc = getSwagger(TC.class);

	@Test
	public void tc01_Body_onParameter_example() throws Exception {
		ParameterInfo x = tc.getParameterInfo("/tc01","get","body",null);
		assertEquals("{f1:'b'}", x.getExample());
	}
	@Test
	public void tc02_Body_onParameter_examples() throws Exception {
		ParameterInfo x = tc.getParameterInfo("/tc02","get","body",null);
		assertObjectEquals("{foo:'bar'}", x.getExamples());
	}
}
