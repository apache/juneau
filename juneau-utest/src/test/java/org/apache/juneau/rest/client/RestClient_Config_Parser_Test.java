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

import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Config_Parser_Test {

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public Reader echoBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getContent().getReader();
		}
	}

	public static class A2 {
		public int f;
	}

	@Test
	public void a02_parser_strict() throws Exception {
		assertThrown(()->MockRestClient.create(A.class).json().strict().build().post("/echoBody",reader("{f:1}")).run().getContent().as(A2.class)).asMessages().isAny(contains("Unquoted attribute detected."));
	}

	public static class A3 {
		public String f;
	}

	@Test
	public void a03_parser_trimStringsOnRead() throws Exception {
		A3 x = client().trimStringsOnRead().build().post("/echoBody",reader("{f:' 1 '}")).run().getContent().as(A3.class);
		assertEquals("1",x.f);
	}


	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5();
	}
}
