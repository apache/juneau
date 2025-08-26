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
package org.apache.juneau.rest.client;

import static org.apache.juneau.TestUtils.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class RestClient_CallbackStrings_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet(path="/*")
		public JsonMap get(RestRequest req) throws Exception {
			return JsonMap.of("method","GET","headers",getFooHeaders(req),"content",req.getContent().asString());
		}
		@RestPut(path="/*")
		public JsonMap put(RestRequest req) throws Exception {
			return JsonMap.of("method","PUT","headers",getFooHeaders(req),"content",req.getContent().asString());
		}
		private Map<String,Object> getFooHeaders(RestRequest req) {
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
		for (String s : list("","GET","GET ","GET {","GET {xxx} /foo",null)) {
			assertThrowsWithMessage(Exception.class, "Invalid format for call string", ()->x.callback(s).run().getContent().asString());
		}
	}
}
