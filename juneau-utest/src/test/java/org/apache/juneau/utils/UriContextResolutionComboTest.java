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
package org.apache.juneau.utils;

import static org.apache.juneau.UriRelativity.*;
import static org.apache.juneau.UriResolution.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that the resolveUri() methods in UriContext work correctly.
 */
@RunWith(Parameterized.class)
@FixMethodOrder(NAME_ASCENDING)
public class UriContextResolutionComboTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getInput() {
		return Arrays.asList(new Object[][] {

			// Happy cases - All URL parts known.
			{	/* 0 */
				"Happy-1a",
				input(
					"http://host:port","/context","/resource","/path",
					"http://foo.com:123/foobar"
				),
				results(
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar"
				)
			},
			{	/* 1 */
				"Happy-2",
				input(
					"http://host:port","/context","/resource","/path",
					"http://foo.com:123"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123"
				)
			},
			{	/* 2 */
				"Happy-3",
				input(
					"http://host:port","/context","/resource","/path",
					"/foobar"
				),
				results(
					"http://host:port/foobar",
					"http://host:port/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar"
				)
			},
			{	/* 3 */
				"Happy-4",
				input(
					"http://host:port","/context","/resource","/path",
					"/"
				),
				results(
					"http://host:port",
					"http://host:port",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 4 */
				"Happy-5",
				input(
					"http://host:port","/context","/resource","/path",
					"foobar"
				),
				results(
					"http://host:port/context/resource/foobar",
					"http://host:port/context/resource/foobar",
					"/context/resource/foobar",
					"/context/resource/foobar",
					"foobar",
					"foobar"
				)
			},
			{	/* 5 */
				"Happy-6",
				input(
					"http://host:port","/context","/resource","/path",
					""
				),
				results(
					"http://host:port/context/resource",
					"http://host:port/context/resource/path",
					"/context/resource",
					"/context/resource/path",
					"",
					""
				)
			},
			{	/* 6 */
				"Happy-7",
				input(
					"http://host:port","/context","/resource","/path",
					"context:/foo"
				),
				results(
					"http://host:port/context/foo",
					"http://host:port/context/foo",
					"/context/foo",
					"/context/foo",
					"/context/foo",
					"/context/foo"
				)
			},
			{	/* 7 */
				"Happy-8",
				input(
					"http://host:port","/context","/resource","/path",
					"context:/"
				),
				results(
					"http://host:port/context",
					"http://host:port/context",
					"/context",
					"/context",
					"/context",
					"/context"
				)
			},
			{	/* 8 */
				"Happy-9",
				input(
					"http://host:port","/context","/resource","/path",
					"servlet:/foo"
				),
				results(
					"http://host:port/context/resource/foo",
					"http://host:port/context/resource/foo",
					"/context/resource/foo",
					"/context/resource/foo",
					"/context/resource/foo",
					"/context/resource/foo"
				)
			},
			{	/* 9 */
				"Happy-10",
				input(
					"http://host:port","/context","/resource","/path",
					"servlet:/"
				),
				results(
					"http://host:port/context/resource",
					"http://host:port/context/resource",
					"/context/resource",
					"/context/resource",
					"/context/resource",
					"/context/resource"
				)
			},

			// Multiple context and resource parts
			{	/* 10 */
				"MultiContextResource-1",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"http://foo.com:123/foobar"
				),
				results(
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar"
				)
			},
			{	/* 11 */
				"MultiContextResource-2",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"http://foo.com:123"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123"
				)
			},
			{	/* 12 */
				"MultiContextResource-3",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"/foobar"
				),
				results(
					"http://host:port/foobar",
					"http://host:port/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar"
				)
			},
			{	/* 13 */
				"MultiContextResource-4",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"/"
				),
				results(
					"http://host:port",
					"http://host:port",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 14 */
				"MultiContextResource-5",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"foobar"
				),
				results(
					"http://host:port/c1/c2/r1/r2/foobar",
					"http://host:port/c1/c2/r1/r2/p1/foobar",
					"/c1/c2/r1/r2/foobar",
					"/c1/c2/r1/r2/p1/foobar",
					"foobar",
					"foobar"
				)
			},
			{	/* 15 */
				"MultiContextResource-6",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					""
				),
				results(
					"http://host:port/c1/c2/r1/r2",
					"http://host:port/c1/c2/r1/r2/p1/p2",
					"/c1/c2/r1/r2",
					"/c1/c2/r1/r2/p1/p2",
					"",
					""
				)
			},
			{	/* 16 */
				"MultiContextResource-7",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"context:/foo"
				),
				results(
					"http://host:port/c1/c2/foo",
					"http://host:port/c1/c2/foo",
					"/c1/c2/foo",
					"/c1/c2/foo",
					"/c1/c2/foo",
					"/c1/c2/foo"
				)
			},
			{	/* 17 */
				"MultiContextResource-8",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"context:/"
				),
				results(
					"http://host:port/c1/c2",
					"http://host:port/c1/c2",
					"/c1/c2",
					"/c1/c2",
					"/c1/c2",
					"/c1/c2"
				)
			},
			{	/* 18 */
				"MultiContextResource-9",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"servlet:/foo"
				),
				results(
					"http://host:port/c1/c2/r1/r2/foo",
					"http://host:port/c1/c2/r1/r2/foo",
					"/c1/c2/r1/r2/foo",
					"/c1/c2/r1/r2/foo",
					"/c1/c2/r1/r2/foo",
					"/c1/c2/r1/r2/foo"
				)
			},
			{	/* 19 */
				"MultiContextResource-10",
				input(
					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
					"servlet:/"
				),
				results(
					"http://host:port/c1/c2/r1/r2",
					"http://host:port/c1/c2/r1/r2",
					"/c1/c2/r1/r2",
					"/c1/c2/r1/r2",
					"/c1/c2/r1/r2",
					"/c1/c2/r1/r2"
				)
			},

			// No authority given
			{	/* 20 */
				"NoAuthority-1",
				input(
					"","/context","/resource","/path",
					"http://foo.com:123/foobar"
				),
				results(
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar"
				)
			},
			{	/* 21 */
				"NoAuthority-2",
				input(
					"","/context","/resource","/path",
					"http://foo.com:123"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123"
				)
			},
			{	/* 22 */
				"NoAuthority-3",
				input(
					"","/context","/resource","/path",
					"/foobar"
				),
				results(
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar"
				)
			},
			{	/* 23 */
				"NoAuthority-4",
				input(
					"","/context","/resource","/path",
					"/"
				),
				results(
					"/",
					"/",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 24 */
				"NoAuthority-5",
				input(
					"","/context","/resource","/path",
					"foobar"
				),
				results(
					"/context/resource/foobar",
					"/context/resource/foobar",
					"/context/resource/foobar",
					"/context/resource/foobar",
					"foobar",
					"foobar"
				)
			},
			{	/* 25 */
				"NoAuthority-6",
				input(
					"","/context","/resource","/path",
					""
				),
				results(
					"/context/resource",
					"/context/resource/path",
					"/context/resource",
					"/context/resource/path",
					"",
					""
				)
			},
			{	/* 26 */
				"NoAuthority-7",
				input(
					"","/context","/resource","/path",
					"context:/foo"
				),
				results(
					"/context/foo",
					"/context/foo",
					"/context/foo",
					"/context/foo",
					"/context/foo",
					"/context/foo"
				)
			},
			{	/* 27 */
				"NoAuthority-8",
				input(
					"","/context","/resource","/path",
					"context:/"
				),
				results(
					"/context",
					"/context",
					"/context",
					"/context",
					"/context",
					"/context"
				)
			},
			{	/* 28 */
				"NoAuthority-9",
				input(
					"","/context","/resource","/path",
					"servlet:/foo"
				),
				results(
					"/context/resource/foo",
					"/context/resource/foo",
					"/context/resource/foo",
					"/context/resource/foo",
					"/context/resource/foo",
					"/context/resource/foo"
				)
			},
			{	/* 29 */
				"NoAuthority-10",
				input(
					"","/context","/resource","/path",
					"servlet:/"
				),
				results(
					"/context/resource",
					"/context/resource",
					"/context/resource",
					"/context/resource",
					"/context/resource",
					"/context/resource"
				)
			},

			// No authority or context given
			{	/* 30 */
				"NoAuthorityOrContext-1",
				input(
					"","","/resource","/path",
					"http://foo.com:123/foobar"
				),
				results(
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar"
				)
			},
			{	/* 31 */
				"NoAuthorityOrContext-2",
				input(
					"","","/resource","/path",
					"http://foo.com:123"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123"
				)
			},
			{	/* 32 */
				"NoAuthorityOrContext-3",
				input(
					"","","/resource","/path",
					"/foobar"
				),
				results(
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar"
				)
			},
			{	/* 33 */
				"NoAuthorityOrContext-4",
				input(
					"","","/resource","/path",
					"/"
				),
				results(
					"/",
					"/",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 34 */
				"NoAuthorityOrContext-5",
				input(
					"","","/resource","/path",
					"foobar"
				),
				results(
					"/resource/foobar",
					"/resource/foobar",
					"/resource/foobar",
					"/resource/foobar",
					"foobar",
					"foobar"
				)
			},
			{	/* 35 */
				"NoAuthorityOrContext-6",
				input(
					"","","/resource","/path",
					""
				),
				results(
					"/resource",
					"/resource/path",
					"/resource",
					"/resource/path",
					"",
					""
				)
			},
			{	/* 36 */
				"NoAuthorityOrContext-7",
				input(
					"","","/resource","/path",
					"context:/foo"
				),
				results(
					"/foo",
					"/foo",
					"/foo",
					"/foo",
					"/foo",
					"/foo"
				)
			},
			{	/* 37 */
				"NoAuthorityOrContext-8",
				input(
					"","","/resource","/path",
					"context:/"
				),
				results(
					"/",
					"/",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 38 */
				"NoAuthorityOrContext-9",
				input(
					"","","/resource","/path",
					"servlet:/foo"
				),
				results(
					"/resource/foo",
					"/resource/foo",
					"/resource/foo",
					"/resource/foo",
					"/resource/foo",
					"/resource/foo"
				)
			},
			{	/* 39 */
				"NoAuthorityOrContext-10",
				input(
					"","","/resource","/path",
					"servlet:/"
				),
				results(
					"/resource",
					"/resource",
					"/resource",
					"/resource",
					"/resource",
					"/resource"
				)
			},

			// No authority or context or resource given
			{	/* 40 */
				"NoAuthorityOrContextOrResource-1",
				input(
					"","","","/path",
					"http://foo.com:123/foobar"
				),
				results(
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar"
				)
			},
			{	/* 41 */
				"NoAuthorityOrContextOrResource-2",
				input(
					"","","","/path",
					"http://foo.com:123"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123"
				)
			},
			{	/* 42 */
				"NoAuthorityOrContextOrResource-3",
				input(
					"","","","/path",
					"/foobar"
				),
				results(
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar"
				)
			},
			{	/* 43 */
				"NoAuthorityOrContextOrResource-4",
				input(
					"","","","/path",
					"/"
				),
				results(
					"/",
					"/",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 44 */
				"NoAuthorityOrContextOrResource-5",
				input(
					"","","","/path",
					"foobar"
				),
				results(
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"foobar",
					"foobar"
				)
			},
			{	/* 45 */
				"NoAuthorityOrContextOrResource-6",
				input(
					"","","","/path",
					""
				),
				results(
					"/",
					"/path",
					"/",
					"/path",
					"",
					""
				)
			},
			{	/* 46 */
				"NoAuthorityOrContextOrResource-7",
				input(
					"","","","/path",
					"context:/foo"
				),
				results(
					"/foo",
					"/foo",
					"/foo",
					"/foo",
					"/foo",
					"/foo"
				)
			},
			{	/* 47 */
				"NoAuthorityOrContextOrResource-8",
				input(
					"","","","/path",
					"context:/"
				),
				results(
					"/",
					"/",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 48 */
				"NoAuthorityOrContextOrResource-9",
				input(
					"","","","/path",
					"servlet:/foo"
				),
				results(
					"/foo",
					"/foo",
					"/foo",
					"/foo",
					"/foo",
					"/foo"
				)
			},
			{	/* 49 */
				"NoAuthorityOrContextOrResource-10",
				input(
					"","","","/path",
					"servlet:/"
				),
				results(
					"/",
					"/",
					"/",
					"/",
					"/",
					"/"
				)
			},

			// No context or resource given.
			{	/* 50 */
				"NoContextOrResource-1",
				input(
					"http://host:port","","","/path",
					"http://foo.com:123/foobar"
				),
				results(
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar",
					"http://foo.com:123/foobar"
				)
			},
			{	/* 51 */
				"NoContextOrResource-2",
				input(
					"http://host:port","","","/path",
					"http://foo.com:123"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123"
				)
			},
			{	/* 52 */
				"NoContextOrResource-3",
				input(
					"http://host:port","","","/path",
					"/foobar"
				),
				results(
					"http://host:port/foobar",
					"http://host:port/foobar",
					"/foobar",
					"/foobar",
					"/foobar",
					"/foobar"
				)
			},
			{	/* 53 */
				"NoContextOrResource-4",
				input(
					"http://host:port","","","/path",
					"/"
				),
				results(
					"http://host:port",
					"http://host:port",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 54 */
				"NoContextOrResource-5",
				input(
					"http://host:port","","","/path",
					"foobar"
				),
				results(
					"http://host:port/foobar",
					"http://host:port/foobar",
					"/foobar",
					"/foobar",
					"foobar",
					"foobar"
				)
			},
			{	/* 55 */
				"NoContextOrResource-6",
				input(
					"http://host:port","","","/path",
					""
				),
				results(
					"http://host:port",
					"http://host:port/path",
					"/",
					"/path",
					"",
					""
				)
			},
			{	/* 56 */
				"NoContextOrResource-7",
				input(
					"http://host:port","","","/path",
					"context:/foo"
				),
				results(
					"http://host:port/foo",
					"http://host:port/foo",
					"/foo",
					"/foo",
					"/foo",
					"/foo"
				)
			},
			{	/* 57 */
				"NoContextOrResource-8",
				input(
					"http://host:port","","","/path",
					"context:/"
				),
				results(
					"http://host:port",
					"http://host:port",
					"/",
					"/",
					"/",
					"/"
				)
			},
			{	/* 58 */
				"NoContextOrResource-9",
				input(
					"http://host:port","","","/path",
					"servlet:/foo"
				),
				results(
					"http://host:port/foo",
					"http://host:port/foo",
					"/foo",
					"/foo",
					"/foo",
					"/foo"
				)
			},
			{	/* 59 */
				"NoContextOrResource-10",
				input(
					"http://host:port","","","/path",
					"servlet:/"
				),
				results(
					"http://host:port",
					"http://host:port",
					"/",
					"/",
					"/",
					"/"
				)
			},
		});
	}

	public static Input input(String authority, String context, String resource, String path, String uri) {
		return new Input(authority, context, resource, path, uri);
	}

	public static Results results(String eAbsResource, String eAbsPathInfo, String eRrResource, String eRrPathInfo, String eNoneResource, String eNonePathInfo) {
		return new Results(eAbsResource, eAbsPathInfo, eRrResource, eRrPathInfo, eNoneResource, eNonePathInfo);
	}

	public static class Input {
		private final String uri;
		private final String authority, context, resource, path;

		public Input(String authority, String context, String resource, String path, String uri) {
			this.authority = authority;
			this.context = context;
			this.resource = resource;
			this.path = path;
			this.uri = uri;
		}
	}

	public static class Results {
		private final String aResource, aPathInfo, rrResource, rrPathInfo, nResource, nPathInfo;

		public Results(String aResource, String aPathInfo, String rrResource, String rrPathInfo, String nResource, String nPathInfo) {
			this.aResource = aResource;
			this.aPathInfo = aPathInfo;
			this.rrResource = rrResource;
			this.rrPathInfo = rrPathInfo;
			this.nResource = nResource;
			this.nPathInfo = nPathInfo;
		}
	}

	private String label;
	private Input in;
	private Results r;

	public UriContextResolutionComboTest(String label, Input in, Results r) throws Exception {
		this.label = label;
		this.in = in;
		this.r = r;
	}

	@Test
	public void a01_testAbsoluteResource() {
		String x = UriResolver.of(ABSOLUTE, RESOURCE, UriContext.of(in.authority, in.context, in.resource, in.path)).resolve(in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.aResource);
	}

	@Test
	public void a02_testAppendAbsoluteResource() {
		Appendable x = UriResolver.of(ABSOLUTE, RESOURCE, UriContext.of(in.authority, in.context, in.resource, in.path)).append(new StringBuilder(), in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.aResource);
	}

	@Test
	public void a03_testAbsolutePathInfo() {
		String x = UriResolver.of(ABSOLUTE, PATH_INFO, UriContext.of(in.authority, in.context, in.resource, in.path)).resolve(in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.aPathInfo);
	}

	@Test
	public void a04_testAppendAbsolutePathInfo() {
		Appendable x = UriResolver.of(ABSOLUTE, PATH_INFO, UriContext.of(in.authority, in.context, in.resource, in.path)).append(new StringBuilder(), in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.aPathInfo);
	}

	@Test
	public void a05_testRootRelativeResource() {
		String x = UriResolver.of(ROOT_RELATIVE, RESOURCE, UriContext.of(in.authority, in.context, in.resource, in.path)).resolve(in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.rrResource);
	}

	@Test
	public void a06_testAppendRootRelativeResource() {
		Appendable x = UriResolver.of(ROOT_RELATIVE, RESOURCE, UriContext.of(in.authority, in.context, in.resource, in.path)).append(new StringBuilder(), in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.rrResource);
	}

	@Test
	public void a07_testRootRelativePathInfo() {
		String x = UriResolver.of(ROOT_RELATIVE, PATH_INFO, UriContext.of(in.authority, in.context, in.resource, in.path)).resolve(in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.rrPathInfo);
	}

	@Test
	public void a08_testAppendRootRelativePathInfo() {
		Appendable x = UriResolver.of(ROOT_RELATIVE, PATH_INFO, UriContext.of(in.authority, in.context, in.resource, in.path)).append(new StringBuilder(), in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.rrPathInfo);
	}

	@Test
	public void a09_testNoneResource() {
		String x = UriResolver.of(NONE, RESOURCE, UriContext.of(in.authority, in.context, in.resource, in.path)).resolve(in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.nResource);
	}

	@Test
	public void a10_testAppendNoneResource() {
		Appendable x = UriResolver.of(NONE, RESOURCE, UriContext.of(in.authority, in.context, in.resource, in.path)).append(new StringBuilder(), in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.nResource);
	}

	@Test
	public void a11_testNonePathInfo() {
		String x = UriResolver.of(NONE, PATH_INFO, UriContext.of(in.authority, in.context, in.resource, in.path)).resolve(in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.nPathInfo);
	}

	@Test
	public void a12_testAppendNonePathInfo() {
		Appendable x = UriResolver.of(NONE, PATH_INFO, UriContext.of(in.authority, in.context, in.resource, in.path)).append(new StringBuilder(), in.uri);
		assertString(x).setMsg("{0}: testAbsolute() failed", label).is(r.nPathInfo);
	}
}
