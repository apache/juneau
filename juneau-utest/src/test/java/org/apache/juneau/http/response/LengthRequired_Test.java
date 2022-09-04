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
import static org.apache.juneau.http.response.LengthRequired.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class LengthRequired_Test {

	@Rest
	public static class A {
		@RestGet
		public void f1() throws LengthRequired {
			throw new LengthRequired();
		}
		@RestGet
		public void f2() throws LengthRequired {
			throw new LengthRequired("foo {0}", "bar");
		}
		@RestGet
		public void f3() throws LengthRequired {
			throw new LengthRequired(new RuntimeException("baz"));
		}
		@RestGet
		public void f4() throws LengthRequired {
			throw new LengthRequired(new RuntimeException("baz"), "foo {0}", "bar");
		}
		@RestGet
		public void f5() throws LengthRequired {
			throw lengthRequired().setHeader2("Foo", "bar");
		}
		@RestGet
		public void f6() throws LengthRequired {
			throw new LengthRequired("foo");
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
			.assertHeader("Foo").is("bar");
		c.get("/f6").run()
			.assertStatus().asCode().is(STATUS_CODE)
			.assertContent("foo");
	}
}
