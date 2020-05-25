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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
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
		@RestMethod
		@BeanConfig(bpi="MyBeanA: a,_b")
		public Object a04() throws Exception {
			return new MyBeanA().init();
		}
		@RestMethod
		@BeanConfig(bpi="MyBeanA: a")
		public Object a05() throws Exception {
			return new MyBeanA().init();
		}
		@RestMethod
		@BeanConfig(bpi="MyBeanA: _b")
		public Object a06() throws Exception {
			return new MyBeanA().init();
		}
	}
	static MockRest a = MockRest.build(A.class);

	@Test
	public void a01() throws Exception {
		a.get("/a01").json().run().assertBody().is("{\"a\":1,\"_b\":\"foo\"}");
		a.get("/a01").xml().run().assertBody().contains("<object><a>1</a><_b>foo</_b></object>");
		a.get("/a01").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		a.get("/a01").uon().run().assertBody().is("(a=1,_b=foo)");
		a.get("/a01").urlEnc().run().assertBody().is("a=1&_b=foo");
	}
	@Test
	public void a02() throws Exception {
		a.get("/a02").json().run().assertBody().is("{\"a\":1}");
		a.get("/a02").xml().run().assertBody().contains("<object><a>1</a></object>");
		a.get("/a02").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		a.get("/a02").uon().run().assertBody().is("(a=1)");
		a.get("/a02").urlEnc().run().assertBody().is("a=1");
	}
	@Test
	public void a03() throws Exception {
		a.get("/a03").json().run().assertBody().is("{\"_b\":\"foo\"}");
		a.get("/a03").xml().run().assertBody().contains("<object><_b>foo</_b></object>");
		a.get("/a03").html().run().assertBody().contains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		a.get("/a03").uon().run().assertBody().is("(_b=foo)");
		a.get("/a03").urlEnc().run().assertBody().is("_b=foo");
	}

	@Test
	public void a04() throws Exception {
		a.get("/a04").json().run().assertBody().is("{\"a\":1,\"_b\":\"foo\"}");
		a.get("/a04").xml().run().assertBody().contains("<object><a>1</a><_b>foo</_b></object>");
		a.get("/a04").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		a.get("/a04").uon().run().assertBody().is("(a=1,_b=foo)");
		a.get("/a04").urlEnc().run().assertBody().is("a=1&_b=foo");
	}
	@Test
	public void a05() throws Exception {
		a.get("/a05").json().run().assertBody().is("{\"a\":1}");
		a.get("/a05").xml().run().assertBody().contains("<object><a>1</a></object>");
		a.get("/a05").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		a.get("/a05").uon().run().assertBody().is("(a=1)");
		a.get("/a05").urlEnc().run().assertBody().is("a=1");
	}
	@Test
	public void a06() throws Exception {
		a.get("/a06").json().run().assertBody().is("{\"_b\":\"foo\"}");
		a.get("/a06").xml().run().assertBody().contains("<object><_b>foo</_b></object>");
		a.get("/a06").html().run().assertBody().contains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		a.get("/a06").uon().run().assertBody().is("(_b=foo)");
		a.get("/a06").urlEnc().run().assertBody().is("_b=foo");
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
		@RestMethod
		@BeanConfig(bpx="MyBeanA: a,_b")
		public Object b04() throws Exception {
			return new MyBeanA().init();
		}
		@RestMethod
		@BeanConfig(bpx="MyBeanA: a")
		public Object b05() throws Exception {
			return new MyBeanA().init();
		}
		@RestMethod
		@BeanConfig(bpx="MyBeanA: _b")
		public Object b06() throws Exception {
			return new MyBeanA().init();
		}
	}
	static MockRest b = MockRest.build(B.class);

	@Test
	public void b01() throws Exception {
		b.get("/b01").json().run().assertBody().is("{}");
		b.get("/b01").xml().run().assertBody().contains("<object/>");
		b.get("/b01").html().run().assertBody().contains("<table></table>");
		b.get("/b01").uon().run().assertBody().is("()");
		b.get("/b01").urlEnc().run().assertBody().is("");
	}
	@Test
	public void b02() throws Exception {
		b.get("/b02").json().run().assertBody().is("{\"_b\":\"foo\"}");
		b.get("/b02").xml().run().assertBody().contains("<object><_b>foo</_b></object>");
		b.get("/b02").html().run().assertBody().contains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		b.get("/b02").uon().run().assertBody().is("(_b=foo)");
		b.get("/b02").urlEnc().run().assertBody().is("_b=foo");
	}
	@Test
	public void b03() throws Exception {
		b.get("/b03").json().run().assertBody().is("{\"a\":1}");
		b.get("/b03").xml().run().assertBody().contains("<object><a>1</a></object>");
		b.get("/b03").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		b.get("/b03").uon().run().assertBody().is("(a=1)");
		b.get("/b03").urlEnc().run().assertBody().is("a=1");
	}
	@Test
	public void b04() throws Exception {
		b.get("/b04").json().run().assertBody().is("{}");
		b.get("/b04").xml().run().assertBody().contains("<object/>");
		b.get("/b04").html().run().assertBody().contains("<table></table>");
		b.get("/b04").uon().run().assertBody().is("()");
		b.get("/b04").urlEnc().run().assertBody().is("");
	}
	@Test
	public void b05() throws Exception {
		b.get("/b05").json().run().assertBody().is("{\"_b\":\"foo\"}");
		b.get("/b05").xml().run().assertBody().contains("<object><_b>foo</_b></object>");
		b.get("/b05").html().run().assertBody().contains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		b.get("/b05").uon().run().assertBody().is("(_b=foo)");
		b.get("/b05").urlEnc().run().assertBody().is("_b=foo");
	}
	@Test
	public void b06() throws Exception {
		b.get("/b06").json().run().assertBody().is("{\"a\":1}");
		b.get("/b06").xml().run().assertBody().contains("<object><a>1</a></object>");
		b.get("/b06").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		b.get("/b06").uon().run().assertBody().is("(a=1)");
		b.get("/b06").urlEnc().run().assertBody().is("a=1");
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
		@RestMethod
		@BeanConfig(bpi="MyBeanB: a,_b")
		public Object c04() throws Exception {
			return new MyBeanB().init();
		}
		@RestMethod
		@BeanConfig(bpi="MyBeanB: a")
		public Object c05() throws Exception {
			return new MyBeanB().init();
		}
		@RestMethod
		@BeanConfig(bpi="MyBeanB: _b")
		public Object c06() throws Exception {
			return new MyBeanB().init();
		}
	}
	static MockRest c = MockRest.build(C.class);

	@Test
	public void c01() throws Exception {
		c.get("/c01").json().run().assertBody().is("{\"a\":1,\"_b\":\"foo\"}");
		c.get("/c01").xml().run().assertBody().contains("<object><a>1</a><_b>foo</_b></object>");
		c.get("/c01").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		c.get("/c01").uon().run().assertBody().is("(a=1,_b=foo)");
		c.get("/c01").urlEnc().run().assertBody().is("a=1&_b=foo");
	}
	@Test
	public void c02() throws Exception {
		c.get("/c02").json().run().assertBody().is("{\"a\":1}");
		c.get("/c02").xml().run().assertBody().contains("<object><a>1</a></object>");
		c.get("/c02").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		c.get("/c02").uon().run().assertBody().is("(a=1)");
		c.get("/c02").urlEnc().run().assertBody().is("a=1");
	}
	@Test
	public void c03() throws Exception {
		c.get("/c03").json().run().assertBody().is("{\"_b\":\"foo\"}");
		c.get("/c03").xml().run().assertBody().contains("<object><_b>foo</_b></object>");
		c.get("/c03").html().run().assertBody().contains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		c.get("/c03").uon().run().assertBody().is("(_b=foo)");
		c.get("/c03").urlEnc().run().assertBody().is("_b=foo");
	}
	@Test
	public void c04() throws Exception {
		c.get("/c04").json().run().assertBody().is("{\"a\":1,\"_b\":\"foo\"}");
		c.get("/c04").xml().run().assertBody().contains("<object><a>1</a><_b>foo</_b></object>");
		c.get("/c04").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		c.get("/c04").uon().run().assertBody().is("(a=1,_b=foo)");
		c.get("/c04").urlEnc().run().assertBody().is("a=1&_b=foo");
	}
	@Test
	public void c05() throws Exception {
		c.get("/c05").json().run().assertBody().is("{\"a\":1}");
		c.get("/c05").xml().run().assertBody().contains("<object><a>1</a></object>");
		c.get("/c05").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		c.get("/c05").uon().run().assertBody().is("(a=1)");
		c.get("/c05").urlEnc().run().assertBody().is("a=1");
	}
	@Test
	public void c06() throws Exception {
		c.get("/c06").json().run().assertBody().is("{\"_b\":\"foo\"}");
		c.get("/c06").xml().run().assertBody().contains("<object><_b>foo</_b></object>");
		c.get("/c06").html().run().assertBody().contains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		c.get("/c06").uon().run().assertBody().is("(_b=foo)");
		c.get("/c06").urlEnc().run().assertBody().is("_b=foo");
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
		@RestMethod
		@BeanConfig(bpx="MyBeanB: a,_b")
		public Object d04() throws Exception {
			return new MyBeanB().init();
		}
		@RestMethod
		@BeanConfig(bpx="MyBeanB: a")
		public Object d05() throws Exception {
			return new MyBeanB().init();
		}
		@RestMethod
		@BeanConfig(bpx="MyBeanB: _b")
		public Object d06() throws Exception {
			return new MyBeanB().init();
		}
	}
	static MockRest d = MockRest.build(D.class);

	@Test
	public void d01() throws Exception {
		d.get("/d01").json().run().assertBody().is("{}");
		d.get("/d01").xml().run().assertBody().contains("<object/>");
		d.get("/d01").html().run().assertBody().contains("<table></table>");
		d.get("/d01").uon().run().assertBody().is("()");
		d.get("/d01").urlEnc().run().assertBody().is("");
	}
	@Test
	public void d02() throws Exception {
		d.get("/d02").json().run().assertBody().is("{\"_b\":\"foo\"}");
		d.get("/d02").xml().run().assertBody().contains("<object><_b>foo</_b></object>");
		d.get("/d02").html().run().assertBody().contains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		d.get("/d02").uon().run().assertBody().is("(_b=foo)");
		d.get("/d02").urlEnc().run().assertBody().is("_b=foo");
	}
	@Test
	public void d03() throws Exception {
		d.get("/d03").json().run().assertBody().is("{\"a\":1}");
		d.get("/d03").xml().run().assertBody().contains("<object><a>1</a></object>");
		d.get("/d03").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		d.get("/d03").uon().run().assertBody().is("(a=1)");
		d.get("/d03").urlEnc().run().assertBody().is("a=1");
	}
	@Test
	public void d04() throws Exception {
		d.get("/d04").json().run().assertBody().is("{}");
		d.get("/d04").xml().run().assertBody().contains("<object/>");
		d.get("/d04").html().run().assertBody().contains("<table></table>");
		d.get("/d04").uon().run().assertBody().is("()");
		d.get("/d04").urlEnc().run().assertBody().is("");
	}
	@Test
	public void d05() throws Exception {
		d.get("/d05").json().run().assertBody().is("{\"_b\":\"foo\"}");
		d.get("/d05").xml().run().assertBody().contains("<object><_b>foo</_b></object>");
		d.get("/d05").html().run().assertBody().contains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		d.get("/d05").uon().run().assertBody().is("(_b=foo)");
		d.get("/d05").urlEnc().run().assertBody().is("_b=foo");
	}
	@Test
	public void d06() throws Exception {
		d.get("/d06").json().run().assertBody().is("{\"a\":1}");
		d.get("/d06").xml().run().assertBody().contains("<object><a>1</a></object>");
		d.get("/d06").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		d.get("/d06").uon().run().assertBody().is("(a=1)");
		d.get("/d06").urlEnc().run().assertBody().is("a=1");
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
		@RestMethod
		@BeanConfig(bpi="*: a")
		public Object e02() throws Exception {
			return new MyBeanA().init();
		}
	}
	static MockRest e = MockRest.build(E.class);

	@Test
	public void e01() throws Exception {
		e.get("/e01").json().run().assertBody().is("{\"a\":1}");
		e.get("/e01").xml().run().assertBody().contains("<object><a>1</a></object>");
		e.get("/e01").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		e.get("/e01").uon().run().assertBody().is("(a=1)");
		e.get("/e01").urlEnc().run().assertBody().is("a=1");
	}
	@Test
	public void e02() throws Exception {
		e.get("/e02").json().run().assertBody().is("{\"a\":1}");
		e.get("/e02").xml().run().assertBody().contains("<object><a>1</a></object>");
		e.get("/e02").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		e.get("/e02").uon().run().assertBody().is("(a=1)");
		e.get("/e02").urlEnc().run().assertBody().is("a=1");
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
		@RestMethod
		@BeanConfig(bpi="org.apache.juneau.rest.annotation.RestMethodBpiTest$MyBeanA: a")
		public Object f02() throws Exception {
			return new MyBeanA().init();
		}
	}
	static MockRest f = MockRest.build(F.class);

	@Test
	public void f01() throws Exception {
		f.get("/f01").json().run().assertBody().is("{\"a\":1}");
		f.get("/f01").xml().run().assertBody().contains("<object><a>1</a></object>");
		f.get("/f01").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		f.get("/f01").uon().run().assertBody().is("(a=1)");
		f.get("/f01").urlEnc().run().assertBody().is("a=1");
	}
	@Test
	public void f02() throws Exception {
		f.get("/f02").json().run().assertBody().is("{\"a\":1}");
		f.get("/f02").xml().run().assertBody().contains("<object><a>1</a></object>");
		f.get("/f02").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr></table>");
		f.get("/f02").uon().run().assertBody().is("(a=1)");
		f.get("/f02").urlEnc().run().assertBody().is("a=1");
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
		@RestMethod
		@BeanConfig(bpi="MyBean: a")
		public Object g03() throws Exception {
			// Should not match.
			return new MyBeanA().init();
		}
		@RestMethod
		@BeanConfig(bpi="MyBean*: a")
		public Object g04() throws Exception {
			// Should not match.  We don't support meta-matches in class names.
			return new MyBeanA().init();
		}
	}
	static MockRest g = MockRest.build(G.class);

	@Test
	public void g01() throws Exception {
		g.get("/g01").json().run().assertBody().is("{\"a\":1,\"_b\":\"foo\"}");
		g.get("/g01").xml().run().assertBody().contains("<object><a>1</a><_b>foo</_b></object>");
		g.get("/g01").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		g.get("/g01").uon().run().assertBody().is("(a=1,_b=foo)");
		g.get("/g01").urlEnc().run().assertBody().is("a=1&_b=foo");
	}
	@Test
	public void g02() throws Exception {
		g.get("/g02").json().run().assertBody().is("{\"a\":1,\"_b\":\"foo\"}");
		g.get("/g02").xml().run().assertBody().contains("<object><a>1</a><_b>foo</_b></object>");
		g.get("/g02").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		g.get("/g02").uon().run().assertBody().is("(a=1,_b=foo)");
		g.get("/g02").urlEnc().run().assertBody().is("a=1&_b=foo");
	}
	@Test
	public void g03() throws Exception {
		g.get("/g03").json().run().assertBody().is("{\"a\":1,\"_b\":\"foo\"}");
		g.get("/g03").xml().run().assertBody().contains("<object><a>1</a><_b>foo</_b></object>");
		g.get("/g03").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		g.get("/g03").uon().run().assertBody().is("(a=1,_b=foo)");
		g.get("/g03").urlEnc().run().assertBody().is("a=1&_b=foo");
	}
	@Test
	public void g04() throws Exception {
		g.get("/g04").json().run().assertBody().is("{\"a\":1,\"_b\":\"foo\"}");
		g.get("/g04").xml().run().assertBody().contains("<object><a>1</a><_b>foo</_b></object>");
		g.get("/g04").html().run().assertBody().contains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		g.get("/g04").uon().run().assertBody().is("(a=1,_b=foo)");
		g.get("/g04").urlEnc().run().assertBody().is("a=1&_b=foo");
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

	@Bean(bpi="_b,a")
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
