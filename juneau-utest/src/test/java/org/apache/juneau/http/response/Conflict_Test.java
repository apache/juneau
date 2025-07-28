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
import static org.apache.juneau.http.response.Conflict.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Conflict_Test {

	@Rest
	public static class A {
		@RestGet
		public void f1() throws Conflict {
			throw new Conflict();
		}
		@RestGet
		public void f2() throws Conflict {
			throw new Conflict("foo {0}", "bar");
		}
		@RestGet
		public void f3() throws Conflict {
			throw new Conflict(new RuntimeException("baz"));
		}
		@RestGet
		public void f4() throws Conflict {
			throw new Conflict(new RuntimeException("baz"), "foo {0}", "bar");
		}
		@RestGet
		public void f5() throws Conflict {
			throw conflict().setHeader2("A", "bar");
		}
		@RestGet
		public void f6() throws Conflict {
			throw new Conflict("foo");
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient c = MockRestClient.create(A.class).ignoreErrors().noTrace().build();

		c.get("/f1").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent(REASON_PHRASE);
		c.get("/f2").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("foo bar");
		c.get("/f3").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("baz");
		c.get("/f4").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("foo bar");
		c.get("/f5").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent(REASON_PHRASE)
			.assertHeader("A").is("bar");
		c.get("/f6").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("foo");
	}
}