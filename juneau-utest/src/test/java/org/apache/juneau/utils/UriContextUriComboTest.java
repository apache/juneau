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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that the getUri() methods in UriContext work correctly.
 */
@RunWith(Parameterized.class)
@FixMethodOrder(NAME_ASCENDING)
public class UriContextUriComboTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getInput() {
		return Arrays.asList(new Object[][] {

			// Happy cases - All URL parts known.
			{
				"Happy-1",
				input(
					"http://foo.com:123","/context","/resource","/path"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123/context",
					"http://foo.com:123/context/resource",
					"http://foo.com:123/context/resource/path",
					"/context",
					"/context/resource",
					"/context/resource/path"
				)
			},
			{
				"Happy-2",
				input(
					"http://foo.com:123","/c1/c2","/r1/r2","/p1/p2"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123/c1/c2",
					"http://foo.com:123/c1/c2/r1/r2",
					"http://foo.com:123/c1/c2/r1/r2/p1/p2",
					"/c1/c2",
					"/c1/c2/r1/r2",
					"/c1/c2/r1/r2/p1/p2"
				)
			},
			{
				"NoAuthority-1",
				input(
					"","/context","/resource","/path"
				),
				results(
					"/",
					"/context",
					"/context/resource",
					"/context/resource/path",
					"/context",
					"/context/resource",
					"/context/resource/path"
				)
			},
			{
				"NoContext-1",
				input(
					"http://foo.com:123","","/resource","/path"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123/resource",
					"http://foo.com:123/resource/path",
					"/",
					"/resource",
					"/resource/path"
				)
			},
			{
				"NoResource-1",
				input(
					"http://foo.com:123","/context","","/path"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123/context",
					"http://foo.com:123/context",
					"http://foo.com:123/context/path",
					"/context",
					"/context",
					"/context/path"
				)
			},
			{
				"NoPath-1",
				input(
					"http://foo.com:123","/context","/resource",""
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123/context",
					"http://foo.com:123/context/resource",
					"http://foo.com:123/context/resource",
					"/context",
					"/context/resource",
					"/context/resource"
				)
			},
			{
				"NoAuthorityNoContext-1",
				input(
					"","","/resource","/path"
				),
				results(
					"/",
					"/",
					"/resource",
					"/resource/path",
					"/",
					"/resource",
					"/resource/path"
				)
			},
			{
				"NoContextNoResource-1",
				input(
					"http://foo.com:123","","","/path"
				),
				results(
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123",
					"http://foo.com:123/path",
					"/",
					"/",
					"/path"
				)
			},
			{
				"NoAuthorityNoContextNoResource-1",
				input(
					"","","","/path"
				),
				results(
					"/",
					"/",
					"/",
					"/path",
					"/",
					"/",
					"/path"
				)
			},
			{
				"Nothing-1",
				input(
					"","","",""
				),
				results(
					"/",
					"/",
					"/",
					"/",
					"/",
					"/",
					"/"
				)
			},
		});
	}

	public static Input input(String authority, String context, String resource, String path) {
		return new Input(authority, context, resource, path);
	}

	public static Results results(String eAbsoluteAuthority, String eAbsoluteContext, String eAbsoluteResource, String eAbsolutePath,
			String eRootRelativeContext, String eRootRelativeResource, String eRootRelativePath) {
		return new Results(eAbsoluteAuthority, eAbsoluteContext, eAbsoluteResource, eAbsolutePath, eRootRelativeContext, eRootRelativeResource, eRootRelativePath);
	}

	public static class Input {
		private final UriContext uriContext;

		public Input(String authority, String context, String resource, String path) {
			this.uriContext = UriContext.of(authority, context, resource, path);
		}
	}

	public static class Results {
		private final String eAbsoluteAuthority, eAbsoluteContext, eAbsoluteResource, eAbsolutePath, eRootRelativeContext, eRootRelativeResource, eRootRelativePath;

		public Results(String eAbsoluteAuthority, String eAbsoluteContext, String eAbsoluteResource, String eAbsolutePath,
					String eRootRelativeContext, String eRootRelativeResource, String eRootRelativePath) {
			this.eAbsoluteAuthority = eAbsoluteAuthority;
			this.eAbsoluteContext = eAbsoluteContext;
			this.eAbsoluteResource = eAbsoluteResource;
			this.eAbsolutePath = eAbsolutePath;
			this.eRootRelativeContext = eRootRelativeContext;
			this.eRootRelativeResource = eRootRelativeResource;
			this.eRootRelativePath = eRootRelativePath;
		}
	}

	private String label;
	private Input in;
	private Results r;

	public UriContextUriComboTest(String label, Input in, Results r) throws Exception {
		this.label = label;
		this.in = in;
		this.r = r;
	}

	@Test
	public void a1_testAbsoluteAuthority() {
		assertString(in.uriContext.getAbsoluteAuthority()).setMsg("{0}: testAbsoluteAuthority() failed", label).is(r.eAbsoluteAuthority);
	}

	@Test
	public void a2_testAbsoluteContext() {
		assertString(in.uriContext.getAbsoluteContextRoot()).setMsg("{0}: testAbsoluteContext() failed", label).is(r.eAbsoluteContext);
	}

	@Test
	public void a3_testAbsoluteResource() {
		assertString(in.uriContext.getAbsoluteServletPath()).setMsg("{0}: testAbsoluteResource() failed", label).is(r.eAbsoluteResource);
	}

	@Test
	public void a4_testAbsolutePath() {
		assertString(in.uriContext.getAbsolutePathInfo()).setMsg("{0}: testAbsolutePath() failed", label).is(r.eAbsolutePath);
	}

	@Test
	public void a5_testRootRelativeContext() {
		assertString(in.uriContext.getRootRelativeContextRoot()).setMsg("{0}: testRootRelativeContext() failed", label).is(r.eRootRelativeContext);
	}

	@Test
	public void a6_testRootRelativeResource() {
		assertString(in.uriContext.getRootRelativeServletPath()).setMsg("{0}: testRootRelativeResource() failed", label).is(r.eRootRelativeResource);
	}

	@Test
	public void a7_testRootRelativePath() {
		assertString(in.uriContext.getRootRelativePathInfo()).setMsg("{0}: testRootRelativePath() failed", label).is(r.eRootRelativePath);
	}
}
