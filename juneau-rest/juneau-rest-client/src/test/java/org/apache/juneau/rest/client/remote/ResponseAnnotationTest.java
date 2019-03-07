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
package org.apache.juneau.rest.client.remote;

import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Request annotation.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResponseAnnotationTest {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}
	}

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod
		public String get(RestResponse res) {
			res.setHeader("X", "x");
			res.setStatus(201);
			return "foo";
		}
	}

	@Response
	public interface AResponse {

		@ResponseBody
		Reader getBody();

		@ResponseHeader("X")
		String getHeader();

		@ResponseStatus
		int getStatus();
	}

	@RemoteResource
	public static interface AR {
		@RemoteMethod AResponse get();
	}

	private static AR ar = MockRemoteResource.build(AR.class, A.class, null);

	@Test
	public void a01_basic() throws Exception {
		AResponse r = ar.get();
		assertEquals("foo", IOUtils.read(r.getBody()));
		assertEquals("x", r.getHeader());
		assertEquals(201, r.getStatus());
	}
}
