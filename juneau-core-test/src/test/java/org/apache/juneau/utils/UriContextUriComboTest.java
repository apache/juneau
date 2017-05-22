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
 * Verifies that the getUri() methods in UriContext work correctly.
 */
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore
public class UriContextUriComboTest {
//
//	@Parameterized.Parameters
//	public static Collection<Object[]> getInput() {
//		return Arrays.asList(new Object[][] {
//
//			// Happy cases - All URL parts known.
//			{
//				input(
//					"Happy-1",
//					"http://foo.com:123","/context","/resource","/path",
//					"http://foo.com:123",
//					"http://foo.com:123/context",
//					"http://foo.com:123/context/resource",
//					"http://foo.com:123/context/resource/path",
//					"/context",
//					"/context/resource",
//					"/context/resource/path"
//				)
//			},
//			{
//				input(
//					"Happy-2",
//					"http://foo.com:123","/c1/c2","/r1/r2","/p1/p2",
//					"http://foo.com:123",
//					"http://foo.com:123/c1/c2",
//					"http://foo.com:123/c1/c2/r1/r2",
//					"http://foo.com:123/c1/c2/r1/r2/p1/p2",
//					"/c1/c2",
//					"/c1/c2/r1/r2",
//					"/c1/c2/r1/r2/p1/p2"
//				)
//			},
//			{
//				input(
//					"NoAuthority-1",
//					"","/context","/resource","/path",
//					"/",
//					"/context",
//					"/context/resource",
//					"/context/resource/path",
//					"/context",
//					"/context/resource",
//					"/context/resource/path"
//				)
//			},
//			{
//				input(
//					"NoContext-1",
//					"http://foo.com:123","","/resource","/path",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123/resource",
//					"http://foo.com:123/resource/path",
//					"/",
//					"/resource",
//					"/resource/path"
//				)
//			},
//			{
//				input(
//					"NoResource-1",
//					"http://foo.com:123","/context","","/path",
//					"http://foo.com:123",
//					"http://foo.com:123/context",
//					"http://foo.com:123/context",
//					"http://foo.com:123/context/path",
//					"/context",
//					"/context",
//					"/context/path"
//				)
//			},
//			{
//				input(
//					"NoPath-1",
//					"http://foo.com:123","/context","/resource","",
//					"http://foo.com:123",
//					"http://foo.com:123/context",
//					"http://foo.com:123/context/resource",
//					"http://foo.com:123/context/resource",
//					"/context",
//					"/context/resource",
//					"/context/resource"
//				)
//			},
//			{
//				input(
//					"NoAuthorityNoContext-1",
//					"","","/resource","/path",
//					"/",
//					"/",
//					"/resource",
//					"/resource/path",
//					"/",
//					"/resource",
//					"/resource/path"
//				)
//			},
//			{
//				input(
//					"NoContextNoResource-1",
//					"http://foo.com:123","","","/path",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123",
//					"http://foo.com:123/path",
//					"/",
//					"/",
//					"/path"
//				)
//			},
//			{
//				input(
//					"NoAuthorityNoContextNoResource-1",
//					"","","","/path",
//					"/",
//					"/",
//					"/",
//					"/path",
//					"/",
//					"/",
//					"/path"
//				)
//			},
//			{
//				input(
//					"Nothing-1",
//					"","","","",
//					"/",
//					"/",
//					"/",
//					"/",
//					"/",
//					"/",
//					"/"
//				)
//			},
//		});		
//	}
//	
//	public static Input input(String label, String authority, String context, String resource, String path, 
//			String eAbsoluteAuthority, String eAbsoluteContext, String eAbsoluteResource, String eAbsolutePath, 
//			String eRootRelativeContext, String eRootRelativeResource, String eRootRelativePath) {
//		return new Input(label, authority, context, resource, path, eAbsoluteAuthority, eAbsoluteContext, eAbsoluteResource, eAbsolutePath, eRootRelativeContext, eRootRelativeResource, eRootRelativePath);
//	}
//	
//	public static class Input {
//		private final UriContext uriContext;
//		private final String label, eAbsoluteAuthority, eAbsoluteContext, eAbsoluteResource, eAbsolutePath, eRootRelativeContext, eRootRelativeResource, eRootRelativePath;
//		
//		public Input(String label, String authority, String context, String resource, String path, 
//					String eAbsoluteAuthority, String eAbsoluteContext, String eAbsoluteResource, String eAbsolutePath, 
//					String eRootRelativeContext, String eRootRelativeResource, String eRootRelativePath) {
//			this.label = label;
//			this.uriContext = new UriContext(authority, context, resource, path);
//			this.eAbsoluteAuthority = eAbsoluteAuthority;
//			this.eAbsoluteContext = eAbsoluteContext;
//			this.eAbsoluteResource = eAbsoluteResource;
//			this.eAbsolutePath = eAbsolutePath;
//			this.eRootRelativeContext = eRootRelativeContext;
//			this.eRootRelativeResource = eRootRelativeResource;
//			this.eRootRelativePath = eRootRelativePath;
//		}
//	}
//	
//	private Input in;
//	
//	public UriContextUriComboTest(Input in) throws Exception {
//		this.in = in;
//	}
//	
//	@Test
//	public void a1_testAbsoluteAuthority() {
//		assertEquals(in.eAbsoluteAuthority, in.uriContext.getAbsoluteAuthority(), "{0}: testAbsoluteAuthority() failed", in.label);
//	}
//
//	@Test
//	public void a2_testAbsoluteContext() {
//		assertEquals(in.eAbsoluteContext, in.uriContext.getAbsoluteContextRoot(), "{0}: testAbsoluteContext() failed", in.label);
//	}
//	
//	@Test
//	public void a3_testAbsoluteResource() {
//		assertEquals(in.eAbsoluteResource, in.uriContext.getAbsoluteServletPath(), "{0}: testAbsoluteResource() failed", in.label);
//	}
//	
//	@Test
//	public void a4_testAbsolutePath() {
//		assertEquals(in.eAbsolutePath, in.uriContext.getAbsolutePathInfo(), "{0}: testAbsolutePath() failed", in.label);
//	}
//	
//	@Test
//	public void a5_testRootRelativeContext() {
//		assertEquals(in.eRootRelativeContext, in.uriContext.getRootRelativeContextRoot(), "{0}: testRootRelativeContext() failed", in.label);
//	}
//	
//	@Test
//	public void a6_testRootRelativeResource() {
//		assertEquals(in.eRootRelativeResource, in.uriContext.getRootRelativeServletPath(), "{0}: testRootRelativeResource() failed", in.label);
//	}
//	
//	@Test
//	public void a7_testRootRelativePath() {
//		assertEquals(in.eRootRelativePath, in.uriContext.getRootRelativePathInfo(), "{0}: testRootRelativePath() failed", in.label);
//	}
}
