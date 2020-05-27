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
package org.apache.juneau.rest.helper;

import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.ReaderResource;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ReaderResourceTest {

	@Rest
	public static class A {

		@RestMethod
		public ReaderResource a01() throws Exception {
			return ReaderResource.create().contents("foo").build();
		}

		@RestMethod
		public ReaderResource a02() throws Exception {
			return ReaderResource.create().header("Foo", "Bar").build();
		}

		@RestMethod
		public ReaderResource a03() throws Exception {
			return ReaderResource.create().mediaType(MediaType.JSON).build();
		}

		@RestMethod
		public ReaderResource a04(RestRequest req) throws Exception {
			return ResolvingReaderResource.create().varResolver(req.getVarResolverSession()).contents("$RQ{foo}").build();
		}

		@RestMethod
		public ReaderResource a05() throws Exception {
			return ReaderResource.create().contents(new ByteArrayInputStream("foo".getBytes())).build();
		}

		@RestMethod
		public ReaderResource a06() throws Exception {
			return ReaderResource.create().contents(new StringReader("foo")).build();
		}

		@RestMethod
		public ReaderResource a07() throws Exception {
			return ReaderResource.create().contents(new StringBuilder("foo")).build();
		}
	}

	static MockRestClient a = MockRestClient.build(A.class);

	@Test
	public void a01_basic() throws Exception {
		a.get("/a01")
			.run()
			.assertBody().is("foo");
	}

	@Test
	public void a02_headers() throws Exception {
		a.get("/a02")
			.run()
			.assertHeader("Foo").is("Bar");
	}

	@Test
	public void a03_contentType() throws Exception {
		a.get("/a03")
			.run()
			.assertHeader("Content-Type").is("application/json");
	}

	@Test
	public void a04_withVars() throws Exception {
		a.get("/a04?foo=bar")
			.run()
			.assertBody().is("bar");
	}

	@Test
	public void a05_inputStream() throws Exception {
		a.get("/a05")
			.run()
			.assertBody().is("foo");
	}

	@Test
	public void a06_reader() throws Exception {
		a.get("/a06")
			.run()
			.assertBody().is("foo");
	}

	@Test
	public void a07_charSequence() throws Exception {
		a.get("/a07")
			.run()
			.assertBody().is("foo");
	}
}
