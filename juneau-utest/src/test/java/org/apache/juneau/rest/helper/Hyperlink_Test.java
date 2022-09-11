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

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@SuppressWarnings("serial")
@FixMethodOrder(NAME_ASCENDING)
public class Hyperlink_Test {

	@Rest
	public static class A extends BasicRestServlet {
		@RestGet
		public Hyperlink a() {
			return new Hyperlink("foo", "bar");
		}
		@RestGet
		public Hyperlink[] b() {
			return new Hyperlink[]{a(),a()};
		}
		@RestGet
		public Collection<Hyperlink> c() {
			return Arrays.asList(b());
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/a")
			.accept("text/html+stripped")
			.run()
			.assertContent("<a href=\"/foo\">bar</a>");
		a.get("/b")
			.accept("text/html+stripped")
			.run()
			.assertContent("<ul><li><a href=\"/foo\">bar</a></li><li><a href=\"/foo\">bar</a></li></ul>");
		a.get("/c")
			.accept("text/html+stripped")
			.run()
			.assertContent("<ul><li><a href=\"/foo\">bar</a></li><li><a href=\"/foo\">bar</a></li></ul>");
	}
}