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
 * Tests for {@link RequestQueryParamList}.
 *
 * <p>Tests exercise the list-mutation, query, and accessor APIs through MockRestClient endpoints
 * since the list requires a live RestRequest to construct.
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class RequestQueryParamList_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Query bean
	//------------------------------------------------------------------------------------------------------------------

	@Query("namedQueryBean")
	public static class NamedQueryBean extends HttpPartBean {
		public NamedQueryBean(String value) {
			super("namedQueryBean", value);
		}
	}

	public static class UnnamedQueryBean {
		public String value;
		public UnnamedQueryBean() { /* no-op */ }
	}

	//------------------------------------------------------------------------------------------------------------------
	// Resource
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		// Echoes name + size.
		@RestGet(path="/echo")
		public String echo(RequestQueryParamList q) {
			return "names=" + q.getNames() + ",size=" + q.size();
		}

		// Exercises contains/containsAny.
		@RestGet(path="/contains")
		public String contains(RequestQueryParamList q) {
			return "f1=" + q.contains("f1")
				+ ",fX=" + q.contains("fX")
				+ ",any=" + q.containsAny("fX","f2")
				+ ",none=" + q.containsAny("fX","fY");
		}

		// Exercises getFirst/getLast/getAll.
		@RestGet(path="/firstLast")
		public String firstLast(RequestQueryParamList q) {
			return "first=" + q.getFirst("a").asString().orElse("?")
				+ ",last=" + q.getLast("a").asString().orElse("?")
				+ ",all=" + q.getAll("a").size()
				+ ",missingFirst=" + q.getFirst("zz").asString().orElse("nope")
				+ ",missingLast=" + q.getLast("zz").asString().orElse("nope")
				+ ",get=" + q.get("a").asString().orElse("?")
				+ ",missingGet=" + q.get("zz").asString().orElse("none");
		}

		// Exercises add(String,Object), add(HttpPart...), set, remove.
		@RestGet(path="/mutate")
		public String mutate(RequestQueryParamList q) {
			q.add("added1", "v1");
			q.add(HttpStringPart.of("added2", "v2"), HttpStringPart.of("added3", "v3"));
			q.set("orig", "newVal");
			q.set(HttpStringPart.of("setHttpPart", "setVal"));
			var beforeRemove = q.size();
			q.remove("removeMe");
			var afterRemove = q.size();
			return "added1=" + q.get("added1").asString().orElse("?")
				+ ",added2=" + q.get("added2").asString().orElse("?")
				+ ",added3=" + q.get("added3").asString().orElse("?")
				+ ",orig=" + q.get("orig").asString().orElse("?")
				+ ",setHttpPart=" + q.get("setHttpPart").asString().orElse("?")
				+ ",sizeBefore=" + beforeRemove
				+ ",sizeAfter=" + afterRemove;
		}

		// Exercises copy() / subset() / asQueryString().
		@RestGet(path="/copySubset")
		public String copySubset(RequestQueryParamList q) {
			var c = q.copy();
			var s = q.subset("a","b");
			var qs = c.asQueryString();
			return "copySize=" + c.size()
				+ ",copyEqualsOrig=" + c.equals(q)
				+ ",subsetSize=" + s.size()
				+ ",qsContainsA=" + qs.contains("a=")
				+ ",empty=" + s.isEmpty();
		}

		// Exercises addDefault: blank/missing values get defaults; non-blank kept.
		@RestGet(path="/defaults")
		public String defaults(RequestQueryParamList q) {
			q.addDefault(HttpStringPart.of("d1", "default1"));
			q.addDefault("d2", "default2");
			q.addDefault(HttpStringPart.of("d3", "default3"), HttpStringPart.of("d4", "default4"));
			return "d1=" + q.get("d1").asString().orElse("?")
				+ ",d2=" + q.get("d2").asString().orElse("?")
				+ ",d3=" + q.get("d3").asString().orElse("?")
				+ ",d4=" + q.get("d4").asString().orElse("?")
				+ ",existing=" + q.get("existing").asString().orElse("?");
		}

		// Exercises caseSensitive(boolean) and getNames().
		@RestGet(path="/caseSensitive")
		public String caseSensitive(RequestQueryParamList q) {
			q.caseSensitive(true);
			q.add("UPPER", "u1");
			q.add("upper", "u2");
			var sensCount = q.getAll("UPPER").size();
			q.caseSensitive(false);
			var insensCount = q.getAll("UPPER").size();
			return "sens=" + sensCount + ",insens=" + insensCount + ",namesContainsUpper=" + q.getNames().contains("upper");
		}

		// Exercises getSorted().
		@RestGet(path="/sorted")
		public String sorted(RequestQueryParamList q) {
			q.caseSensitive(false);
			var ci = q.getSorted().toList().stream().map(RequestQueryParam::getName).toList();
			q.caseSensitive(true);
			var cs = q.getSorted().toList().stream().map(RequestQueryParam::getName).toList();
			return "ci=" + ci + ",cs=" + cs;
		}

		// Exercises set(HttpPart...) and equals/hashCode and toString.
		@SuppressWarnings({
			"unlikely-arg-type" // Intentionally tests equals() returns false for a non-compatible argument type.
		})
		@RestGet(path="/equality")
		public String equality(RequestQueryParamList q) {
			var c1 = q.copy();
			var c2 = q.copy();
			q.set(HttpStringPart.of("a", "modified"));
			var s = q.toString();
			var hc = c1.hashCode() == c2.hashCode();
			return "eqSelf=" + c1.equals(c1)
				+ ",eqOther=" + c1.equals(c2)
				+ ",eqDiff=" + c1.equals(q)
				+ ",eqString=" + c1.equals("not a list")
				+ ",hashEq=" + hc
				+ ",toStringNotNull=" + (s != null && !s.isEmpty());
		}

		// Exercises get(name) when multiple values present.
		@RestGet(path="/multi")
		public String multi(RequestQueryParamList q) {
			return "combined=" + q.get("m").asString().orElse("?");
		}

		// Exercises parser() setter.
		@RestGet(path="/parser")
		public String parser(RestRequest req) {
			var q = req.getQueryParams();
			q.parser(req.getPartParserSession());
			return "parserNotNull=" + (q.get("a").asString().orElse(null) != null);
		}

		// Exercises asQueryString.
		@RestGet(path="/qs")
		public String qs(RequestQueryParamList q) {
			return q.asQueryString();
		}

		// Exercises get(Class) success case.
		@RestGet(path="/getByClass")
		public String getByClass(RequestQueryParamList q) {
			return "v=" + q.get(NamedQueryBean.class).map(b -> b.getValue()).orElse("?");
		}

		// Exercises get(Class) failure case (no @Query annotation).
		@RestGet(path="/getByUnannotatedClass")
		public String getByUnannotatedClass(RequestQueryParamList q) {
			try {
				q.get(UnnamedQueryBean.class);
				return "no-throw";
			} catch (RuntimeException e) {
				return "rex:" + e.getMessage().contains("@Query(name) not found");
			}
		}

		// Exercises add(HttpPart...) with a null entry inside the array.
		@RestGet(path="/addWithNullEntry")
		public String addWithNullEntry(RequestQueryParamList q) {
			q.add(HttpStringPart.of("k1","v1"), null, HttpStringPart.of("k2","v2"));
			return "k1=" + q.get("k1").asString().orElse("?")
				+ ",k2=" + q.get("k2").asString().orElse("?");
		}

		// Exercises getPageArgs / getSearchArgs / getSortArgs / getViewArgs.
		@RestGet(path="/searchArgs")
		public String searchArgs(RequestQueryParamList q) {
			return "page=" + q.getPageArgs().isPresent()
				+ ",search=" + q.getSearchArgs().isPresent()
				+ ",sort=" + q.getSortArgs().isPresent()
				+ ",view=" + q.getViewArgs().isPresent();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a01_basicEcho() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/echo?f1=v1&f2=v2").run().assertContent("names=[f1, f2],size=2");
	}

	@Test
	void a02_contains() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/contains?f1=v1&f2=v2").run()
			.assertContent("f1=true,fX=false,any=true,none=false");
	}

	@Test
	void a03_firstLast_multipleValuesAndMissing() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/firstLast?a=1&a=2&a=3").run()
			.assertContent("first=1,last=3,all=3,missingFirst=nope,missingLast=nope,get=1, 2, 3,missingGet=none");
	}

	@Test
	void a04_firstLast_singleValue() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/firstLast?a=only").run()
			.assertContent("first=only,last=only,all=1,missingFirst=nope,missingLast=nope,get=only,missingGet=none");
	}

	@Test
	void a05_mutate() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/mutate?orig=oldVal&removeMe=byebye&removeMe=again").run()
			.assertContent("added1=v1,added2=v2,added3=v3,orig=newVal,setHttpPart=setVal,sizeBefore=7,sizeAfter=5");
	}

	@Test
	void a06_copyAndSubset() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/copySubset?a=1&b=2&c=3").run()
			.assertContent("copySize=3,copyEqualsOrig=true,subsetSize=2,qsContainsA=true,empty=false");
	}

	@Test
	void a07_addDefault() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/defaults?d1=preset&existing=keep").run()
			.assertContent("d1=preset,d2=default2,d3=default3,d4=default4,existing=keep");
	}

	@Test
	void a08_addDefault_blankIsReplaced() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/defaults?d1=").run()
			.assertContent("d1=default1,d2=default2,d3=default3,d4=default4,existing=?");
	}

	@Test
	void a09_caseSensitive() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/caseSensitive").run()
			.assertContent("sens=1,insens=2,namesContainsUpper=true");
	}

	@Test
	void a10_sorted() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/sorted?B=2&a=1&C=3").run()
			.assertContent("ci=[a, B, C],cs=[B, C, a]");
	}

	@Test
	void a11_equality() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/equality?a=v").run()
			.assertContent("eqSelf=true,eqOther=true,eqDiff=false,eqString=false,hashEq=true,toStringNotNull=true");
	}

	@Test
	void a12_multipleValuesCombined() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/multi?m=a&m=b&m=c").run()
			.assertContent("combined=a, b, c");
	}

	@Test
	void a13_parser() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/parser?a=1").run()
			.assertContent("parserNotNull=true");
	}

	@Test
	void a14_asQueryString() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/qs?a=hello&b=world").run()
			.assertContent("a=hello&b=world");
	}

	@Test
	void a15_emptyQuery_emptyQueryString() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/qs").run().assertContent("");
	}

	@Test
	void a16_blankParam_paths() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/echo?p1&p2=v2").run()
			.assertContent("names=[p1, p2],size=2");
		c.get("/echo?p1=").run()
			.assertContent("names=[p1],size=1");
	}

	@Test
	void a17_getByClass() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/getByClass?namedQueryBean=hello").run()
			.assertContent("v=hello");
	}

	@Test
	void a18_getByClass_noAnnotation() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/getByUnannotatedClass").run()
			.assertContent("rex:true");
	}

	@Test
	void a19_addWithNullEntry() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/addWithNullEntry").run()
			.assertContent("k1=v1,k2=v2");
	}

	@Test
	void a20_searchArgs_present() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/searchArgs?p=0&l=10&s=name=foo&o=name&v=name").run()
			.assertContent("page=true,search=true,sort=true,view=true");
	}

	@Test
	void a21_searchArgs_empty() throws Exception {
		var c = MockRestClient.build(A.class);
		c.get("/searchArgs").run()
			.assertContent("page=false,search=false,sort=false,view=false");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Argument validation
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet(path="/addNullArrayThrows")
		public String addNullArrayThrows(RequestQueryParamList q) {
			try {
				q.add((HttpPart[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("parameters");
			}
		}
		@RestGet(path="/setNullArrayThrows")
		public String setNullArrayThrows(RequestQueryParamList q) {
			try {
				q.set((HttpPart[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("headers");
			}
		}
		@RestGet(path="/addNullNameThrows")
		public String addNullNameThrows(RequestQueryParamList q) {
			try {
				q.add((String)null, "v");
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/containsAnyNullThrows")
		public String containsAnyNullThrows(RequestQueryParamList q) {
			try {
				q.containsAny((String[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("names");
			}
		}
		@RestGet(path="/setNullNameThrows")
		public String setNullNameThrows(RequestQueryParamList q) {
			try {
				q.set((String)null, "v");
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/removeNullThrows")
		public String removeNullThrows(RequestQueryParamList q) {
			try {
				q.remove(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/getFirstNullThrows")
		public String getFirstNullThrows(RequestQueryParamList q) {
			try {
				q.getFirst(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestGet(path="/getLastNullThrows")
		public String getLastNullThrows(RequestQueryParamList q) {
			try {
				q.getLast(null);
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
	}
}
