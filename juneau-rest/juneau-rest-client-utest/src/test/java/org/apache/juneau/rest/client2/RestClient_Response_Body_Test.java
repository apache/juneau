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
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Response_Body_Test {

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
		@RestMethod
		public InputStream postEcho(InputStream is) {
			return is;
		}
		@RestMethod
		public ABean getBean() {
			return bean;
		}
	}

	@Test
	public void a01_basic() throws Exception {
		client().build().post("/echo",bean).run().assertBody(ABean.class).json().is("{f:1}");
	}

	@Test
	public void a02_overrideParser() throws Exception {
		RestClient x = client().build();
		ABean b = x.post("/echo",bean).run().getBody().parser(JsonParser.DEFAULT).as(ABean.class);
		assertObject(b).json().is("{f:1}");
		assertThrown(()->x.post("/echo",bean).run().getBody().parser(XmlParser.DEFAULT).as(ABean.class)).contains("ParseError at [row,col]:[1,1]");
		assertThrown(()->x.post("/echo",bean).run().getBody().parser(XmlParser.DEFAULT).assertObject(ABean.class)).contains("ParseError at [row,col]:[1,1]");
	}

	@Test
	public void a03_asInputStream() throws Exception {
		RestResponse x = client().build().get("/bean").run();
		InputStream is = x.getBody().asInputStream();
		assertStream(is).string().is("{f:1}");
		assertThrown(()->x.getBody().asInputStream()).contains("Response has already been consumed.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
