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
package org.apache.juneau.rest.mock2;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class MockServletRequest_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// URIs
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_uris_basic() {
		var req = MockServletRequest.create("GET", "/foo");
		assertBean(
			req,
			"contextPath,pathInfo,pathTranslated,queryString,requestURI,requestURL,servletPath",
			",/foo,/mock-path/foo,null,/foo,/foo,"
		);
	}

	@Test void a02_uris_full() {
		var req = MockServletRequest.create("GET", "http://localhost:8080/foo?bar=baz#quz");
		assertBean(
			req,
			"contextPath,pathInfo,pathTranslated,queryString,requestURI,requestURL,servletPath",
			",/foo,/mock-path/foo,bar=baz,/foo,http://localhost:8080/foo,"
		);
	}

	@Test void a03_uris_full2() {
		var req = MockServletRequest.create("GET", "http://localhost:8080/foo/bar/baz?bar=baz#quz");
		assertBean(
			req,
			"contextPath,pathInfo,pathTranslated,queryString,requestURI,requestURL,servletPath",
			",/foo/bar/baz,/mock-path/foo/bar/baz,bar=baz,/foo/bar/baz,http://localhost:8080/foo/bar/baz,"
		);
	}

	@Test void a04_uris_contextPath() {
		var req = MockServletRequest.create("GET", "http://localhost:8080/foo/bar/baz?bar=baz#quz").contextPath("/foo");
		assertBean(
			req,
			"contextPath,pathInfo,pathTranslated,queryString,requestURI,requestURL,servletPath",
			"/foo,/bar/baz,/mock-path/bar/baz,bar=baz,/foo/bar/baz,http://localhost:8080/foo/bar/baz,"
		);
	}

	@Test void a05_uris_servletPath() {
		assertBean(
			MockServletRequest.create("GET", "http://localhost:8080/foo/bar/baz?bar=baz#quz").servletPath("/foo"),
			"contextPath,pathInfo,pathTranslated,queryString,requestURI,requestURL,servletPath",
			",/bar/baz,/mock-path/bar/baz,bar=baz,/foo/bar/baz,http://localhost:8080/foo/bar/baz,/foo"
		);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query strings
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_query_basic() {
		var req = MockServletRequest.create("GET", "/foo?bar=baz&bing=qux");
		assertBean(
			req,
			"queryString,parameterMap,parameterNames",
			"bar=baz&bing=qux,{bar=[baz],bing=[qux]},[bar,bing]"
		);
		assertEquals("baz", req.getParameter("bar"));
		assertJson("['baz']", req.getParameterValues("bar"));
	}

	@Test void b02_query_multivalues() {
		var req = MockServletRequest.create("GET", "/foo?bar=baz&bar=bing");
		assertBean(
			req,
			"queryString,parameterMap,parameterNames",
			"bar=baz&bar=bing,{bar=[baz,bing]},[bar]"
		);
		assertEquals("baz", req.getParameter("bar"));
		assertJson("['baz','bing']", req.getParameterValues("bar"));
	}
}