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

import static org.apache.juneau.http.HttpMethodName.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;


import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the {@link Body} annotation.
 */
@SuppressWarnings({"javadoc","serial"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BodyTest {
	
	//-----------------------------------------------------------------------------------------------------------------
	// @Body on parameter
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource(serializers=JsonSerializer.Simple.class, parsers=JsonParser.class)
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
		
		@RestMethod(name=PUT, path="/bean")
		public A11 a11(@Body A11 b) {
			return b;
		}

		@RestMethod(name=PUT, path="/inputStream")
		public String a12(@Body InputStream b) throws Exception {
			return IOUtils.read(b);
		}

		@RestMethod(name=PUT, path="/reader")
		public String a13(@Body Reader b) throws Exception {
			return IOUtils.read(b);
		}

		public static class A14 {
			String s;
			
			public A14(InputStream in) throws Exception {
				this.s = IOUtils.read(in);
			}
			
			@Override /* Object */
			public String toString() {
				return s;
			}
		}
		
		@RestMethod(name=PUT, path="/inputStreamTransform")
		public A14 a14(@Body A14 b) throws Exception {
			return b;
		}

		public static class A15 {
			private String s;
			
			public A15(Reader in) throws Exception {
				this.s = IOUtils.read(in);
			}
			
			@Override /* Object */
			public String toString() {
				return s;
			}
		}
		
		@RestMethod(name=PUT, path="/readerTransform")
		public A15 a15(@Body A15 b) throws Exception {
			return b;
		}
	}
	
	private MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01_onParameter_String() throws Exception {
		assertEquals("'foo'", a.request("PUT", "/String").body("'foo'").execute().getBodyAsString());
	}
	@Test
	public void a02_onParameter_Integer() throws Exception {
		assertEquals("123", a.request("PUT", "/Integer").body("123").execute().getBodyAsString());
	}
	@Test
	public void a03_onParameter_int() throws Exception {
		assertEquals("123", a.request("PUT", "/int").body("123").execute().getBodyAsString());
	}
	@Test
	public void a04_onParameter_Boolean() throws Exception {
		assertEquals("true", a.request("PUT", "/Boolean").body("true").execute().getBodyAsString());
	}
	@Test
	public void a05_onParameter_boolean() throws Exception {
		assertEquals("true", a.request("PUT", "/boolean").body("true").execute().getBodyAsString());
	}
	@Test
	public void a06_onParameter_float() throws Exception {
		assertEquals("1.23", a.request("PUT", "/float").body("1.23").execute().getBodyAsString());
	}
	@Test
	public void a07_onParameter_Float() throws Exception {
		assertEquals("1.23", a.request("PUT", "/Float").body("1.23").execute().getBodyAsString());
	}
	@Test
	public void a08_onParameter_Map() throws Exception {
		assertEquals("{foo:123}", a.request("PUT", "/Map").body("{foo:123}").execute().getBodyAsString());
	}
	@Test
	public void a09_onParameter_enum() throws Exception {
		assertEquals("'ONE'", a.request("PUT", "/enum").body("'ONE'").execute().getBodyAsString());
	}
	@Test
	public void a11_onParameter_bean() throws Exception {
		assertEquals("{f1:'a'}", a.request("PUT", "/bean").body("{f1:'a'}").execute().getBodyAsString());
	}
	@Test
	public void a12_onParameter_inputStream() throws Exception {
		assertEquals("'a'", a.request("PUT", "/inputStream").body("a").execute().getBodyAsString());
	}
	@Test
	public void a13_onParameter_reader() throws Exception {
		assertEquals("'a'", a.request("PUT", "/reader").body("a").execute().getBodyAsString());
	}
	@Test
	public void a14_onParameter_inputStreamTransform() throws Exception {
		assertEquals("'a'", a.request("PUT", "/inputStreamTransform").body("a").execute().getBodyAsString());
	}
	@Test
	public void a15_onParameter_readerTransform() throws Exception {
		assertEquals("'a'", a.request("PUT", "/readerTransform").body("a").execute().getBodyAsString());
	}
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// @Body on POJO
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource(serializers=JsonSerializer.Simple.class, parsers=JsonParser.class)
	public static class B {
		
		@Body 
		public static class B01 {
			private String val;
			
			public B01(String val) {
				this.val = val;
			}
			
			@Override
			public String toString() {
				return val;
			}
		}
		
		@RestMethod(name=PUT, path="/string")
		public B01 simple(B01 b) {
			return b;
		}

		@Body
		public static class B02 {
			public String f1;
		}
		
		@RestMethod(name=PUT, path="/bean")
		public B02 b02(B02 b) {
			return b;
		}

		@Body
		public static class B03 extends LinkedList<B02> {}
		
		@RestMethod(name=PUT, path="/beanList")
		public B03 b03(B03 b) {
			return b;
		}
		
		@Body 
		public static class B04 {
			String s;
			
			public B04(InputStream in) throws Exception {
				this.s = IOUtils.read(in);
			}
			
			@Override /* Object */
			public String toString() {
				return s;
			}
		}
		
		@RestMethod(name=PUT, path="/inputStreamTransform")
		public B04 b04(B04 b) throws Exception {
			return b;
		}
		
		@Body 
		public static class B05 {
			private String s;
			
			public B05(Reader in) throws Exception {
				this.s = IOUtils.read(in);
			}
			
			@Override /* Object */
			public String toString() {
				return s;
			}
		}
		
		@RestMethod(name=PUT, path="/readerTransform")
		public B05 b05(B05 b) throws Exception {
			return b;
		}
	}
	
	private MockRest b = MockRest.create(B.class);

	@Test
	public void b01_onPojo_string() throws Exception {
		assertEquals("'foo'", b.request("PUT", "/string").body("'foo'").execute().getBodyAsString());
	}
	@Test
	public void b02_onPojo_bean() throws Exception {
		assertEquals("{f1:'a'}", b.request("PUT", "/bean").body("{f1:'a'}").execute().getBodyAsString());
	}
	@Test
	public void b03_onPojo_beanList() throws Exception {
		assertEquals("[{f1:'a'}]", b.request("PUT", "/beanList").body("[{f1:'a'}]").execute().getBodyAsString());
	}
	@Test
	public void b04_onPojo_inputStream() throws Exception {
		assertEquals("'a'", b.request("PUT", "/inputStreamTransform").body("a").execute().getBodyAsString());
	}
	@Test
	public void b05_onPojo_reader() throws Exception {
		assertEquals("'a'", b.request("PUT", "/readerTransform").body("a").execute().getBodyAsString());
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests using @Body parameter
	//-----------------------------------------------------------------------------------------------------------------

	public void c01_bodyParam_String() throws Exception {
		assertEquals("'foo'", a.request("PUT", "/String?body=foo").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/String?body=null").execute().getBodyAsString());
		assertEquals("''", a.request("PUT", "/String?body=").execute().getBodyAsString());
	}
	@Test
	public void c02_bodyParam_Integer() throws Exception {
		assertEquals("123", a.request("PUT", "/Integer?body=123").execute().getBodyAsString());
		assertEquals("-123", a.request("PUT", "/Integer?body=-123").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/Integer?body=null").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/Integer?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/Integer?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c03_bodyParam_int() throws Exception {
		assertEquals("123", a.request("PUT", "/int?body=123").execute().getBodyAsString());
		assertEquals("-123", a.request("PUT", "/int?body=-123").execute().getBodyAsString());
		assertEquals("0", a.request("PUT", "/int?body=null").execute().getBodyAsString());
		assertEquals("0", a.request("PUT", "/int?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/int?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c04_bodyParam_Boolean() throws Exception {
		assertEquals("true", a.request("PUT", "/Boolean?body=true").execute().getBodyAsString());
		assertEquals("false", a.request("PUT", "/Boolean?body=false").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/Boolean?body=null").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/Boolean?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/Boolean?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c05_bodyParam_boolean() throws Exception {
		assertEquals("true", a.request("PUT", "/boolean?body=true").execute().getBodyAsString());
		assertEquals("false", a.request("PUT", "/boolean?body=false").execute().getBodyAsString());
		assertEquals("false", a.request("PUT", "/boolean?body=null").execute().getBodyAsString());
		assertEquals("false", a.request("PUT", "/boolean?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/boolean?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c06_bodyParam_Float() throws Exception {
		assertEquals("1.23", a.request("PUT", "/Float?body=1.23").execute().getBodyAsString());
		assertEquals("-1.23", a.request("PUT", "/Float?body=-1.23").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/Float?body=null").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/Float?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/Float?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c07_bodyParam_float() throws Exception {
		assertEquals("1.23", a.request("PUT", "/float?body=1.23").execute().getBodyAsString());
		assertEquals("-1.23", a.request("PUT", "/float?body=-1.23").execute().getBodyAsString());
		assertEquals("0.0", a.request("PUT", "/float?body=null").execute().getBodyAsString());
		assertEquals("0.0", a.request("PUT", "/float?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/float?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c08_bodyParam_Map() throws Exception {
		assertEquals("{foo:123}", a.request("PUT", "/Map?body=(foo=123)").execute().getBodyAsString());
		assertEquals("{}", a.request("PUT", "/Map?body=()").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/Map?body=null").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/Map?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/Map?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c09_bodyParam_enum() throws Exception {
		assertEquals("'ONE'", a.request("PUT", "/enum?body=ONE").execute().getBodyAsString());
		assertEquals("'TWO'", a.request("PUT", "/enum?body=TWO").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/enum?body=null").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/enum?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/enum?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c11_bodyParam_bean() throws Exception {
		assertEquals("{f1:'a'}", a.request("PUT", "/bean?body=(f1=a)").execute().getBodyAsString());
		assertEquals("{}", a.request("PUT", "/bean?body=()").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/bean?body=null").execute().getBodyAsString());
		assertEquals("null", a.request("PUT", "/bean?body=").execute().getBodyAsString());
		assertEquals(400, a.request("PUT", "/bean?body=bad&noTrace=true").execute().getStatus());
	}
	@Test
	public void c12_bodyParam_inputStream() throws Exception {
		assertEquals("'a'", a.request("PUT", "/inputStream?body=a").execute().getBodyAsString());
		assertEquals("'null'", a.request("PUT", "/inputStream?body=null").execute().getBodyAsString());
		assertEquals("''", a.request("PUT", "/inputStream?body=").execute().getBodyAsString());
	}
	@Test
	public void c13_bodyParam_reader() throws Exception {
		assertEquals("'a'", a.request("PUT", "/reader?body=a").execute().getBodyAsString());
		assertEquals("'null'", a.request("PUT", "/reader?body=null").execute().getBodyAsString());
		assertEquals("''", a.request("PUT", "/reader?body=").execute().getBodyAsString());
	}
	@Test
	public void c14_bodyParam_inputStreamTransform() throws Exception {
		assertEquals("'a'", a.request("PUT", "/inputStreamTransform?body=a").execute().getBodyAsString());
		assertEquals("'null'", a.request("PUT", "/inputStreamTransform?body=null").execute().getBodyAsString());
		assertEquals("''", a.request("PUT", "/inputStreamTransform?body=").execute().getBodyAsString());
	}
	@Test
	public void c15_bodyParam_readerTransform() throws Exception {
		assertEquals("'a'", a.request("PUT", "/readerTransform?body=a").execute().getBodyAsString());
		assertEquals("'null'", a.request("PUT", "/readerTransform?body=null").execute().getBodyAsString());
		assertEquals("''", a.request("PUT", "/readerTransform?body=").execute().getBodyAsString());
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// No serializers or parsers needed when using only streams and readers.
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource
	public static class D {
		
		@RestMethod(name=PUT, path="/inputStream")
		public InputStream d01(@Body InputStream b) throws Exception {
			return b;
		}

		@RestMethod(name=PUT, path="/reader")
		public Reader d02(@Body Reader b) throws Exception {
			return b;
		}

		public static class D03 {
			String s;
			
			public D03(InputStream in) throws Exception {
				this.s = IOUtils.read(in);
			}
			
			@Override /* Object */
			public String toString() {
				return s;
			}
		}
		
		@RestMethod(name=PUT, path="/inputStreamTransform")
		public Reader d03(@Body D03 b) throws Exception {
			return new StringReader(b.toString());
		}

		public static class D04 {
			private String s;
			
			public D04(Reader in) throws Exception {
				this.s = IOUtils.read(in);
			}
			
			@Override /* Object */
			public String toString() {
				return s;
			}
		}
		
		@RestMethod(name=PUT, path="/readerTransform")
		public Reader d04(@Body D04 b) throws Exception {
			return new StringReader(b.toString());
		}
	}
	
	private MockRest d = MockRest.create(D.class);
	
	@Test
	public void d01_noMediaTypes_inputStream() throws Exception {
		assertEquals("a", d.request("PUT", "/inputStream").body("a").execute().getBodyAsString());
	}
	@Test
	public void d02_noMediaTypes_reader() throws Exception {
		assertEquals("a", d.request("PUT", "/reader").body("a").execute().getBodyAsString());
	}
	@Test
	public void d03_noMediaTypes_inputStreamTransform() throws Exception {
		assertEquals("a", d.request("PUT", "/inputStreamTransform").body("a").execute().getBodyAsString());
	}
	@Test
	public void d04_noMediaTypes_readerTransform() throws Exception {
		assertEquals("a", d.request("PUT", "/readerTransform").body("a").execute().getBodyAsString());
	}
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// Complex POJOs
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource(serializers=JsonSerializer.Simple.class, parsers=JsonParser.class)
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
	
	private MockRest e = MockRest.create(E.class);
	
	@Test
	public void e01_complexPojos_B_body() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		assertEquals(expected, e.request("PUT", "/B").body(JsonSerializer.DEFAULT_LAX.toString(DTOs.B.INSTANCE)).execute().getBodyAsString());
	}
	@Test
	public void e02_complexPojos_B_bodyParam() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		assertEquals(expected, e.request("PUT", "/B?body=" + UonSerializer.DEFAULT.serialize(DTOs.B.INSTANCE)).body("a").execute().getBodyAsString());
	}
	@Test
	public void e03_complexPojos_C_body() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		assertEquals(expected, e.request("PUT", "/C").body(JsonSerializer.DEFAULT_LAX.toString(DTOs.B.INSTANCE)).execute().getBodyAsString());
	}
	@Test
	public void e04_complexPojos_C_bodyParam() throws Exception {
		String expected = "{f01:['a','b'],f02:['c','d'],f03:[1,2],f04:[3,4],f05:[['e','f'],['g','h']],f06:[['i','j'],['k','l']],f07:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f08:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f09:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f10:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f11:['a','b'],f12:['c','d'],f13:[1,2],f14:[3,4],f15:[['e','f'],['g','h']],f16:[['i','j'],['k','l']],f17:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f18:[{a:'a',b:1,c:true},{a:'a',b:1,c:true}],f19:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]],f20:[[{a:'a',b:1,c:true}],[{a:'a',b:1,c:true}]]}";
		assertEquals(expected, e.request("PUT", "/C?body=" + UonSerializer.DEFAULT.serialize(DTOs.B.INSTANCE)).body("a").execute().getBodyAsString());
	}
}
