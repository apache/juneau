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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.RestRequest;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Response_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
	}

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRest {
		@RestMethod(path="/bean")
		public ABean getBean() {
			return bean;
		}
	}

	public static class A1 extends MockRestClient {
		public A1(PropertyStore ps) {
			super(ps);
		}
		@Override
		protected MockRestResponse createResponse(RestRequest request, HttpResponse httpResponse, Parser parser) throws RestCallException {
			return new MockRestResponse(this, request, null, parser);
		}
	}

	@Test
	public void a01_getStatusLine() throws RestCallException {
		assertEquals(200,client().build().get("/bean").run().getStatusLine().getStatusCode());
		assertThrown(()->client().build(A1.class).get("/bean").run()).contains("caused response code '0, null'");
		assertEquals(0,client().ignoreErrors().build(A1.class).get("/bean").run().getStatusLine().getStatusCode());
	}

	@Test
	public void a02_getStatusLine_Mutable() throws RestCallException {
		Mutable<StatusLine> m = Mutable.create();
		client().build().get("/bean").run().getStatusLine(m);
		assertEquals(200,m.get().getStatusCode());
	}

	@Test
	public void a03_getStatusCode() throws RestCallException {
		assertEquals(200,client().build().get("/bean").run().getStatusCode());
	}

	@Test
	public void a04_getStatusCode_Mutable() throws RestCallException {
		Mutable<Integer> m = Mutable.create();
		client().build().get("/bean").run().getStatusCode(m);
		assertEquals(200,m.get().intValue());
	}

	@Test
	public void a05_getReasonPhrase() throws RestCallException {
		assertNull(client().build().get("/bean").run().getReasonPhrase());
	}

	@Test
	public void a06_getReasonPhrase_Mutable() throws RestCallException {
		Mutable<String> m = Mutable.create();
		client().build().get("/bean").run().getReasonPhrase(m);
		assertNull(m.get());
	}


	//------------------------------------------------------------------------------------------------------------------
	// Response headers.
	//------------------------------------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
