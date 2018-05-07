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

import javax.servlet.http.*;

import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.junit.*;
import org.junit.runners.*;

@SuppressWarnings("javadoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BodyTest {

	private void call(Object resource, HttpServletRequest req, HttpServletResponse res) throws Exception {
		RestContext rc = RestContext.create(resource).build();
		rc.getCallHandler().service(req, res);
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
	}
	
	@Test
	public void a01_onParameter_string() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/string").body("'foo'");
		MockServletResponse res = MockServletResponse.create();
		call(new A(), req, res);
		assertEquals("'foo'", res.getBodyAsString());
	}
	@Test
	public void a02_onParameter_integer() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/integer").body("123");
		MockServletResponse res = MockServletResponse.create();
		call(new A(), req, res);
		assertEquals("123", res.getBodyAsString());
	}
	@Test
	public void a03_onParameter_boolean() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/boolean").body("true");
		MockServletResponse res = MockServletResponse.create();
		call(new A(), req, res);
		assertEquals("true", res.getBodyAsString());
	}
	@Test
	public void a04_onParameter_bean() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/bean").body("{f1:'a'}");
		MockServletResponse res = MockServletResponse.create();
		call(new A(), req, res);
		assertEquals("{f1:'a'}", res.getBodyAsString());
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
	}
	
	@Test
	public void a01_onPojo_string() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/string").body("'foo'");
		MockServletResponse res = MockServletResponse.create();
		call(new B(), req, res);
		assertEquals("'foo'", res.getBodyAsString());
	}
	@Test
	public void a04_onPojo_bean() throws Exception {
		MockServletRequest req = MockServletRequest.create("PUT", "/bean").body("{f1:'a'}");
		MockServletResponse res = MockServletResponse.create();
		call(new B(), req, res);
		assertEquals("{f1:'a'}", res.getBodyAsString());
	}
}
