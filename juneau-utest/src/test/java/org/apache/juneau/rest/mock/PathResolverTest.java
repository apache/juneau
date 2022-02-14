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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.assertions.*;
import org.junit.*;

public class PathResolverTest {

	public static class PathResolver2 extends MockPathResolver {

		public PathResolver2(String target, String contextPath, String servletPath, String pathToResolve, Map<String,Object> pathVars) {
			super(target, contextPath, servletPath, pathToResolve, pathVars);
		}

		FluentStringAssertion<PathResolver2> assertUri() {
			return new FluentStringAssertion<>(getURI(), this);
		}

		FluentStringAssertion<PathResolver2> assertContextPath() {
			return new FluentStringAssertion<>(getContextPath(), this);
		}

		FluentStringAssertion<PathResolver2> assertServletPath() {
			return new FluentStringAssertion<>(getServletPath(), this);
		}

		FluentStringAssertion<PathResolver2> assertPathInfo() {
			return new FluentStringAssertion<>(getRemainder(), this);
		}

		FluentStringAssertion<PathResolver2> assertTarget() {
			return new FluentStringAssertion<>(getTarget(), this);
		}

		FluentStringAssertion<PathResolver2> assertError() {
			return new FluentStringAssertion<>(getError(), this);
		}
	}

	private static PathResolver2 create(String target, String contextPath, String servletPath, String pathToResolve, Map<String,Object> pathVars) {
		return new PathResolver2(target, contextPath, servletPath, pathToResolve, pathVars);
	}

	@Test
	public void basicDefaultTarget() {
		create(null, null, null, "/foo", null)
			.assertUri().is("http://localhost/foo")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create(null, null, "/sp", "/foo", null)
			.assertUri().is("http://localhost/sp/foo")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("")
			.assertServletPath().is("/sp")
			.assertPathInfo().is("/foo");

		create(null, "/cp", null, "/foo", null)
			.assertUri().is("http://localhost/cp/foo")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("/cp")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create(null, "/cp", "/sp", "/foo", null)
			.assertUri().is("http://localhost/cp/sp/foo")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("/cp")
			.assertServletPath().is("/sp")
			.assertPathInfo().is("/foo");

		create(null, "/", "/", "/foo", null)
			.assertUri().is("http://localhost/foo")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create(null, "", "", "/foo", null)
			.assertUri().is("http://localhost/foo")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create(null, "", "", "/", null)
			.assertUri().is("http://localhost")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("");

		create(null, "", "", "", null)
			.assertUri().is("http://localhost")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("");

		create(null, "", "", null, null)
			.assertUri().is("http://localhost")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("");

		create(null, "/cp/cp2", "/sp/sp2", "/foo/foo2", null)
			.assertUri().is("http://localhost/cp/cp2/sp/sp2/foo/foo2")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("/cp/cp2")
			.assertServletPath().is("/sp/sp2")
			.assertPathInfo().is("/foo/foo2");

		create(null, "cp/cp2/", "sp/sp2/", "foo/foo2/", null)
			.assertUri().is("http://localhost/cp/cp2/sp/sp2/foo/foo2")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("/cp/cp2")
			.assertServletPath().is("/sp/sp2")
			.assertPathInfo().is("/foo/foo2");
	}

	@Test
	public void basicWithTarget() {
		create("http://foobar", null, null, "/foo", null)
			.assertUri().is("http://foobar/foo")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create("http://foobar", null, "/sp", "/foo", null)
			.assertUri().is("http://foobar/sp/foo")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("/sp")
			.assertPathInfo().is("/foo");

		create("http://foobar", "/cp", null, "/foo", null)
			.assertUri().is("http://foobar/cp/foo")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/cp")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create("http://foobar", "/cp", "/sp", "/foo", null)
			.assertUri().is("http://foobar/cp/sp/foo")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/cp")
			.assertServletPath().is("/sp")
			.assertPathInfo().is("/foo");

		create("http://foobar", "/", "/", "/foo", null)
			.assertUri().is("http://foobar/foo")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create("http://foobar", "", "", "/foo", null)
			.assertUri().is("http://foobar/foo")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create("http://foobar", "", "", "/", null)
			.assertUri().is("http://foobar")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("");

		create("http://foobar", "", "", "", null)
			.assertUri().is("http://foobar")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("");

		create("http://foobar", "", "", null, null)
			.assertUri().is("http://foobar")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("");
	}

	@Test
	public void basicWithPathVars() {
		Map<String,Object> vars = map("foo","123");

		create(null, null, null, "/foo", vars)
			.assertUri().is("http://localhost/foo")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create(null, "/cp/{foo}", "/sp/{foo}", "/foo/{foo}", vars)
			.assertUri().is("http://localhost/cp/123/sp/123/foo/{foo}")
			.assertTarget().is("http://localhost")
			.assertContextPath().is("/cp/123")
			.assertServletPath().is("/sp/123")
			.assertPathInfo().is("/foo/{foo}");
	}

	@Test
	public void fullPaths() {
		Map<String,Object> vars = map("foo","123");

		create(null, null, null, "http://foobar/foo", vars)
			.assertUri().is("http://foobar/foo")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo");

		create(null, null, null, "http://foobar/foo/bar", vars)
			.assertUri().is("http://foobar/foo/bar")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/foo/bar");

		create(null, "/foo", null, "http://foobar/foo/bar", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("")
			.assertPathInfo().is("/bar");

		create(null, "/", "/foo", "http://foobar/foo/bar", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("/foo")
			.assertPathInfo().is("/bar");

		create(null, "/foo", "/bar", "http://foobar/foo/bar", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("/bar")
			.assertPathInfo().is("");

		create(null, "/foo", "/bar", "http://foobar/foo/bar/baz", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar/baz")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("/bar")
			.assertPathInfo().is("/baz");

		create(null, "/foo/bar", "/baz", "http://foobar/foo/bar/baz", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar/baz")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo/bar")
			.assertServletPath().is("/baz")
			.assertPathInfo().is("");

		create(null, "/foo", "/bar/baz", "http://foobar/foo/bar/baz", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar/baz")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("/bar/baz")
			.assertPathInfo().is("");

		create(null, "/foo", "/bar/baz", "http://foobar/foo/bar/baz/qux", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar/baz/qux")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("/bar/baz")
			.assertPathInfo().is("/qux");

		create(null, "/foo", "/bar/baz", "http://foobar/foo/bar/baz/qux/quux", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar/baz/qux/quux")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("/bar/baz")
			.assertPathInfo().is("/qux/quux");

		create(null, "/foo", "/bar/{x}", "http://foobar/foo/bar/baz/qux/quux", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar/baz/qux/quux")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("/bar/baz")
			.assertPathInfo().is("/qux/quux");
		create(null, "/{x}", "/{x}/{x}", "http://foobar/foo/bar/baz/qux/quux", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar/baz/qux/quux")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("/bar/baz")
			.assertPathInfo().is("/qux/quux");
		create(null, "/{bar}", "/{bar}/{bar}", "http://foobar/foo/bar/baz/qux/quux", vars)
			.assertError().isNull()
			.assertUri().is("http://foobar/foo/bar/baz/qux/quux")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("/bar/baz")
			.assertPathInfo().is("/qux/quux");

		create(null, null, null, "http://foobar", vars)
			.assertUri().is("http://foobar")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("");

		create(null, null, null, "http://foobar/", vars)
			.assertUri().is("http://foobar/")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("")
			.assertServletPath().is("")
			.assertPathInfo().is("/");

		create(null, "/foo", null, "http://foobar/foo", vars)
			.assertUri().is("http://foobar/foo")
			.assertTarget().is("http://foobar")
			.assertContextPath().is("/foo")
			.assertServletPath().is("")
			.assertPathInfo().is("");
	}

	@Test
	public void errors() {
		create(null, null, null, "http://", null)
			.assertError().is("Invalid URI pattern encountered:  http://");
		create(null, null, null, "http:///", null)
			.assertError().is("Invalid URI pattern encountered:  http:///");
		create(null, "/foo", null, "http://localhost/bar/baz", null)
			.assertError().is("Context path [/foo] not found in URI:  http://localhost/bar/baz");
		create(null, "/foo", "/bar", "http://localhost/foo/baz/qux", null)
			.assertError().is("Servlet path [/bar] not found in URI:  http://localhost/foo/baz/qux");
	}
}
