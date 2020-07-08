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
import java.net.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.client.utils.*;
import org.apache.http.entity.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.testutils.*;
import org.junit.*;

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
	public static class A extends BasicRest {
		@RestMethod(path="/bean")
		public ABean getBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public ABean postBean(@Body ABean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public ABean putBean(@Body ABean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public ABean patchBean(@Body ABean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public ABean deleteBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public ABean optionsBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public ABean headBean() {
			return bean;
		}
		@RestMethod(path="/checkHeader")
		public String[] postHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().get(req.getHeader("Check"));
		}
		@RestMethod(path="/",name="*")
		public Reader echoMethod(@Method String method) {
			return new StringReader(method);
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient x = client().build();
		x.get().run().assertBody().is("GET");
		x.get("/").run().assertBody().is("GET");
		x.get("").run().assertBody().is("GET");
		x.put("/",null).run().assertBody().is("PUT");
		x.post("/",null).run().assertBody().is("POST");
		x.delete("/").run().assertBody().is("DELETE");
		x.formPost("/").run().assertBody().is("POST");
	}

	@Test
	public void a02_get() throws Exception {
		client().build().get("/bean").run().assertBody().is("{f:1}");

		assertThrown(()->client().build().get("/bean").body(bean).run()).contains("Method does not support content entity.");
	}

	@Test
	public void a03_get_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.get(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a04_put() throws Exception {
		client().build().put("/bean",bean).run().assertBody().is("{f:1}");
		client().build().put("/bean").body(bean).run().assertBody().is("{f:1}");
		client().build().put("/bean",TestSupplier.of(bean)).run().assertBody().is("{f:1}");
		client().build().put("/bean").body(TestSupplier.of(bean)).run().assertBody().is("{f:1}");
	}

	@Test
	public void a05_put_bodyString() throws Exception {
		client().build().put("/bean","{f:1}","application/json").run().assertBody().is("{f:1}");
		client().build().put("/bean").bodyString("{f:1}").simpleJson().run().assertBody().is("{f:1}");
		client().build().put("/bean").bodyString("").simpleJson().run().assertBody().is("{f:0}");
		client().build().put("/bean").bodyString(null).simpleJson().run().assertBody().is("null");
 	}

	@Test
	public void a06_put_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.put(url,bean).run().assertBody().is("{f:1}");
			x.put(url,"{f:1}","application/json").run().assertBody().is("{f:1}");
			x.put(url).body(bean).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a07_put_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = AList.<Object>of(
			new StringReader("{f:1}"),
			new ByteArrayInputStream("{f:1}".getBytes()),
			ReaderResource.create().content("{f:1}"),
			StreamResource.create().content("{f:1}"),
			bean,
			new StringEntity("{f:1}"),
			pairs("f",1)
		);
		for (Object body : bodies) {
			client().contentType(body instanceof NameValuePairSupplier ? "application/x-www-form-urlencoded" : "application/json").build().put("/bean",body).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a08_post() throws Exception {
		client().build().post("/bean",bean).run().assertBody().is("{f:1}");
		client().build().post("/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a09_post_stringBody() throws Exception {
		client().build().post("/bean","{f:1}","application/json").run().assertBody().is("{f:1}");
	}

	@Test
	public void a10_post_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.post(url,bean).run().assertBody().is("{f:1}");
			x.post(url,"{f:1}","application/json").run().assertBody().is("{f:1}");
			x.post(url).body(bean).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a11_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = AList.<Object>of(
			new StringReader("{f:1}"),
			new ByteArrayInputStream("{f:1}".getBytes()),
			ReaderResource.create().content("{f:1}"),
			StreamResource.create().content("{f:1}"),
			bean,
			new StringEntity("{f:1}"),
			pairs("f",1)
		);
		for (Object body : bodies) {
			client().contentType(body instanceof NameValuePairSupplier ? "application/x-www-form-urlencoded" : "application/json").build().post("/bean",body).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a12_delete() throws Exception {
		client().build().delete("/bean").run().assertBody().is("{f:1}");
	}

	@Test
	public void a13_delete_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.delete(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a14_options() throws Exception {
		client().build().options("/bean").run().assertBody().is("{f:1}");
	}

	@Test
	public void a15_options_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.options(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a16_head() throws Exception {
		client().build().head("/bean").run().assertBody().is("");
	}

	@Test
	public void a17_head_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.head(url).run().assertBody().is("");
		}
	}

	@Test
	public void a18_formPost() throws Exception {
		client().build().formPost("/bean",bean).accept("application/json+simple").run().assertBody().is("{f:1}");

		client().build().formPost("/bean",bean).body(bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		client().build().post("/bean").urlEnc().formDatas(bean).body(bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		client().build().post("/bean").urlEnc().body(bean).formDatas(bean).accept("application/json+simple").run().assertBody().is("{f:1}");
	}

	@Test
	public void a19_formPost_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.formPost(url,bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a20_formPost_exhaustiveBodyTypes() throws Exception {
		Supplier<Object>
			s1 = () -> new StringReader("f=1"),
			s2 = () -> new ByteArrayInputStream("f=1".getBytes());
		List<Object> bodies = AList.of(
			/*[ 0]*/ bean,
			/*[ 1]*/ pairs("f","1"),
			/*[ 2]*/ new NameValuePair[]{pair("f","1")},
			/*[ 3]*/ new StringEntity("f=1",org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED),
			/*[ 4]*/ new StringEntity("f=1",(org.apache.http.entity.ContentType)null),
			/*[ 5]*/ pair("f","1"),
			/*[ 6]*/ ReaderResource.create().content("f=1"),
			/*[ 7]*/ ReaderResource.create().content("f=1"),
			/*[ 8]*/ ReaderResource.create().content("f=1").contentType("application/x-www-form-urlencoded"),
			/*[ 9]*/ ReaderResource.create().content("f=1").contentType("application/x-www-form-urlencoded"),
			/*[10]*/ StreamResource.create().content("f=1"),
			/*[11]*/ StreamResource.create().content("f=1"),
			/*[12]*/ StreamResource.create().content("f=1").contentType("application/x-www-form-urlencoded"),
			/*[13]*/ StreamResource.create().content("f=1").contentType("application/x-www-form-urlencoded"),
			/*[14]*/ s1,
			/*[15]*/ s2
		);
		for (int i = 0; i < bodies.size(); i++) {
			client().header("Check","Content-Type").accept("application/json+simple").build().formPost("/checkHeader",bodies.get(i)).run().assertBody().msg("Body {0} failed",i).matchesSimple("['application/x-www-form-urlencoded*']");
			client().build().formPost("/bean",bodies.get(i)).accept("application/json+simple").run().assertBody().msg("Body {0} failed","#"+i).is("{f:1}");
		}
	}

	@Test
	public void a21_formPostPairs() throws Exception {
		client().build().formPostPairs("/bean",new StringBuilder("f"),new StringBuilder("1")).accept("application/json+simple").run().assertBody().is("{f:1}");
	}

	@Test
	public void a22_patch() throws Exception {
		client().build().patch("/bean",bean).run().assertBody().is("{f:1}");
		client().build().patch("/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a23_patch_fromString() throws Exception {
		client().build().patch("/bean","{f:1}","application/json").run().assertBody().is("{f:1}");
	}

	@Test
	public void a24_patch_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = AList.<Object>of(
			new StringReader("{f:1}"),
			new ByteArrayInputStream("{f:1}".getBytes()),
			ReaderResource.create().content("{f:1}"),
			StreamResource.create().content("{f:1}"),
			bean,
			new StringEntity("{f:1}"),
			pairs("f",1)
		);
		RestClient x = client().build();
		for (Object body : bodies) {
			x.patch("/bean",body).contentType(body instanceof NameValuePairSupplier ? "application/x-www-form-urlencoded" : "application/json").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a25_patch_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.patch(url,bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a26_request_PATCH() throws Exception {
		client().build().request(HttpMethod.PATCH,"/bean",bean).run().assertBody().is("{f:1}");
		client().build().request(HttpMethod.PATCH,"/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a27_request_PATCH_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = AList.<Object>of(
			new StringReader("{f:1}"),
			new ByteArrayInputStream("{f:1}".getBytes()),
			ReaderResource.create().content("{f:1}"),
			StreamResource.create().content("{f:1}"),
			bean,
			new StringEntity("{f:1}"),
			pairs("f",1)
		);
		RestClient x = client().build();
		for (Object body : bodies) {
			x.request(HttpMethod.PATCH,"/bean",body).contentType(body instanceof NameValuePairSupplier ? "application/x-www-form-urlencoded" : "application/json").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a28_request_PATCH_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.request(HttpMethod.PATCH,url,bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a29_request_GET() throws Exception {
		client().build().request(HttpMethod.GET,"/bean",null).run().assertBody().is("{f:1}");
		client().build().request(HttpMethod.GET,"/bean").run().assertBody().is("{f:1}");
	}

	@Test
	public void a30_request_GET_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		RestClient x = client().build();
		for (Object url : urls) {
			x.request(HttpMethod.GET,url).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static NameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}

	private static NameValuePairSupplier pairs(Object...pairs) {
		return NameValuePairSupplier.ofPairs(pairs);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}
}
