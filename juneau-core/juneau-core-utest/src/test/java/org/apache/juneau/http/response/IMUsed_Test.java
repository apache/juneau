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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class IMUsed_Test {

	@Rest
	public static class A {
		@RestMethod public IMUsed getA1() { return new IMUsed(); }
		@RestMethod public IMUsed getA2() { return new IMUsed("foo"); }
		@RestMethod public IMUsed getA3() { return new IMUsed().header("Foo","bar"); }
	}

	@Test
	public void a01_basic() throws Exception {
		MockRestClient client = MockRestClient.createLax(A.class).build();

		client.get("/a1")
			.run()
			.assertCode().is(226)
			.assertBody().is("IM Used");
		client.get("/a2")
			.run()
			.assertCode().is(226)
			.assertBody().is("foo");
		client.get("/a3")
			.run()
			.assertCode().is(226)
			.assertStringHeader("Foo").is("bar");
	}
}
