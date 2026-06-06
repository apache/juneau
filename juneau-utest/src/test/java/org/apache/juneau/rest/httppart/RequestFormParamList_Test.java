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
package org.apache.juneau.rest.httppart;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.HttpPart;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RequestFormParamList}.
 *
 * <p>Tests exercise the list-mutation, query, and accessor APIs through MockRestClient endpoints
 * since the list requires a live RestRequest to construct.
 */
class RequestFormParamList_Test extends TestBase {

	private static final String CT_FORM = "application/x-www-form-urlencoded";

	//------------------------------------------------------------------------------------------------------------------
	// FormData bean
	//------------------------------------------------------------------------------------------------------------------

	@FormData("namedFormBean")
	public static class NamedFormBean extends HttpPartBean {
		public NamedFormBean(String value) {
			super("namedFormBean", value);
		}
	}

	public static class UnnamedFormBean {
		public String value;
		public UnnamedFormBean() {}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Resource
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		// Echoes the names + values + getNames() (case-insensitive lower-cased).
		@RestPost(path="/echo")
		public String echo(RequestFormParamList f) {
			return "names=" + f.getNames() + ",size=" + f.size();
		}

		// Exercises contains/containsAny.
		@RestPost(path="/contains")
		public String contains(RequestFormParamList f) {
			return "f1=" + f.contains("f1")
				+ ",fX=" + f.contains("fX")
				+ ",any=" + f.containsAny("fX","f2")
				+ ",none=" + f.containsAny("fX","fY");
		}

		// Exercises getFirst/getLast/getAll.
		@RestPost(path="/firstLast")
		public String firstLast(RequestFormParamList f) {
			return "first=" + f.getFirst("a").asString().orElse("?")
				+ ",last=" + f.getLast("a").asString().orElse("?")
				+ ",all=" + f.getAll("a").size()
				+ ",missingFirst=" + f.getFirst("zz").asString().orElse("nope")
				+ ",missingLast=" + f.getLast("zz").asString().orElse("nope")
				+ ",get=" + f.get("a").asString().orElse("?")
				+ ",missingGet=" + f.get("zz").asString().orElse("none");
		}

		// Exercises add(String,Object) and add(HttpPart...) and set + remove.
		@RestPost(path="/mutate")
		public String mutate(RequestFormParamList f) {
			f.add("added1", "v1");
			f.add(HttpStringPart.of("added2", "v2"), HttpStringPart.of("added3", "v3"));
			f.set("orig", "newVal");
			f.set(HttpStringPart.of("setHttpPart", "setVal"));
			var beforeRemove = f.size();
			f.remove("removeMe");
			var afterRemove = f.size();
			return "added1=" + f.get("added1").asString().orElse("?")
				+ ",added2=" + f.get("added2").asString().orElse("?")
				+ ",added3=" + f.get("added3").asString().orElse("?")
				+ ",orig=" + f.get("orig").asString().orElse("?")
				+ ",setHttpPart=" + f.get("setHttpPart").asString().orElse("?")
				+ ",sizeBefore=" + beforeRemove
				+ ",sizeAfter=" + afterRemove;
		}

		// Exercises copy() and subset() and asQueryString().
		@RestPost(path="/copySubset")
		public String copySubset(RequestFormParamList f) {
			var c = f.copy();
			var s = f.subset("a","b");
			var qs = c.asQueryString();
			return "copySize=" + c.size()
				+ ",copyEqualsOrig=" + c.equals(f)
				+ ",subsetSize=" + s.size()
				+ ",qsContainsA=" + qs.contains("a=")
				+ ",empty=" + s.isEmpty();
		}

		// Exercises addDefault: blank values should be replaced; non-blank should be preserved.
		@RestPost(path="/defaults")
		public String defaults(RequestFormParamList f) {
			f.addDefault(HttpStringPart.of("d1", "default1"));
			f.addDefault("d2", "default2");
			f.addDefault(HttpStringPart.of("d3", "default3"), HttpStringPart.of("d4", "default4"));
			return "d1=" + f.get("d1").asString().orElse("?")
				+ ",d2=" + f.get("d2").asString().orElse("?")
				+ ",d3=" + f.get("d3").asString().orElse("?")
				+ ",d4=" + f.get("d4").asString().orElse("?")
				+ ",existing=" + f.get("existing").asString().orElse("?");
		}

		// Exercises caseSensitive(boolean) and getNames().
		@RestPost(path="/caseSensitive")
		public String caseSensitive(RequestFormParamList f) {
			f.caseSensitive(true);
			f.add("UPPER", "u1");
			f.add("upper", "u2");
			var sensCount = f.getAll("UPPER").size();
			f.caseSensitive(false);
			var insensCount = f.getAll("UPPER").size();
			return "sens=" + sensCount + ",insens=" + insensCount + ",namesContainsUpper=" + f.getNames().contains("upper");
		}

		// Exercises getSorted().
		@RestPost(path="/sorted")
		public String sorted(RequestFormParamList f) {
			f.caseSensitive(false);
			var ci = f.getSorted().toList().stream().map(RequestFormParam::getName).toList();
			f.caseSensitive(true);
			var cs = f.getSorted().toList().stream().map(RequestFormParam::getName).toList();
			return "ci=" + ci + ",cs=" + cs;
		}

		// Exercises set(HttpPart...) and equals/hashCode and toString.
		@RestPost(path="/equality")
		public String equality(RequestFormParamList f) {
			var c1 = f.copy();
			var c2 = f.copy();
			f.set(HttpStringPart.of("a", "modified"));
			var s = f.toString();
			var hc = c1.hashCode() == c2.hashCode();
			return "eqSelf=" + c1.equals(c1)
				+ ",eqOther=" + c1.equals(c2)
				+ ",eqDiff=" + c1.equals(f)
				+ ",eqString=" + c1.equals("not a list")
				+ ",hashEq=" + hc
				+ ",toStringNotNull=" + (s != null && !s.isEmpty());
		}

		// Exercises get(name) when multiple values present (combined comma-separated).
		@RestPost(path="/multi")
		public String multi(RequestFormParamList f) {
			return "combined=" + f.get("m").asString().orElse("?");
		}

		// Exercises parser() setter.
		@RestPost(path="/parser")
		public String parser(RestRequest req) throws Exception {
			var f = req.getFormParams();
			f.parser(req.getPartParserSession());
			return "parserNotNull=" + (f.get("a").asString().orElse(null) != null);
		}

		// Exercises asQueryString.
		@RestPost(path="/qs")
		public String qs(RequestFormParamList f) {
			return f.asQueryString();
		}

		// Exercises add(HttpPart...) with a null entry inside the array.
		@RestPost(path="/addWithNullEntry")
		public String addWithNullEntry(RequestFormParamList f) {
			f.add(HttpStringPart.of("k1","v1"), null, HttpStringPart.of("k2","v2"));
			return "k1=" + f.get("k1").asString().orElse("?")
				+ ",k2=" + f.get("k2").asString().orElse("?");
		}

		// Exercises get(Class) success case.
		@RestPost(path="/getByClass")
		public String getByClass(RequestFormParamList f) {
			return "v=" + f.get(NamedFormBean.class).map(b -> b.getValue()).orElse("?");
		}

		// Exercises get(Class) failure case (no @FormData annotation).
		@RestPost(path="/getByUnannotatedClass")
		public String getByUnannotatedClass(RequestFormParamList f) {
			try {
				f.get(UnnamedFormBean.class);
				return "no-throw";
			} catch (RuntimeException e) {
				return "rex:" + e.getMessage().contains("@FormData(name) not found");
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a01_basicEcho() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/echo", "f1=v1&f2=v2").contentType(CT_FORM).run().assertContent("names=[f1, f2],size=2");
	}

	@Test
	void a02_contains() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/contains", "f1=v1&f2=v2").contentType(CT_FORM).run()
			.assertContent("f1=true,fX=false,any=true,none=false");
	}

	@Test
	void a03_firstLast_multipleValuesAndMissing() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/firstLast", "a=1&a=2&a=3").contentType(CT_FORM).run()
			.assertContent("first=1,last=3,all=3,missingFirst=nope,missingLast=nope,get=1, 2, 3,missingGet=none");
	}

	@Test
	void a04_firstLast_singleValue() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/firstLast", "a=only").contentType(CT_FORM).run()
			.assertContent("first=only,last=only,all=1,missingFirst=nope,missingLast=nope,get=only,missingGet=none");
	}

	@Test
	void a05_mutate() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/mutate", "orig=oldVal&removeMe=byebye&removeMe=again").contentType(CT_FORM).run()
			.assertContent("added1=v1,added2=v2,added3=v3,orig=newVal,setHttpPart=setVal,sizeBefore=7,sizeAfter=5");
	}

	@Test
	void a06_copyAndSubset() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/copySubset", "a=1&b=2&c=3").contentType(CT_FORM).run()
			.assertContent("copySize=3,copyEqualsOrig=true,subsetSize=2,qsContainsA=true,empty=false");
	}

	@Test
	void a07_addDefault() throws Exception {
		var c = MockRestClient.build(A.class);
		// d1 already present and non-blank => default not used.
		// d2 absent => default applied.
		// d3/d4 absent => defaults applied.
		// existing already present => default not used.
		c.post("/defaults", "d1=preset&existing=keep").contentType(CT_FORM).run()
			.assertContent("d1=preset,d2=default2,d3=default3,d4=default4,existing=keep");
	}

	@Test
	void a08_addDefault_blankIsReplaced() throws Exception {
		var c = MockRestClient.build(A.class);
		// d1 present but blank => default applied.
		c.post("/defaults", "d1=").contentType(CT_FORM).run()
			.assertContent("d1=default1,d2=default2,d3=default3,d4=default4,existing=?");
	}

	@Test
	void a09_caseSensitive() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/caseSensitive", "").contentType(CT_FORM).run()
			.assertContent("sens=1,insens=2,namesContainsUpper=true");
	}

	@Test
	void a10_sorted() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/sorted", "B=2&a=1&C=3").contentType(CT_FORM).run()
			.assertContent("ci=[a, B, C],cs=[B, C, a]");
	}

	@Test
	void a11_equality() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/equality", "a=v").contentType(CT_FORM).run()
			.assertContent("eqSelf=true,eqOther=true,eqDiff=false,eqString=false,hashEq=true,toStringNotNull=true");
	}

	@Test
	void a12_multipleValuesCombined() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/multi", "m=a&m=b&m=c").contentType(CT_FORM).run()
			.assertContent("combined=a, b, c");
	}

	@Test
	void a13_parser() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/parser", "a=1").contentType(CT_FORM).run()
			.assertContent("parserNotNull=true");
	}

	@Test
	void a14_asQueryString() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/qs", "a=hello&b=world").contentType(CT_FORM).run()
			.assertContent("a=hello&b=world");
	}

	@Test
	void a15_emptyContent_emptyQueryString() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/qs", "").contentType(CT_FORM).run()
			.assertContent("");
	}

	@Test
	void a16_getByClass() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/getByClass", "namedFormBean=hello").contentType(CT_FORM).run()
			.assertContent("v=hello");
	}

	@Test
	void a17_getByClass_noAnnotation() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/getByUnannotatedClass", "").contentType(CT_FORM).run()
			.assertContent("rex:true");
	}

	@Test
	void a18_addWithNullEntry() throws Exception {
		var c = MockRestClient.build(A.class);
		c.post("/addWithNullEntry", "").contentType(CT_FORM).run()
			.assertContent("k1=v1,k2=v2");
	}

	@Test
	void a19_blankParam_paths() throws Exception {
		// Exercise constructor path with empty values.
		var c = MockRestClient.build(A.class);
		c.post("/echo", "p1&p2=v2").contentType(CT_FORM).run()
			.assertContent("names=[p1, p2],size=2");
		c.post("/echo", "p1=").contentType(CT_FORM).run()
			.assertContent("names=[p1],size=1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Argument validation
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestPost
		public String addNullArrayThrows(RequestFormParamList f) {
			try {
				f.add((HttpPart[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("parameters");
			}
		}
		@RestPost
		public String setNullArrayThrows(RequestFormParamList f) {
			try {
				f.set((HttpPart[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("headers");
			}
		}
		@RestPost
		public String addNullNameThrows(RequestFormParamList f) {
			try {
				f.add((String)null, "v");
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestPost
		public String containsAnyNullThrows(RequestFormParamList f) {
			try {
				f.containsAny((String[])null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("names");
			}
		}
		@RestPost
		public String setNullNameThrows(RequestFormParamList f) {
			try {
				f.set((String)null, "v");
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestPost
		public String removeNullThrows(RequestFormParamList f) {
			try {
				f.remove(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestPost
		public String getFirstNullThrows(RequestFormParamList f) {
			try {
				f.getFirst(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
		@RestPost
		public String getLastNullThrows(RequestFormParamList f) {
			try {
				f.getLast(null);
				return "no-throw";
			} catch (IllegalArgumentException e) {
				return "iae:" + e.getMessage().contains("name");
			}
		}
	}

	@Test
	void b01_argValidation() throws Exception {
		var c = MockRestClient.build(B.class);
		c.post("/addNullArrayThrows", "").contentType(CT_FORM).run().assertContent("iae:true");
		c.post("/setNullArrayThrows", "").contentType(CT_FORM).run().assertContent("iae:true");
		c.post("/addNullNameThrows", "").contentType(CT_FORM).run().assertContent("iae:true");
		c.post("/containsAnyNullThrows", "").contentType(CT_FORM).run().assertContent("iae:true");
		c.post("/setNullNameThrows", "").contentType(CT_FORM).run().assertContent("iae:true");
		c.post("/removeNullThrows", "").contentType(CT_FORM).run().assertContent("iae:true");
		c.post("/getFirstNullThrows", "").contentType(CT_FORM).run().assertContent("iae:true");
		c.post("/getLastNullThrows", "").contentType(CT_FORM).run().assertContent("iae:true");
	}
}
