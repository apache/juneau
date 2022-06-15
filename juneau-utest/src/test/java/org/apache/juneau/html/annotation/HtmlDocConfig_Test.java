package org.apache.juneau.html.annotation;

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

//***************************************************************************************************************************
//* Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
//* distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
//* to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
//* with the License.  You may obtain a copy of the License at                                                              *
//*                                                                                                                         *
//*  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
//*                                                                                                                         *
//* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
//* specific language governing permissions and limitations under the License.                                              *
//***************************************************************************************************************************

@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings({"serial"})
public class HtmlDocConfig_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @HtmlDocConfig(aside)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(aside={"a01a","a01b","INHERIT"})
	public static class A1 extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public Object a01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside={"a02a","a02b"})
		public Object a02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside={"INHERIT","a03a","a03b"})
		public Object a03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside={"a04a","INHERIT","a04b"})
		public Object a04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside={"a05a","a05b","INHERIT"})
		public Object a05() {
			return "OK";
		}
	}

	@Test
	public void a01_aside() throws Exception {
		RestClient a1 = client(A1.class);
		a1.get("/a01").accept("text/html").run().assertContent().isContains("<aside>a01a a01b</aside>");
		a1.get("/a02").accept("text/html").run().assertContent().isContains("<aside>a02a a02b</aside>");
		a1.get("/a03").accept("text/html").run().assertContent().isContains("<aside>a01a a01b a03a a03b</aside>");
		a1.get("/a04").accept("text/html").run().assertContent().isContains("<aside>a04a a01a a01b a04b</aside>");
		a1.get("/a05").accept("text/html").run().assertContent().isContains("<aside>a05a a05b a01a a01b</aside>");
	}

	@Rest
	@HtmlDocConfig(aside={"INHERIT","b01a","b01b"})
	public static class A2 extends A1 {
		@RestGet
		public Object b01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside={"b02a","b02b"})
		public Object b02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside={"INHERIT","b03a","b03b"})
		public Object b03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside={"b04a","INHERIT","b04b"})
		public Object b04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(aside={"b05a","b05b","INHERIT"})
		public Object b05() {
			return "OK";
		}
	}

	@Test
	public void a02_aside_inherited() throws Exception {
		RestClient a2 = client(A2.class);
		a2.get("/b01").accept("text/html").run().assertContent().isContains("<aside>a01a a01b b01a b01b</aside>");
		a2.get("/b02").accept("text/html").run().assertContent().isContains("<aside>b02a b02b</aside>");
		a2.get("/b03").accept("text/html").run().assertContent().isContains("<aside>a01a a01b b01a b01b b03a b03b</aside>");
		a2.get("/b04").accept("text/html").run().assertContent().isContains("<aside>b04a a01a a01b b01a b01b b04b</aside>");
		a2.get("/b05").accept("text/html").run().assertContent().isContains("<aside>b05a b05b a01a a01b b01a b01b</aside>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @HtmlDocConfig(footer)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(footer={"a01a","a01b"})
	public static class B1 extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public Object a01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer={"a02a","a02b"})
		public Object a02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer={"INHERIT","a03a","a03b"})
		public Object a03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer={"a04a","INHERIT","a04b"})
		public Object a04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer={"a05a","a05b","INHERIT"})
		public Object a05() {
			return "OK";
		}
	}

	@Test
	public void b01_footer() throws Exception {
		RestClient b1 = client(B1.class);
		b1.get("/a01").accept("text/html").run().assertContent().isContains("<footer>a01a a01b</footer>");
		b1.get("/a02").accept("text/html").run().assertContent().isContains("<footer>a02a a02b</footer>");
		b1.get("/a03").accept("text/html").run().assertContent().isContains("<footer>a01a a01b a03a a03b</footer>");
		b1.get("/a04").accept("text/html").run().assertContent().isContains("<footer>a04a a01a a01b a04b</footer>");
		b1.get("/a05").accept("text/html").run().assertContent().isContains("<footer>a05a a05b a01a a01b</footer>");
	}

	@Rest
	@HtmlDocConfig(footer={"b01a","INHERIT","b01b"})
	public static class B2 extends B1 {
		@RestGet
		public Object b01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer={"b02a","b02b"})
		public Object b02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer={"INHERIT","b03a","b03b"})
		public Object b03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer={"b04a","INHERIT","b04b"})
		public Object b04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(footer={"b05a","b05b","INHERIT"})
		public Object b05() {
			return "OK";
		}
	}

	@Test
	public void b02_footer_inherited() throws Exception {
		RestClient b2 = client(B2.class);
		b2.get("/b01").accept("text/html").run().assertContent().isContains("<footer>b01a a01a a01b b01b</footer>");
		b2.get("/b02").accept("text/html").run().assertContent().isContains("<footer>b02a b02b</footer>");
		b2.get("/b03").accept("text/html").run().assertContent().isContains("<footer>b01a a01a a01b b01b b03a b03b</footer>");
		b2.get("/b04").accept("text/html").run().assertContent().isContains("<footer>b04a b01a a01a a01b b01b b04b</footer>");
		b2.get("/b05").accept("text/html").run().assertContent().isContains("<footer>b05a b05b b01a a01a a01b b01b</footer>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @HtmlDocConfig(header)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(header={"a01a","a01b"})
	public static class C1 extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public Object a01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header={"a02a","a02b"})
		public Object a02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header={"INHERIT","a03a","a03b"})
		public Object a03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header={"a04a","INHERIT","a04b"})
		public Object a04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header={"a05a","a05b","INHERIT"})
		public Object a05() {
			return "OK";
		}
	}

	@Test
	public void c01_header() throws Exception {
		RestClient c1 = client(C1.class);
		c1.get("/a01").accept("text/html").run().assertContent().isContains("<header>a01a a01b</header>");
		c1.get("/a02").accept("text/html").run().assertContent().isContains("<header>a02a a02b</header>");
		c1.get("/a03").accept("text/html").run().assertContent().isContains("<header>a01a a01b a03a a03b</header>");
		c1.get("/a04").accept("text/html").run().assertContent().isContains("<header>a04a a01a a01b a04b</header>");
		c1.get("/a05").accept("text/html").run().assertContent().isContains("<header>a05a a05b a01a a01b</header>");
	}

	@Rest
	@HtmlDocConfig(header={"b01a","b01b","INHERIT"})
	public static class C2 extends C1 {
		@RestGet
		public Object b01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header={"b02a","b02b"})
		public Object b02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header={"INHERIT","b03a","b03b"})
		public Object b03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header={"b04a","INHERIT","b04b"})
		public Object b04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(header={"b05a","b05b","INHERIT"})
		public Object b05() {
			return "OK";
		}
	}

	@Test
	public void c02_header_inherited() throws Exception {
		RestClient c2 = client(C2.class);
		c2.get("/b01").accept("text/html").run().assertContent().isContains("<header>b01a b01b a01a a01b</header>");
		c2.get("/b02").accept("text/html").run().assertContent().isContains("<header>b02a b02b</header>");
		c2.get("/b03").accept("text/html").run().assertContent().isContains("<header>b01a b01b a01a a01b b03a b03b</header>");
		c2.get("/b04").accept("text/html").run().assertContent().isContains("<header>b04a b01a b01b a01a a01b b04b</header>");
		c2.get("/b05").accept("text/html").run().assertContent().isContains("<header>b05a b05b b01a b01b a01a a01b</header>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @HtmlDocConfig(navlinks)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(navlinks={"a01a","a01b"})
	public static class D1 extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public Object a01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"a02a","a02b"})
		public Object a02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"INHERIT","a03a","a03b"})
		public Object a03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"a04a","INHERIT","a04b"})
		public Object a04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"a05a","a05b","INHERIT"})
		public Object a05() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"INHERIT","[0]:a06a","[3]:a06b"})
		public Object a06() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"[1]:a07a","[2]:a07b","INHERIT"})
		public Object a07() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"[1]:a08a","[0]:a08b"})
		public Object a08() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"INHERIT","foo[0]:a09a","bar[3]:a09b"})
		public Object a09() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"foo[1]:a10a","bar[2]:a10b","INHERIT"})
		public Object a10() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"foo[1]:a11a","bar[0]:a11b"})
		public Object a11() {
			return "OK";
		}
	}

	@Test
	public void d01_navlinks() throws Exception {
		RestClient d1 = client(D1.class);
		d1.get("/a01").accept("text/html").run().assertContent().isContains("<nav><ol><li>a01a</li><li>a01b</li></ol></nav>");
		d1.get("/a02").accept("text/html").run().assertContent().isContains("<nav><ol><li>a02a</li><li>a02b</li></ol></nav>");
		d1.get("/a03").accept("text/html").run().assertContent().isContains("<nav><ol><li>a01a</li><li>a01b</li><li>a03a</li><li>a03b</li></ol></nav>");
		d1.get("/a04").accept("text/html").run().assertContent().isContains("<nav><ol><li>a04a</li><li>a01a</li><li>a01b</li><li>a04b</li></ol></nav>");
		d1.get("/a05").accept("text/html").run().assertContent().isContains("<nav><ol><li>a05a</li><li>a05b</li><li>a01a</li><li>a01b</li></ol></nav>");
		d1.get("/a06").accept("text/html").run().assertContent().isContains("<nav><ol><li>a06a</li><li>a01a</li><li>a01b</li><li>a06b</li></ol></nav>");
		d1.get("/a07").accept("text/html").run().assertContent().isContains("<nav><ol><li>a07a</li><li>a07b</li><li>a01a</li><li>a01b</li></ol></nav>");
		d1.get("/a08").accept("text/html").run().assertContent().isContains("<nav><ol><li>a08b</li><li>a08a</li></ol></nav>");
		d1.get("/a09").accept("text/html").run().assertContent().isContains("<nav><ol><li><a href=\"/a09a\">foo</a></li><li>a01a</li><li>a01b</li><li><a href=\"/a09b\">bar</a></li></ol></nav>");
		d1.get("/a10").accept("text/html").run().assertContent().isContains("<nav><ol><li><a href=\"/a10a\">foo</a></li><li><a href=\"/a10b\">bar</a></li><li>a01a</li><li>a01b</li></ol></nav>");
		d1.get("/a11").accept("text/html").run().assertContent().isContains("<nav><ol><li><a href=\"/a11b\">bar</a></li><li><a href=\"/a11a\">foo</a></li></ol></nav>");
	}

	@Rest
	@HtmlDocConfig(navlinks={"INHERIT","b01a","b01b"})
	public static class D2 extends D1 {
		@RestGet
		public Object b01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"b02a","b02b"})
		public Object b02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"INHERIT","b03a","b03b"})
		public Object b03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"b04a","INHERIT","b04b"})
		public Object b04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"b05a","b05b","INHERIT"})
		public Object b05() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"INHERIT","[0]:b06a","[3]:b06b"})
		public Object b06() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"[1]:b07a","[2]:b07b","INHERIT"})
		public Object b07() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"[1]:b08a","[0]:b08b"})
		public Object b08() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"INHERIT","foo[0]:b09a","bar[3]:b09b"})
		public Object b09() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"foo[1]:b10a","bar[2]:b10b","INHERIT"})
		public Object b10() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(navlinks={"foo[1]:b11a","bar[0]:b11b"})
		public Object b11() {
			return "OK";
		}
	}

	@Test
	public void d02_navlinks_inherited() throws Exception {
		RestClient d2 = client(D2.class);
		d2.get("/b01").accept("text/html").run().assertContent().isContains("<nav><ol><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li></ol></nav>");
		d2.get("/b02").accept("text/html").run().assertContent().isContains("<nav><ol><li>b02a</li><li>b02b</li></ol></nav>");
		d2.get("/b03").accept("text/html").run().assertContent().isContains("<nav><ol><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li><li>b03a</li><li>b03b</li></ol></nav>");
		d2.get("/b04").accept("text/html").run().assertContent().isContains("<nav><ol><li>b04a</li><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li><li>b04b</li></ol></nav>");
		d2.get("/b05").accept("text/html").run().assertContent().isContains("<nav><ol><li>b05a</li><li>b05b</li><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li></ol></nav>");
		d2.get("/b06").accept("text/html").run().assertContent().isContains("<nav><ol><li>b06a</li><li>a01a</li><li>a01b</li><li>b06b</li><li>b01a</li><li>b01b</li></ol></nav>");
		d2.get("/b07").accept("text/html").run().assertContent().isContains("<nav><ol><li>b07a</li><li>b07b</li><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li></ol></nav>");
		d2.get("/b08").accept("text/html").run().assertContent().isContains("<nav><ol><li>b08b</li><li>b08a</li></ol></nav>");
		d2.get("/b09").accept("text/html").run().assertContent().isContains("<nav><ol><li><a href=\"/b09a\">foo</a></li><li>a01a</li><li>a01b</li><li><a href=\"/b09b\">bar</a></li><li>b01a</li><li>b01b</li></ol></nav>");
		d2.get("/b10").accept("text/html").run().assertContent().isContains("<nav><ol><li><a href=\"/b10a\">foo</a></li><li><a href=\"/b10b\">bar</a></li><li>a01a</li><li>a01b</li><li>b01a</li><li>b01b</li></ol></nav>");
		d2.get("/b11").accept("text/html").run().assertContent().isContains("<nav><ol><li><a href=\"/b11b\">bar</a></li><li><a href=\"/b11a\">foo</a></li></ol></nav>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @HtmlDocConfig(nav)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(navlinks={"NONE"},nav={"a01a","a01b"})
	public static class E1 extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public Object a01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(nav={"a02a","a02b"})
		public Object a02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(nav={"INHERIT","a03a","a03b"})
		public Object a03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(nav={"a04a","INHERIT","a04b"})
		public Object a04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(nav={"a05a","a05b","INHERIT"})
		public Object a05() {
			return "OK";
		}
	}

	@Test
	public void e01_nav() throws Exception {
		RestClient e1 = client(E1.class);
		e1.get("/a01").accept("text/html").run().assertContent().isContains("<nav>a01a a01b</nav>");
		e1.get("/a02").accept("text/html").run().assertContent().isContains("<nav>a02a a02b</nav>");
		e1.get("/a03").accept("text/html").run().assertContent().isContains("<nav>a01a a01b a03a a03b</nav>");
		e1.get("/a04").accept("text/html").run().assertContent().isContains("<nav>a04a a01a a01b a04b</nav>");
		e1.get("/a05").accept("text/html").run().assertContent().isContains("<nav>a05a a05b a01a a01b</nav>");
	}

	@Rest
	@HtmlDocConfig(nav={"INHERIT","b01a","b01b"})
	public static class E2 extends E1 {
		@RestGet
		public Object b01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(nav={"b02a","b02b"})
		public Object b02() {
			return "OK";
		}
		@RestGet(path="/b03")
		@HtmlDocConfig(nav={"INHERIT","b03a","b03b"})
		public Object b03() {
			return "OK";
		}
		@RestGet(path="/b04")
		@HtmlDocConfig(nav={"b04a","INHERIT","b04b"})
		public Object b04() {
			return "OK";
		}
		@RestGet(path="/b05")
		@HtmlDocConfig(nav={"b05a","b05b","INHERIT"})
		public Object b05() {
			return "OK";
		}
	}

	@Test
	public void e02_nav_inherited() throws Exception {
		RestClient e2 = client(E2.class);
		e2.get("/b01").accept("text/html").run().assertContent().isContains("<nav>a01a a01b b01a b01b</nav>");
		e2.get("/b02").accept("text/html").run().assertContent().isContains("<nav>b02a b02b</nav>");
		e2.get("/b03").accept("text/html").run().assertContent().isContains("<nav>a01a a01b b01a b01b b03a b03b</nav>");
		e2.get("/b04").accept("text/html").run().assertContent().isContains("<nav>b04a a01a a01b b01a b01b b04b</nav>");
		e2.get("/b05").accept("text/html").run().assertContent().isContains("<nav>b05a b05b a01a a01b b01a b01b</nav>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @HtmlDocConfig(script)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(script={"a01a","a01b"})
	public static class F1 extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public Object a01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(script={"a02a","a02b"})
		public Object a02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(script={"INHERIT","a03a","a03b"})
		public Object a03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(script={"a04a","INHERIT","a04b"})
		public Object a04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(script={"a05a","a05b","INHERIT"})
		public Object a05() {
			return "OK";
		}
	}

	@Test
	public void f01_script() throws Exception {
		RestClient f1 = client(F1.class);
		f1.get("/a01").accept("text/html").run().assertContent().isContains("<script>a01a\n a01b\n</script>");
		f1.get("/a02").accept("text/html").run().assertContent().isContains("<script>a02a\n a02b\n</script>");
		f1.get("/a03").accept("text/html").run().assertContent().isContains("<script>a01a\n a01b\n a03a\n a03b\n</script>");
		f1.get("/a04").accept("text/html").run().assertContent().isContains("<script>a04a\n a01a\n a01b\n a04b\n</script>");
		f1.get("/a05").accept("text/html").run().assertContent().isContains("<script>a05a\n a05b\n a01a\n a01b\n</script>");
	}

	@Rest
	@HtmlDocConfig(script={"b01a","b01b"})
	public static class F2 extends F1 {
		@RestGet
		public Object b01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(script={"b02a","b02b"})
		public Object b02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(script={"INHERIT","b03a","b03b"})
		public Object b03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(script={"b04a","INHERIT","b04b"})
		public Object b04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(script={"b05a","b05b","INHERIT"})
		public Object b05() {
			return "OK";
		}
	}

	@Test
	public void f02_script_inherited() throws Exception {
		RestClient f2 = client(F2.class);
		f2.get("/b01").accept("text/html").run().assertContent().isContains("<script>b01a\n b01b\n</script>");
		f2.get("/b02").accept("text/html").run().assertContent().isContains("<script>b02a\n b02b\n</script>");
		f2.get("/b03").accept("text/html").run().assertContent().isContains("<script>b01a\n b01b\n b03a\n b03b\n</script>");
		f2.get("/b04").accept("text/html").run().assertContent().isContains("<script>b04a\n b01a\n b01b\n b04b\n</script>");
		f2.get("/b05").accept("text/html").run().assertContent().isContains("<script>b05a\n b05b\n b01a\n b01b\n</script>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @HtmlDocConfig(style)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	@HtmlDocConfig(style={"a01a","a01b"},stylesheet="a01s",nowrap="false")
	public static class G1 extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public Object a01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(style={"a02a","a02b"},stylesheet="a02s")
		public Object a02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(style={"INHERIT","a03a","a03b"})
		public Object a03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(style={"a04a","INHERIT","a04b"})
		public Object a04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(style={"a05a","a05b","INHERIT"})
		public Object a05() {
			return "OK";
		}
	}

	@Test
	public void g01_style() throws Exception {
		RestClient g1 = client(G1.class);
		g1.get("/a01").accept("text/html").run().assertContent().isContains("<style>@import \"/a01s\"; a01a a01b</style>");
		g1.get("/a02").accept("text/html").run().assertContent().isContains("<style>@import \"/a02s\"; a02a a02b</style>");
		g1.get("/a03").accept("text/html").run().assertContent().isContains("<style>@import \"/a01s\"; a01a a01b a03a a03b</style>");
		g1.get("/a04").accept("text/html").run().assertContent().isContains("<style>@import \"/a01s\"; a04a a01a a01b a04b</style>");
		g1.get("/a05").accept("text/html").run().assertContent().isContains("<style>@import \"/a01s\"; a05a a05b a01a a01b</style>");
	}

	@Rest
	@HtmlDocConfig(style={"b01a","b01b"},stylesheet="b01s")
	public static class G2 extends G1 {
		@RestGet
		public Object b01() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(style={"b02a","b02b"},stylesheet="b02s")
		public Object b02() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(style={"INHERIT","b03a","b03b"})
		public Object b03() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(style={"b04a","INHERIT","b04b"})
		public Object b04() {
			return "OK";
		}
		@RestGet
		@HtmlDocConfig(style={"b05a","b05b","INHERIT"})
		public Object b05() {
			return "OK";
		}
	}

	@Test
	public void g02_style_inherited() throws Exception {
		RestClient g2 = client(G2.class);
		g2.get("/b01").accept("text/html").run().assertContent().isContains("<style>@import \"/b01s\"; b01a b01b</style>");
		g2.get("/b02").accept("text/html").run().assertContent().isContains("<style>@import \"/b02s\"; b02a b02b</style>");
		g2.get("/b03").accept("text/html").run().assertContent().isContains("<style>@import \"/b01s\"; b01a b01b b03a b03b</style>");
		g2.get("/b04").accept("text/html").run().assertContent().isContains("<style>@import \"/b01s\"; b04a b01a b01b b04b</style>");
		g2.get("/b05").accept("text/html").run().assertContent().isContains("<style>@import \"/b01s\"; b05a b05b b01a b01b</style>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private RestClient client(Class<?> c) {
		return MockRestClient.build(c);
	}
}
