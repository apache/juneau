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
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class EarlyHints_Test {

	@Rest
	public static class A {
		@RestGet public EarlyHints a1() { return EARLY_HINTS; }
		@RestGet public EarlyHints a2() { return earlyHints().setContent("foo"); }
		@RestGet public EarlyHints a3() { return earlyHints().setHeader2("Foo","bar"); }
	}

	@Test
	public void a01_basic() throws Exception {
		MockRestClient client = MockRestClient.createLax(A.class).build();

		client.get("/a1")
			.run()
			.assertStatus(1103)
			.assertContent("Early Hints");
		client.get("/a2")
			.run()
			.assertStatus(1103)
			.assertContent("foo");
		client.get("/a3")
			.run()
			.assertStatus(1103)
			.assertHeader("Foo").is("bar");
	}
}
