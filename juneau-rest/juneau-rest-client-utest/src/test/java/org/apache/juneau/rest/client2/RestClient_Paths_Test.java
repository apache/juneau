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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

public class RestClient_Paths_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
	}

	public static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRest {
		@RestMethod(path="/echo/*")
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
		client().build().get("/echo/{x}").path("x",new A1().init()).run().assertBody().contains("HTTP GET /echo/x=1");
		client().build().get("/echo/*").path("/*",new A1().init()).run().assertBody().contains("HTTP GET /echo/x=1");
		assertThrown(()->{client().build().get("/echo/{x}").path("y","foo");}).is("Path variable {y} was not found in path.");
	}

	@Test
	public void a02_path_NameValuePair() throws Exception {
		client().build().get("/echo/{x}").path(pair("x","foo")).run().assertBody().contains("HTTP GET /echo/foo");
	}

	@Test
	public void a03_paths_Object() throws Exception {
		client().build().get("/echo/{x}").paths(pair("x","foo")).run().assertBody().contains("HTTP GET /echo/foo");
		client().build().get("/echo/{x}").paths(pairs("x","foo")).run().assertBody().contains("HTTP GET /echo/foo");
		client().build().get("/echo/{x}").paths(OMap.of("x","foo")).run().assertBody().contains("HTTP GET /echo/foo");
		client().build().get("/echo/{x}").paths((Object)new NameValuePair[]{pair("x","foo")}).run().assertBody().contains("HTTP GET /echo/foo");
		client().build().get("/echo/{x}").paths(new A1().init()).run().assertBody().contains("HTTP GET /echo/1");
		assertThrown(()->{client().build().get("/echo/{x}").paths("x");}).is("Invalid type passed to paths(): java.lang.String");
		client().build().get("/echo/{x}").paths((Object)null).run().assertBody().contains("HTTP GET /echo/%7Bx%7D");
	}

	@Test
	public void a04_pathPairs_Objects() throws Exception {
		client().build().get("/echo/{x}").pathPairs("x",1).run().assertBody().contains("HTTP GET /echo/1");
		assertThrown(()->{client().build().get("/echo/{x}").pathPairs("x");}).is("Odd number of parameters passed into pathPairs()");
	}

	@Test
	public void a05_path_String_Object_Schema() throws Exception {
		String[] a = new String[]{"foo","bar"};
		client().build().get("/echo/{x}").path("x",a,T_ARRAY_PIPES).run().assertBody().contains("HTTP GET /echo/foo%7Cbar");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static NameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}

	private static NameValuePairs pairs(Object...pairs) {
		return NameValuePairs.of(pairs);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
