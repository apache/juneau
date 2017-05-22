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

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

/**
 * Verifies that the resolveUri() methods in UriContext work correctly.
 */
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore
public class UriContextResolutionComboTest {
//
//	@Parameterized.Parameters
//	public static Collection<Object[]> getInput() {
//		return Arrays.asList(new Object[][] {
//
//			// Happy cases - All URL parts known.
//			{
//				input(
//					"Happy-1",
//					"http://host:port","/context","/resource","/path",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar"
//				)
//			},
//			{
//				input(
//					"Happy-2",
//					"http://host:port","/context","/resource","/path",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123"
//				)
//			},
//			{
//				input(
//					"Happy-3",
//					"http://host:port","/context","/resource","/path",
//					"/foobar",
//					"http://host:port/foobar",
//					"/foobar"
//				)
//			},
//			{
//				input(
//					"Happy-4",
//					"http://host:port","/context","/resource","/path",
//					"/",
//					"http://host:port",
//					"/"
//				)
//			},
//			{
//				input(
//					"Happy-5",
//					"http://host:port","/context","/resource","/path",
//					"foobar",
//					"http://host:port/context/resource/foobar",
//					"/context/resource/foobar"
//				)
//			},
//			{
//				input(
//					"Happy-6",
//					"http://host:port","/context","/resource","/path",
//					"",
//					"http://host:port/context/resource/path",
//					"/context/resource/path"
//				)
//			},
//			{
//				input(
//					"Happy-7",
//					"http://host:port","/context","/resource","/path",
//					"context:/foo",
//					"http://host:port/context/foo",
//					"/context/foo"
//				)
//			},
//			{
//				input(
//					"Happy-8",
//					"http://host:port","/context","/resource","/path",
//					"context:/",
//					"http://host:port/context",
//					"/context"
//				)
//			},
//			{
//				input(
//					"Happy-9",
//					"http://host:port","/context","/resource","/path",
//					"servlet:/foo",
//					"http://host:port/context/resource/foo",
//					"/context/resource/foo"
//				)
//			},
//			{
//				input(
//					"Happy-10",
//					"http://host:port","/context","/resource","/path",
//					"servlet:/",
//					"http://host:port/context/resource",
//					"/context/resource"
//				)
//			},
//			
//			// Multiple context and resource parts
//			{
//				input(
//					"MultiContextResource-1",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-2",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-3",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"/foobar",
//					"http://host:port/foobar",
//					"/foobar"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-4",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"/",
//					"http://host:port",
//					"/"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-5",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"foobar",
//					"http://host:port/c1/c2/r1/r2/p1/foobar",
//					"/c1/c2/r1/r2/p1/foobar"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-6",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"",
//					"http://host:port/c1/c2/r1/r2/p1/p2",
//					"/c1/c2/r1/r2/p1/p2"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-7",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"context:/foo",
//					"http://host:port/c1/c2/foo",
//					"/c1/c2/foo"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-8",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"context:/",
//					"http://host:port/c1/c2",
//					"/c1/c2"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-9",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"servlet:/foo",
//					"http://host:port/c1/c2/r1/r2/foo",
//					"/c1/c2/r1/r2/foo"
//				)
//			},
//			{
//				input(
//					"MultiContextResource-10",
//					"http://host:port","/c1/c2","/r1/r2","/p1/p2",
//					"servlet:/",
//					"http://host:port/c1/c2/r1/r2",
//					"/c1/c2/r1/r2"
//				)
//			},
//			
//			// No authority given
//			{
//				input(
//					"NoAuthority-1",
//					"","/context","/resource","/path",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthority-2",
//					"","/context","/resource","/path",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123"
//				)
//			},
//			{
//				input(
//					"NoAuthority-3",
//					"","/context","/resource","/path",
//					"/foobar",
//					"/foobar",
//					"/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthority-4",
//					"","/context","/resource","/path",
//					"/",
//					"/",
//					"/"
//				)
//			},
//			{
//				input(
//					"NoAuthority-5",
//					"","/context","/resource","/path",
//					"foobar",
//					"/context/resource/foobar",
//					"/context/resource/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthority-6",
//					"","/context","/resource","/path",
//					"",
//					"/context/resource/path",
//					"/context/resource/path"
//				)
//			},
//			{
//				input(
//					"NoAuthority-7",
//					"","/context","/resource","/path",
//					"context:/foo",
//					"/context/foo",
//					"/context/foo"
//				)
//			},
//			{
//				input(
//					"NoAuthority-8",
//					"","/context","/resource","/path",
//					"context:/",
//					"/context",
//					"/context"
//				)
//			},
//			{
//				input(
//					"NoAuthority-9",
//					"","/context","/resource","/path",
//					"servlet:/foo",
//					"/context/resource/foo",
//					"/context/resource/foo"
//				)
//			},
//			{
//				input(
//					"NoAuthority-10",
//					"","/context","/resource","/path",
//					"servlet:/",
//					"/context/resource",
//					"/context/resource"
//				)
//			},
//			
//			// No authority or context given
//			{
//				input(
//					"NoAuthorityOrContext-1",
//					"","","/resource","/path",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-2",
//					"","","/resource","/path",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-3",
//					"","","/resource","/path",
//					"/foobar",
//					"/foobar",
//					"/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-4",
//					"","","/resource","/path",
//					"/",
//					"/",
//					"/"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-5",
//					"","","/resource","/path",
//					"foobar",
//					"/resource/foobar",
//					"/resource/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-6",
//					"","","/resource","/path",
//					"",
//					"/resource/path",
//					"/resource/path"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-7",
//					"","","/resource","/path",
//					"context:/foo",
//					"/foo",
//					"/foo"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-8",
//					"","","/resource","/path",
//					"context:/",
//					"/",
//					"/"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-9",
//					"","","/resource","/path",
//					"servlet:/foo",
//					"/resource/foo",
//					"/resource/foo"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContext-10",
//					"","","/resource","/path",
//					"servlet:/",
//					"/resource",
//					"/resource"
//				)
//			},
//
//			// No authority or context or resource given
//			{
//				input(
//					"NoAuthorityOrContextOrResource-1",
//					"","","","/path",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-2",
//					"","","","/path",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-3",
//					"","","","/path",
//					"/foobar",
//					"/foobar",
//					"/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-4",
//					"","","","/path",
//					"/",
//					"/",
//					"/"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-5",
//					"","","","/path",
//					"foobar",
//					"/foobar",
//					"/foobar"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-6",
//					"","","","/path",
//					"",
//					"/path",
//					"/path"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-7",
//					"","","","/path",
//					"context:/foo",
//					"/foo",
//					"/foo"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-8",
//					"","","","/path",
//					"context:/",
//					"/",
//					"/"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-9",
//					"","","","/path",
//					"servlet:/foo",
//					"/foo",
//					"/foo"
//				)
//			},
//			{
//				input(
//					"NoAuthorityOrContextOrResource-10",
//					"","","","/path",
//					"servlet:/",
//					"/",
//					"/"
//				)
//			},
//			
//			// No context or resource given.
//			{
//				input(
//					"NoContextOrResource-1",
//					"http://host:port","","","/path",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar",
//					"http://foo.com:123/foobar"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-2",
//					"http://host:port","","","/path",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-3",
//					"http://host:port","","","/path",
//					"/foobar",
//					"http://host:port/foobar",
//					"/foobar"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-4",
//					"http://host:port","","","/path",
//					"/",
//					"http://host:port",
//					"/"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-5",
//					"http://host:port","","","/path",
//					"foobar",
//					"http://host:port/foobar",
//					"/foobar"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-6",
//					"http://host:port","","","/path",
//					"",
//					"http://host:port/path",
//					"/path"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-7",
//					"http://host:port","","","/path",
//					"context:/foo",
//					"http://host:port/foo",
//					"/foo"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-8",
//					"http://host:port","","","/path",
//					"context:/",
//					"http://host:port",
//					"/"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-9",
//					"http://host:port","","","/path",
//					"servlet:/foo",
//					"http://host:port/foo",
//					"/foo"
//				)
//			},
//			{
//				input(
//					"NoContextOrResource-10",
//					"http://host:port","","","/path",
//					"servlet:/",
//					"http://host:port",
//					"/"
//				)
//			},
//		});		
//	}
//	
//	public static Input input(String label, String authority, String context, String resource, String path, String uri, String expectedAbsolute, String expectedRootRelative) {
//		return new Input(label, authority, context, resource, path, uri, expectedAbsolute, expectedRootRelative);
//	}
//	
//	public static class Input {
//		private final UriContext uriContext;
//		private final String label, uri, expectedAbsolute, expectedRootRelative;
//		
//		public Input(String label, String authority, String context, String resource, String path, String uri, String expectedAbsolute, String expectedRootRelative) {
//			this.label = label;
//			this.uriContext = new UriContext(authority, context, resource, path);
//			this.uri = uri;
//			this.expectedAbsolute = expectedAbsolute;
//			this.expectedRootRelative = expectedRootRelative;
//		}
//	}
//	
//	private Input in;
//	
//	public UriContextResolutionComboTest(Input in) throws Exception {
//		this.in = in;
//	}
//	
//	@Test
//	public void testAbsolute() {
//		assertEquals(in.expectedAbsolute, in.uriContext.resolve(in.uri), "{0}: testAbsolute() failed", in.label);
//	}
//		
//	@Test
//	public void testRootRelative() {
//		assertEquals(in.expectedRootRelative, in.uriContext.resolveRootRelative(in.uri), "{0}: testRootRelative() failed", in.label);
//	}
//
//	@Test
//	public void testAbsoluteAppend() {
//		assertEquals(in.expectedAbsolute, in.uriContext.append(new StringBuilder(), in.uri).toString(), "{0}: testAbsolute() failed", in.label);
//	}
//		
//	@Test
//	public void testRootRelativeAppend() {
//		assertEquals(in.expectedRootRelative, in.uriContext.appendRootRelative(new StringBuilder(), in.uri).toString(), "{0}: testRootRelative() failed", in.label);
//	}
}
