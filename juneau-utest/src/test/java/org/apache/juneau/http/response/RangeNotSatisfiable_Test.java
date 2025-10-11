/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.response;

import static org.apache.juneau.http.HttpResponses.*;
import static org.apache.juneau.http.response.RangeNotSatisfiable.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class RangeNotSatisfiable_Test extends TestBase {

	@Rest
	public static class A {
		@RestGet
		public void f1() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable();
		}
		@RestGet
		public void f2() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable("foo {0}", "bar");
		}
		@RestGet
		public void f3() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable(new RuntimeException("baz"));
		}
		@RestGet
		public void f4() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable(new RuntimeException("baz"), "foo {0}", "bar");
		}
		@RestGet
		public void f5() throws RangeNotSatisfiable {
			throw rangeNotSatisfiable().setHeader2("Foo", "bar");
		}
		@RestGet
		public void f6() throws RangeNotSatisfiable {
			throw new RangeNotSatisfiable("foo");
		}
	}

	@Test void a01_basic() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().noTrace().build();

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