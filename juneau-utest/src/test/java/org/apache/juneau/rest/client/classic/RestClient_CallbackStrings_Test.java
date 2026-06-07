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
package org.apache.juneau.rest.client.classic;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

class RestClient_CallbackStrings_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet(path="/*")
		public Json5Map get(org.apache.juneau.rest.server.RestRequest req) throws Exception {
			return Json5Map.of("method","GET","headers",getFooHeaders(req),"content",req.getContent().asString());
		}
		@RestPut(path="/*")
		public Json5Map put(org.apache.juneau.rest.server.RestRequest req) throws Exception {
			return Json5Map.of("method","PUT","headers",getFooHeaders(req),"content",req.getContent().asString());
		}
		private static Map<String,Object> getFooHeaders(org.apache.juneau.rest.server.RestRequest req) {
			var m = new TreeMap<String,Object>();
			req.getHeaders().stream().filter(x -> x.getName().startsWith("Foo-")).forEach(x -> m.put(x.getName(), x.getValue()));
			return m;
		}
	}

	@Test void a01_callback() throws Exception {
		var x = MockRestClient.build(A.class);
		x.callback("GET /testCallback").run().assertContent("{method:'GET',headers:{},content:''}");
		x.callback("GET /testCallback some sample content").run().assertContent("{method:'GET',headers:{},content:'some sample content'}");
		x.callback("GET {Foo-X:123,Foo-Y:'abc'} /testCallback").run().assertContent("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:''}");
		x.callback("GET  { Foo-X : 123,Foo-Y : 'abc' } /testCallback").run().assertContent("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:''}");
		x.callback("GET {Foo-X:123,Foo-Y:'abc'} /testCallback   some sample content  ").run().assertContent("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:'some sample content'}");
		x.callback("PUT {Foo-X:123,Foo-Y:'abc'} /testCallback   some sample content  ").run().assertContent("{method:'PUT',headers:{'Foo-X':'123','Foo-Y':'abc'},content:'some sample content'}");
	}

	@Test void a02_callback_invalidStrings() {
		var x = MockRestClient.build(A.class);
		for (var s : l("","GET","GET ","GET {","GET {xxx} /foo",null)) {
			assertThrowsWithMessage(Exception.class, "Invalid format for call string", ()->x.callback(s).run().getContent().asString());
		}
	}
}