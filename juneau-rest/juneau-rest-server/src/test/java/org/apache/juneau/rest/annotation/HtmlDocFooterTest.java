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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @HtmlDoc(footer) annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({"javadoc","serial"})
public class HtmlDocFooterTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@RestResource(htmldoc=@HtmlDoc(footer={"a01a","a01b"}))
	public static class A extends BasicRestServlet {
		@RestMethod(path="/a01")
		public Object a01() {
			return "OK";
		}
		@RestMethod(path="/a02", htmldoc=@HtmlDoc(footer={"a02a","a02b"}))
		public Object a02() {
			return "OK";
		}
		@RestMethod(path="/a03", htmldoc=@HtmlDoc(footer={"INHERIT","a03a","a03b"}))
		public Object a03() {
			return "OK";
		}
		@RestMethod(path="/a04", htmldoc=@HtmlDoc(footer={"a04a","INHERIT","a04b"}))
		public Object a04() {
			return "OK";
		}
		@RestMethod(path="/a05", htmldoc=@HtmlDoc(footer={"a05a","a05b","INHERIT"}))
		public Object a05() {
			return "OK";
		}
	}
	static MockRest a = MockRest.create(A.class);

	@Test
	public void a01() throws Exception {
		a.request("GET", "/a01").accept("text/html").execute().assertBodyContains("<footer>a01a a01b</footer>");
	}
	@Test
	public void a02() throws Exception {
		a.request("GET", "/a02").accept("text/html").execute().assertBodyContains("<footer>a02a a02b</footer>");
	}
	@Test
	public void a03() throws Exception {
		a.request("GET", "/a03").accept("text/html").execute().assertBodyContains("<footer>a01a a01b a03a a03b</footer>");
	}
	@Test
	public void a04() throws Exception {
		a.request("GET", "/a04").accept("text/html").execute().assertBodyContains("<footer>a04a a01a a01b a04b</footer>");
	}
	@Test
	public void a05() throws Exception {
		a.request("GET", "/a05").accept("text/html").execute().assertBodyContains("<footer>a05a a05b a01a a01b</footer>");
	}

	//=================================================================================================================
	// Inheritance
	//=================================================================================================================
	
	@RestResource(htmldoc=@HtmlDoc(footer={"b01a","INHERIT","b01b"}))
	public static class B extends A {
		@RestMethod(path="/b01")
		public Object b01() {
			return "OK";
		}
		@RestMethod(path="/b02", htmldoc=@HtmlDoc(footer={"b02a","b02b"}))
		public Object b02() {
			return "OK";
		}
		@RestMethod(path="/b03", htmldoc=@HtmlDoc(footer={"INHERIT","b03a","b03b"}))
		public Object b03() {
			return "OK";
		}
		@RestMethod(path="/b04", htmldoc=@HtmlDoc(footer={"b04a","INHERIT","b04b"}))
		public Object b04() {
			return "OK";
		}
		@RestMethod(path="/b05", htmldoc=@HtmlDoc(footer={"b05a","b05b","INHERIT"}))
		public Object b05() {
			return "OK";
		}
	}
	static MockRest b = MockRest.create(B.class);

	@Test
	public void b01() throws Exception {
		b.request("GET", "/b01").accept("text/html").execute().assertBodyContains("<footer>b01a a01a a01b b01b</footer>");
	}
	@Test
	public void b02() throws Exception {
		b.request("GET", "/b02").accept("text/html").execute().assertBodyContains("<footer>b02a b02b</footer>");
	}
	@Test
	public void b03() throws Exception {
		b.request("GET", "/b03").accept("text/html").execute().assertBodyContains("<footer>b01a a01a a01b b01b b03a b03b</footer>");
	}
	@Test
	public void b04() throws Exception {
		b.request("GET", "/b04").accept("text/html").execute().assertBodyContains("<footer>b04a b01a a01a a01b b01b b04b</footer>");
	}
	@Test
	public void b05() throws Exception {
		b.request("GET", "/b05").accept("text/html").execute().assertBodyContains("<footer>b05a b05b b01a a01a a01b b01b</footer>");
	}
}