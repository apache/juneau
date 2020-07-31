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

import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_StaticFiles_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest(staticFiles={"xdocs:xdocs","xdocs2:xdocs2:{Foo:'Bar'}"})
	public static class A {
		@RestMethod
		public String a() {
			return null;
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.get("/xdocs/test.txt")
			.run()
			.assertBody().contains("OK-1");
		a.get("/xdocs/xsubdocs/test.txt")
			.run()
			.assertBody().contains("OK-2");
		a.get("/xdocs/xsubdocs/../test.txt?noTrace=true")
			.run()
			.assertCode().is(404);
		a.get("/xdocs/xsubdocs/%2E%2E/test.txt?noTrace=true")
			.run()
			.assertCode().is(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Static files with response headers.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(staticFiles={"xdocs:xdocs:{Foo:'Bar',Baz:'Qux'}"})
	public static class B {
		@RestMethod
		public String a() {
			return null;
		}
	}

	@Test
	public void b01_withResponseHeaders() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		b.get("/xdocs/test.txt")
			.run()
			.assertStringHeader("Foo").is("Bar")
			.assertStringHeader("Baz").is("Qux")
			.assertBody().contains("OK-1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Class hierarchy
	//------------------------------------------------------------------------------------------------------------------

	@Rest(staticFiles={"xdocs:xdocs"})
	public static class C1 {
		@RestMethod
		public String a() {
			return null;
		}
	}

	@Rest(staticFiles={"xdocs:/xdocs"})
	public static class C2 extends C1 {
		@RestMethod
		public String b() {
			return null;
		}
	}

	@Test
	public void c01_classHierarchy() throws Exception {
		RestClient c1 = MockRestClient.build(C1.class);
		// Should resolve to relative xdocs folder.
		c1.get("/xdocs/test.txt")
			.run()
			.assertBody().contains("OK-1");
		c1.get("/xdocs/xsubdocs/test.txt")
			.run()
			.assertBody().contains("OK-2");

		// Should be overridden to absolute xdocs folder.
		RestClient c2 = MockRestClient.build(C2.class);
		c2.get("/xdocs/test.txt")
			.run()
			.assertBody().contains("OK-3");
		c2.get("/xdocs/xsubdocs/test.txt")
			.run()
			.assertBody().contains("OK-4");

		// Should pick up from file system.
		c1.get("/xdocs/test2.txt")
			.run()
			.assertBody().contains("OK-5");
		c2.get("/xdocs/test2.txt")
			.run()
			.assertBody().contains("OK-5");
		c1.get("/xdocs/xsubdocs/test2.txt")
			.run()
			.assertBody().contains("OK-6");
		c2.get("/xdocs/xsubdocs/test2.txt")
			.run()
			.assertBody().contains("OK-6");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Overridden patterns
	//------------------------------------------------------------------------------------------------------------------

	@Rest(staticFiles={"xdocs:/xdocs,xdocs:xdocs"})
	public static class D {
		@RestMethod
		public String a() {
			return null;
		}
	}

	@Test
	public void d01_overriddenPatterns() throws Exception {
		RestClient d = MockRestClient.build(D.class);

		// Should be overridden to absolute xdocs folder.
		d.get("/xdocs/test.txt")
			.run()
			.assertBody().contains("OK-3");
		d.get("/xdocs/xsubdocs/test.txt")
			.run()
			.assertBody().contains("OK-4");

		// Should pick up from file system.
		d.get("/xdocs/test2.txt")
			.run()
			.assertBody().contains("OK-5");
		d.get("/xdocs/xsubdocs/test2.txt")
			.run()
			.assertBody().contains("OK-6");
	}

}
