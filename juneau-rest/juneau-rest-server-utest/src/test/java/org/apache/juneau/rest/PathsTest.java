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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests various aspects of URL path parts.
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PathsTest {

	//=================================================================================================================
	// Setup
	//=================================================================================================================

	static ObjectMap getPaths(RestRequest req) {
		return new ObjectMap()
			.append("pathInfo", req.getPathInfo())
			.append("pathRemainder", req.getPathMatch().getRemainder())
			.append("pathRemainderUndecoded", req.getPathMatch().getRemainderUndecoded())
			.append("requestURI", req.getRequestURI())
			.append("requestParentURI", req.getUriContext().getRootRelativePathInfoParent())
			.append("requestURL", req.getRequestURL())
			.append("servletPath", req.getServletPath())
			.append("servletURI", req.getUriContext().getRootRelativeServletPath())
			.append("servletParentURI", req.getUriContext().getRootRelativeServletPathParent());
	}

	//=================================================================================================================
	// No subpath
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod(name=GET,path="/*")
		public ObjectMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",1);
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01() throws Exception {
		a.get("http://localhost/cp/sp").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:null")
			.assertBodyContains("pathRemainder:null")
			.assertBodyContains("pathRemainderUndecoded:null")
			.assertBodyContains("pathRemainder2:null")
			.assertBodyContains("requestURI:'/cp/sp'")
			.assertBodyContains("requestParentURI:'/cp'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}
	@Test
	public void a02() throws Exception {
		a.get("http://localhost/cp/sp/").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/'")
			.assertBodyContains("pathRemainder:''")
			.assertBodyContains("pathRemainderUndecoded:''")
			.assertBodyContains("pathRemainder2:''")
			.assertBodyContains("requestURI:'/cp/sp/'")
			.assertBodyContains("requestParentURI:'/cp'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}
	@Test
	public void a03() throws Exception {
		a.get("http://localhost/cp/sp//").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'//'")
			.assertBodyContains("pathRemainder:'/'")
			.assertBodyContains("pathRemainderUndecoded:'/'")
			.assertBodyContains("pathRemainder2:'/'")
			.assertBodyContains("requestURI:'/cp/sp//'")
			.assertBodyContains("requestParentURI:'/cp'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp//'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}
	@Test
	public void a04() throws Exception {
		a.get("http://localhost/cp/sp///").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'///'")
			.assertBodyContains("pathRemainder:'//'")
			.assertBodyContains("pathRemainderUndecoded:'//'")
			.assertBodyContains("pathRemainder2:'//'")
			.assertBodyContains("requestURI:'/cp/sp///'")
			.assertBodyContains("requestParentURI:'/cp'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp///'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}
	@Test
	public void a05() throws Exception {
		a.get("http://localhost/cp/sp/foo/bar").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/foo/bar'")
			.assertBodyContains("pathRemainder:'foo/bar'")
			.assertBodyContains("pathRemainderUndecoded:'foo/bar'")
			.assertBodyContains("pathRemainder2:'foo/bar'")
			.assertBodyContains("requestURI:'/cp/sp/foo/bar'")
			.assertBodyContains("requestParentURI:'/cp/sp/foo'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/foo/bar'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}
	@Test
	public void a06() throws Exception {
		a.get("http://localhost/cp/sp/foo/bar/").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/foo/bar/'")
			.assertBodyContains("pathRemainder:'foo/bar/'")
			.assertBodyContains("pathRemainderUndecoded:'foo/bar/'")
			.assertBodyContains("pathRemainder2:'foo/bar/'")
			.assertBodyContains("requestURI:'/cp/sp/foo/bar/'")
			.assertBodyContains("requestParentURI:'/cp/sp/foo'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/foo/bar/'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}
	@Test
	public void a07() throws Exception {
		a.get("http://localhost/cp/sp//foo//bar//").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'//foo//bar//'")
			.assertBodyContains("pathRemainder:'/foo//bar//'")
			.assertBodyContains("pathRemainderUndecoded:'/foo//bar//'")
			.assertBodyContains("pathRemainder2:'/foo//bar//'")
			.assertBodyContains("requestURI:'/cp/sp//foo//bar//'")
			.assertBodyContains("requestParentURI:'/cp/sp/foo/'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp//foo//bar//'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}
	@Test
	public void a08() throws Exception {
		a.get("http://localhost/cp/sp/%20").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/ '")
			.assertBodyContains("pathRemainder:' '")
			.assertBodyContains("pathRemainderUndecoded:'%20'")
			.assertBodyContains("pathRemainder2:' '")
			.assertBodyContains("requestURI:'/cp/sp/%20'")
			.assertBodyContains("requestParentURI:'/cp/sp")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/%20'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}
	@Test
	public void a09() throws Exception {
		a.get("http://localhost/cp/sp/+").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/ '")
			.assertBodyContains("pathRemainder:' '")
			.assertBodyContains("pathRemainderUndecoded:'+'")
			.assertBodyContains("pathRemainder2:' '")
			.assertBodyContains("requestURI:'/cp/sp/+'")
			.assertBodyContains("requestParentURI:'/cp/sp")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/+'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:1")
		;
	}

	//=================================================================================================================
	// Subpath in method
	//=================================================================================================================

	public static class B {
		@RestMethod(name=GET, path="/subpath/*")
		public ObjectMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",2);
		}
	}
	static MockRest b = MockRest.build(B.class, null);

	@Test
	public void b01() throws Exception {
		b.get("http://localhost/cp/sp/subpath").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath'")
			.assertBodyContains("pathRemainder:null")
			.assertBodyContains("pathRemainderUndecoded:null")
			.assertBodyContains("pathRemainder2:null")
			.assertBodyContains("requestURI:'/cp/sp/subpath'")
			.assertBodyContains("requestParentURI:'/cp/sp'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}
	@Test
	public void b02() throws Exception {
		b.get("http://localhost/cp/sp/subpath/").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/'")
			.assertBodyContains("pathRemainder:''")
			.assertBodyContains("pathRemainderUndecoded:''")
			.assertBodyContains("pathRemainder2:''")
			.assertBodyContains("requestURI:'/cp/sp/subpath/'")
			.assertBodyContains("requestParentURI:'/cp/sp'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath/'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}
	@Test
	public void b03() throws Exception {
		b.get("http://localhost/cp/sp/subpath//").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath//'")
			.assertBodyContains("pathRemainder:'/'")
			.assertBodyContains("pathRemainderUndecoded:'/'")
			.assertBodyContains("pathRemainder2:'/'")
			.assertBodyContains("requestURI:'/cp/sp/subpath//'")
			.assertBodyContains("requestParentURI:'/cp/sp'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath//'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}
	@Test
	public void b04() throws Exception {
		b.get("http://localhost/cp/sp/subpath///").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath///'")
			.assertBodyContains("pathRemainder:'//'")
			.assertBodyContains("pathRemainderUndecoded:'//'")
			.assertBodyContains("pathRemainder2:'//'")
			.assertBodyContains("requestURI:'/cp/sp/subpath///'")
			.assertBodyContains("requestParentURI:'/cp/sp'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath///'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}
	@Test
	public void b05() throws Exception {
		b.get("http://localhost/cp/sp/subpath/foo/bar").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/foo/bar'")
			.assertBodyContains("pathRemainder:'foo/bar'")
			.assertBodyContains("pathRemainderUndecoded:'foo/bar'")
			.assertBodyContains("pathRemainder2:'foo/bar'")
			.assertBodyContains("requestURI:'/cp/sp/subpath/foo/bar'")
			.assertBodyContains("requestParentURI:'/cp/sp/subpath/foo'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath/foo/bar'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}
	@Test
	public void b06() throws Exception {
		b.get("http://localhost/cp/sp/subpath/foo/bar/").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/foo/bar/'")
			.assertBodyContains("pathRemainder:'foo/bar/'")
			.assertBodyContains("pathRemainderUndecoded:'foo/bar/'")
			.assertBodyContains("pathRemainder2:'foo/bar/'")
			.assertBodyContains("requestURI:'/cp/sp/subpath/foo/bar/'")
			.assertBodyContains("requestParentURI:'/cp/sp/subpath/foo'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath/foo/bar/'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}
	@Test
	public void b07() throws Exception {
		b.get("http://localhost/cp/sp/subpath//foo//bar//").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath//foo//bar//'")
			.assertBodyContains("pathRemainder:'/foo//bar//'")
			.assertBodyContains("pathRemainderUndecoded:'/foo//bar//'")
			.assertBodyContains("pathRemainder2:'/foo//bar//'")
			.assertBodyContains("requestURI:'/cp/sp/subpath//foo//bar//'")
			.assertBodyContains("requestParentURI:'/cp/sp/subpath//foo/'")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath//foo//bar//'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}
	@Test
	public void b08() throws Exception {
		b.get("http://localhost/cp/sp/subpath/%20").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/ '")
			.assertBodyContains("pathRemainder:' '")
			.assertBodyContains("pathRemainderUndecoded:'%20'")
			.assertBodyContains("pathRemainder2:' '")
			.assertBodyContains("requestURI:'/cp/sp/subpath/%20'")
			.assertBodyContains("requestParentURI:'/cp/sp/subpath")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath/%20'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}
	@Test
	public void b09() throws Exception {
		b.get("http://localhost/cp/sp/subpath/+").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/ '")
			.assertBodyContains("pathRemainder:' '")
			.assertBodyContains("pathRemainderUndecoded:'+'")
			.assertBodyContains("pathRemainder2:' '")
			.assertBodyContains("requestURI:'/cp/sp/subpath/+'")
			.assertBodyContains("requestParentURI:'/cp/sp/subpath")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/subpath/+'")
			.assertBodyContains("servletPath:'/sp'")
			.assertBodyContains("servletURI:'/cp/sp'")
			.assertBodyContains("method:2")
		;
	}

	//=================================================================================================================
	// Child resource
	//=================================================================================================================

	@Rest(children={C01.class})
	public static class C {}

	@Rest(path="/a")
	public static class C01 {
		@RestMethod(name=GET,path="/*")
		public ObjectMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",3);
		}
	}
	static MockRest c = MockRest.build(C.class, null);

	@Test
	public void c01() throws Exception {
		c.get("http://localhost/cp/sp/a").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:null")
			.assertBodyContains("pathRemainder:null")
			.assertBodyContains("pathRemainderUndecoded:null")
			.assertBodyContains("pathRemainder2:null")
			.assertBodyContains("requestURI:'/cp/sp/a'")
			.assertBodyContains("requestParentURI:'/cp/sp")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}
	@Test
	public void c02() throws Exception {
		c.get("http://localhost/cp/sp/a/").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/'")
			.assertBodyContains("pathRemainder:''")
			.assertBodyContains("pathRemainderUndecoded:''")
			.assertBodyContains("pathRemainder2:''")
			.assertBodyContains("requestURI:'/cp/sp/a/'")
			.assertBodyContains("requestParentURI:'/cp/sp")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}
	@Test
	public void c03() throws Exception {
		c.get("http://localhost/cp/sp/a//").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'//'")
			.assertBodyContains("pathRemainder:'/'")
			.assertBodyContains("pathRemainderUndecoded:'/'")
			.assertBodyContains("pathRemainder2:'/'")
			.assertBodyContains("requestURI:'/cp/sp/a//'")
			.assertBodyContains("requestParentURI:'/cp/sp")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a//'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}
	@Test
	public void c04() throws Exception {
		c.get("http://localhost/cp/sp/a///").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'///'")
			.assertBodyContains("pathRemainder:'//'")
			.assertBodyContains("pathRemainderUndecoded:'//'")
			.assertBodyContains("pathRemainder2:'//'")
			.assertBodyContains("requestURI:'/cp/sp/a///'")
			.assertBodyContains("requestParentURI:'/cp/sp")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a///'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}
	@Test
	public void c05() throws Exception {
		c.get("http://localhost/cp/sp/a/foo/bar").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/foo/bar'")
			.assertBodyContains("pathRemainder:'foo/bar'")
			.assertBodyContains("pathRemainderUndecoded:'foo/bar'")
			.assertBodyContains("pathRemainder2:'foo/bar'")
			.assertBodyContains("requestURI:'/cp/sp/a/foo/bar'")
			.assertBodyContains("requestParentURI:'/cp/sp/a/foo")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/foo/bar'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}
	@Test
	public void c06() throws Exception {
		c.get("http://localhost/cp/sp/a/foo/bar/").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/foo/bar/'")
			.assertBodyContains("pathRemainder:'foo/bar/'")
			.assertBodyContains("pathRemainderUndecoded:'foo/bar/'")
			.assertBodyContains("pathRemainder2:'foo/bar/'")
			.assertBodyContains("requestURI:'/cp/sp/a/foo/bar/'")
			.assertBodyContains("requestParentURI:'/cp/sp/a/foo")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/foo/bar/'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}
	@Test
	public void c07() throws Exception {
		c.get("http://localhost/cp/sp/a//foo//bar//").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'//foo//bar//'")
			.assertBodyContains("pathRemainder:'/foo//bar//'")
			.assertBodyContains("pathRemainderUndecoded:'/foo//bar//'")
			.assertBodyContains("pathRemainder2:'/foo//bar//'")
			.assertBodyContains("requestURI:'/cp/sp/a//foo//bar//'")
			.assertBodyContains("requestParentURI:'/cp/sp/a/foo/")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a//foo//bar//'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}
	@Test
	public void c08() throws Exception {
		c.get("http://localhost/cp/sp/a/%20").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/ '")
			.assertBodyContains("pathRemainder:' '")
			.assertBodyContains("pathRemainderUndecoded:'%20'")
			.assertBodyContains("pathRemainder2:' '")
			.assertBodyContains("requestURI:'/cp/sp/a/%20'")
			.assertBodyContains("requestParentURI:'/cp/sp/a")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/%20'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}
	@Test
	public void c09() throws Exception {
		c.get("http://localhost/cp/sp/a/+").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/ '")
			.assertBodyContains("pathRemainder:' '")
			.assertBodyContains("pathRemainderUndecoded:'+'")
			.assertBodyContains("pathRemainder2:' '")
			.assertBodyContains("requestURI:'/cp/sp/a/+'")
			.assertBodyContains("requestParentURI:'/cp/sp/a")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/+'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:3")
		;
	}

	//=================================================================================================================
	// Child resource and subpath in method
	//=================================================================================================================

	@Rest(children={D01.class})
	public static class D {}

	@Rest(path="/a")
	public static class D01 {
		@RestMethod(name=GET, path="/subpath/*")
		public ObjectMap get(RestRequest req, @Path("/*") String r) {
			return getPaths(req).append("pathRemainder2", r).append("method",4);
		}
	}
	static MockRest d = MockRest.build(D.class, null);

	@Test
	public void d01() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath'")
			.assertBodyContains("pathRemainder:null")
			.assertBodyContains("pathRemainderUndecoded:null")
			.assertBodyContains("pathRemainder2:null")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath'")
			.assertBodyContains("requestParentURI:'/cp/sp/a")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
	@Test
	public void d02() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/'")
			.assertBodyContains("pathRemainder:''")
			.assertBodyContains("pathRemainderUndecoded:''")
			.assertBodyContains("pathRemainder2:''")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath/'")
			.assertBodyContains("requestParentURI:'/cp/sp/a")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath/'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
	@Test
	public void d03() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath//").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath//'")
			.assertBodyContains("pathRemainder:'/'")
			.assertBodyContains("pathRemainderUndecoded:'/'")
			.assertBodyContains("pathRemainder2:'/'")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath//'")
			.assertBodyContains("requestParentURI:'/cp/sp/a")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath//'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
	@Test
	public void d04() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath///").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath///'")
			.assertBodyContains("pathRemainder:'//'")
			.assertBodyContains("pathRemainderUndecoded:'//'")
			.assertBodyContains("pathRemainder2:'//'")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath///'")
			.assertBodyContains("requestParentURI:'/cp/sp/a")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath///'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
	@Test
	public void d05() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/foo/bar").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/foo/bar'")
			.assertBodyContains("pathRemainder:'foo/bar'")
			.assertBodyContains("pathRemainderUndecoded:'foo/bar'")
			.assertBodyContains("pathRemainder2:'foo/bar'")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath/foo/bar'")
			.assertBodyContains("requestParentURI:'/cp/sp/a/subpath/foo")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath/foo/bar'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
	@Test
	public void d06() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/foo/bar/").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/foo/bar/'")
			.assertBodyContains("pathRemainder:'foo/bar/'")
			.assertBodyContains("pathRemainderUndecoded:'foo/bar/'")
			.assertBodyContains("pathRemainder2:'foo/bar/'")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath/foo/bar/'")
			.assertBodyContains("requestParentURI:'/cp/sp/a/subpath/foo")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath/foo/bar/'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
	@Test
	public void d07() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath//foo//bar//").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath//foo//bar//'")
			.assertBodyContains("pathRemainder:'/foo//bar//'")
			.assertBodyContains("pathRemainderUndecoded:'/foo//bar//'")
			.assertBodyContains("pathRemainder2:'/foo//bar//'")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath//foo//bar//'")
			.assertBodyContains("requestParentURI:'/cp/sp/a/subpath//foo/")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath//foo//bar//'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
	@Test
	public void d08() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/%20").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/ '")
			.assertBodyContains("pathRemainder:' '")
			.assertBodyContains("pathRemainderUndecoded:'%20'")
			.assertBodyContains("pathRemainder2:' '")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath/%20'")
			.assertBodyContains("requestParentURI:'/cp/sp/a/subpath")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath/%20'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
	@Test
	public void d09() throws Exception {
		d.get("http://localhost/cp/sp/a/subpath/+").contextPath("/cp").servletPath("/sp").execute()
			.assertBodyContains("pathInfo:'/subpath/ '")
			.assertBodyContains("pathRemainder:' '")
			.assertBodyContains("pathRemainderUndecoded:'+'")
			.assertBodyContains("pathRemainder2:' '")
			.assertBodyContains("requestURI:'/cp/sp/a/subpath/+'")
			.assertBodyContains("requestParentURI:'/cp/sp/a/subpath")
			.assertBodyContains("requestURL:'http://localhost/cp/sp/a/subpath/+'")
			.assertBodyContains("servletPath:'/sp/a'")
			.assertBodyContains("servletURI:'/cp/sp/a'")
			.assertBodyContains("method:4")
		;
	}
}