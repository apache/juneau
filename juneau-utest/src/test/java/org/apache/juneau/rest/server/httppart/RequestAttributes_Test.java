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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RequestAttributes}.
 *
 * <p>Exercises addDefault overloads, asMap, contains/containsAny, get/getAll,
 * remove (by name and by NamedAttribute), set (by name/value, by NamedAttribute, varargs),
 * and toString through a {@link MockRestClient}-driven REST endpoint since {@link RequestAttributes}
 * requires a live {@link org.apache.juneau.rest.server.server.RestRequest}.
 */
class RequestAttributes_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Resource exposing branches of RequestAttributes.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		// asMap + getAll + size.
		@RestGet(path="/asMap")
		public String asMap(RequestAttributes attrs) {
			attrs.set("k1", "v1");
			attrs.set("k2", 42);
			var m = attrs.asMap();
			return "size=" + m.size()
				+ ",hasK1=" + m.containsKey("k1")
				+ ",k2=" + m.get("k2")
				+ ",getAllSize=" + attrs.getAll().size();
		}

		// contains / containsAny.
		@RestGet(path="/contains")
		public String contains(RequestAttributes attrs) {
			attrs.set("a", "x");
			attrs.set("b", "y");
			return "bothPresent=" + attrs.contains("a", "b")
				+ ",missing=" + attrs.contains("a", "missing")
				+ ",anyPresent=" + attrs.containsAny("missing", "a")
				+ ",noneFound=" + attrs.containsAny("missing1", "missing2");
		}

		// get(name) and RequestAttribute return value.
		@RestGet(path="/get")
		public String get(RequestAttributes attrs) {
			attrs.set("present", "hello");
			var present = attrs.get("present").asString().orElse("?");
			var absent = attrs.get("absent").asString().orElse("nope");
			return "present=" + present + ",absent=" + absent;
		}

		// set(name, value) - single-arg form.
		@RestGet(path="/set")
		public String set(RequestAttributes attrs) {
			attrs.set("k1", "v1");
			attrs.set("k2", 123);
			attrs.set("k3", null);
			return "k1=" + attrs.get("k1").asString().orElse("?")
				+ ",k2=" + attrs.get("k2").asString().orElse("?")
				+ ",k3=" + attrs.get("k3").asString().orElse("nullValue");
		}

		// set(NamedAttribute...) varargs form.
		@RestGet(path="/setVarargs")
		public String setVarargs(RequestAttributes attrs) {
			attrs.set(BasicNamedAttribute.of("v1", "one"), BasicNamedAttribute.of("v2", 2));
			return "v1=" + attrs.get("v1").asString().orElse("?")
				+ ",v2=" + attrs.get("v2").asString().orElse("?");
		}

		// remove(String...) and remove(NamedAttribute...).
		@RestGet(path="/remove")
		public String remove(RequestAttributes attrs) {
			attrs.set("a", "1");
			attrs.set("b", "2");
			attrs.set("c", "3");
			attrs.set("d", "4");
			attrs.remove("a", "b");
			attrs.remove(BasicNamedAttribute.of("c", "irrelevant"));
			return "a=" + attrs.get("a").asString().orElse("gone")
				+ ",b=" + attrs.get("b").asString().orElse("gone")
				+ ",c=" + attrs.get("c").asString().orElse("gone")
				+ ",d=" + attrs.get("d").asString().orElse("gone");
		}

		// addDefault(String, Object) - missing key gets set, existing key untouched.
		@RestGet(path="/addDefaultStringObj")
		public String addDefaultStringObj(RequestAttributes attrs) {
			attrs.set("preExisting", "kept");
			attrs.addDefault("preExisting", "shouldNotOverride");
			attrs.addDefault("newKey", "added");
			return "pre=" + attrs.get("preExisting").asString().orElse("?")
				+ ",new=" + attrs.get("newKey").asString().orElse("?");
		}

		// addDefault(NamedAttribute...) varargs.
		@RestGet(path="/addDefaultVarargs")
		public String addDefaultVarargs(RequestAttributes attrs) {
			attrs.set("a", "preset");
			attrs.addDefault(BasicNamedAttribute.of("a", "DEFAULT"), BasicNamedAttribute.of("b", "BVAL"));
			return "a=" + attrs.get("a").asString().orElse("?")
				+ ",b=" + attrs.get("b").asString().orElse("?");
		}

		// addDefault(List).
		@RestGet(path="/addDefaultList")
		public String addDefaultList(RequestAttributes attrs) {
			attrs.addDefault(l((NamedAttribute)BasicNamedAttribute.of("listed1", "L1"), BasicNamedAttribute.of("listed2", 2)));
			return "l1=" + attrs.get("listed1").asString().orElse("?")
				+ ",l2=" + attrs.get("listed2").asString().orElse("?");
		}

		// addDefault(NamedAttributeMap).
		@RestGet(path="/addDefaultMap")
		public String addDefaultMap(RequestAttributes attrs) {
			var map = NamedAttributeMap.create()
				.add(BasicNamedAttribute.of("m1", "mv1"))
				.add(BasicNamedAttribute.of("m2", "mv2"));
			attrs.addDefault(map);
			return "m1=" + attrs.get("m1").asString().orElse("?")
				+ ",m2=" + attrs.get("m2").asString().orElse("?");
		}

		// addDefault var-resolution: $E{...} would be resolved if it referenced a var. Use a literal for safe assertion.
		@RestGet(path="/addDefaultVarResolve")
		public String addDefaultVarResolve(RequestAttributes attrs) {
			attrs.addDefault("plain", "no-vars-here");
			return "plain=" + attrs.get("plain").asString().orElse("?");
		}

		// toString returns non-empty descriptive content.
		@RestGet(path="/toString")
		public String tostr(RequestAttributes attrs) {
			attrs.set("ts", "v");
			var s = attrs.toString();
			return "len=" + (!s.isEmpty()) + ",hasTs=" + s.contains("ts");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tests.
	//------------------------------------------------------------------------------------------------------------------

	private static MockRestClient client() { return MockRestClient.build(A.class); }

	@Test void a01_asMap_and_getAll() throws Exception {
		client().get("/asMap").run().assertContent().isContains("hasK1=true").assertContent().isContains("k2=42");
	}

	@Test void a02_contains_and_containsAny() throws Exception {
		client().get("/contains").run().assertContent("bothPresent=true,missing=false,anyPresent=true,noneFound=false");
	}

	@Test void a03_get_presentAndAbsent() throws Exception {
		client().get("/get").run().assertContent("present=hello,absent=nope");
	}

	@Test void a04_set_byNameAndValue() throws Exception {
		client().get("/set").run().assertContent("k1=v1,k2=123,k3=nullValue");
	}

	@Test void a04b_set_byNamedAttributeVarargs() throws Exception {
		client().get("/setVarargs").run().assertContent("v1=one,v2=2");
	}

	@Test void a05_remove_byNameAndNamedAttribute() throws Exception {
		client().get("/remove").run().assertContent("a=gone,b=gone,c=gone,d=4");
	}

	@Test void a06_addDefault_stringObject() throws Exception {
		client().get("/addDefaultStringObj").run().assertContent("pre=kept,new=added");
	}

	@Test void a07_addDefault_varargs() throws Exception {
		client().get("/addDefaultVarargs").run().assertContent("a=preset,b=BVAL");
	}

	@Test void a08_addDefault_list() throws Exception {
		client().get("/addDefaultList").run().assertContent("l1=L1,l2=2");
	}

	@Test void a09_addDefault_map() throws Exception {
		client().get("/addDefaultMap").run().assertContent("m1=mv1,m2=mv2");
	}

	@Test void a10_addDefault_varResolve_pathPlainString() throws Exception {
		client().get("/addDefaultVarResolve").run().assertContent("plain=no-vars-here");
	}

	@Test void a11_toString_isInformative() throws Exception {
		client().get("/toString").run().assertContent("len=true,hasTs=true");
	}
}
