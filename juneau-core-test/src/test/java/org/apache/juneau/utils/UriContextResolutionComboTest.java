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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.UriRelativity.*;
import static org.apache.juneau.UriResolution.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that the resolveUri() methods in UriContext work correctly.
 */
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UriContextResolutionComboTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getInput() {
		return Arrays.asList(new Object[][] {

			// Happy cases - All URL parts known.
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
//			return new Results(eAbsResource, eAbsPathInfo, eRrResource, eRrPathInfo, eNoneResource, eNonePathInfo);
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
			{
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
		assertEquals(r.aResource, new UriContext(ABSOLUTE, RESOURCE, in.authority, in.context, in.resource, in.path).resolve(in.uri), "{0}: testAbsolute() failed", label);
	}	
		
	@Test
	public void a02_testAppendAbsoluteResource() {
		assertEquals(r.aResource, new UriContext(ABSOLUTE, RESOURCE, in.authority, in.context, in.resource, in.path).append(new StringBuilder(), in.uri).toString(), "{0}: testAbsolute() failed", label);
	}
	
	@Test
	public void a03_testAbsolutePathInfo() {
		assertEquals(r.aPathInfo, new UriContext(ABSOLUTE, PATH_INFO, in.authority, in.context, in.resource, in.path).resolve(in.uri), "{0}: testAbsolute() failed", label);
	}	
		
	@Test
	public void a04_testAppendAbsolutePathInfo() {
		assertEquals(r.aPathInfo, new UriContext(ABSOLUTE, PATH_INFO, in.authority, in.context, in.resource, in.path).append(new StringBuilder(), in.uri).toString(), "{0}: testAbsolute() failed", label);
	}

	@Test
	public void a05_testRootRelativeResource() {
		assertEquals(r.rrResource, new UriContext(ROOT_RELATIVE, RESOURCE, in.authority, in.context, in.resource, in.path).resolve(in.uri), "{0}: testAbsolute() failed", label);
	}	
		
	@Test
	public void a06_testAppendRootRelativeResource() {
		assertEquals(r.rrResource, new UriContext(ROOT_RELATIVE, RESOURCE, in.authority, in.context, in.resource, in.path).append(new StringBuilder(), in.uri).toString(), "{0}: testAbsolute() failed", label);
	}

	@Test
	public void a07_testRootRelativePathInfo() {
		assertEquals(r.rrPathInfo, new UriContext(ROOT_RELATIVE, PATH_INFO, in.authority, in.context, in.resource, in.path).resolve(in.uri), "{0}: testAbsolute() failed", label);
	}	
		
	@Test
	public void a08_testAppendRootRelativePathInfo() {
		assertEquals(r.rrPathInfo, new UriContext(ROOT_RELATIVE, PATH_INFO, in.authority, in.context, in.resource, in.path).append(new StringBuilder(), in.uri).toString(), "{0}: testAbsolute() failed", label);
	}

	@Test
	public void a09_testNoneResource() {
		assertEquals(r.nResource, new UriContext(NONE, RESOURCE, in.authority, in.context, in.resource, in.path).resolve(in.uri), "{0}: testAbsolute() failed", label);
	}	
		
	@Test
	public void a10_testAppendNoneResource() {
		assertEquals(r.nResource, new UriContext(NONE, RESOURCE, in.authority, in.context, in.resource, in.path).append(new StringBuilder(), in.uri).toString(), "{0}: testAbsolute() failed", label);
	}

	@Test
	public void a11_testNonePathInfo() {
		assertEquals(r.nPathInfo, new UriContext(NONE, PATH_INFO, in.authority, in.context, in.resource, in.path).resolve(in.uri), "{0}: testAbsolute() failed", label);
	}	
		
	@Test
	public void a12_testAppendNonePathInfo() {
		assertEquals(r.nPathInfo, new UriContext(NONE, PATH_INFO, in.authority, in.context, in.resource, in.path).append(new StringBuilder(), in.uri).toString(), "{0}: testAbsolute() failed", label);
	}
}
