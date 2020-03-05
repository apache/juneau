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

import static org.apache.juneau.internal.IOUtils.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.client2.ext.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.test.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests client-side form posts.
 */
public class FormDataTest extends RestTestcase {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	public static class A {
		@RestMethod
		public Reader post(RestRequest req) throws IOException {
			return new StringReader("Content-Type=["+req.getContentType()+"], contents=["+read(req.getReader())+"]");
		}
	}
	static RestClient a = MockRestClient.build(A.class, SimpleJson.DEFAULT);

	@Test
	public void a01_formDataMethod() throws Exception {
		String r = a.post("")
			.formData("foo", 123)
			.formData("bar", "baz")
			.run()
			.getBody().asString();
		assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=123&bar=baz]", r);
	}

	@Test
	public void a02_nameValuePairs() throws Exception {
		String r = a.post("", new NameValuePairs().append("foo",123).append("bar","baz")).run().getBody().asString();
		assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=123&bar=baz]", r);
	}

	@Test
	public void a03_plainTextParams() throws Exception {
		RestClient c = MockRestClient.create(A.class, UrlEncoding.DEFAULT).paramFormatPlain().build();
		try {
			String r;

			Map<String,Object> m = new AMap<String,Object>()
				.append("foo", "foo")
				.append("'foo'", "'foo'")
				.append("(foo)", "(foo)")
				.append("@(foo)", "@(foo)");

			r = c.post("", m).run().getBody().asString();
			assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=foo&'foo'='foo'&(foo)=(foo)&@(foo)=@(foo)]", r);

			List<String> l = new AList<String>().appendAll("foo", "'foo'", "(foo)", "@(foo)");
			r = c.post("", l).run().getBody().asString();
			assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[0=foo&1='foo'&2=(foo)&3=@(foo)]", r);

			NameValuePairs nvp = new NameValuePairs()
				.append("foo", "foo")
				.append("'foo'", "'foo'")
				.append("(foo)", "(foo)")
				.append("@(foo)", "@(foo)");
			r = c.post("", nvp).run().getBody().asString();
			assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=foo&%27foo%27=%27foo%27&%28foo%29=%28foo%29&%40%28foo%29=%40%28foo%29]", r);

			r = c.post("")
				.formData("foo", "foo")
				.formData("'foo'", "'foo'")
				.formData("(foo)", "(foo)")
				.formData("@(foo)", "@(foo)")
				.run()
				.getBody().asString();
			assertEquals("Content-Type=[application/x-www-form-urlencoded], contents=[foo=foo&%27foo%27=%27foo%27&%28foo%29=%28foo%29&%40%28foo%29=%40%28foo%29]", r);
		} finally {
			c.close();
		}
	}
}
