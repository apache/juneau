/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import static org.junit.Assert.*;

import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Verifies that all the RestRequest.getXXX() methods involving URIs work correctly.
 */
public class TestUrisTest {

	private static String URL2 = Constants.getServerTestUrl() + "/testuris";           // /jazz/juneau/sample/testuris
	private static int port = getPort(Constants.getServerTestUrl());                  // 9443
	private static String path = Constants.getServerTestUri().getPath();              // /jazz/juneau/sample

	//====================================================================================================
	// testRoot - http://localhost:8080/sample/testuris
	//====================================================================================================
	@Test
	public void testRoot() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r;

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris").getResponse(ObjectMap.class);
		assertEquals("root.test1", r.getString("testMethod"));
		assertNull(r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/foo").getResponse(ObjectMap.class);
		assertEquals("root.test1", r.getString("testMethod"));
		assertEquals("/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/foo"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/foo/bar").getResponse(ObjectMap.class);
		assertEquals("root.test1", r.getString("testMethod"));
		assertEquals("/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("root.test1", r.getString("testMethod"));
		assertEquals("/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test2
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test2").getResponse(ObjectMap.class);
		assertEquals("root.test2", r.getString("testMethod"));
		assertEquals("/test2", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test2", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test2"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test2/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test2/foo").getResponse(ObjectMap.class);
		assertEquals("root.test2", r.getString("testMethod"));
		assertEquals("/test2/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test2", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test2/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test2/foo"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test2/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test2/foo/bar").getResponse(ObjectMap.class);
		assertEquals("root.test2", r.getString("testMethod"));
		assertEquals("/test2/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test2/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test2/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test2/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test3%2Ftest3
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test3%2Ftest3").getResponse(ObjectMap.class);
		assertEquals("root.test3", r.getString("testMethod"));
		assertEquals("/test3/test3", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test3%2Ftest3", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test3%2Ftest3"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test3%2Ftest3/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test3%2Ftest3/foo").getResponse(ObjectMap.class);
		assertEquals("root.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test3%2Ftest3", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test3%2Ftest3/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test3%2Ftest3/foo"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test3%2Ftest3/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test3%2Ftest3/foo/bar").getResponse(ObjectMap.class);
		assertEquals("root.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test3%2Ftest3/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test3%2Ftest3/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test3%2Ftest3/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test3%2Ftest3/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test3%2Ftest3/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("root.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test3%2Ftest3/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test3%2Ftest3/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test3%2Ftest3/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test4/test4
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test4/test4").getResponse(ObjectMap.class);
		assertEquals("root.test4", r.getString("testMethod"));
		assertEquals("/test4/test4", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test4", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test4/test4", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test4/test4"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test4/test4/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test4/test4/foo").getResponse(ObjectMap.class);
		assertEquals("root.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test4/test4", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test4/test4/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test4/test4/foo"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test4/test4/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test4/test4/foo/bar").getResponse(ObjectMap.class);
		assertEquals("root.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test4/test4/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test4/test4/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test4/test4/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/test4/test4/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/test4/test4/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("root.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/test4/test4/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/test4/test4/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/test4/test4/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2, r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		client.closeQuietly();
	}

	//====================================================================================================
	// testChild - http://localhost:8080/sample/testuris/child
	//====================================================================================================
	@Test
	public void testChild() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r;

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child").getResponse(ObjectMap.class);
		assertEquals("child.test1", r.getString("testMethod"));
		assertNull(r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/foo").getResponse(ObjectMap.class);
		assertEquals("child.test1", r.getString("testMethod"));
		assertEquals("/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/foo"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/foo/bar").getResponse(ObjectMap.class);
		assertEquals("child.test1", r.getString("testMethod"));
		assertEquals("/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("child.test1", r.getString("testMethod"));
		assertEquals("/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test2
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test2").getResponse(ObjectMap.class);
		assertEquals("child.test2", r.getString("testMethod"));
		assertEquals("/test2", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test2", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test2"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test2/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test2/foo").getResponse(ObjectMap.class);
		assertEquals("child.test2", r.getString("testMethod"));
		assertEquals("/test2/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test2", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test2/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test2/foo"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test2/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test2/foo/bar").getResponse(ObjectMap.class);
		assertEquals("child.test2", r.getString("testMethod"));
		assertEquals("/test2/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test2/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test2/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test2/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test2/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test2/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("child.test2", r.getString("testMethod"));
		assertEquals("/test2/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test2/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test2/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test2/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test3%2Ftest3
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test3%2Ftest3").getResponse(ObjectMap.class);
		assertEquals("child.test3", r.getString("testMethod"));
		assertEquals("/test3/test3", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test3%2Ftest3", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test3%2Ftest3"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test3%2Ftest3/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test3%2Ftest3/foo").getResponse(ObjectMap.class);
		assertEquals("child.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test3%2Ftest3", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test3%2Ftest3/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test3%2Ftest3/foo"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test3%2Ftest3/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test3%2Ftest3/foo/bar").getResponse(ObjectMap.class);
		assertEquals("child.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test3%2Ftest3/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test3%2Ftest3/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test3%2Ftest3/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test3%2Ftest3/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test3%2Ftest3/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("child.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test3%2Ftest3/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test3%2Ftest3/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test3%2Ftest3/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test4/test4
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test4/test4").getResponse(ObjectMap.class);
		assertEquals("child.test4", r.getString("testMethod"));
		assertEquals("/test4/test4", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test4", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test4/test4", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test4/test4"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test4/test4/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test4/test4/foo").getResponse(ObjectMap.class);
		assertEquals("child.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test4/test4", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test4/test4/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test4/test4/foo"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test4/test4/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test4/test4/foo/bar").getResponse(ObjectMap.class);
		assertEquals("child.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test4/test4/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test4/test4/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test4/test4/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test4/test4/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/test4/test4/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("child.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/test4/test4/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/test4/test4/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/test4/test4/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris/child", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		client.closeQuietly();
	}

	//====================================================================================================
	// testGrandChild - http://localhost:8080/sample/testuris/child/grandchild
	//====================================================================================================
	@Test
	public void testGrandChild() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r;

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild").getResponse(ObjectMap.class);
		assertEquals("grandchild.test1", r.getString("testMethod"));
		assertNull(r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/foo").getResponse(ObjectMap.class);
		assertEquals("grandchild.test1", r.getString("testMethod"));
		assertEquals("/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/foo"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/foo/bar").getResponse(ObjectMap.class);
		assertEquals("grandchild.test1", r.getString("testMethod"));
		assertEquals("/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("grandchild.test1", r.getString("testMethod"));
		assertEquals("/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test2
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test2").getResponse(ObjectMap.class);
		assertEquals("grandchild.test2", r.getString("testMethod"));
		assertEquals("/test2", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test2", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test2"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test2/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test2/foo").getResponse(ObjectMap.class);
		assertEquals("grandchild.test2", r.getString("testMethod"));
		assertEquals("/test2/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test2", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test2/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test2/foo"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test2/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test2/foo/bar").getResponse(ObjectMap.class);
		assertEquals("grandchild.test2", r.getString("testMethod"));
		assertEquals("/test2/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test2/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test2/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test2/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test2/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test2/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("grandchild.test2", r.getString("testMethod"));
		assertEquals("/test2/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test2/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test2/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test2/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test3%2Ftest3
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test3%2Ftest3").getResponse(ObjectMap.class);
		assertEquals("grandchild.test3", r.getString("testMethod"));
		assertEquals("/test3/test3", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test3%2Ftest3", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test3%2Ftest3"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test3%2Ftest3/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test3%2Ftest3/foo").getResponse(ObjectMap.class);
		assertEquals("grandchild.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test3%2Ftest3", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test3%2Ftest3/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test3%2Ftest3/foo"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test3%2Ftest3/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test3%2Ftest3/foo/bar").getResponse(ObjectMap.class);
		assertEquals("grandchild.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test3%2Ftest3/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test3%2Ftest3/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test3%2Ftest3/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test3%2Ftest3/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test3%2Ftest3/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("grandchild.test3", r.getString("testMethod"));
		assertEquals("/test3/test3/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test3%2Ftest3/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test3%2Ftest3/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test3%2Ftest3/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test4/test4
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test4/test4").getResponse(ObjectMap.class);
		assertEquals("grandchild.test4", r.getString("testMethod"));
		assertEquals("/test4/test4", r.getString("pathInfo"));
		assertNull(r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test4", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test4/test4", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test4/test4"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test4/test4/foo
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test4/test4/foo").getResponse(ObjectMap.class);
		assertEquals("grandchild.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo", r.getString("pathInfo"));
		assertEquals("foo", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test4/test4", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test4/test4/foo", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test4/test4/foo"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test4/test4/foo/bar
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test4/test4/foo/bar").getResponse(ObjectMap.class);
		assertEquals("grandchild.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo/bar", r.getString("pathInfo"));
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test4/test4/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test4/test4/foo/bar", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test4/test4/foo/bar"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		//--------------------------------------------------------------------------------
		// http://localhost:8080/sample/testuris/child/test4/test4/foo/bar%2Fbaz
		//--------------------------------------------------------------------------------
		r = client.doGet("/testuris/child/grandchild/test4/test4/foo/bar%2Fbaz").getResponse(ObjectMap.class);
		assertEquals("grandchild.test4", r.getString("testMethod"));
		assertEquals("/test4/test4/foo/bar/baz", r.getString("pathInfo"));
		assertEquals("foo/bar/baz", r.getString("pathRemainder"));
		assertEquals(path + "/testuris/child/grandchild/test4/test4/foo", r.getString("requestParentURI"));
		assertEquals(path + "/testuris/child/grandchild/test4/test4/foo/bar%2Fbaz", r.getString("requestURI"));
		assertTrue(r.getString("requestURL").endsWith(port + path + "/testuris/child/grandchild/test4/test4/foo/bar%2Fbaz"));
		// Same for servlet
		assertEquals(path + "/testuris/child/grandchild", r.getString("contextPath") + r.getString("servletPath"));  // App may not have context path, but combination should always equal path.
		assertEquals(URL2 + "/child/grandchild", r.getString("servletURI"));
		assertTrue(r.getString("testURL1").endsWith(port + path + "/testuris/child/grandchild/testURL"));
		// Always the same
		assertTrue(r.getString("testURL2").endsWith(port + "/testURL"));
		assertEquals("http://testURL", r.getString("testURL3"));

		client.closeQuietly();
	}

	private static int getPort(String url) {
		Pattern p = Pattern.compile("\\:(\\d{2,5})");
		Matcher m = p.matcher(url);
		if (m.find())
			return Integer.parseInt(m.group(1));
		return -1;
	}
}
