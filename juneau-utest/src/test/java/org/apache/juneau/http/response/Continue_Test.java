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
package org.apache.juneau.http.response;

import static org.apache.juneau.http.HttpResponses.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class Continue_Test extends SimpleTestBase {

	@Rest
	public static class A {
		@RestGet public Continue a1() { return CONTINUE; }
		@RestGet public Continue a2() { return _continue().setContent("foo"); }
		@RestGet public Continue a3() { return _continue().setHeader2("A","bar"); }
	}

	@Test void a01_basic() throws Exception {
		var client = MockRestClient.createLax(A.class).build();

		client.get("/a1")
			.run()
			.assertStatus(1100)
			.assertContent("Continue");
		client.get("/a2")
			.run()
			.assertStatus(1100)
			.assertContent("foo");
		client.get("/a3")
			.run()
			.assertStatus(1100)
			.assertHeader("A").is("bar");
	}
}