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
 * Tests related to @HtmlDoc(navlinks) annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings({"javadoc","serial"})
public class HtmlDocNavlinksTest {
	
	//=================================================================================================================
	// Basic tests
	//=================================================================================================================
	
	@RestResource(htmldoc=@HtmlDoc(navlinks={"a01a","a01b"}))
	public static class A extends BasicRestServlet {
		@RestMethod(path="/a01")
		public Object a01() {
			return "OK";
		}
		@RestMethod(path="/a02", htmldoc=@HtmlDoc(navlinks={"a02a","a02b"}))
		public Object test2() {
			return "OK";
		}
		@RestMethod(path="/a03", htmldoc=@HtmlDoc(navlinks={"INHERIT","a03a","a03b"}))
		public Object a03() {
			return "OK";
		}
		@RestMethod(path="/a04", htmldoc=@HtmlDoc(navlinks={"a04a","INHERIT","a04b"}))
		public Object test4() {
			return "OK";
		}
		@RestMethod(path="/a05", htmldoc=@HtmlDoc(navlinks={"a05a","a05b","INHERIT"}))
		public Object test5() {
			return "OK";
		}
		@RestMethod(path="/a06", htmldoc=@HtmlDoc(navlinks={"INHERIT","[0]:a06a","[3]:a06b"}))
		public Object test6a() {
			return "OK";
		}
		@RestMethod(path="/a07", htmldoc=@HtmlDoc(navlinks={"[1]:a07a","[2]:a07b","INHERIT"}))
		public Object test6b() {
			return "OK";
		}
		@RestMethod(path="/a08", htmldoc=@HtmlDoc(navlinks={"[1]:a08a","[0]:a08b"}))
		public Object test6c() {
			return "OK";
		}
		@RestMethod(path="/a09", htmldoc=@HtmlDoc(navlinks={"INHERIT","foo[0]:a09a","bar[3]:a09b"}))
		public Object test6d() {
			return "OK";
		}
		@RestMethod(path="/a10", htmldoc=@HtmlDoc(navlinks={"foo[1]:a10a","bar[2]:a10b","INHERIT"}))
		public Object test6e() {
			return "OK";
		}
		@RestMethod(path="/a11", htmldoc=@HtmlDoc(navlinks={"foo[1]:a11a","bar[0]:a11b"}))
		public Object test6f() {
			return "OK";
		}
	}
	static MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01() throws Exception {
		a.request("GET", "/a01").accept("text/html").execute().assertBodyContains("<nav><ol><li>a01a</li><li>a01b</li></ol></nav>");
	}
	@Test
	public void a02() throws Exception {
		a.request("GET", "/a02").accept("text/html").execute().assertBodyContains("<nav><ol><li>a02a</li><li>a02b</li></ol></nav>");
	}
	@Test
	public void a03() throws Exception {
		a.request("GET", "/a03").accept("text/html").execute().assertBodyContains("<nav><ol><li>a01a</li><li>a01b</li><li>a03a</li><li>a03b</li></ol></nav>");
	}
	@Test
	public void a04() throws Exception {
		a.request("GET", "/a04").accept("text/html").execute().assertBodyContains("<nav><ol><li>a04a</li><li>a01a</li><li>a01b</li><li>a04b</li></ol></nav>");
	}
	@Test
	public void a05() throws Exception {
		a.request("GET", "/a05").accept("text/html").execute().assertBodyContains("<nav><ol><li>a05a</li><li>a05b</li><li>a01a</li><li>a01b</li></ol></nav>");
	}
	@Test
	public void a06() throws Exception {
		a.request("GET", "/a06").accept("text/html").execute().assertBodyContains("<nav><ol><li>a06a</li><li>a01a</li><li>a01b</li><li>a06b</li></ol></nav>");
	}
	@Test
	public void a07() throws Exception {
		a.request("GET", "/a07").accept("text/html").execute().assertBodyContains("<nav><ol><li>a07a</li><li>a07b</li><li>a01a</li><li>a01b</li></ol></nav>");
	}
	@Test
	public void a08() throws Exception {
		a.request("GET", "/a08").accept("text/html").execute().assertBodyContains("<nav><ol><li>a08b</li><li>a08a</li></ol></nav>");
	}
	@Test
	public void a09() throws Exception {
		a.request("GET", "/a09").accept("text/html").execute().assertBodyContains("<nav><ol><li><a href=\"/a09a\">foo</a></li><li>a01a</li><li>a01b</li><li><a href=\"/a09b\">bar</a></li></ol></nav>");
	}
	@Test
	public void a10() throws Exception {
		a.request("GET", "/a10").accept("text/html").execute().assertBodyContains("<nav><ol><li><a href=\"/a10a\">foo</a></li><li><a href=\"/a10b\">bar</a></li><li>a01a</li><li>a01b</li></ol></nav>");
	}
	@Test
	public void a11() throws Exception {
		a.request("GET", "/a11").accept("text/html").execute().assertBodyContains("<nav><ol><li><a href=\"/a11b\">bar</a></li><li><a href=\"/a11a\">foo</a></li></ol></nav>");
	}
	
	//=================================================================================================================
	// Inheritance
	//=================================================================================================================
	
	@RestResource(htmldoc=@HtmlDoc(navlinks={"INHERIT","b01a","b01b"}))
	public static class B extends A {
		@RestMethod(path="/b01")
		public Object b01() {
			return "OK";
		}
		@RestMethod(path="/b02", htmldoc=@HtmlDoc(navlinks={"b02a","b02b"}))
		public Object b02() {
			return "OK";
		}
		@RestMethod(path="/b03", htmldoc=@HtmlDoc(navlinks={"INHERIT","b03a","b03b"}))
		public Object b03() {
			return "OK";
		}
		@RestMethod(path="/b04", htmldoc=@HtmlDoc(navlinks={"b04a","INHERIT","b04b"}))
		public Object b04() {
			return "OK";
		}
		@RestMethod(path="/b05", htmldoc=@HtmlDoc(navlinks={"b05a","b05b","INHERIT"}))
		public Object b05() {
			return "OK";
		}
		@RestMethod(path="/b06", htmldoc=@HtmlDoc(navlinks={"INHERIT","[0]:b06a","[3]:b06b"}))
		public Object b06() {
			return "OK";
		}
		@RestMethod(path="/b07", htmldoc=@HtmlDoc(navlinks={"[1]:b07a","[2]:b07b","INHERIT"}))
		public Object b07() {
			return "OK";
		}
		@RestMethod(path="/b08", htmldoc=@HtmlDoc(navlinks={"[1]:b08a","[0]:b08b"}))
		public Object b08() {
			return "OK";
		}
		@RestMethod(path="/b09", htmldoc=@HtmlDoc(navlinks={"INHERIT","foo[0]:b09a","bar[3]:b09b"}))
		public Object b09() {
			return "OK";
		}
		@RestMethod(path="/b10", htmldoc=@HtmlDoc(navlinks={"foo[1]:b10a","bar[2]:b10b","INHERIT"}))
		public Object b10() {
			return "OK";
		}
		@RestMethod(path="/b11", htmldoc=@HtmlDoc(navlinks={"foo[1]:b11a","bar[0]:b11b"}))
		public Object b11() {
			return "OK";
		}
	}
	static MockRest b = MockRest.create(B.class);
	
	
	@Test
	public void b01() throws Exception {
		b.request("GET", "/b01").accept("text/html").execute().assertBodyContains("<nav><ol><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li></ol></nav>");
	}
	@Test
	public void b02() throws Exception {
		b.request("GET", "/b02").accept("text/html").execute().assertBodyContains("<nav><ol><li>b02a</li><li>b02b</li></ol></nav>");
	}
	@Test
	public void b03() throws Exception {
		b.request("GET", "/b03").accept("text/html").execute().assertBodyContains("<nav><ol><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li><li>b03a</li><li>b03b</li></ol></nav>");
	}
	@Test
	public void b04() throws Exception {
		b.request("GET", "/b04").accept("text/html").execute().assertBodyContains("<nav><ol><li>b04a</li><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li><li>b04b</li></ol></nav>");
	}
	@Test
	public void b05() throws Exception {
		b.request("GET", "/b05").accept("text/html").execute().assertBodyContains("<nav><ol><li>b05a</li><li>b05b</li><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li></ol></nav>");
	}
	@Test
	public void b06() throws Exception {
		b.request("GET", "/b06").accept("text/html").execute().assertBodyContains("<nav><ol><li>b06a</li><li>a01a</li><li>a01b</li><li>b06b</li><li>b01a</li><li>b01b</li></ol></nav>");
	}
	@Test
	public void b07() throws Exception {
		b.request("GET", "/b07").accept("text/html").execute().assertBodyContains("<nav><ol><li>b07a</li><li>b07b</li><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li></ol></nav>");
	}
	@Test
	public void b08() throws Exception {
		b.request("GET", "/b08").accept("text/html").execute().assertBodyContains("<nav><ol><li>b08b</li><li>b08a</li></ol></nav>");
	}
	@Test
	public void b09() throws Exception {
		b.request("GET", "/b09").accept("text/html").execute().assertBodyContains("<nav><ol><li><a href=\"/b09a\">foo</a></li><li>a01a</li><li>a01b</li><li><a href=\"/b09b\">bar</a></li><li>b01a</li><li>b01b</li></ol></nav>");
	}
	@Test
	public void b10() throws Exception {
		b.request("GET", "/b10").accept("text/html").execute().assertBodyContains("<nav><ol><li><a href=\"/b10a\">foo</a></li><li><a href=\"/b10b\">bar</a></li><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li></ol></nav>");
	}
	@Test
	public void b11() throws Exception {
		b.request("GET", "/b11").accept("text/html").execute().assertBodyContains("<nav><ol><li><a href=\"/b11b\">bar</a></li><li><a href=\"/b11a\">foo</a></li></ol></nav>");
	}
}