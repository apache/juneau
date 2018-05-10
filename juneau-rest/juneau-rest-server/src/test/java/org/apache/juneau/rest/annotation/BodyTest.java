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

	@Test
	public void a01_onParameter_String() throws Exception {
		assertEquals("'foo'", MockRest.create(A.class).request("PUT", "/String").body("'foo'").execute().getBodyAsString());
	}
	@Test
	public void a02_onParameter_Integer() throws Exception {
		assertEquals("123", MockRest.create(A.class).request("PUT", "/Integer").body("123").execute().getBodyAsString());
	}
	@Test
	public void a03_onParameter_int() throws Exception {
		assertEquals("123", MockRest.create(A.class).request("PUT", "/int").body("123").execute().getBodyAsString());
	}
	@Test
	public void a04_onParameter_Boolean() throws Exception {
		assertEquals("true", MockRest.create(A.class).request("PUT", "/Boolean").body("true").execute().getBodyAsString());
	}
	@Test
	public void a05_onParameter_boolean() throws Exception {
		assertEquals("true", MockRest.create(A.class).request("PUT", "/boolean").body("true").execute().getBodyAsString());
	}
	@Test
	public void a06_onParameter_float() throws Exception {
		assertEquals("1.23", MockRest.create(A.class).request("PUT", "/float").body("1.23").execute().getBodyAsString());
	}
	@Test
	public void a07_onParameter_Float() throws Exception {
		assertEquals("1.23", MockRest.create(A.class).request("PUT", "/Float").body("1.23").execute().getBodyAsString());
	}
	@Test
	public void a11_onParameter_bean() throws Exception {
		assertEquals("{f1:'a'}", MockRest.create(A.class).request("PUT", "/bean").body("{f1:'a'}").execute().getBodyAsString());
	}
	@Test
	public void a12_onParameter_inputStream() throws Exception {
		assertEquals("'a'", MockRest.create(A.class).request("PUT", "/inputStream").body("a").execute().getBodyAsString());
	}
	@Test
	public void a13_onParameter_reader() throws Exception {
		assertEquals("'a'", MockRest.create(A.class).request("PUT", "/reader").body("a").execute().getBodyAsString());
	}
	@Test
	public void a14_onParameter_inputStreamTransform() throws Exception {
		assertEquals("'a'", MockRest.create(A.class).request("PUT", "/inputStreamTransform").body("a").execute().getBodyAsString());
	}
	@Test
	public void a15_onParameter_readerTransform() throws Exception {
		assertEquals("'a'", MockRest.create(A.class).request("PUT", "/readerTransform").body("a").execute().getBodyAsString());
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
	
	@Test
	public void b01_onPojo_string() throws Exception {
		assertEquals("'foo'", MockRest.create(B.class).request("PUT", "/string").body("'foo'").execute().getBodyAsString());
	}
	@Test
	public void b02_onPojo_bean() throws Exception {
		assertEquals("{f1:'a'}", MockRest.create(B.class).request("PUT", "/bean").body("{f1:'a'}").execute().getBodyAsString());
	}
	@Test
	public void b03_onPojo_beanList() throws Exception {
		assertEquals("[{f1:'a'}]", MockRest.create(B.class).request("PUT", "/beanList").body("[{f1:'a'}]").execute().getBodyAsString());
	}
	@Test
	public void b04_onPojo_inputStream() throws Exception {
		assertEquals("'a'", MockRest.create(B.class).request("PUT", "/inputStreamTransform").body("a").execute().getBodyAsString());
	}
	@Test
	public void b05_onPojo_reader() throws Exception {
		assertEquals("'a'", MockRest.create(B.class).request("PUT", "/readerTransform").body("a").execute().getBodyAsString());
	}
}
