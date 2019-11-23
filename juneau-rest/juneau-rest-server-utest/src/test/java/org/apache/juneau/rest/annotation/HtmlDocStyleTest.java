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
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @HtmlDoc(style) annotation.
 *
 * TODO - Remove in 9.0.  Replaced by HtmlDocConfigStyleTest.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({"serial","deprecation"})
public class HtmlDocStyleTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@Rest(htmldoc=@HtmlDoc(style={"a01a","a01b"},stylesheet="a01s",nowrap="false"))
	public static class A extends BasicRestServlet {
		@RestMethod(path="/a01")
		public Object a01() {
			return "OK";
		}
		@RestMethod(path="/a02", htmldoc=@HtmlDoc(style={"a02a","a02b"},stylesheet="a02s"))
		public Object a02() {
			return "OK";
		}
		@RestMethod(path="/a03", htmldoc=@HtmlDoc(style={"INHERIT","a03a","a03b"}))
		public Object a03() {
			return "OK";
		}
		@RestMethod(path="/a04", htmldoc=@HtmlDoc(style={"a04a","INHERIT","a04b"}))
		public Object a04() {
			return "OK";
		}
		@RestMethod(path="/a05", htmldoc=@HtmlDoc(style={"a05a","a05b","INHERIT"}))
		public Object a05() {
			return "OK";
		}
	}
	static MockRest a = MockRest.build(A.class);

	@Test
	public void a01() throws Exception {
		a.get("/a01").accept("text/html").execute().assertBodyContains("<style>@import \"/a01s\"; a01a a01b</style>");
	}
	@Test
	public void a02() throws Exception {
		a.get("/a02").accept("text/html").execute().assertBodyContains("<style>@import \"/a02s\"; a02a a02b</style>");
	}
	@Test
	public void a03() throws Exception {
		a.get("/a03").accept("text/html").execute().assertBodyContains("<style>@import \"/a01s\"; a01a a01b a03a a03b</style>");
	}
	@Test
	public void a04() throws Exception {
		a.get("/a04").accept("text/html").execute().assertBodyContains("<style>@import \"/a01s\"; a04a a01a a01b a04b</style>");
	}
	@Test
	public void a05() throws Exception {
		a.get("/a05").accept("text/html").execute().assertBodyContains("<style>@import \"/a01s\"; a05a a05b a01a a01b</style>");
	}

	//=================================================================================================================
	// Inheritance
	//=================================================================================================================

	@Rest(htmldoc=@HtmlDoc(style={"b01a","b01b"},stylesheet="b01s"))
	public static class B extends A {
		@RestMethod(path="/b01")
		public Object b01() {
			return "OK";
		}
		@RestMethod(path="/b02", htmldoc=@HtmlDoc(style={"b02a","b02b"},stylesheet="b02s"))
		public Object b02() {
			return "OK";
		}
		@RestMethod(path="/b03", htmldoc=@HtmlDoc(style={"INHERIT","b03a","b03b"}))
		public Object b03() {
			return "OK";
		}
		@RestMethod(path="/b04", htmldoc=@HtmlDoc(style={"b04a","INHERIT","b04b"}))
		public Object b04() {
			return "OK";
		}
		@RestMethod(path="/b05", htmldoc=@HtmlDoc(style={"b05a","b05b","INHERIT"}))
		public Object b05() {
			return "OK";
		}
	}
	static MockRest b = MockRest.build(B.class);

	@Test
	public void b01() throws Exception {
		b.get("/b01").accept("text/html").execute().assertBodyContains("<style>@import \"/b01s\"; b01a b01b</style>");
	}
	@Test
	public void b02() throws Exception {
		b.get("/b02").accept("text/html").execute().assertBodyContains("<style>@import \"/b02s\"; b02a b02b</style>");
	}
	@Test
	public void b03() throws Exception {
		b.get("/b03").accept("text/html").execute().assertBodyContains("<style>@import \"/b01s\"; b01a b01b b03a b03b</style>");
	}
	@Test
	public void b04() throws Exception {
		b.get("/b04").accept("text/html").execute().assertBodyContains("<style>@import \"/b01s\"; b04a b01a b01b b04b</style>");
	}
	@Test
	public void b05() throws Exception {
		b.get("/b05").accept("text/html").execute().assertBodyContains("<style>@import \"/b01s\"; b05a b05b b01a b01b</style>");
	}
}