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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.http.HttpParts.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Paths_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestGet(path="/echo/*")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	public static class A1 {
		public int x;
		public A1 init() {
			x = 1;
			return this;
		}
		@Override
		public String toString() {
			return "xxx";
		}
	}

	@Test
	public void a01_path_String_Object() throws Exception {
		client().build().get("/echo/{x}").path("x",new A1().init()).run().assertBody().contains("GET /echo/x=1 HTTP/1.1");
		client().build().get("/echo/*").path("/*",new A1().init()).run().assertBody().contains("GET /echo/x=1 HTTP/1.1");
		assertThrown(()->client().build().get("/echo/{x}").path("y","foo")).is("Path variable 'y' was not found in path.");
	}

	@Test
	public void a02_path_NameValuePair() throws Exception {
		client().build().get("/echo/{x}").path(part("x","foo")).run().assertBody().contains("GET /echo/foo HTTP/1.1");
	}

	@Test
	public void a03_paths_Object() throws Exception {
		client().build().get("/echo/{x}").paths(part("x","foo")).run().assertBody().contains("GET /echo/foo HTTP/1.1");
		client().build().get("/echo/{x}").paths(AList.of(part("x","foo"))).run().assertBody().contains("GET /echo/foo HTTP/1.1");
		client().build().get("/echo/{x}").paths(pairs("x","foo")).run().assertBody().contains("GET /echo/foo HTTP/1.1");
		client().build().get("/echo/{x}").paths(OMap.of("x","foo")).run().assertBody().contains("GET /echo/foo HTTP/1.1");
		client().build().get("/echo/{x}").paths((Object)new NameValuePair[]{part("x","foo")}).run().assertBody().contains("GET /echo/foo HTTP/1.1");
		client().build().get("/echo/{x}").paths(new A1().init()).run().assertBody().contains("GET /echo/1 HTTP/1.1");
		assertThrown(()->client().build().get("/echo/{x}").paths("x")).is("Invalid type passed to paths(): java.lang.String");
		client().build().get("/echo/{x}").paths((Object)null).run().assertBody().contains("GET /echo/%7Bx%7D HTTP/1.1");
	}

	@Test
	public void a04_pathPairs_Objects() throws Exception {
		client().build().get("/echo/{x}").pathPairs("x",1).run().assertBody().contains("GET /echo/1 HTTP/1.1");
		assertThrown(()->client().build().get("/echo/{x}").pathPairs("x")).is("Odd number of parameters passed into pathPairs()");
	}

	@Test
	public void a05_path_String_Object_Schema() throws Exception {
		String[] a = new String[]{"foo","bar"};
		client().build().get("/echo/{x}").path("x",a,T_ARRAY_PIPES).run().assertBody().contains("GET /echo/foo%7Cbar HTTP/1.1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static BasicPart part(String name, Object val) {
		return basicPart(name, val);
	}

	private static PartList pairs(Object...pairs) {
		return partList(pairs);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
