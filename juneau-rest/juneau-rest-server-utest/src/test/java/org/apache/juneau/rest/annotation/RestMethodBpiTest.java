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

import org.apache.juneau.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestMethod(bpi/bpx).
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestMethodBpiTest {

	//=================================================================================================================
	// BPI on normal bean
	//=================================================================================================================

	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod(bpi="MyBeanA: a,_b")
		public Object a01() throws Exception {
			return new MyBeanA().init();
		}
		@RestMethod(bpi="MyBeanA: a")
		public Object a02() throws Exception {
			return new MyBeanA().init();
		}
		@RestMethod(bpi="MyBeanA: _b")
		public Object a03() throws Exception {
			return new MyBeanA().init();
		}
	}
	static MockRest a = MockRest.build(A.class);

	@Test
	public void a01() throws Exception {
		a.get("/a01").json().execute().assertBody("{\"a\":1,\"_b\":\"foo\"}");
		a.get("/a01").xml().execute().assertBodyContains("<object><a>1</a><_b>foo</_b></object>");
		a.get("/a01").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		a.get("/a01").uon().execute().assertBody("(a=1,_b=foo)");
		a.get("/a01").urlEnc().execute().assertBody("a=1&_b=foo");
	}
	@Test
	public void a02() throws Exception {
		a.get("/a02").json().execute().assertBody("{\"a\":1}");
		a.get("/a02").xml().execute().assertBodyContains("<object><a>1</a></object>");
		a.get("/a02").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr></table>");
		a.get("/a02").uon().execute().assertBody("(a=1)");
		a.get("/a02").urlEnc().execute().assertBody("a=1");
	}
	@Test
	public void a03() throws Exception {
		a.get("/a03").json().execute().assertBody("{\"_b\":\"foo\"}");
		a.get("/a03").xml().execute().assertBodyContains("<object><_b>foo</_b></object>");
		a.get("/a03").html().execute().assertBodyContains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		a.get("/a03").uon().execute().assertBody("(_b=foo)");
		a.get("/a03").urlEnc().execute().assertBody("_b=foo");
	}

	//=================================================================================================================
	// BPX on normal bean
	//=================================================================================================================

	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod(bpx="MyBeanA: a,_b")
		public Object b01() throws Exception {
			return new MyBeanA().init();
		}
		@RestMethod(bpx="MyBeanA: a")
		public Object b02() throws Exception {
			return new MyBeanA().init();
		}
		@RestMethod(bpx="MyBeanA: _b")
		public Object b03() throws Exception {
			return new MyBeanA().init();
		}
	}
	static MockRest b = MockRest.build(B.class);

	@Test
	public void b01() throws Exception {
		b.get("/b01").json().execute().assertBody("{}");
		b.get("/b01").xml().execute().assertBodyContains("<object/>");
		b.get("/b01").html().execute().assertBodyContains("<table></table>");
		b.get("/b01").uon().execute().assertBody("()");
		b.get("/b01").urlEnc().execute().assertBody("");
	}
	@Test
	public void b02() throws Exception {
		b.get("/b02").json().execute().assertBody("{\"_b\":\"foo\"}");
		b.get("/b02").xml().execute().assertBodyContains("<object><_b>foo</_b></object>");
		b.get("/b02").html().execute().assertBodyContains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		b.get("/b02").uon().execute().assertBody("(_b=foo)");
		b.get("/b02").urlEnc().execute().assertBody("_b=foo");
	}
	@Test
	public void b03() throws Exception {
		b.get("/b03").json().execute().assertBody("{\"a\":1}");
		b.get("/b03").xml().execute().assertBodyContains("<object><a>1</a></object>");
		b.get("/b03").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr></table>");
		b.get("/b03").uon().execute().assertBody("(a=1)");
		b.get("/b03").urlEnc().execute().assertBody("a=1");
	}

	//=================================================================================================================
	// BPI on bean using @Bean(properties)
	//=================================================================================================================

	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod(bpi="MyBeanB: a,_b")
		public Object c01() throws Exception {
			return new MyBeanB().init();
		}
		@RestMethod(bpi="MyBeanB: a")
		public Object c02() throws Exception {
			return new MyBeanB().init();
		}
		@RestMethod(bpi="MyBeanB: _b")
		public Object c03() throws Exception {
			return new MyBeanB().init();
		}
	}
	static MockRest c = MockRest.build(C.class);

	@Test
	public void c01() throws Exception {
		c.get("/c01").json().execute().assertBody("{\"_b\":\"foo\",\"a\":1}");
		c.get("/c01").xml().execute().assertBodyContains("<object><_b>foo</_b><a>1</a></object>");
		c.get("/c01").html().execute().assertBodyContains("<table><tr><td>_b</td><td>foo</td></tr><tr><td>a</td><td>1</td></tr></table>");
		c.get("/c01").uon().execute().assertBody("(_b=foo,a=1)");
		c.get("/c01").urlEnc().execute().assertBody("_b=foo&a=1");
	}
	@Test
	public void c02() throws Exception {
		c.get("/c02").json().execute().assertBody("{\"a\":1}");
		c.get("/c02").xml().execute().assertBodyContains("<object><a>1</a></object>");
		c.get("/c02").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr></table>");
		c.get("/c02").uon().execute().assertBody("(a=1)");
		c.get("/c02").urlEnc().execute().assertBody("a=1");
	}
	@Test
	public void c03() throws Exception {
		c.get("/c03").json().execute().assertBody("{\"_b\":\"foo\"}");
		c.get("/c03").xml().execute().assertBodyContains("<object><_b>foo</_b></object>");
		c.get("/c03").html().execute().assertBodyContains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		c.get("/c03").uon().execute().assertBody("(_b=foo)");
		c.get("/c03").urlEnc().execute().assertBody("_b=foo");
	}

	//=================================================================================================================
	// BPX on bean using @Bean(properties)
	//=================================================================================================================

	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod(bpx="MyBeanB: a,_b")
		public Object d01() throws Exception {
			return new MyBeanB().init();
		}
		@RestMethod(bpx="MyBeanB: a")
		public Object d02() throws Exception {
			return new MyBeanB().init();
		}
		@RestMethod(bpx="MyBeanB: _b")
		public Object d03() throws Exception {
			return new MyBeanB().init();
		}
	}
	static MockRest d = MockRest.build(D.class);

	@Test
	public void d01() throws Exception {
		d.get("/d01").json().execute().assertBody("{}");
		d.get("/d01").xml().execute().assertBodyContains("<object/>");
		d.get("/d01").html().execute().assertBodyContains("<table></table>");
		d.get("/d01").uon().execute().assertBody("()");
		d.get("/d01").urlEnc().execute().assertBody("");
	}
	@Test
	public void d02() throws Exception {
		d.get("/d02").json().execute().assertBody("{\"_b\":\"foo\"}");
		d.get("/d02").xml().execute().assertBodyContains("<object><_b>foo</_b></object>");
		d.get("/d02").html().execute().assertBodyContains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		d.get("/d02").uon().execute().assertBody("(_b=foo)");
		d.get("/d02").urlEnc().execute().assertBody("_b=foo");
	}
	@Test
	public void d03() throws Exception {
		d.get("/d03").json().execute().assertBody("{\"a\":1}");
		d.get("/d03").xml().execute().assertBodyContains("<object><a>1</a></object>");
		d.get("/d03").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr></table>");
		d.get("/d03").uon().execute().assertBody("(a=1)");
		d.get("/d03").urlEnc().execute().assertBody("a=1");
	}

	//=================================================================================================================
	// BPI meta-matching
	//=================================================================================================================

	public static class E extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod(bpi="*: a")
		public Object e01() throws Exception {
			return new MyBeanA().init();
		}
	}
	static MockRest e = MockRest.build(E.class);

	@Test
	public void e01() throws Exception {
		e.get("/e01").json().execute().assertBody("{\"a\":1}");
		e.get("/e01").xml().execute().assertBodyContains("<object><a>1</a></object>");
		e.get("/e01").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr></table>");
		e.get("/e01").uon().execute().assertBody("(a=1)");
		e.get("/e01").urlEnc().execute().assertBody("a=1");
	}

	//=================================================================================================================
	// BPI fully-qualified class name
	//=================================================================================================================

	public static class F extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod(bpi="org.apache.juneau.rest.annotation.RestMethodBpiTest$MyBeanA: a")
		public Object f01() throws Exception {
			return new MyBeanA().init();
		}
	}
	static MockRest f = MockRest.build(F.class);

	@Test
	public void f01() throws Exception {
		f.get("/f01").json().execute().assertBody("{\"a\":1}");
		f.get("/f01").xml().execute().assertBodyContains("<object><a>1</a></object>");
		f.get("/f01").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr></table>");
		f.get("/f01").uon().execute().assertBody("(a=1)");
		f.get("/f01").urlEnc().execute().assertBody("a=1");
	}

	//=================================================================================================================
	// Negative matching
	//=================================================================================================================

	public static class G extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod(bpi="MyBean: a")
		public Object g01() throws Exception {
			// Should not match.
			return new MyBeanA().init();
		}
		@RestMethod(bpi="MyBean*: a")
		public Object g02() throws Exception {
			// Should not match.  We don't support meta-matches in class names.
			return new MyBeanA().init();
		}
	}
	static MockRest g = MockRest.build(G.class);

	@Test
	public void g01() throws Exception {
		g.get("/g01").json().execute().assertBody("{\"a\":1,\"_b\":\"foo\"}");
		g.get("/g01").xml().execute().assertBodyContains("<object><a>1</a><_b>foo</_b></object>");
		g.get("/g01").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		g.get("/g01").uon().execute().assertBody("(a=1,_b=foo)");
		g.get("/g01").urlEnc().execute().assertBody("a=1&_b=foo");
	}
	@Test
	public void g02() throws Exception {
		g.get("/g02").json().execute().assertBody("{\"a\":1,\"_b\":\"foo\"}");
		g.get("/g02").xml().execute().assertBodyContains("<object><a>1</a><_b>foo</_b></object>");
		g.get("/g02").html().execute().assertBodyContains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		g.get("/g02").uon().execute().assertBody("(a=1,_b=foo)");
		g.get("/g02").urlEnc().execute().assertBody("a=1&_b=foo");
	}

	//=================================================================================================================
	// Beans
	//=================================================================================================================

	public static class MyBeanA {
		public int a;
		@Beanp("_b") public String b;

		MyBeanA init() {
			a = 1;
			b = "foo";
			return this;
		}
	}

	@Bean(properties="_b,a")
	public static class MyBeanB {
		public int a;
		@Beanp("_b") public String b;

		MyBeanB init() {
			a = 1;
			b = "foo";
			return this;
		}
	}
}
