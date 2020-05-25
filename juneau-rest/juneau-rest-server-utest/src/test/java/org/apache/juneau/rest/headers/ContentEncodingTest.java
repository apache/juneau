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
package org.apache.juneau.rest.headers;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ContentEncodingTest {

	//=================================================================================================================
	// Setup classes
	//=================================================================================================================

	public static class MyEncoder extends GzipEncoder {
		@Override /* ConfigEncoder */
		public String[] getCodings() {
			return new String[]{"mycoding"};
		}
	}

	//=================================================================================================================
	// Test with no compression enabled.
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod
		public String put(@Body String in) {
			return in;
		}
	}
	static MockRest a = MockRest.build(A.class);

	@Test
	public void a01_noCompression() throws Exception {
		a.put("/", "foo").run().assertBody().is("foo");
		a.put("/", "foo").contentEncoding("").run().assertBody().is("foo");
		a.put("/", "foo").contentEncoding("identity").run().assertBody().is("foo");
	}
	@Test
	public void a02_noCompression_invalid() throws Exception {
		a.put("?noTrace=true", compress("foo")).contentEncoding("mycoding").run()
			.assertStatus().is(415)
			.assertBody().contains(
				"Unsupported encoding in request header 'Content-Encoding': 'mycoding'",
				"Supported codings: ['identity']"
			);
	}

	//=================================================================================================================
	// Test with compression enabled.
	//=================================================================================================================

	@Rest(encoders=MyEncoder.class)
	public static class B {
		@RestMethod(name=PUT, path="/")
		public String test1put(@Body String in) {
			return in;
		}
	}
	static MockRest b = MockRest.build(B.class);

	@Test
	public void b01_withCompression_identity() throws Exception {
		b.put("/", "foo")
			.run()
			.assertBody().is("foo");
		b.put("/", "foo")
			.contentEncoding("")
			.run()
			.assertBody().is("foo");
		b.put("/", "foo")
			.contentEncoding("identity")
			.run()
			.assertBody().is("foo");
	}
	@Test
	@Ignore
	public void b02_withCompression_gzip() throws Exception {
		b.put("/", compress("foo"))
			.contentEncoding("mycoding")
			.run()
			.assertBody().is("foo");
	}
}
