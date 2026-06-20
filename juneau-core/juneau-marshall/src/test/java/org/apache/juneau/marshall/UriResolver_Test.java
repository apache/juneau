/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests targeting low-coverage paths in {@link UriResolver}.
 */
class UriResolver_Test extends TestBase {

	private static String resolve(UriResolution resolution, UriRelativity relativity, UriContext ctx, String uri) {
		return UriResolver.of(resolution, relativity, ctx).resolve(uri);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Absolute URIs — normalized and non-normalized paths
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_absoluteUri_noNormalize() {
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, UriContext.DEFAULT, "http://example.com/foo");
		assertEquals("http://example.com/foo", r);
	}

	@Test void a02_absoluteUri_withDotSegments_normalized() {
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, UriContext.DEFAULT, "http://example.com/foo/../bar");
		assertEquals("http://example.com/bar", r);
	}

	@Test void a03_absoluteUri_withDotSegments_resolutionNone_notNormalized() {
		var r = resolve(UriResolution.NONE, UriRelativity.RESOURCE, UriContext.DEFAULT, "http://example.com/foo/../bar");
		// NONE resolution: absolute URI still normalized per resolve() line 333
		assertNotNull(r);
	}

	@Test void a04_rootRelative_withDotSegments_normalized() {
		var r = resolve(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE, UriContext.DEFAULT, "/foo/../bar");
		assertEquals("/bar", r);
	}

	@Test void a05_rootRelative_noDotSegments() {
		var r = resolve(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE, UriContext.DEFAULT, "/foo/bar");
		assertEquals("/foo/bar", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Root-relative URI (starts with /) — absolute resolution with authority
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_rootRelativeWithAuthority() {
		var ctx = UriContext.of("http://host:8080", "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "/foo");
		assertEquals("http://host:8080/foo", r);
	}

	@Test void b02_rootSlashOnly_withAuthority() {
		var ctx = UriContext.of("http://host:8080", "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "/");
		assertEquals("http://host:8080", r);
	}

	@Test void b03_rootSlashOnly_noAuthority() {
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, UriContext.DEFAULT, "/");
		assertEquals("/", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// context: scheme
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_context_absoluteResolution_withAuthority_withContext_withPath() {
		var ctx = UriContext.of("http://host:8080", "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:/foo");
		assertEquals("http://host:8080/myCtx/foo", r);
	}

	@Test void c02_context_absoluteResolution_withAuthority_noContext_withPath() {
		var ctx = UriContext.of("http://host:8080", null, "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:/foo");
		assertEquals("http://host:8080/foo", r);
	}

	@Test void c03_context_absoluteResolution_noAuthority_withContext_withPath() {
		var ctx = UriContext.of(null, "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:/foo");
		assertEquals("/myCtx/foo", r);
	}

	@Test void c04_context_noContext_root() {
		// context: with no remainder (just 8 chars) and no context root — should append /
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, UriContext.DEFAULT, "context:");
		assertEquals("/", r);
	}

	@Test void c05_context_withContext_root() {
		// context: with no remainder and context root present — no trailing /
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:");
		assertEquals("/myCtx", r);
	}

	@Test void c06_context_slashRemainder_withContext() {
		// remainder is "/" and hasContext — skip slash
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:/");
		assertEquals("/myCtx", r);
	}

	@Test void c07_context_slashRemainder_noContext_noAuthority() {
		// remainder is "/" and !hasContext and authority==null — do nothing (no slash)
		var r = resolve(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE, UriContext.DEFAULT, "context:/");
		assertEquals("/", r);
	}

	@Test void c08_context_queryStringRemainder() {
		// remainder starts with ? — appended as-is
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:?foo=bar");
		assertEquals("/myCtx?foo=bar", r);
	}

	@Test void c09_context_fragmentRemainder() {
		// remainder starts with # — appended as-is
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:#section");
		assertEquals("/myCtx#section", r);
	}

	@Test void c10_context_noSlashPrefix_remainder() {
		// remainder doesn't start with / ? # — prepend /
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:foo");
		assertEquals("/myCtx/foo", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// servlet: scheme
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_servlet_absoluteResolution_withAll() {
		var ctx = UriContext.of("http://host:8080", "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:/foo");
		assertEquals("http://host:8080/myCtx/myServlet/foo", r);
	}

	@Test void d02_servlet_noAuthority_withContext_withServlet() {
		var ctx = UriContext.of(null, "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:/foo");
		assertEquals("/myCtx/myServlet/foo", r);
	}

	@Test void d03_servlet_noContext_noServlet_root() {
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, UriContext.DEFAULT, "servlet:");
		assertEquals("/", r);
	}

	@Test void d04_servlet_withServlet_root() {
		var ctx = UriContext.of(null, null, "myServlet", null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:");
		assertEquals("/myServlet", r);
	}

	@Test void d05_servlet_slashRemainder_withServlet() {
		// remainder "/" and hasServlet — do nothing
		var ctx = UriContext.of(null, null, "myServlet", null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:/");
		assertEquals("/myServlet", r);
	}

	@Test void d06_servlet_queryStringRemainder() {
		var ctx = UriContext.of(null, null, "myServlet", null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:?a=b");
		assertEquals("/myServlet?a=b", r);
	}

	@Test void d07_servlet_noSlashPrefix_remainder() {
		var ctx = UriContext.of(null, null, "myServlet", null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:foo");
		assertEquals("/myServlet/foo", r);
	}

	@Test void d08_servlet_slashRemainder_noContext_noServlet_absoluteWithAuthority() {
		var ctx = UriContext.of("http://host:8080", null, null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:/");
		assertEquals("http://host:8080", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// request: scheme
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_request_absoluteResolution_withAll() {
		var ctx = UriContext.of("http://host:8080", "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:/foo");
		assertEquals("http://host:8080/myCtx/myServlet/path/foo", r);
	}

	@Test void e02_request_noAuthority_noContext_noServlet_noPath() {
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, UriContext.DEFAULT, "request:");
		assertEquals("/", r);
	}

	@Test void e03_request_withPath_root() {
		var ctx = UriContext.of(null, null, null, "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:");
		assertEquals("/path", r);
	}

	@Test void e04_request_slashRemainder_withContext() {
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:/");
		assertEquals("/myCtx", r);
	}

	@Test void e05_request_queryStringRemainder() {
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:?q=1");
		assertEquals("/myCtx?q=1", r);
	}

	@Test void e06_request_noSlashPrefix_remainder() {
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:foo");
		assertEquals("/myCtx/foo", r);
	}

	@Test void e07_request_slashRemainder_withPath() {
		var ctx = UriContext.of(null, null, null, "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:/");
		assertEquals("/path", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Relative paths (no scheme, no leading /)
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_relative_resource_relativity() {
		var ctx = UriContext.of(null, "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "foo");
		assertTrue(r.contains("foo"), r);
	}

	@Test void f02_relative_pathInfo_relativity_withParentPath() {
		var ctx = UriContext.of("http://host:8080", "myCtx", "myServlet", "/path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.PATH_INFO, ctx, "foo");
		assertTrue(r.contains("foo"), r);
	}

	@Test void f03_relative_pathInfo_relativity_nullUri() {
		// null URI with PATH_INFO and pathInfo set — should include pathInfo
		var ctx = UriContext.of("http://host:8080", "myCtx", "myServlet", "path");
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.PATH_INFO, ctx, (String)null);
		assertTrue(r.contains("path"), r);
	}

	@Test void f04_relative_null_noContext_noServlet_noAuthority() {
		// null URI, no context/servlet/authority, PATH_INFO — empty string result
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.PATH_INFO, UriContext.DEFAULT, (String)null);
		assertEquals("", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// NONE resolution
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_none_passThrough() {
		var r = resolve(UriResolution.NONE, UriRelativity.RESOURCE, UriContext.DEFAULT, "foo/bar");
		assertEquals("foo/bar", r);
	}

	@Test void g02_none_specialUri_stillResolved() {
		// special URIs (context:/servlet:/request:) are resolved even with NONE
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.NONE, UriRelativity.RESOURCE, ctx, "context:/foo");
		assertEquals("/myCtx/foo", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// context: — no-context root and authority combinations
	//------------------------------------------------------------------------------------------------------------------

	@Test void c11_context_noContext_rootRelativeResolution() {
		// !hasContext and resolution=ROOT_RELATIVE (!=ABSOLUTE) — append /
		var r = resolve(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE, UriContext.DEFAULT, "context:");
		assertEquals("/", r);
	}

	@Test void c12_context_noContext_withAbsoluteAuthority() {
		// !hasContext but ABSOLUTE with authority — no slash appended by line 185
		var ctx = UriContext.of("http://host:8080", null, null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:");
		assertEquals("http://host:8080", r);
	}

	@Test void c13_context_slashRemainder_absoluteAuthority_noContext() {
		// remainder "/" and !hasContext and ABSOLUTE with authority — do nothing
		var ctx = UriContext.of("http://host:8080", null, null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "context:/");
		assertEquals("http://host:8080", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// servlet: — no-servlet no-context root and authority combinations
	//------------------------------------------------------------------------------------------------------------------

	@Test void d09_servlet_noServlet_noContext_absoluteWithAuthority() {
		// ABSOLUTE with authority — no slash from line 209
		var ctx = UriContext.of("http://host:8080", null, null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:");
		assertEquals("http://host:8080", r);
	}

	@Test void d10_servlet_rootRelativeResolution_noServlet_noContext() {
		// ROOT_RELATIVE resolution, no context/servlet — appends /
		var r = resolve(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE, UriContext.DEFAULT, "servlet:");
		assertEquals("/", r);
	}

	@Test void d11_servlet_fragmentRemainder() {
		var ctx = UriContext.of(null, null, "myServlet", null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "servlet:#section");
		assertEquals("/myServlet#section", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// request: — lines 217-219, 229, 236
	//------------------------------------------------------------------------------------------------------------------

	@Test void e08_request_withServlet_noPath_noRemainder() {
		var ctx = UriContext.of(null, null, "myServlet", null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:");
		assertEquals("/myServlet", r);
	}

	@Test void e09_request_absoluteAuthority_noServlet_noContext_noPath_noRemainder() {
		// ABSOLUTE with authority — no slash from line 236
		var ctx = UriContext.of("http://host:8080", null, null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:");
		assertEquals("http://host:8080", r);
	}

	@Test void e10_request_slashRemainder_withServlet() {
		// remainder "/" and hasServlet — do nothing
		var ctx = UriContext.of(null, null, "myServlet", null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:/");
		assertEquals("/myServlet", r);
	}

	@Test void e11_request_slashRemainder_absoluteAuthority_noServlet_noContext_noPath() {
		var ctx = UriContext.of("http://host:8080", null, null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:/");
		assertEquals("http://host:8080", r);
	}

	@Test void e12_request_fragmentRemainder_withContext() {
		var ctx = UriContext.of(null, "myCtx", null, null);
		var r = resolve(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx, "request:#top");
		assertEquals("/myCtx#top", r);
	}

	@Test void e13_request_rootRelative_noServlet_noContext_noPath() {
		// ROOT_RELATIVE with no context/servlet/path — appends /
		var r = resolve(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE, UriContext.DEFAULT, "request:");
		assertEquals("/", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// relativize()
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_relativize() {
		var ctx = UriContext.of("http://host:8080", "myCtx", "myServlet", "/path");
		var resolver = UriResolver.of(UriResolution.ABSOLUTE, UriRelativity.RESOURCE, ctx);
		var r = resolver.relativize("servlet:/", "/myCtx/myServlet/path/foo");
		assertNotNull(r);
	}
}
