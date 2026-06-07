/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.httppart;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RequestPathParamList}.
 *
 * <p>Tests exercise the list-mutation, query, and accessor APIs through MockRestClient endpoints
 * since the list requires a live RestRequest to construct.
 */
class RequestPathParamList_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Path bean
	//------------------------------------------------------------------------------------------------------------------

	@Path("namedPathBean")
	public static class NamedPathBean extends HttpPartBean {
		public NamedPathBean(String value) {
			super("namedPathBean", value);
		}
	}

	public static class UnnamedPathBean {
		public String value;
		public UnnamedPathBean() {}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Resource
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		// Single path variable.
		@RestGet(path="/single/{a}")
		public String single(RequestPathParamList p) {
			return "names=" + p.getNames() + ",a=" + p.get("a").asString().orElse("?");
		}

		// Multiple path variables - exercises getFirst/getLast/getAll.
		@RestGet(path="/multi/{a}/{b}")
		public String multi(RequestPathParamList p) {
			return "first=" + p.getFirst("a").asString().orElse("?")
				+ ",last=" + p.getLast("b").asString().orElse("?")
				+ ",missingFirst=" + p.getFirst("zz").asString().orElse("nope")
				+ ",missingLast=" + p.getLast("zz").asString().orElse("nope")
				+ ",getAll=" + p.getAll("a").size()
				+ ",missingGet=" + p.get("zz").asString().orElse("none");
		}

		// Exercises remainder paths.
		@RestGet(path="/rem/{a}/*")
		public String remainder(RequestPathParamList p) {
			return "rem=" + p.getRemainder().asString().orElse("?")
				+ ",remU=" + p.getRemainderUndecoded().asString().orElse("?")
				+ ",a=" + p.get("a").asString().orElse("?");
		}

		// Exercises contains/containsAny.
		@RestGet(path="/contains/{a}/{b}")
		public String contains(RequestPathParamList p) {
			return "a=" + p.contains("a")
				+ ",x=" + p.contains("x")
				+ ",any=" + p.containsAny("x","b")
				+ ",none=" + p.containsAny("x","y");
		}

		// Exercises add(String,Object), add(HttpPart...), set, remove.
		@RestGet(path="/mutate/{a}")
		public String mutate(RequestPathParamList p) {
			p.add("added1", "v1");
			p.add(HttpStringPart.of("added2", "v2"), HttpStringPart.of("added3", "v3"));
			p.set("a", "newA");
			p.set(HttpStringPart.of("setHttpPart", "setVal"));
			var beforeRemove = p.size();
			p.remove("added3");
			var afterRemove = p.size();
			return "added1=" + p.get("added1").asString().orElse("?")
				+ ",added2=" + p.get("added2").asString().orElse("?")
				+ ",added3=" + p.get("added3").asString().orElse("none")
				+ ",a=" + p.get("a").asString().orElse("?")
				+ ",setHttpPart=" + p.get("setHttpPart").asString().orElse("?")
				+ ",sizeBefore=" + beforeRemove
				+ ",sizeAfter=" + afterRemove;
		}

		// Exercises copy() and subset().
		@RestGet(path="/copySubset/{a}/{b}")
		public String copySubset(RequestPathParamList p) {
			var c = p.copy();
			var s = p.subset("a");
			return "copySize=" + c.size()
				+ ",copyEqualsOrig=" + c.equals(p)
				+ ",subsetSize=" + s.size()
				+ ",subsetEmpty=" + s.isEmpty();
		}

		// Exercises addDefault: blank/missing values get defaults; non-blank kept.
		@RestGet(path="/defaults/{a}")
		public String defaults(RequestPathParamList p) {
			p.addDefault(HttpStringPart.of("d1", "default1"));
			p.addDefault("d2", "default2");
			p.addDefault(HttpStringPart.of("d3", "default3"), HttpStringPart.of("d4", "default4"));
			return "a=" + p.get("a").asString().orElse("?")
				+ ",d1=" + p.get("d1").asString().orElse("?")
				+ ",d2=" + p.get("d2").asString().orElse("?")
				+ ",d3=" + p.get("d3").asString().orElse("?")
				+ ",d4=" + p.get("d4").asString().orElse("?");
		}

		// Exercises caseSensitive(boolean) + getNames().
		@RestGet(path="/case/{a}")
		public String caseSensitive(RequestPathParamList p) {
			p.caseSensitive(true);
			p.add("UPPER", "u1");
			p.add("upper", "u2");
			var sensCount = p.getAll("UPPER").size();
			p.caseSensitive(false);
			var insensCount = p.getAll("UPPER").size();
			return "sens=" + sensCount + ",insens=" + insensCount + ",namesContainsUpper=" + p.getNames().contains("upper");
		}

		// Exercises getSorted().
		@RestGet(path="/sorted/{a}/{b}")
		public String sorted(RequestPathParamList p) {
			p.add("Z", "z");
			p.add("A", "a");
			p.caseSensitive(false);
			var ci = p.getSorted().toList().stream().map(RequestPathParam::getName).toList();
			p.caseSensitive(true);
			var cs = p.getSorted().toList().stream().map(RequestPathParam::getName).toList();
			return "ciSize=" + ci.size() + ",csSize=" + cs.size();
		}

		// Exercises set(HttpPart...) and equals/hashCode and toString.
		@RestGet(path="/equality/{a}")
		public String equality(RequestPathParamList p) {
			var c1 = p.copy();
			var c2 = p.copy();
			p.set(HttpStringPart.of("a", "modified"));
			var s = p.toString();
			var hc = c1.hashCode() == c2.hashCode();
			return "eqSelf=" + c1.equals(c1)
				+ ",eqOther=" + c1.equals(c2)
				+ ",eqDiff=" + c1.equals(p)
				+ ",eqString=" + c1.equals("not a list")
				+ ",hashEq=" + hc
				+ ",toStringNotNull=" + (s != null && !s.isEmpty());
		}

		// Exercises get(name) when multiple values present (combined comma-separated).
		@RestGet(path="/multiVal/{a}")
		public String multiVal(RequestPathParamList p) {
			p.add("m", "x");
			p.add("m", "y");
			p.add("m", "z");
			return "combined=" + p.get("m").asString().orElse("?");
		}

		// Exercises parser() setter.
		@RestGet(path="/parser/{a}")
		public String parser(RestRequest req) throws Exception {
			var p = req.getPathParams();
			p.parser(req.getPartParserSession());
			return "parserNotNull=" + (p.get("a").asString().orElse(null) != null);
		}

		// Exercises get(Class) success case.
		@RestGet(path="/getByClass/{namedPathBean}")
		public String getByClass(RequestPathParamList p) {
			return "v=" + p.get(NamedPathBean.class).map(b -> b.getValue()).orElse("?");
		}

		// Exercises get(Class) failure case (no @Path annotation).
		@RestGet(path="/getByUnannotatedClass/{a}")
		public String getByUnannotatedClass(RequestPathParamList p) {
			try {
				p.get(UnnamedPathBean.class);
				return "no-throw";
			} catch (RuntimeException e) {
				return "rex:" + e.getMessage().contains("@Path(name) not found");
			}
		}

		// Exercises add(HttpPart...) with a null entry inside the array.
		@RestGet(path="/addWithNullEntry/{a}")
		public String addWithNullEntry(RequestPathParamList p) {
			p.add(HttpStringPart.of("k1","v1"), null, HttpStringPart.of("k2","v2"));
			return "k1=" + p.get("k1").asString().orElse("?")
				+ ",k2=" + p.get("k2").asString().orElse("?");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a01_single() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/single/foo").run().assertContent("names=[a],a=foo");
	}

	@Test
	void a02_multi() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/multi/x/y").run()
			.assertContent("first=x,last=y,missingFirst=nope,missingLast=nope,getAll=1,missingGet=none");
	}

	@Test
	void a03_remainder() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/rem/foo/abc/def").run()
			.assertContent("rem=abc/def,remU=abc/def,a=foo");
	}

	@Test
	void a04_remainder_encoded() throws Exception {
		var c = MockRestClient.build(A.class);
		// %2F is encoded slash; remainder() decodes; remainderUndecoded does not.
		c.get("/rem/foo/a%2Fb").run()
			.assertContent("rem=a/b,remU=a%2Fb,a=foo");
	}

	@Test
	void a05_contains() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/contains/1/2").run()
			.assertContent("a=true,x=false,any=true,none=false");
	}

	@Test
	void a06_mutate() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/mutate/orig").run()
			.assertContent("added1=v1,added2=v2,added3=none,a=newA,setHttpPart=setVal,sizeBefore=5,sizeAfter=4");
	}

	@Test
	void a07_copySubset() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/copySubset/x/y").run()
			.assertContent("copySize=2,copyEqualsOrig=true,subsetSize=1,subsetEmpty=false");
	}

	@Test
	void a08_addDefault() throws Exception {
		var c = MockRestClient.build(A.class);
		// d1-d4 absent => defaults applied; a present => kept.
		c.get("/defaults/preset").run()
			.assertContent("a=preset,d1=default1,d2=default2,d3=default3,d4=default4");
	}

	@Test
	void a09_caseSensitive() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/case/x").run()
			.assertContent("sens=1,insens=2,namesContainsUpper=true");
	}

	@Test
	void a10_sorted() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/sorted/x/y").run()
			.assertContent("ciSize=4,csSize=4");
	}

	@Test
	void a11_equality() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/equality/x").run()
			.assertContent("eqSelf=true,eqOther=true,eqDiff=false,eqString=false,hashEq=true,toStringNotNull=true");
	}

	@Test
	void a12_multipleValuesCombined() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/multiVal/v").run()
			.assertContent("combined=x, y, z");
	}

	@Test
	void a13_parser() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/parser/v").run()
			.assertContent("parserNotNull=true");
	}

	@Test
	void a14_getByClass() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/getByClass/hello").run()
			.assertContent("v=hello");
	}

	@Test
	void a15_getByClass_noAnnotation() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/getByUnannotatedClass/x").run()
			.assertContent("rex:true");
	}

	@Test
	void a16_addWithNullEntry() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/addWithNullEntry/x").run()
			.assertContent("k1=v1,k2=v2");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Argument validation
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet(path="/addNullArrayThrows")
		public String addNullArrayThrows(RequestPathParamList p) {
			try {
				p.add((HttpPart[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("parameters");
			}
		}
		@RestGet(path="/setNullArrayThrows")
		public String setNullArrayThrows(RequestPathParamList p) {
			try {
				p.set((HttpPart[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("headers");
			}
		}
		@RestGet(path="/addNullNameThrows")
		public String addNullNameThrows(RequestPathParamList p) {
			try {
				p.add((String)null, "v");
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/containsAnyNullThrows")
		public String containsAnyNullThrows(RequestPathParamList p) {
			try {
				p.containsAny((String[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("names");
			}
		}
		@RestGet(path="/setNullNameThrows")
		public String setNullNameThrows(RequestPathParamList p) {
			try {
				p.set((String)null, "v");
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/removeNullThrows")
		public String removeNullThrows(RequestPathParamList p) {
			try {
				p.remove(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/getFirstNullThrows")
		public String getFirstNullThrows(RequestPathParamList p) {
			try {
				p.getFirst(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/getLastNullThrows")
		public String getLastNullThrows(RequestPathParamList p) {
			try {
				p.getLast(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/containsNullThrows")
		public String containsNullThrows(RequestPathParamList p) {
			try {
				p.contains(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/getAllNullThrows")
		public String getAllNullThrows(RequestPathParamList p) {
			try {
				p.getAll(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
	}

	@Test
	void b01_argValidation() throws Exception {
		var c = MockRestClient.build(B.class);
		c.get("/addNullArrayThrows").run().assertContent("iae:true");
		c.get("/setNullArrayThrows").run().assertContent("iae:true");
		c.get("/addNullNameThrows").run().assertContent("iae:true");
		c.get("/containsAnyNullThrows").run().assertContent("iae:true");
		c.get("/setNullNameThrows").run().assertContent("iae:true");
		c.get("/removeNullThrows").run().assertContent("iae:true");
		c.get("/getFirstNullThrows").run().assertContent("iae:true");
		c.get("/getLastNullThrows").run().assertContent("iae:true");
		c.get("/containsNullThrows").run().assertContent("iae:true");
		c.get("/getAllNullThrows").run().assertContent("iae:true");
	}
}
