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
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.http.HttpEntities.*;
import static org.apache.juneau.http.HttpResources.*;
import static org.apache.juneau.http.header.ContentType.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.client.utils.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.testutils.*;
import org.junit.*;
import org.apache.juneau.http.header.ContentType;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_BasicCalls_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
		@Override
		public String toString() {
			return SimpleJson.DEFAULT.toString(this);
		}
	}

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRestObject implements BasicUniversalConfig {
		@RestOp(path="/bean")
		public ABean getBean() {
			return bean;
		}
		@RestOp(path="/bean")
		public ABean postBean(@Body ABean b) {
			return b;
		}
		@RestOp(path="/bean")
		public ABean putBean(@Body ABean b) {
			return b;
		}
		@RestOp(path="/bean")
		public ABean patchBean(@Body ABean b) {
			return b;
		}
		@RestOp(path="/bean")
		public ABean deleteBean() {
			return bean;
		}
		@RestOp(path="/bean")
		public ABean optionsBean() {
			return bean;
		}
		@RestOp(path="/bean")
		public ABean headBean() {
			return bean;
		}
		@RestOp(path="/checkHeader")
		public String[] postHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().getAll(req.getHeader("Check").orElse(null)).stream().map(x -> x.getValue()).toArray(String[]::new);
		}
		@RestOp(path="/",method="*")
		public Reader echoMethod(@Method String method) {
			return reader(method);
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient x = client().build();
		x.get().run().assertContent().is("GET");
		x.get("/").run().assertContent().is("GET");
		x.get("").run().assertContent().is("GET");
		x.put("/",null).run().assertContent().is("PUT");
		x.post("/",null).run().assertContent().is("POST");
		x.delete("/").run().assertContent().is("DELETE");
		x.formPost("/").run().assertContent().is("POST");
	}

	@Test
	public void a02_get() throws Exception {
		client().build().get("/bean").run().assertContent().is("{f:1}");

		assertThrown(()->client().build().get("/bean").content(bean).run()).asMessage().isContains("Method does not support content entity.");
	}

	@Test
	public void a03_get_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.get(url).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a04_put() throws Exception {
		client().build().put("/bean",bean).run().assertContent().is("{f:1}");
		client().build().put("/bean").content(bean).run().assertContent().is("{f:1}");
		client().build().put("/bean",TestSupplier.of(bean)).run().assertContent().is("{f:1}");
		client().build().put("/bean").content(TestSupplier.of(bean)).run().assertContent().is("{f:1}");
	}

	@Test
	public void a05_put_bodyString() throws Exception {
		client().build().put("/bean","{f:1}",APPLICATION_JSON).run().assertContent().is("{f:1}");
		client().build().put("/bean").contentString("{f:1}").simpleJson().run().assertContent().is("{f:1}");
		client().build().put("/bean").contentString("").simpleJson().run().assertContent().is("{f:0}");
		client().build().put("/bean").contentString(null).simpleJson().run().assertContent().is("null");
 	}

	@Test
	public void a06_put_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.put(url,bean).run().assertContent().is("{f:1}");
			x.put(url,"{f:1}",APPLICATION_JSON).run().assertContent().is("{f:1}");
			x.put(url).content(bean).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a07_put_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = list(
			reader("{f:1}"),
			inputStream("{f:1}"),
			stringResource("{f:1}").build(),
			bean,
			stringEntity("{f:1}").build(),
			parts("f",1)
		);
		for (Object body : bodies) {
			client().headers(body instanceof PartList ? APPLICATION_FORM_URLENCODED : APPLICATION_JSON).build().put("/bean",body).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a08_post() throws Exception {
		client().build().post("/bean",bean).run().assertContent().is("{f:1}");
		client().build().post("/bean").content(bean).run().assertContent().is("{f:1}");
	}

	@Test
	public void a09_post_stringBody() throws Exception {
		client().build().post("/bean","{f:1}",APPLICATION_JSON).run().assertContent().is("{f:1}");
	}

	@Test
	public void a10_post_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.post(url,bean).run().assertContent().is("{f:1}");
			x.post(url,"{f:1}",APPLICATION_JSON).run().assertContent().is("{f:1}");
			x.post(url).content(bean).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a11_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = list(
			reader("{f:1}"),
			inputStream("{f:1}"),
			stringResource("{f:1}").build(),
			bean,
			stringEntity("{f:1}").build(),
			parts("f",1)
		);
		for (Object body : bodies) {
			client().headers(body instanceof PartList ? APPLICATION_FORM_URLENCODED : APPLICATION_JSON).build().post("/bean",body).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a12_delete() throws Exception {
		client().build().delete("/bean").run().assertContent().is("{f:1}");
	}

	@Test
	public void a13_delete_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.delete(url).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a14_options() throws Exception {
		client().build().options("/bean").run().assertContent().is("{f:1}");
	}

	@Test
	public void a15_options_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.options(url).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a16_head() throws Exception {
		client().build().head("/bean").run().assertContent().is("");
	}

	@Test
	public void a17_head_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.head(url).run().assertContent().is("");
		}
	}

	@Test
	public void a18_formPost() throws Exception {
		client().build().formPost("/bean",bean).accept("application/json+simple").run().assertContent().is("{f:1}");

		client().build().formPost("/bean",bean).content(bean).accept("application/json+simple").run().assertContent().is("{f:1}");
		client().build().post("/bean").urlEnc().formDataBean(bean).content(bean).accept("application/json+simple").run().assertContent().is("{f:1}");
		client().build().post("/bean").urlEnc().content(bean).formDataBean(bean).accept("application/json+simple").run().assertContent().is("{f:1}");
	}

	@Test
	public void a19_formPost_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.formPost(url,bean).accept("application/json+simple").run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a20_formPost_exhaustiveBodyTypes() throws Exception {
		Supplier<Object>
			s1 = () -> reader("f=1"),
			s2 = () -> inputStream("f=1");
		List<Object> bodies = list(
			/*[ 0]*/ bean,
			/*[ 1]*/ parts("f","1"),
			/*[ 2]*/ new NameValuePair[]{part("f","1")},
			/*[ 3]*/ stringEntity("f=1", ContentType.APPLICATION_FORM_URLENCODED).build(),
			/*[ 4]*/ stringEntity("f=1", null).build(),
			/*[ 5]*/ part("f","1"),
			/*[ 6]*/ stringResource("f=1").build(),
			/*[ 7]*/ stringResource("f=1").build(),
			/*[ 8]*/ stringResource("f=1").contentType(APPLICATION_FORM_URLENCODED).build(),
			/*[ 9]*/ stringResource("f=1").contentType(APPLICATION_FORM_URLENCODED).build(),
			/*[14]*/ s1,
			/*[15]*/ s2
		);
		for (int i = 0; i < bodies.size(); i++) {
			client().header("Check","Content-Type").accept("application/json+simple").build().formPost("/checkHeader",bodies.get(i)).run().assertContent().setMsg("Body {0} failed",i).asString().isMatches("['application/x-www-form-urlencoded*']");
			client().build().formPost("/bean",bodies.get(i)).accept("application/json+simple").run().assertContent().setMsg("Body {0} failed","#"+i).is("{f:1}");
		}
	}

	@Test
	public void a21_formPostPairs() throws Exception {
		client().build().formPostPairs("/bean",new StringBuilder("f"),new StringBuilder("1")).accept("application/json+simple").run().assertContent().is("{f:1}");
	}

	@Test
	public void a22_patch() throws Exception {
		client().build().patch("/bean",bean).run().assertContent().is("{f:1}");
		client().build().patch("/bean").content(bean).run().assertContent().is("{f:1}");
	}

	@Test
	public void a23_patch_fromString() throws Exception {
		client().build().patch("/bean","{f:1}",APPLICATION_JSON).run().assertContent().is("{f:1}");
	}

	@Test
	public void a24_patch_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = list(
			reader("{f:1}"),
			inputStream("{f:1}"),
			stringResource("{f:1}").build(),
			bean,
			stringEntity("{f:1}").build(),
			parts("f",1)
		);
		RestClient x = client().build();
		for (Object body : bodies) {
			x.patch("/bean",body).header(body instanceof PartList ? APPLICATION_FORM_URLENCODED : APPLICATION_JSON).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a25_patch_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.patch(url,bean).accept("application/json+simple").run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a26_request_PATCH() throws Exception {
		client().build().request("patch","/bean",bean).run().assertContent().is("{f:1}");
		client().build().request("patch","/bean").content(bean).run().assertContent().is("{f:1}");
	}

	@Test
	public void a27_request_PATCH_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = list(
			reader("{f:1}"),
			inputStream("{f:1}"),
			stringResource("{f:1}").build(),
			bean,
			stringEntity("{f:1}").build(),
			parts("f",1)
		);
		RestClient x = client().build();
		for (Object body : bodies) {
			x.request("patch","/bean",body).header(body instanceof PartList ? APPLICATION_FORM_URLENCODED : APPLICATION_JSON).run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a28_request_PATCH_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.request("patch",url,bean).accept("application/json+simple").run().assertContent().is("{f:1}");
		}
	}

	@Test
	public void a29_request_GET() throws Exception {
		client().build().request("get","/bean").run().assertContent().is("{f:1}");
	}

	@Test
	public void a30_request_GET_exhaustiveUrls() throws Exception {
		List<Object> urls = list(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.request("get",url).accept("application/json+simple").run().assertContent().is("{f:1}");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static NameValuePair part(String name, Object val) {
		return basicPart(name, val);
	}

	private static PartList parts(Object...pairs) {
		return partList(pairs);
	}

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
