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
package org.apache.juneau.rest;

import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RestRequest} session property methods.
 */
class RestRequest_SessionProperties_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic setter tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class)
	public static class A {
		@RestGet(path="/test")
		public String test(RestRequest req) {
			req.setSerializerSessionProperty("key1", "value1");
			var map = req.getSerializerSessionPropertyMap();
			return map.get("key1") != null ? "ok" : "fail";
		}
	}

	@Test
	void a01_setSerializerSessionProperty_single() throws Exception {
		MockRestClient.create(A.class).plainText().build()
			.get("/test")
			.run()
			.assertContent("ok");
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class)
	public static class B {
		@RestPost(path="/test")
		public String test(RestRequest req) {
			req.setParserSessionProperty("key1", "value1");
			var map = req.getParserSessionPropertyMap();
			return map.get("key1") != null ? "ok" : "fail";
		}
	}

	@Test
	void a02_setParserSessionProperty_single() throws Exception {
		MockRestClient.create(B.class).plainText().build()
			.post("/test", "")
			.run()
			.assertContent("ok");
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class)
	public static class C {
		@RestGet(path="/test")
		public String test(RestRequest req) {
			var props = Map.<String,Object>of("key1", "value1", "key2", "value2");
			req.setSerializerSessionProperties(props);
			var map = req.getSerializerSessionPropertyMap();
			return map.get("key1") != null && map.get("key2") != null ? "ok" : "fail";
		}
	}

	@Test
	void a03_setSerializerSessionProperties_bulk() throws Exception {
		MockRestClient.create(C.class).plainText().build()
			.get("/test")
			.run()
			.assertContent("ok");
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class)
	public static class D {
		@RestPost(path="/test")
		public String test(RestRequest req) {
			var props = Map.<String,Object>of("key1", "value1", "key2", "value2");
			req.setParserSessionProperties(props);
			var map = req.getParserSessionPropertyMap();
			return map.get("key1") != null && map.get("key2") != null ? "ok" : "fail";
		}
	}

	@Test
	void a04_setParserSessionProperties_bulk() throws Exception {
		MockRestClient.create(D.class).plainText().build()
			.post("/test", "")
			.run()
			.assertContent("ok");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method chaining
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class)
	public static class E {
		@RestGet(path="/test")
		public String test(RestRequest req) {
			req.setSerializerSessionProperty("key1", "value1")
				.setSerializerSessionProperty("key2", "value2");
			var map = req.getSerializerSessionPropertyMap();
			return map.get("key1") != null && map.get("key2") != null ? "ok" : "fail";
		}
	}

	@Test
	void b01_methodChaining_serializer() throws Exception {
		MockRestClient.create(E.class).plainText().build()
			.get("/test")
			.run()
			.assertContent("ok");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Integration with HTTP options (query/header) and programmatic override
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class)
	public static class G {
		@RestGet(path="/test", allowedSerializerOptions="useWhitespace,customKey", allowedParserOptions="strict,customKey")
		public String test(RestRequest req) {
			req.setSerializerSessionProperty("customKey", "customValue");
			req.setParserSessionProperty("customKey", "customValue");
			
			var smap = req.getSerializerSessionPropertyMap();
			var pmap = req.getParserSessionPropertyMap();
			
			return "s:" + smap.get("customKey") + ",p:" + pmap.get("customKey");
		}
	}

	@Test
	void c01_integration_programmatic_override() throws Exception {
		MockRestClient.create(G.class).plainText().build()
			.get("/test")
			.run()
			.assertContent("s:customValue,p:customValue");
	}
}
