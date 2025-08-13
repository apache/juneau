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
package org.apache.juneau.rest;

import org.apache.http.client.config.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class Paths_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	static JsonMap getPaths(RestRequest req) {
		return JsonMap.create()
			.append("pathInfo", req.getPathInfo())
			.append("pathRemainder", req.getPathParams().getRemainder().orElse(null))
			.append("pathRemainderUndecoded", req.getPathParams().getRemainderUndecoded().orElse(null))
			.append("requestURI", req.getRequestURI())
			.append("requestParentURI", req.getUriContext().getRootRelativePathInfoParent())
			.append("requestURL", req.getRequestURL())
			.append("servletPath", req.getServletPath())
			.append("servletURI", req.getUriContext().getRootRelativeServletPath())
			.append("servletParentURI", req.getUriContext().getRootRelativeServletPathParent());
	}

	//------------------------------------------------------------------------------------------------------------------
	// No subpath
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet(path="/*")
		public JsonMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",1);
		}
	}
	static MockRestClient a = MockRestClient.create(A.class).contextPath("/cp").servletPath("/sp").defaultRequestConfig(RequestConfig.custom().setNormalizeUri(false).build()).build();

	@Test void a01() throws Exception {
		a.get("http://localhost/cp/sp").run()
			.assertContent().isContains("pathInfo:null")
			.assertContent().isContains("pathRemainder:null")
			.assertContent().isContains("pathRemainderUndecoded:null")
			.assertContent().isContains("pathRemainder2:null")
			.assertContent().isContains("requestURI:'/cp/sp'")
			.assertContent().isContains("requestParentURI:'/cp'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}
	@Test void a02() throws Exception {
		a.get("http://localhost/cp/sp/").run()
			.assertContent().isContains("pathInfo:'/'")
			.assertContent().isContains("pathRemainder:''")
			.assertContent().isContains("pathRemainderUndecoded:''")
			.assertContent().isContains("pathRemainder2:''")
			.assertContent().isContains("requestURI:'/cp/sp/'")
			.assertContent().isContains("requestParentURI:'/cp'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}
	@Test void a03() throws Exception {
		a.get("http://localhost/cp/sp//").run()
			.assertContent().isContains("pathInfo:'//'")
			.assertContent().isContains("pathRemainder:'/'")
			.assertContent().isContains("pathRemainderUndecoded:'/'")
			.assertContent().isContains("pathRemainder2:'/'")
			.assertContent().isContains("requestURI:'/cp/sp//'")
			.assertContent().isContains("requestParentURI:'/cp'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp//'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}
	@Test void a04() throws Exception {
		a.get("http://localhost/cp/sp///").run()
			.assertContent().isContains("pathInfo:'///'")
			.assertContent().isContains("pathRemainder:'//'")
			.assertContent().isContains("pathRemainderUndecoded:'//'")
			.assertContent().isContains("pathRemainder2:'//'")
			.assertContent().isContains("requestURI:'/cp/sp///'")
			.assertContent().isContains("requestParentURI:'/cp'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp///'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}
	@Test void a05() throws Exception {
		a.get("http://localhost/cp/sp/foo/bar").run()
			.assertContent().isContains("pathInfo:'/foo/bar'")
			.assertContent().isContains("pathRemainder:'foo/bar'")
			.assertContent().isContains("pathRemainderUndecoded:'foo/bar'")
			.assertContent().isContains("pathRemainder2:'foo/bar'")
			.assertContent().isContains("requestURI:'/cp/sp/foo/bar'")
			.assertContent().isContains("requestParentURI:'/cp/sp/foo'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/foo/bar'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}
	@Test void a06() throws Exception {
		a.get("http://localhost/cp/sp/foo/bar/").run()
			.assertContent().isContains("pathInfo:'/foo/bar/'")
			.assertContent().isContains("pathRemainder:'foo/bar/'")
			.assertContent().isContains("pathRemainderUndecoded:'foo/bar/'")
			.assertContent().isContains("pathRemainder2:'foo/bar/'")
			.assertContent().isContains("requestURI:'/cp/sp/foo/bar/'")
			.assertContent().isContains("requestParentURI:'/cp/sp/foo'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/foo/bar/'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}
	@Test void a07() throws Exception {
		a.get("http://localhost/cp/sp//foo//bar//").run()
			.assertContent().isContains("pathInfo:'//foo//bar//'")
			.assertContent().isContains("pathRemainder:'/foo//bar//'")
			.assertContent().isContains("pathRemainderUndecoded:'/foo//bar//'")
			.assertContent().isContains("pathRemainder2:'/foo//bar//'")
			.assertContent().isContains("requestURI:'/cp/sp//foo//bar//'")
			.assertContent().isContains("requestParentURI:'/cp/sp/foo/'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp//foo//bar//'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}
	@Test void a08() throws Exception {
		a.get("http://localhost/cp/sp/%20").run()
			.assertContent().isContains("pathInfo:'/ '")
			.assertContent().isContains("pathRemainder:' '")
			.assertContent().isContains("pathRemainderUndecoded:'%20'")
			.assertContent().isContains("pathRemainder2:' '")
			.assertContent().isContains("requestURI:'/cp/sp/%20'")
			.assertContent().isContains("requestParentURI:'/cp/sp")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/%20'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}
	@Test void a09() throws Exception {
		a.get("http://localhost/cp/sp/+").run()
			.assertContent().isContains("pathInfo:'/ '")
			.assertContent().isContains("pathRemainder:' '")
			.assertContent().isContains("pathRemainderUndecoded:'+'")
			.assertContent().isContains("pathRemainder2:' '")
			.assertContent().isContains("requestURI:'/cp/sp/+'")
			.assertContent().isContains("requestParentURI:'/cp/sp")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/+'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:1")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Subpath in method
	//------------------------------------------------------------------------------------------------------------------

	public static class B {
		@RestGet(path="/subpath/*")
		public JsonMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",2);
		}
	}
	static MockRestClient b = MockRestClient.create(B.class).contextPath("/cp").servletPath("/sp").defaultRequestConfig(RequestConfig.custom().setNormalizeUri(false).build()).build();

	@Test void b01() throws Exception {
		b.get("http://localhost/cp/sp/subpath").run()
			.assertContent().isContains("pathInfo:'/subpath'")
			.assertContent().isContains("pathRemainder:null")
			.assertContent().isContains("pathRemainderUndecoded:null")
			.assertContent().isContains("pathRemainder2:null")
			.assertContent().isContains("requestURI:'/cp/sp/subpath'")
			.assertContent().isContains("requestParentURI:'/cp/sp'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}
	@Test void b02() throws Exception {
		b.get("http://localhost/cp/sp/subpath/").run()
			.assertContent().isContains("pathInfo:'/subpath/'")
			.assertContent().isContains("pathRemainder:''")
			.assertContent().isContains("pathRemainderUndecoded:''")
			.assertContent().isContains("pathRemainder2:''")
			.assertContent().isContains("requestURI:'/cp/sp/subpath/'")
			.assertContent().isContains("requestParentURI:'/cp/sp'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath/'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}
	@Test void b03() throws Exception {
		b.get("http://localhost/cp/sp/subpath//").run()
			.assertContent().isContains("pathInfo:'/subpath//'")
			.assertContent().isContains("pathRemainder:'/'")
			.assertContent().isContains("pathRemainderUndecoded:'/'")
			.assertContent().isContains("pathRemainder2:'/'")
			.assertContent().isContains("requestURI:'/cp/sp/subpath//'")
			.assertContent().isContains("requestParentURI:'/cp/sp'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath//'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}
	@Test void b04() throws Exception {
		b.get("http://localhost/cp/sp/subpath///").run()
			.assertContent().isContains("pathInfo:'/subpath///'")
			.assertContent().isContains("pathRemainder:'//'")
			.assertContent().isContains("pathRemainderUndecoded:'//'")
			.assertContent().isContains("pathRemainder2:'//'")
			.assertContent().isContains("requestURI:'/cp/sp/subpath///'")
			.assertContent().isContains("requestParentURI:'/cp/sp'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath///'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}
	@Test void b05() throws Exception {
		b.get("http://localhost/cp/sp/subpath/foo/bar").run()
			.assertContent().isContains("pathInfo:'/subpath/foo/bar'")
			.assertContent().isContains("pathRemainder:'foo/bar'")
			.assertContent().isContains("pathRemainderUndecoded:'foo/bar'")
			.assertContent().isContains("pathRemainder2:'foo/bar'")
			.assertContent().isContains("requestURI:'/cp/sp/subpath/foo/bar'")
			.assertContent().isContains("requestParentURI:'/cp/sp/subpath/foo'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath/foo/bar'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}
	@Test void b06() throws Exception {
		b.get("http://localhost/cp/sp/subpath/foo/bar/").run()
			.assertContent().isContains("pathInfo:'/subpath/foo/bar/'")
			.assertContent().isContains("pathRemainder:'foo/bar/'")
			.assertContent().isContains("pathRemainderUndecoded:'foo/bar/'")
			.assertContent().isContains("pathRemainder2:'foo/bar/'")
			.assertContent().isContains("requestURI:'/cp/sp/subpath/foo/bar/'")
			.assertContent().isContains("requestParentURI:'/cp/sp/subpath/foo'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath/foo/bar/'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}
	@Test void b07() throws Exception {
		b.get("http://localhost/cp/sp/subpath//foo//bar//").run()
			.assertContent().isContains("pathInfo:'/subpath//foo//bar//'")
			.assertContent().isContains("pathRemainder:'/foo//bar//'")
			.assertContent().isContains("pathRemainderUndecoded:'/foo//bar//'")
			.assertContent().isContains("pathRemainder2:'/foo//bar//'")
			.assertContent().isContains("requestURI:'/cp/sp/subpath//foo//bar//'")
			.assertContent().isContains("requestParentURI:'/cp/sp/subpath//foo/'")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath//foo//bar//'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}
	@Test void b08() throws Exception {
		b.get("http://localhost/cp/sp/subpath/%20").run()
			.assertContent().isContains("pathInfo:'/subpath/ '")
			.assertContent().isContains("pathRemainder:' '")
			.assertContent().isContains("pathRemainderUndecoded:'%20'")
			.assertContent().isContains("pathRemainder2:' '")
			.assertContent().isContains("requestURI:'/cp/sp/subpath/%20'")
			.assertContent().isContains("requestParentURI:'/cp/sp/subpath")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath/%20'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}
	@Test void b09() throws Exception {
		b.get("http://localhost/cp/sp/subpath/+").run()
			.assertContent().isContains("pathInfo:'/subpath/ '")
			.assertContent().isContains("pathRemainder:' '")
			.assertContent().isContains("pathRemainderUndecoded:'+'")
			.assertContent().isContains("pathRemainder2:' '")
			.assertContent().isContains("requestURI:'/cp/sp/subpath/+'")
			.assertContent().isContains("requestParentURI:'/cp/sp/subpath")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/subpath/+'")
			.assertContent().isContains("servletPath:'/sp'")
			.assertContent().isContains("servletURI:'/cp/sp'")
			.assertContent().isContains("method:2")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Child resource
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={C1.class})
	public static class C {}

	@Rest(path="/a")
	public static class C1 {
		@RestGet(path="/*")
		public JsonMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",3);
		}
	}
	static MockRestClient c = MockRestClient.create(C.class).contextPath("/cp").servletPath("/sp").defaultRequestConfig(RequestConfig.custom().setNormalizeUri(false).build()).build();

	@Test void c01() throws Exception {
		c.get("http://localhost/cp/sp/a").run()
			.assertContent().isContains("pathInfo:null")
			.assertContent().isContains("pathRemainder:null")
			.assertContent().isContains("pathRemainderUndecoded:null")
			.assertContent().isContains("pathRemainder2:null")
			.assertContent().isContains("requestURI:'/cp/sp/a'")
			.assertContent().isContains("requestParentURI:'/cp/sp")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}
	@Test void c02() throws Exception {
		c.get("http://localhost/cp/sp/a/").run()
			.assertContent().isContains("pathInfo:'/'")
			.assertContent().isContains("pathRemainder:''")
			.assertContent().isContains("pathRemainderUndecoded:''")
			.assertContent().isContains("pathRemainder2:''")
			.assertContent().isContains("requestURI:'/cp/sp/a/'")
			.assertContent().isContains("requestParentURI:'/cp/sp")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}
	@Test void c03() throws Exception {
		c.get("http://localhost/cp/sp/a//").run()
			.assertContent().isContains("pathInfo:'//'")
			.assertContent().isContains("pathRemainder:'/'")
			.assertContent().isContains("pathRemainderUndecoded:'/'")
			.assertContent().isContains("pathRemainder2:'/'")
			.assertContent().isContains("requestURI:'/cp/sp/a//'")
			.assertContent().isContains("requestParentURI:'/cp/sp")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a//'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}
	@Test void c04() throws Exception {
		c.get("http://localhost/cp/sp/a///").run()
			.assertContent().isContains("pathInfo:'///'")
			.assertContent().isContains("pathRemainder:'//'")
			.assertContent().isContains("pathRemainderUndecoded:'//'")
			.assertContent().isContains("pathRemainder2:'//'")
			.assertContent().isContains("requestURI:'/cp/sp/a///'")
			.assertContent().isContains("requestParentURI:'/cp/sp")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a///'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}
	@Test void c05() throws Exception {
		c.get("http://localhost/cp/sp/a/foo/bar").run()
			.assertContent().isContains("pathInfo:'/foo/bar'")
			.assertContent().isContains("pathRemainder:'foo/bar'")
			.assertContent().isContains("pathRemainderUndecoded:'foo/bar'")
			.assertContent().isContains("pathRemainder2:'foo/bar'")
			.assertContent().isContains("requestURI:'/cp/sp/a/foo/bar'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a/foo")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/foo/bar'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}
	@Test void c06() throws Exception {
		c.get("http://localhost/cp/sp/a/foo/bar/").run()
			.assertContent().isContains("pathInfo:'/foo/bar/'")
			.assertContent().isContains("pathRemainder:'foo/bar/'")
			.assertContent().isContains("pathRemainderUndecoded:'foo/bar/'")
			.assertContent().isContains("pathRemainder2:'foo/bar/'")
			.assertContent().isContains("requestURI:'/cp/sp/a/foo/bar/'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a/foo")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/foo/bar/'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}
	@Test void c07() throws Exception {
		c.get("http://localhost/cp/sp/a//foo//bar//").run()
			.assertContent().isContains("pathInfo:'//foo//bar//'")
			.assertContent().isContains("pathRemainder:'/foo//bar//'")
			.assertContent().isContains("pathRemainderUndecoded:'/foo//bar//'")
			.assertContent().isContains("pathRemainder2:'/foo//bar//'")
			.assertContent().isContains("requestURI:'/cp/sp/a//foo//bar//'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a/foo/")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a//foo//bar//'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}
	@Test void c08() throws Exception {
		c.get("http://localhost/cp/sp/a/%20").run()
			.assertContent().isContains("pathInfo:'/ '")
			.assertContent().isContains("pathRemainder:' '")
			.assertContent().isContains("pathRemainderUndecoded:'%20'")
			.assertContent().isContains("pathRemainder2:' '")
			.assertContent().isContains("requestURI:'/cp/sp/a/%20'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/%20'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}
	@Test void c09() throws Exception {
		c.get("http://localhost/cp/sp/a/+").run()
			.assertContent().isContains("pathInfo:'/ '")
			.assertContent().isContains("pathRemainder:' '")
			.assertContent().isContains("pathRemainderUndecoded:'+'")
			.assertContent().isContains("pathRemainder2:' '")
			.assertContent().isContains("requestURI:'/cp/sp/a/+'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/+'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:3")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Child resource and subpath in method
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={D1.class})
	public static class D {}

	@Rest(path="/a")
	public static class D1 {
		@RestGet(path="/subpath/*")
		public JsonMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",4);
		}
	}
	static MockRestClient d = MockRestClient.create(D.class).contextPath("/cp").servletPath("/sp").defaultRequestConfig(RequestConfig.custom().setNormalizeUri(false).build()).build();

	@Test void d01() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath").run()
			.assertContent().isContains("pathInfo:'/subpath'")
			.assertContent().isContains("pathRemainder:null")
			.assertContent().isContains("pathRemainderUndecoded:null")
			.assertContent().isContains("pathRemainder2:null")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
	@Test void d02() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/").run()
			.assertContent().isContains("pathInfo:'/subpath/'")
			.assertContent().isContains("pathRemainder:''")
			.assertContent().isContains("pathRemainderUndecoded:''")
			.assertContent().isContains("pathRemainder2:''")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath/'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath/'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
	@Test void d03() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath//").run()
			.assertContent().isContains("pathInfo:'/subpath//'")
			.assertContent().isContains("pathRemainder:'/'")
			.assertContent().isContains("pathRemainderUndecoded:'/'")
			.assertContent().isContains("pathRemainder2:'/'")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath//'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath//'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
	@Test void d04() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath///").run()
			.assertContent().isContains("pathInfo:'/subpath///'")
			.assertContent().isContains("pathRemainder:'//'")
			.assertContent().isContains("pathRemainderUndecoded:'//'")
			.assertContent().isContains("pathRemainder2:'//'")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath///'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath///'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
	@Test void d05() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/foo/bar").run()
			.assertContent().isContains("pathInfo:'/subpath/foo/bar'")
			.assertContent().isContains("pathRemainder:'foo/bar'")
			.assertContent().isContains("pathRemainderUndecoded:'foo/bar'")
			.assertContent().isContains("pathRemainder2:'foo/bar'")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath/foo/bar'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a/subpath/foo")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath/foo/bar'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
	@Test void d06() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/foo/bar/").run()
			.assertContent().isContains("pathInfo:'/subpath/foo/bar/'")
			.assertContent().isContains("pathRemainder:'foo/bar/'")
			.assertContent().isContains("pathRemainderUndecoded:'foo/bar/'")
			.assertContent().isContains("pathRemainder2:'foo/bar/'")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath/foo/bar/'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a/subpath/foo")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath/foo/bar/'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
	@Test void d07() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath//foo//bar//").run()
			.assertContent().isContains("pathInfo:'/subpath//foo//bar//'")
			.assertContent().isContains("pathRemainder:'/foo//bar//'")
			.assertContent().isContains("pathRemainderUndecoded:'/foo//bar//'")
			.assertContent().isContains("pathRemainder2:'/foo//bar//'")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath//foo//bar//'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a/subpath//foo/")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath//foo//bar//'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
	@Test void d08() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/%20").run()
			.assertContent().isContains("pathInfo:'/subpath/ '")
			.assertContent().isContains("pathRemainder:' '")
			.assertContent().isContains("pathRemainderUndecoded:'%20'")
			.assertContent().isContains("pathRemainder2:' '")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath/%20'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a/subpath")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath/%20'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
	@Test void d09() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/+").run()
			.assertContent().isContains("pathInfo:'/subpath/ '")
			.assertContent().isContains("pathRemainder:' '")
			.assertContent().isContains("pathRemainderUndecoded:'+'")
			.assertContent().isContains("pathRemainder2:' '")
			.assertContent().isContains("requestURI:'/cp/sp/a/subpath/+'")
			.assertContent().isContains("requestParentURI:'/cp/sp/a/subpath")
			.assertContent().isContains("requestURL:'http://localhost/cp/sp/a/subpath/+'")
			.assertContent().isContains("servletPath:'/sp/a'")
			.assertContent().isContains("servletURI:'/cp/sp/a'")
			.assertContent().isContains("method:4")
		;
	}
}