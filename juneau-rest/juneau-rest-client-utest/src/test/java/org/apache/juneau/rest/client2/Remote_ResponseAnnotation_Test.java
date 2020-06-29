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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Remote_ResponseAnnotation_Test {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod
		public String get(RestResponse res) {
			res.setHeader("X", "x");
			res.setStatus(201);
			return "foo";
		}
	}

	@Response
	public interface A1 {
		@ResponseBody Reader getBody();
		@ResponseHeader("X") String getHeader();
		@ResponseStatus int getStatus();
	}

	@Remote
	public static interface A2 {
		@RemoteMethod A1 get();
	}

	@Test
	public void a01_basic() throws Exception {
		A1 x = remote(A.class, A2.class).get();
		assertEquals("foo", IOUtils.read(x.getBody()));
		assertEquals("x", x.getHeader());
		assertEquals(201, x.getStatus());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static <T> T remote(Class<?> rest, Class<T> t) {
		return MockRestClient.build(rest).getRemote(t);
	}
}
