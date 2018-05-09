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

import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the {@link Body} annotation.
 */
@SuppressWarnings({"javadoc","serial"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BodyTest {
	
	private static Map<Class<?>,RestContext> CONTEXTS = new HashMap<>();
	
	private static void call(Class<?> c, HttpServletRequest req, HttpServletResponse res) throws Exception {
		if (! CONTEXTS.containsKey(c))
			CONTEXTS.put(c, RestContext.create(c.newInstance()).build());
		CONTEXTS.get(c).getCallHandler().service(req, res);
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// @Body on parameter
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource(serializers=JsonSerializer.Simple.class, parsers=JsonParser.class)
	public static class A {
		
		@RestMethod(name=PUT, path="/string")
		public String a01(@Body String b) {
			return b;
		}

		@RestMethod(name=PUT, path="/integer")
		public Integer a02(@Body Integer b) {
			return b;
		}

		@RestMethod(name=PUT, path="/boolean")
		public Boolean a03(@Body Boolean b) {
			return b;
		}

		public static class A04 {
			public String f1;
		}
		
		@RestMethod(name=PUT, path="/bean")
		public A04 a04(@Body A04 b) {
			return b;
		}

		public static class A05 {
			String s;
			
			public A05(InputStream in) throws Exception {
				this.s = IOUtils.read(in);
			}
			
			@Override /* Object */
			public String toString() {
				return s;
			}
		}
		
		@RestMethod(name=PUT, path="/inputStream")
		public A05 a05(@Body A05 b) throws Exception {
			return b;
		}

		public static class A06 {
			private String s;
			
			public A06(Reader in) throws Exception {
				this.s = IOUtils.read(in);
			}
			
			@Override /* Object */
			public String toString() {
				return s;
			}
		}
		
		@RestMethod(name=PUT, path="/reader")
		public A06 a06(@Body A06 b) throws Exception {
			return b;
		}
	}
	
	@Test
	public void a01_onParameter_string() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/string").body("'foo'");
		MockServletResponse res = MockServletResponse.create();
		call(A.class, req, res);
		assertEquals("'foo'", res.getBodyAsString());
	}
	@Test
	public void a02_onParameter_integer() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/integer").body("123");
		MockServletResponse res = MockServletResponse.create();
		call(A.class, req, res);
		assertEquals("123", res.getBodyAsString());
	}
	@Test
	public void a03_onParameter_boolean() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/boolean").body("true");
		MockServletResponse res = MockServletResponse.create();
		call(A.class, req, res);
		assertEquals("true", res.getBodyAsString());
	}
	@Test
	public void a04_onParameter_bean() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/bean").body("{f1:'a'}");
		MockServletResponse res = MockServletResponse.create();
		call(A.class, req, res);
		assertEquals("{f1:'a'}", res.getBodyAsString());
	}
	@Test
	public void a05_onParameter_inputStream() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/inputStream").body("a");
		MockServletResponse res = MockServletResponse.create();
		call(A.class, req, res);
		assertEquals("'a'", res.getBodyAsString());
	}
	@Test
	public void a06_onParameter_reader() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/reader").body("a");
		MockServletResponse res = MockServletResponse.create();
		call(A.class, req, res);
		assertEquals("'a'", res.getBodyAsString());
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
		
		@RestMethod(name=PUT, path="/inputStream")
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
		
		@RestMethod(name=PUT, path="/reader")
		public B05 b05(B05 b) throws Exception {
			return b;
		}
	}
	
	@Test
	public void b01_onPojo_string() throws Exception {
		MockServletRequest req = MockServletRequest.create(PUT, "/string").body("'foo'");
		MockServletResponse res = MockServletResponse.create();
		call(B.class, req, res);
		assertEquals("'foo'", res.getBodyAsString());
	}
	@Test
	public void b02_onPojo_bean() throws Exception {
		MockServletRequest req = MockServletRequest.create(PUT, "/bean").body("{f1:'a'}");
		MockServletResponse res = MockServletResponse.create();
		call(B.class, req, res);
		assertEquals("{f1:'a'}", res.getBodyAsString());
	}
	@Test
	public void b03_onPojo_beanList() throws Exception {
		MockServletRequest req = MockServletRequest.create(PUT, "/beanList").body("[{f1:'a'}]");
		MockServletResponse res = MockServletResponse.create();
		call(B.class, req, res);
		assertEquals("[{f1:'a'}]", res.getBodyAsString());
	}
	@Test
	public void b04_onPojo_inputStream() throws Exception {
		MockServletRequest req = MockServletRequest.create(PUT, "/inputStream").body("a");
		MockServletResponse res = MockServletResponse.create();
		call(B.class, req, res);
		assertEquals("'a'", res.getBodyAsString());
	}
	@Test
	public void b05_onPojo_reader() throws Exception {
		MockServletRequest req = MockServletRequest.create(PUT, "/reader").body("a");
		MockServletResponse res = MockServletResponse.create();
		call(B.class, req, res);
		assertEquals("'a'", res.getBodyAsString());
	}
}
