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
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
@SuppressWarnings("serial")
public class RestOp_BeanConfig_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpi)
	//------------------------------------------------------------------------------------------------------------------

	public static class A1 extends BasicRestServlet {
		@RestGet
		@Bean(onClass=X1.class, properties="a,_b")
		public Object a() throws Exception {
			return new X1().init();
		}
		@RestGet
		@Bean(onClass=X1.class, p="a")
		public Object b() throws Exception {
			return new X1().init();
		}
		@RestGet
		@Bean(onClass=X1.class, p="_b")
		public Object c() throws Exception {
			return new X1().init();
		}
	}

	@Test
	public void a01_bpi() throws Exception {
		RestClient a1 = MockRestClient.build(A1.class);
		a1.get("/a").json().run().assertContent("{\"a\":1,\"_b\":\"foo\"}");
		a1.get("/a").xml().run().assertContent().isContains("<object><a>1</a><_b>foo</_b></object>");
		a1.get("/a").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		a1.get("/a").uon().run().assertContent("(a=1,_b=foo)");
		a1.get("/a").urlEnc().run().assertContent("a=1&_b=foo");
		a1.get("/b").json().run().assertContent("{\"a\":1}");
		a1.get("/b").xml().run().assertContent().isContains("<object><a>1</a></object>");
		a1.get("/b").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr></table>");
		a1.get("/b").uon().run().assertContent("(a=1)");
		a1.get("/b").urlEnc().run().assertContent("a=1");
		a1.get("/c").json().run().assertContent("{\"_b\":\"foo\"}");
		a1.get("/c").xml().run().assertContent().isContains("<object><_b>foo</_b></object>");
		a1.get("/c").html().run().assertContent().isContains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		a1.get("/c").uon().run().assertContent("(_b=foo)");
		a1.get("/c").urlEnc().run().assertContent("_b=foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpx)
	//------------------------------------------------------------------------------------------------------------------

	public static class A2 extends BasicRestServlet {
		@RestGet
		@Bean(on="X1", excludeProperties="a,_b")
		public Object a() throws Exception {
			return new X1().init();
		}
		@RestGet
		@Bean(on="X1", xp="a")
		public Object b() throws Exception {
			return new X1().init();
		}
		@RestGet
		@Bean(on="X1", xp="_b")
		public Object c() throws Exception {
			return new X1().init();
		}
	}

	@Test
	public void a02_bpx() throws Exception {
		RestClient a2 = MockRestClient.build(A2.class);
		a2.get("/a").json().run().assertContent("{}");
		a2.get("/a").xml().run().assertContent().isContains("<object/>");
		a2.get("/a").html().run().assertContent().isContains("<table></table>");
		a2.get("/a").uon().run().assertContent("()");
		a2.get("/a").urlEnc().run().assertContent("");
		a2.get("/b").json().run().assertContent("{\"_b\":\"foo\"}");
		a2.get("/b").xml().run().assertContent().isContains("<object><_b>foo</_b></object>");
		a2.get("/b").html().run().assertContent().isContains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		a2.get("/b").uon().run().assertContent("(_b=foo)");
		a2.get("/b").urlEnc().run().assertContent("_b=foo");
		a2.get("/c").json().run().assertContent("{\"a\":1}");
		a2.get("/c").xml().run().assertContent().isContains("<object><a>1</a></object>");
		a2.get("/c").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr></table>");
		a2.get("/c").uon().run().assertContent("(a=1)");
		a2.get("/c").urlEnc().run().assertContent("a=1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpi) and @Bean(bpi)
	//------------------------------------------------------------------------------------------------------------------

	public static class A3 extends BasicRestServlet {
		@RestGet
		@Bean(onClass=X2.class, p="a,_b")
		public Object a() throws Exception {
			return new X2().init();
		}
		@RestGet
		@Bean(onClass=X2.class, p="a")
		public Object b() throws Exception {
			return new X2().init();
		}
		@RestGet
		@Bean(onClass=X2.class, p="_b")
		public Object c() throws Exception {
			return new X2().init();
		}
	}

	@Test
	public void a03_bpi_overridesClass() throws Exception {
		RestClient c = MockRestClient.build(A3.class);
		c.get("/a").json().run().assertContent("{\"a\":1,\"_b\":\"foo\"}");
		c.get("/a").xml().run().assertContent().isContains("<object><a>1</a><_b>foo</_b></object>");
		c.get("/a").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		c.get("/a").uon().run().assertContent("(a=1,_b=foo)");
		c.get("/a").urlEnc().run().assertContent("a=1&_b=foo");
		c.get("/b").json().run().assertContent("{\"a\":1}");
		c.get("/b").xml().run().assertContent().isContains("<object><a>1</a></object>");
		c.get("/b").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr></table>");
		c.get("/b").uon().run().assertContent("(a=1)");
		c.get("/b").urlEnc().run().assertContent("a=1");
		c.get("/c").json().run().assertContent("{\"_b\":\"foo\"}");
		c.get("/c").xml().run().assertContent().isContains("<object><_b>foo</_b></object>");
		c.get("/c").html().run().assertContent().isContains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		c.get("/c").uon().run().assertContent("(_b=foo)");
		c.get("/c").urlEnc().run().assertContent("_b=foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpx) and @Bean(bpi)
	//------------------------------------------------------------------------------------------------------------------

	public static class A4 extends BasicRestServlet {
		@RestGet
		@Bean(onClass=X2.class, xp="a,_b")
		public Object a() throws Exception {
			return new X2().init();
		}
		@RestGet
		@Bean(onClass=X2.class, xp="a")
		public Object b() throws Exception {
			return new X2().init();
		}
		@RestGet
		@Bean(onClass=X2.class, xp="_b")
		public Object c() throws Exception {
			return new X2().init();
		}
	}

	@Test
	public void a04_bpx_overridesClass() throws Exception {
		RestClient a4 = MockRestClient.build(A4.class);
		a4.get("/a").json().run().assertContent("{}");
		a4.get("/a").xml().run().assertContent().isContains("<object/>");
		a4.get("/a").html().run().assertContent().isContains("<table></table>");
		a4.get("/a").uon().run().assertContent("()");
		a4.get("/a").urlEnc().run().assertContent("");
		a4.get("/b").json().run().assertContent("{\"_b\":\"foo\"}");
		a4.get("/b").xml().run().assertContent().isContains("<object><_b>foo</_b></object>");
		a4.get("/b").html().run().assertContent().isContains("<table><tr><td>_b</td><td>foo</td></tr></table>");
		a4.get("/b").uon().run().assertContent("(_b=foo)");
		a4.get("/b").urlEnc().run().assertContent("_b=foo");
		a4.get("/c").json().run().assertContent("{\"a\":1}");
		a4.get("/c").xml().run().assertContent().isContains("<object><a>1</a></object>");
		a4.get("/c").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr></table>");
		a4.get("/c").uon().run().assertContent("(a=1)");
		a4.get("/c").urlEnc().run().assertContent("a=1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpi), meta-matching
	//------------------------------------------------------------------------------------------------------------------

	public static class A5 extends BasicRestServlet {
		@RestGet
		@Bean(on="*", p="a")
		public Object a() throws Exception {
			return new X1().init();
		}
	}

	@Test
	public void a05_bpi_metaMatching() throws Exception {
		RestClient a5 = MockRestClient.build(A5.class);
		a5.get("/a").json().run().assertContent("{\"a\":1}");
		a5.get("/a").xml().run().assertContent().isContains("<object><a>1</a></object>");
		a5.get("/a").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr></table>");
		a5.get("/a").uon().run().assertContent("(a=1)");
		a5.get("/a").urlEnc().run().assertContent("a=1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpi), fully-qualified class name
	//------------------------------------------------------------------------------------------------------------------

	public static class A6 extends BasicRestServlet {
		@RestGet
		@Bean(on="org.apache.juneau.rest.annotation.RestOp_BeanConfig_Test$X1", p="a")
		public Object a() throws Exception {
			return new X1().init();
		}
	}

	@Test
	public void a06_bpi_fullyQualifiedClassNames() throws Exception {
		RestClient a6 = MockRestClient.build(A6.class);
		a6.get("/a").json().run().assertContent("{\"a\":1}");
		a6.get("/a").xml().run().assertContent().isContains("<object><a>1</a></object>");
		a6.get("/a").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr></table>");
		a6.get("/a").uon().run().assertContent("(a=1)");
		a6.get("/a").urlEnc().run().assertContent("a=1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @BeanConfig(bpi), negative matching
	//------------------------------------------------------------------------------------------------------------------

	public static class A7 extends BasicRestServlet {
		@RestGet
		@Bean(on="MyBean", p="a")
		public Object a() throws Exception {
			// Should not match.
			return new X1().init();
		}
		@RestGet
		@Bean(on="MyBean", p="a")
		public Object b() throws Exception {
			// Should not match.  We don't support meta-matches in class names.
			return new X1().init();
		}
	}

	@Test
	public void a07_bpi_negativeMatching() throws Exception {
		RestClient a7 = MockRestClient.build(A7.class);
		a7.get("/a").json().run().assertContent("{\"a\":1,\"_b\":\"foo\"}");
		a7.get("/a").xml().run().assertContent().isContains("<object><a>1</a><_b>foo</_b></object>");
		a7.get("/a").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		a7.get("/a").uon().run().assertContent("(a=1,_b=foo)");
		a7.get("/a").urlEnc().run().assertContent("a=1&_b=foo");
		a7.get("/b").json().run().assertContent("{\"a\":1,\"_b\":\"foo\"}");
		a7.get("/b").xml().run().assertContent().isContains("<object><a>1</a><_b>foo</_b></object>");
		a7.get("/b").html().run().assertContent().isContains("<table><tr><td>a</td><td>1</td></tr><tr><td>_b</td><td>foo</td></tr></table>");
		a7.get("/b").uon().run().assertContent("(a=1,_b=foo)");
		a7.get("/b").urlEnc().run().assertContent("a=1&_b=foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Beans
	//------------------------------------------------------------------------------------------------------------------

	public static class X1 {
		public int a;
		@Beanp("_b") public String b;

		X1 init() {
			a = 1;
			b = "foo";
			return this;
		}
	}

	@Bean(properties="_b,a")
	public static class X2 {
		public int a;
		@Beanp("_b") public String b;

		X2 init() {
			a = 1;
			b = "foo";
			return this;
		}
	}
}
