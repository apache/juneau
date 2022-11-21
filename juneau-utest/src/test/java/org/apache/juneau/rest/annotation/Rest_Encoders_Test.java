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
package org.apache.juneau.rest.annotation;

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_Encoders_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Setup classes
	//------------------------------------------------------------------------------------------------------------------

	public static class MyEncoder extends GzipEncoder {
		@Override /* ConfigEncoder */
		public String[] getCodings() {
			return new String[]{"mycoding"};
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test with no compression enabled.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestOp
		public String put(@Content String in) {
			return in;
		}
	}

	@Test
	public void a01_noCompression() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.put("/", "foo").run().assertContent("foo");
		a.put("/", "foo").header(ContentEncoding.of("")).run().assertContent("foo");
		a.put("/", "foo").header(ContentEncoding.of("identity")).run().assertContent("foo");
		a.put("?noTrace=true", StringUtils.compress("foo")).header(ContentEncoding.of("mycoding")).run()
			.assertStatus(415)
			.assertContent().isContains(
				"Unsupported encoding in request header 'Content-Encoding': 'mycoding'",
				"Supported codings: ['identity']"
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test with compression enabled.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(encoders=MyEncoder.class)
	public static class B {
		@RestOp
		public String put(@Content String in) {
			return in;
		}
	}

	@Test
	public void b01_withCompression() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.put("/", "foo")
			.run()
			.assertContent("foo");
		b.put("/", "foo")
			.header(ContentEncoding.of(""))
			.run()
			.assertContent("foo");
		b.put("/", "foo")
			.header(ContentEncoding.of("identity"))
			.run()
			.assertContent("foo");
//		b.put("/", StringUtils.compress("foo"))
//			.contentEncoding("mycoding")
//			.run()
//			.assertBody().is("foo");
	}
}
