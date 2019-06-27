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
package org.apache.jueau.rest.helper;

import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.ReaderResource;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the {@link BasicRestInfoProvider} class.
 */
@SuppressWarnings("javadoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReaderResourceTest {

	@RestResource
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

	static MockRest a = MockRest.build(A.class);

	@Test
	public void a01_basic() throws Exception {
		assertEquals("foo", a.get("/a01").execute().getBodyAsString());
	}

	@Test
	public void a02_headers() throws Exception {
		assertEquals("Bar", a.get("/a02").execute().getHeader("Foo"));
	}

	@Test
	public void a03_contentType() throws Exception {
		assertEquals("application/json", a.get("/a03").execute().getHeader("Content-Type"));
	}

	@Test
	public void a04_withVars() throws Exception {
		assertEquals("bar", a.get("/a04?foo=bar").execute().getBodyAsString());
	}

	@Test
	public void a05_inputStream() throws Exception {
		assertEquals("foo", a.get("/a05").execute().getBodyAsString());
	}

	@Test
	public void a06_reader() throws Exception {
		assertEquals("foo", a.get("/a06").execute().getBodyAsString());
	}

	@Test
	public void a07_charSequence() throws Exception {
		assertEquals("foo", a.get("/a07").execute().getBodyAsString());
	}
}
