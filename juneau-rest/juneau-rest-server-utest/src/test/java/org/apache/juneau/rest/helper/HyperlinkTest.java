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
package org.apache.juneau.rest.helper;

import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HyperlinkTest {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod
		public Hyperlink a01() {
			return new Hyperlink("foo", "bar");
		}

		@RestMethod
		public Hyperlink[] a02() {
			return new Hyperlink[]{a01(),a01()};
		}

		@RestMethod
		public Collection<Hyperlink> a03() {
			return Arrays.asList(a02());
		}
	}

	static MockRestClient a = MockRestClient.build(A.class);

	@Test
	public void a01_basic() throws Exception {
		a.get("/a01")
			.accept("text/html+stripped")
			.run()
			.assertBody().is("<a href=\"/foo\">bar</a>");
	}

	@Test
	public void a02_array() throws Exception {
		a.get("/a02")
			.accept("text/html+stripped")
			.run()
			.assertBody().is("<ul><li><a href=\"/foo\">bar</a></li><li><a href=\"/foo\">bar</a></li></ul>");
	}

	@Test
	public void a03_collection() throws Exception {
		a.get("/a03")
			.accept("text/html+stripped")
			.run()
			.assertBody().is("<ul><li><a href=\"/foo\">bar</a></li><li><a href=\"/foo\">bar</a></li></ul>");
	}
}