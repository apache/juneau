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

import static org.apache.juneau.http.HttpMethod.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Paths_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	static OMap getPaths(RestRequest req) {
		return OMap.create()
			.a("pathInfo", req.getPathInfo())
			.a("pathRemainder", req.getPathMatch().getRemainder())
			.a("pathRemainderUndecoded", req.getPathMatch().getRemainderUndecoded())
			.a("requestURI", req.getRequestURI())
			.a("requestParentURI", req.getUriContext().getRootRelativePathInfoParent())
			.a("requestURL", req.getRequestURL())
			.a("servletPath", req.getServletPath())
			.a("servletURI", req.getUriContext().getRootRelativeServletPath())
			.a("servletParentURI", req.getUriContext().getRootRelativeServletPathParent());
	}

	//------------------------------------------------------------------------------------------------------------------
	// No subpath
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestOp(method=GET,path="/*")
		public OMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",1);
		}
	}
	static MockRestClient a = MockRestClient.create(A.class).contextPath("/cp").servletPath("/sp").build();

	@Test
	public void a01() throws Exception {
		a.get("http://localhost/cp/sp").run()
			.assertBody().contains("pathInfo:null")
			.assertBody().contains("pathRemainder:null")
			.assertBody().contains("pathRemainderUndecoded:null")
			.assertBody().contains("pathRemainder2:null")
			.assertBody().contains("requestURI:'/cp/sp'")
			.assertBody().contains("requestParentURI:'/cp'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}
	@Test
	public void a02() throws Exception {
		a.get("http://localhost/cp/sp/").run()
			.assertBody().contains("pathInfo:'/'")
			.assertBody().contains("pathRemainder:''")
			.assertBody().contains("pathRemainderUndecoded:''")
			.assertBody().contains("pathRemainder2:''")
			.assertBody().contains("requestURI:'/cp/sp/'")
			.assertBody().contains("requestParentURI:'/cp'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}
	@Test
	public void a03() throws Exception {
		a.get("http://localhost/cp/sp//").run()
			.assertBody().contains("pathInfo:'//'")
			.assertBody().contains("pathRemainder:'/'")
			.assertBody().contains("pathRemainderUndecoded:'/'")
			.assertBody().contains("pathRemainder2:'/'")
			.assertBody().contains("requestURI:'/cp/sp//'")
			.assertBody().contains("requestParentURI:'/cp'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp//'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}
	@Test
	public void a04() throws Exception {
		a.get("http://localhost/cp/sp///").run()
			.assertBody().contains("pathInfo:'///'")
			.assertBody().contains("pathRemainder:'//'")
			.assertBody().contains("pathRemainderUndecoded:'//'")
			.assertBody().contains("pathRemainder2:'//'")
			.assertBody().contains("requestURI:'/cp/sp///'")
			.assertBody().contains("requestParentURI:'/cp'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp///'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}
	@Test
	public void a05() throws Exception {
		a.get("http://localhost/cp/sp/foo/bar").run()
			.assertBody().contains("pathInfo:'/foo/bar'")
			.assertBody().contains("pathRemainder:'foo/bar'")
			.assertBody().contains("pathRemainderUndecoded:'foo/bar'")
			.assertBody().contains("pathRemainder2:'foo/bar'")
			.assertBody().contains("requestURI:'/cp/sp/foo/bar'")
			.assertBody().contains("requestParentURI:'/cp/sp/foo'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/foo/bar'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}
	@Test
	public void a06() throws Exception {
		a.get("http://localhost/cp/sp/foo/bar/").run()
			.assertBody().contains("pathInfo:'/foo/bar/'")
			.assertBody().contains("pathRemainder:'foo/bar/'")
			.assertBody().contains("pathRemainderUndecoded:'foo/bar/'")
			.assertBody().contains("pathRemainder2:'foo/bar/'")
			.assertBody().contains("requestURI:'/cp/sp/foo/bar/'")
			.assertBody().contains("requestParentURI:'/cp/sp/foo'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/foo/bar/'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}
	@Test
	public void a07() throws Exception {
		a.get("http://localhost/cp/sp//foo//bar//").run()
			.assertBody().contains("pathInfo:'//foo//bar//'")
			.assertBody().contains("pathRemainder:'/foo//bar//'")
			.assertBody().contains("pathRemainderUndecoded:'/foo//bar//'")
			.assertBody().contains("pathRemainder2:'/foo//bar//'")
			.assertBody().contains("requestURI:'/cp/sp//foo//bar//'")
			.assertBody().contains("requestParentURI:'/cp/sp/foo/'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp//foo//bar//'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}
	@Test
	public void a08() throws Exception {
		a.get("http://localhost/cp/sp/%20").run()
			.assertBody().contains("pathInfo:'/ '")
			.assertBody().contains("pathRemainder:' '")
			.assertBody().contains("pathRemainderUndecoded:'%20'")
			.assertBody().contains("pathRemainder2:' '")
			.assertBody().contains("requestURI:'/cp/sp/%20'")
			.assertBody().contains("requestParentURI:'/cp/sp")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/%20'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}
	@Test
	public void a09() throws Exception {
		a.get("http://localhost/cp/sp/+").run()
			.assertBody().contains("pathInfo:'/ '")
			.assertBody().contains("pathRemainder:' '")
			.assertBody().contains("pathRemainderUndecoded:'+'")
			.assertBody().contains("pathRemainder2:' '")
			.assertBody().contains("requestURI:'/cp/sp/+'")
			.assertBody().contains("requestParentURI:'/cp/sp")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/+'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:1")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Subpath in method
	//------------------------------------------------------------------------------------------------------------------

	public static class B {
		@RestOp(method=GET, path="/subpath/*")
		public OMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",2);
		}
	}
	static MockRestClient b = MockRestClient.create(B.class).contextPath("/cp").servletPath("/sp").build();

	@Test
	public void b01() throws Exception {
		b.get("http://localhost/cp/sp/subpath").run()
			.assertBody().contains("pathInfo:'/subpath'")
			.assertBody().contains("pathRemainder:null")
			.assertBody().contains("pathRemainderUndecoded:null")
			.assertBody().contains("pathRemainder2:null")
			.assertBody().contains("requestURI:'/cp/sp/subpath'")
			.assertBody().contains("requestParentURI:'/cp/sp'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}
	@Test
	public void b02() throws Exception {
		b.get("http://localhost/cp/sp/subpath/").run()
			.assertBody().contains("pathInfo:'/subpath/'")
			.assertBody().contains("pathRemainder:''")
			.assertBody().contains("pathRemainderUndecoded:''")
			.assertBody().contains("pathRemainder2:''")
			.assertBody().contains("requestURI:'/cp/sp/subpath/'")
			.assertBody().contains("requestParentURI:'/cp/sp'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath/'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}
	@Test
	public void b03() throws Exception {
		b.get("http://localhost/cp/sp/subpath//").run()
			.assertBody().contains("pathInfo:'/subpath//'")
			.assertBody().contains("pathRemainder:'/'")
			.assertBody().contains("pathRemainderUndecoded:'/'")
			.assertBody().contains("pathRemainder2:'/'")
			.assertBody().contains("requestURI:'/cp/sp/subpath//'")
			.assertBody().contains("requestParentURI:'/cp/sp'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath//'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}
	@Test
	public void b04() throws Exception {
		b.get("http://localhost/cp/sp/subpath///").run()
			.assertBody().contains("pathInfo:'/subpath///'")
			.assertBody().contains("pathRemainder:'//'")
			.assertBody().contains("pathRemainderUndecoded:'//'")
			.assertBody().contains("pathRemainder2:'//'")
			.assertBody().contains("requestURI:'/cp/sp/subpath///'")
			.assertBody().contains("requestParentURI:'/cp/sp'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath///'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}
	@Test
	public void b05() throws Exception {
		b.get("http://localhost/cp/sp/subpath/foo/bar").run()
			.assertBody().contains("pathInfo:'/subpath/foo/bar'")
			.assertBody().contains("pathRemainder:'foo/bar'")
			.assertBody().contains("pathRemainderUndecoded:'foo/bar'")
			.assertBody().contains("pathRemainder2:'foo/bar'")
			.assertBody().contains("requestURI:'/cp/sp/subpath/foo/bar'")
			.assertBody().contains("requestParentURI:'/cp/sp/subpath/foo'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath/foo/bar'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}
	@Test
	public void b06() throws Exception {
		b.get("http://localhost/cp/sp/subpath/foo/bar/").run()
			.assertBody().contains("pathInfo:'/subpath/foo/bar/'")
			.assertBody().contains("pathRemainder:'foo/bar/'")
			.assertBody().contains("pathRemainderUndecoded:'foo/bar/'")
			.assertBody().contains("pathRemainder2:'foo/bar/'")
			.assertBody().contains("requestURI:'/cp/sp/subpath/foo/bar/'")
			.assertBody().contains("requestParentURI:'/cp/sp/subpath/foo'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath/foo/bar/'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}
	@Test
	public void b07() throws Exception {
		b.get("http://localhost/cp/sp/subpath//foo//bar//").run()
			.assertBody().contains("pathInfo:'/subpath//foo//bar//'")
			.assertBody().contains("pathRemainder:'/foo//bar//'")
			.assertBody().contains("pathRemainderUndecoded:'/foo//bar//'")
			.assertBody().contains("pathRemainder2:'/foo//bar//'")
			.assertBody().contains("requestURI:'/cp/sp/subpath//foo//bar//'")
			.assertBody().contains("requestParentURI:'/cp/sp/subpath//foo/'")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath//foo//bar//'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}
	@Test
	public void b08() throws Exception {
		b.get("http://localhost/cp/sp/subpath/%20").run()
			.assertBody().contains("pathInfo:'/subpath/ '")
			.assertBody().contains("pathRemainder:' '")
			.assertBody().contains("pathRemainderUndecoded:'%20'")
			.assertBody().contains("pathRemainder2:' '")
			.assertBody().contains("requestURI:'/cp/sp/subpath/%20'")
			.assertBody().contains("requestParentURI:'/cp/sp/subpath")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath/%20'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}
	@Test
	public void b09() throws Exception {
		b.get("http://localhost/cp/sp/subpath/+").run()
			.assertBody().contains("pathInfo:'/subpath/ '")
			.assertBody().contains("pathRemainder:' '")
			.assertBody().contains("pathRemainderUndecoded:'+'")
			.assertBody().contains("pathRemainder2:' '")
			.assertBody().contains("requestURI:'/cp/sp/subpath/+'")
			.assertBody().contains("requestParentURI:'/cp/sp/subpath")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/subpath/+'")
			.assertBody().contains("servletPath:'/sp'")
			.assertBody().contains("servletURI:'/cp/sp'")
			.assertBody().contains("method:2")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Child resource
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={C1.class})
	public static class C {}

	@Rest(path="/a")
	public static class C1 {
		@RestOp(method=GET,path="/*")
		public OMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",3);
		}
	}
	static MockRestClient c = MockRestClient.create(C.class).contextPath("/cp").servletPath("/sp").build();

	@Test
	public void c01() throws Exception {
		c.get("http://localhost/cp/sp/a").run()
			.assertBody().contains("pathInfo:null")
			.assertBody().contains("pathRemainder:null")
			.assertBody().contains("pathRemainderUndecoded:null")
			.assertBody().contains("pathRemainder2:null")
			.assertBody().contains("requestURI:'/cp/sp/a'")
			.assertBody().contains("requestParentURI:'/cp/sp")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}
	@Test
	public void c02() throws Exception {
		c.get("http://localhost/cp/sp/a/").run()
			.assertBody().contains("pathInfo:'/'")
			.assertBody().contains("pathRemainder:''")
			.assertBody().contains("pathRemainderUndecoded:''")
			.assertBody().contains("pathRemainder2:''")
			.assertBody().contains("requestURI:'/cp/sp/a/'")
			.assertBody().contains("requestParentURI:'/cp/sp")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}
	@Test
	public void c03() throws Exception {
		c.get("http://localhost/cp/sp/a//").run()
			.assertBody().contains("pathInfo:'//'")
			.assertBody().contains("pathRemainder:'/'")
			.assertBody().contains("pathRemainderUndecoded:'/'")
			.assertBody().contains("pathRemainder2:'/'")
			.assertBody().contains("requestURI:'/cp/sp/a//'")
			.assertBody().contains("requestParentURI:'/cp/sp")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a//'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}
	@Test
	public void c04() throws Exception {
		c.get("http://localhost/cp/sp/a///").run()
			.assertBody().contains("pathInfo:'///'")
			.assertBody().contains("pathRemainder:'//'")
			.assertBody().contains("pathRemainderUndecoded:'//'")
			.assertBody().contains("pathRemainder2:'//'")
			.assertBody().contains("requestURI:'/cp/sp/a///'")
			.assertBody().contains("requestParentURI:'/cp/sp")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a///'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}
	@Test
	public void c05() throws Exception {
		c.get("http://localhost/cp/sp/a/foo/bar").run()
			.assertBody().contains("pathInfo:'/foo/bar'")
			.assertBody().contains("pathRemainder:'foo/bar'")
			.assertBody().contains("pathRemainderUndecoded:'foo/bar'")
			.assertBody().contains("pathRemainder2:'foo/bar'")
			.assertBody().contains("requestURI:'/cp/sp/a/foo/bar'")
			.assertBody().contains("requestParentURI:'/cp/sp/a/foo")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/foo/bar'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}
	@Test
	public void c06() throws Exception {
		c.get("http://localhost/cp/sp/a/foo/bar/").run()
			.assertBody().contains("pathInfo:'/foo/bar/'")
			.assertBody().contains("pathRemainder:'foo/bar/'")
			.assertBody().contains("pathRemainderUndecoded:'foo/bar/'")
			.assertBody().contains("pathRemainder2:'foo/bar/'")
			.assertBody().contains("requestURI:'/cp/sp/a/foo/bar/'")
			.assertBody().contains("requestParentURI:'/cp/sp/a/foo")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/foo/bar/'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}
	@Test
	public void c07() throws Exception {
		c.get("http://localhost/cp/sp/a//foo//bar//").run()
			.assertBody().contains("pathInfo:'//foo//bar//'")
			.assertBody().contains("pathRemainder:'/foo//bar//'")
			.assertBody().contains("pathRemainderUndecoded:'/foo//bar//'")
			.assertBody().contains("pathRemainder2:'/foo//bar//'")
			.assertBody().contains("requestURI:'/cp/sp/a//foo//bar//'")
			.assertBody().contains("requestParentURI:'/cp/sp/a/foo/")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a//foo//bar//'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}
	@Test
	public void c08() throws Exception {
		c.get("http://localhost/cp/sp/a/%20").run()
			.assertBody().contains("pathInfo:'/ '")
			.assertBody().contains("pathRemainder:' '")
			.assertBody().contains("pathRemainderUndecoded:'%20'")
			.assertBody().contains("pathRemainder2:' '")
			.assertBody().contains("requestURI:'/cp/sp/a/%20'")
			.assertBody().contains("requestParentURI:'/cp/sp/a")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/%20'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}
	@Test
	public void c09() throws Exception {
		c.get("http://localhost/cp/sp/a/+").run()
			.assertBody().contains("pathInfo:'/ '")
			.assertBody().contains("pathRemainder:' '")
			.assertBody().contains("pathRemainderUndecoded:'+'")
			.assertBody().contains("pathRemainder2:' '")
			.assertBody().contains("requestURI:'/cp/sp/a/+'")
			.assertBody().contains("requestParentURI:'/cp/sp/a")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/+'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:3")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Child resource and subpath in method
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={D1.class})
	public static class D {}

	@Rest(path="/a")
	public static class D1 {
		@RestOp(method=GET, path="/subpath/*")
		public OMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",4);
		}
	}
	static MockRestClient d = MockRestClient.create(D.class).contextPath("/cp").servletPath("/sp").build();

	@Test
	public void d01() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath").run()
			.assertBody().contains("pathInfo:'/subpath'")
			.assertBody().contains("pathRemainder:null")
			.assertBody().contains("pathRemainderUndecoded:null")
			.assertBody().contains("pathRemainder2:null")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath'")
			.assertBody().contains("requestParentURI:'/cp/sp/a")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
	@Test
	public void d02() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/").run()
			.assertBody().contains("pathInfo:'/subpath/'")
			.assertBody().contains("pathRemainder:''")
			.assertBody().contains("pathRemainderUndecoded:''")
			.assertBody().contains("pathRemainder2:''")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath/'")
			.assertBody().contains("requestParentURI:'/cp/sp/a")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath/'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
	@Test
	public void d03() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath//").run()
			.assertBody().contains("pathInfo:'/subpath//'")
			.assertBody().contains("pathRemainder:'/'")
			.assertBody().contains("pathRemainderUndecoded:'/'")
			.assertBody().contains("pathRemainder2:'/'")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath//'")
			.assertBody().contains("requestParentURI:'/cp/sp/a")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath//'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
	@Test
	public void d04() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath///").run()
			.assertBody().contains("pathInfo:'/subpath///'")
			.assertBody().contains("pathRemainder:'//'")
			.assertBody().contains("pathRemainderUndecoded:'//'")
			.assertBody().contains("pathRemainder2:'//'")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath///'")
			.assertBody().contains("requestParentURI:'/cp/sp/a")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath///'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
	@Test
	public void d05() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/foo/bar").run()
			.assertBody().contains("pathInfo:'/subpath/foo/bar'")
			.assertBody().contains("pathRemainder:'foo/bar'")
			.assertBody().contains("pathRemainderUndecoded:'foo/bar'")
			.assertBody().contains("pathRemainder2:'foo/bar'")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath/foo/bar'")
			.assertBody().contains("requestParentURI:'/cp/sp/a/subpath/foo")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath/foo/bar'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
	@Test
	public void d06() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/foo/bar/").run()
			.assertBody().contains("pathInfo:'/subpath/foo/bar/'")
			.assertBody().contains("pathRemainder:'foo/bar/'")
			.assertBody().contains("pathRemainderUndecoded:'foo/bar/'")
			.assertBody().contains("pathRemainder2:'foo/bar/'")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath/foo/bar/'")
			.assertBody().contains("requestParentURI:'/cp/sp/a/subpath/foo")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath/foo/bar/'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
	@Test
	public void d07() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath//foo//bar//").run()
			.assertBody().contains("pathInfo:'/subpath//foo//bar//'")
			.assertBody().contains("pathRemainder:'/foo//bar//'")
			.assertBody().contains("pathRemainderUndecoded:'/foo//bar//'")
			.assertBody().contains("pathRemainder2:'/foo//bar//'")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath//foo//bar//'")
			.assertBody().contains("requestParentURI:'/cp/sp/a/subpath//foo/")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath//foo//bar//'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
	@Test
	public void d08() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/%20").run()
			.assertBody().contains("pathInfo:'/subpath/ '")
			.assertBody().contains("pathRemainder:' '")
			.assertBody().contains("pathRemainderUndecoded:'%20'")
			.assertBody().contains("pathRemainder2:' '")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath/%20'")
			.assertBody().contains("requestParentURI:'/cp/sp/a/subpath")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath/%20'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
	@Test
	public void d09() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/+").run()
			.assertBody().contains("pathInfo:'/subpath/ '")
			.assertBody().contains("pathRemainder:' '")
			.assertBody().contains("pathRemainderUndecoded:'+'")
			.assertBody().contains("pathRemainder2:' '")
			.assertBody().contains("requestURI:'/cp/sp/a/subpath/+'")
			.assertBody().contains("requestParentURI:'/cp/sp/a/subpath")
			.assertBody().contains("requestURL:'http://localhost/cp/sp/a/subpath/+'")
			.assertBody().contains("servletPath:'/sp/a'")
			.assertBody().contains("servletURI:'/cp/sp/a'")
			.assertBody().contains("method:4")
		;
	}
}