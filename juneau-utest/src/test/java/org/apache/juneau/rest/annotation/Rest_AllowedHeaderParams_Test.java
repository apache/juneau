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
package org.apache.juneau.rest.annotation;

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_AllowedHeaderParams_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(allowedHeaderParams)
	//------------------------------------------------------------------------------------------------------------------

	public static class A {
		@RestOp
		public String put(RequestHeaders h) {
			Accept accept = h.get("Accept").as(Accept.class).orElse(Accept.NULL);
			ContentType contentType = h.get("Content-Type").as(ContentType.class).orElse(ContentType.NULL);
			return "Accept="+(accept.isPresent() ? accept.get() : null)+",Content-Type=" + (contentType.isPresent() ? contentType.get() : null) + ",Custom=" + h.get("Custom").orElse(null);
		}
	}

	@Rest() /* Default is allowedHeaderParams="Accept, Content-Type" */
	public static class A1 extends A {}

	@Rest(allowedHeaderParams="Accept, Content-Type")
	public static class A2 extends A {}

	@Rest(allowedHeaderParams="ACCEPT, CONTENT-TYPE")
	public static class A3 extends A {}

	@Rest(allowedHeaderParams="Custom")
	public static class A4 extends A {}

	@Rest(allowedHeaderParams="*")
	public static class A5 extends A {}

	@Rest(allowedHeaderParams="NONE")
	public static class A6 extends A {}

	@Rest(allowedHeaderParams="None")
	public static class A7 extends A {}

	@Rest(allowedHeaderParams="None")
	public static class A8 extends A5 {}


	@Test
	public void a01_basic() throws Exception {
		RestClient a1 = MockRestClient.build(A1.class);
		a1.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a1.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		a1.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");

		RestClient a2 = MockRestClient.build(A2.class);
		a2.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a2.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		a2.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");

		RestClient a3 = MockRestClient.build(A3.class);
		a3.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a3.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		a3.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");

		RestClient a4 = MockRestClient.build(A4.class);
		a4.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a4.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=bar3");
		a4.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=bar3");

		RestClient a5 = MockRestClient.build(A5.class);
		a5.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a5.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=bar3");
		a5.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=bar3");

		RestClient a6 = MockRestClient.build(A6.class);
		a6.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a6.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a6.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");

		RestClient a7 = MockRestClient.build(A7.class);
		a7.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a7.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a7.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");

		RestClient a8 = MockRestClient.build(A8.class);
		a8.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a8.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		a8.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
	}
}
