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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.http.entity.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestCallException_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
		@Override
		public String toString() {
			return Json5.of(this);
		}
	}

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public InputStream echo(InputStream is) {
			return is;
		}
	}

	@Test
	public void a01_basic() throws Exception {
		try {
			client().build().get().run();
			fail();
		} catch (RestCallException e) {
			assertInteger(e.getResponse().getStatusCode()).is(404);
			assertNull(e.getCause());
		}

		try {
			client().build().post("/echo",new StringEntity("{f:")).run().getContent().as(ABean.class);
			fail();
		} catch (RestCallException e) {
			assertThrowable(e.getCause(ParseException.class)).asMessage().isContains("Could not find '}'");
		}

		RestCallException e = new RestCallException(null, null, null);
		assertNotNull(e.getThrown());
		assertFalse(e.getThrown().isPresent());
		assertEquals(0, e.getResponseCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5().noTrace();
	}
}
