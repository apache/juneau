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

import static org.apache.juneau.httppart.HttpPartCollectionFormat.*;
import static org.apache.juneau.uon.ParamFormat.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Config_OpenApi_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public Reader echoBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getContent().getReader();
		}
		@RestGet
		public String[] checkHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().getAll(req.getHeaderParam("Check").orElse(null)).stream().map(x -> x.getValue()).toArray(String[]::new);
		}
		@RestGet
		public Reader checkQuery(org.apache.juneau.rest.RestRequest req) {
			return reader(req.getQueryParams().asQueryString());
		}
		@RestPost
		public Reader checkFormData(org.apache.juneau.rest.RestRequest req) {
			return reader(req.getFormParams().asQueryString());
		}
	}

	@Test
	public void a01_oapiFormat() throws Exception {
		client().oapiFormat(HttpPartFormat.UON).build().get("/checkQuery").queryData("Foo","bar baz").run().assertContent().asString().asUrlDecode().is("Foo='bar baz'");
	}

	@Test
	public void a02_oapiCollectionFormat() throws Exception {
		String[] a = {"bar","baz"};
		RestClient x = client().oapiCollectionFormat(PIPES).build();
		x.get("/checkQuery").queryData("Foo",a).run().assertContent().asString().asUrlDecode().is("Foo=bar|baz");
		x.post("/checkFormData").formData("Foo",a).run().assertContent().asString().asUrlDecode().is("Foo=bar|baz");
		x.get("/checkHeader").header("Check","Foo").header("Foo",a).accept("text/json5").run().assertContent("['bar|baz']");
	}

	@Test
	public void a03_paramFormat() throws Exception {
		 JsonMap m = JsonMap.of(
			"foo","bar",
			"baz",new String[]{"qux","true","123"}
		);
		client().urlEnc().paramFormat(PLAINTEXT).build().post("/echoBody",m).run().assertContent("foo=bar&baz=qux,true,123");
		client().urlEnc().paramFormatPlain().build().post("/echoBody",m).run().assertContent("foo=bar&baz=qux,true,123");
		client().urlEnc().paramFormat(UON).build().post("/echoBody",m).run().assertContent("foo=bar&baz=@(qux,'true','123')");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}
