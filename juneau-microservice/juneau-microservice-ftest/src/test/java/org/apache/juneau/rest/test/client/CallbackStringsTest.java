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
package org.apache.juneau.rest.test.client;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CallbackStringsTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod(name=GET,path="/*")
		public ObjectMap get(RestRequest req) throws Exception {
			return new ObjectMap().append("method","GET").append("headers", getFooHeaders(req)).append("content", req.getBody().asString());
		}
		@RestMethod(name=PUT,path="/*")
		public ObjectMap put(RestRequest req) throws Exception {
			return new ObjectMap().append("method","PUT").append("headers", getFooHeaders(req)).append("content", req.getBody().asString());
		}
		private Map<String,Object> getFooHeaders(RestRequest req) {
			Map<String,Object> m = new TreeMap<>();
			for (Map.Entry<String,String[]> e : req.getHeaders().entrySet())
				if (e.getKey().startsWith("Foo-"))
					m.put(e.getKey(), e.getValue()[0]);
			return m;
		}
	}
	static RestClient a = MockRestClient.build(A.class);

	@Test
	public void a01() throws Exception {
		String r;

		r = a.callback("GET /testCallback").run().getBody().asString();
		assertEquals("{method:'GET',headers:{},content:''}", r);

		r = a.callback("GET /testCallback some sample content").run().getBody().asString();
		assertEquals("{method:'GET',headers:{},content:'some sample content'}", r);

		r = a.callback("GET {Foo-X:123,Foo-Y:'abc'} /testCallback").run().getBody().asString();
		assertEquals("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:''}", r);

		r = a.callback("GET  { Foo-X : 123, Foo-Y : 'abc' } /testCallback").run().getBody().asString();
		assertEquals("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:''}", r);

		r = a.callback("GET {Foo-X:123,Foo-Y:'abc'} /testCallback   some sample content  ").run().getBody().asString();
		assertEquals("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:'some sample content'}", r);

		r = a.callback("PUT {Foo-X:123,Foo-Y:'abc'} /testCallback   some sample content  ").run().getBody().asString();
		assertEquals("{method:'PUT',headers:{'Foo-X':'123','Foo-Y':'abc'},content:'some sample content'}", r);
	}
}
