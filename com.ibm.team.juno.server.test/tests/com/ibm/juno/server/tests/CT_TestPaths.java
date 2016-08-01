/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.*;
import com.ibm.juno.core.json.*;

public class CT_TestPaths {

	private static String URL = "/testPaths";

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		ObjectMap r;
		String url;

		// [/test/testPaths]
		//	{
		//		pathInfo:null,
		//		pathInfoUndecoded:null,
		//		pathInfoParts:[],
		//		pathRemainder:null,
		//		pathRemainderUndecoded:null,
		//		requestURI:'/jazz/juno/test/testPaths',
		//		requestParentURI:'/jazz/juno/test',
		//		requestURL:'https://localhost:9443/jazz/juno/test/testPaths',
		//		servletPath:'/juno/test/testPaths',
		//		relativeServletURI:'/jazz/juno/test/testPaths',
		//		pathRemainder2:null
		//	}
		url = URL;
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertNull(r.getString("pathInfo"));
		assertNull(r.getString("pathInfoUndecoded"));
		assertEquals("[]", r.getObjectList("pathInfoParts").toString());
		assertNull(r.getString("pathRemainder"));
		assertNull(r.getString("pathRemainderUndecoded"));
		assertNull(r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestParentURI").endsWith("/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));


		// [/test/testPaths/]
		//		{
		//			pathInfo: '/',
		//			pathInfoUndecoded: '/',
		//			pathInfoParts: [
		//			],
		//			pathRemainder: '',
		//			pathRemainderUndecoded: '',
		//			requestURI: '/jazz/juno/test/testPaths/',
		//			requestParentURI: '/jazz/juno/test',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/',
		//			servletPath: '/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: ''
		//		}
		url = URL + '/';
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/", r.getString("pathInfo"));
		assertEquals("/", r.getString("pathInfoUndecoded"));
		assertEquals("[]", r.getObjectList("pathInfoParts").toString());
		assertEquals("", r.getString("pathRemainder"));
		assertEquals("", r.getString("pathRemainderUndecoded"));
		assertEquals("", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/"));
		assertTrue(r.getString("requestParentURI").endsWith("/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths//]
		//		{
		//			pathInfo: '//',
		//			pathInfoParts: [''],
		//			pathRemainder: '/',
		//			requestURI: '/jazz/juno/test/testPaths//',
		//			requestParentURI: '/jazz/juno/test',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths//',
		//			servletPath: '/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '/'
		//		}
		url = URL + "//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//", r.getString("pathInfo"));
		assertEquals("['']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/", r.getString("pathRemainder"));
		assertEquals("/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths//"));
		assertTrue(r.getString("requestParentURI").endsWith("/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths///]
		//		{
		//			pathInfo: '///',
		//			pathInfoParts: ['',''],
		//			pathRemainder: '//',
		//			requestURI: '/jazz/juno/test/testPaths///',
		//			requestParentURI: '/jazz/juno/test',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths///',
		//			servletPath: '/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '//'
		//		}
		url = URL + "///";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("///", r.getString("pathInfo"));
		assertEquals("['','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("//", r.getString("pathRemainder"));
		assertEquals("//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths///"));
		assertTrue(r.getString("requestParentURI").endsWith("/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths///"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths/foo/bar]
		//		{
		//			pathInfo: '/foo/bar',
		//			pathInfoParts: [
		//				'foo',
		//				'bar'
		//			],
		//			pathRemainder: 'foo/bar',
		//			requestURI: '/jazz/juno/test/testPaths/foo/bar',
		//			requestParentURI: '/jazz/juno/test/testPaths/foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/foo/bar',
		//			servletPath: '/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: 'foo/bar'
		//		}
		url = URL + "/foo/bar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo/bar", r.getString("pathInfo"));
		assertEquals("['foo','bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals("foo/bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/foo/bar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/foo/bar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths/foo/bar/]
		//		{
		//			pathInfo: '/foo/bar/',
		//			pathInfoParts: [
		//				'foo',
		//				'bar'
		//			],
		//			pathRemainder: 'foo/bar/',
		//			requestURI: '/jazz/juno/test/testPaths/foo/bar/',
		//			requestParentURI: '/jazz/juno/test/testPaths/foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/foo/bar/',
		//			servletPath: '/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: 'foo/bar/'
		//		}
		url = URL + "/foo/bar/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo/bar/", r.getString("pathInfo"));
		assertEquals("['foo','bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo/bar/", r.getString("pathRemainder"));
		assertEquals("foo/bar/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/foo/bar/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/foo/bar/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths//foo//bar//]
		//		{
		//			pathInfo: '//foo//bar//',
		//			pathInfoParts: [
		//				'',
		//				'foo',
		//				'',
		//				'bar',
		//				''
		//			],
		//			pathRemainder: '/foo//bar//',
		//			requestURI: '/jazz/juno/test/testPaths//foo//bar//',
		//			requestParentURI: '/jazz/juno/test/testPaths//foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths//foo//bar//',
		//			servletPath: '/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '/foo//bar//'
		//		}
		url = URL + "//foo//bar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//foo//bar//", r.getString("pathInfo"));
		assertEquals("['','foo','','bar','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths//foo//bar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths//foo/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths//foo//bar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths/foo%2Fbar]
		//		{
		//			pathInfo: '/foo//bar',
		//			pathInfoUndecoded: '/foo%2F%2Fbar',
		//			pathInfoParts: [
		//				'foo//bar'
		//			],
		//			pathRemainder: 'foo//bar',
		//			pathRemainderUndecoded: 'foo%2F%2Fbar',
		//			requestURI: '/jazz/juno/test/testPaths/foo%2F%2Fbar',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/foo%2F%2Fbar',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: 'foo//bar',
		//			method: 1
		//		}
		url = URL + "/foo%2F%2Fbar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo//bar", r.getString("pathInfo"));
		assertEquals("/foo%2F%2Fbar", r.getString("pathInfoUndecoded"));
		assertEquals("['foo//bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo//bar", r.getString("pathRemainder"));
		assertEquals("foo%2F%2Fbar", r.getString("pathRemainderUndecoded"));
		assertEquals("foo//bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/foo%2F%2Fbar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/foo%2F%2Fbar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths//foo%2Fbar//]
		//		{
		//			pathInfo: '//foo//bar//',
		//			pathInfoUndecoded: '//foo%2F%2Fbar//',
		//			pathInfoParts: [
		//				'',
		//				'foo//bar',
		//				''
		//			],
		//			pathRemainder: '/foo//bar//',
		//			pathRemainderUndecoded: '/foo%2F%2Fbar//',
		//			requestURI: '/jazz/juno/test/testPaths//foo%2F%2Fbar//',
		//			requestParentURI: '/jazz/juno/test/testPaths/',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths//foo%2F%2Fbar//',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '/foo//bar//',
		//			method: 1
		//		}
		url = URL + "//foo%2F%2Fbar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//foo//bar//", r.getString("pathInfo"));
		assertEquals("//foo%2F%2Fbar//", r.getString("pathInfoUndecoded"));
		assertEquals("['','foo//bar','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo%2F%2Fbar//", r.getString("pathRemainderUndecoded"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths//foo%2F%2Fbar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths//foo%2F%2Fbar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		// [/test/testPaths/test2]
		//		{
		//			pathInfo: '/test2',
		//			pathInfoParts: [
		//				'test2'
		//			],
		//			pathRemainder: null,
		//			requestURI: '/jazz/juno/test/testPaths/test2',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: null,
		//			method: 2
		//		}
		url = URL + "/test2";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2", r.getString("pathInfo"));
		assertEquals("['test2']", r.getObjectList("pathInfoParts").toString());
		assertNull(r.getString("pathRemainder"));
		assertNull(r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));


		// [/test/testPaths/test2/]
		//		{
		//			pathInfo: '/test2/',
		//			pathInfoParts: [
		//				'test2'
		//			],
		//			pathRemainder: '',
		//			requestURI: '/jazz/juno/test/testPaths/test2/',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2/',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '',
		//			method: 2
		//		}
		url = URL + "/test2/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/", r.getString("pathInfo"));
		assertEquals("['test2']", r.getObjectList("pathInfoParts").toString());
		assertEquals("", r.getString("pathRemainder"));
		assertEquals("", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2//]
		//		{
		//			pathInfo: '/test2//',
		//			pathInfoParts: [
		//				'test2',
		//				''
		//			],
		//			pathRemainder: '/',
		//			requestURI: '/jazz/juno/test/testPaths/test2//',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2//',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '/',
		//			method: 2
		//		}
		url = URL + "/test2//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//", r.getString("pathInfo"));
		assertEquals("['test2','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/", r.getString("pathRemainder"));
		assertEquals("/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2///]
		//		{
		//			pathInfo: '/test2///',
		//			pathInfoParts: [
		//				'test2',
		//				'',
		//				''
		//			],
		//			pathRemainder: '//',
		//			requestURI: '/jazz/juno/test/testPaths/test2///',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2///',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '//',
		//			method: 2
		//		}
		url = URL + "/test2///";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2///", r.getString("pathInfo"));
		assertEquals("['test2','','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("//", r.getString("pathRemainder"));
		assertEquals("//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2///"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2///"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2/foo/bar]
		//		{
		//			pathInfo: '/test2/foo/bar',
		//			pathInfoParts: [
		//				'test2',
		//				'foo',
		//				'bar'
		//			],
		//			pathRemainder: 'foo/bar',
		//			requestURI: '/jazz/juno/test/testPaths/test2/foo/bar',
		//			requestParentURI: '/jazz/juno/test/testPaths/test2/foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2/foo/bar',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: 'foo/bar',
		//			method: 2
		//		}
		url = URL + "/test2/foo/bar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo/bar", r.getString("pathInfo"));
		assertEquals("['test2','foo','bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals("foo/bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/foo/bar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/foo/bar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2/foo/bar/]
		//		{
		//			pathInfo: '/test2/foo/bar/',
		//			pathInfoParts: [
		//				'test2',
		//				'foo',
		//				'bar'
		//			],
		//			pathRemainder: 'foo/bar/',
		//			requestURI: '/jazz/juno/test/testPaths/test2/foo/bar/',
		//			requestParentURI: '/jazz/juno/test/testPaths/test2/foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2/foo/bar/',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: 'foo/bar/',
		//			method: 2
		//		}
		url = URL + "/test2/foo/bar/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo/bar/", r.getString("pathInfo"));
		assertEquals("['test2','foo','bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo/bar/", r.getString("pathRemainder"));
		assertEquals("foo/bar/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/foo/bar/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/foo/bar/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2//foo//bar//]
		//		{
		//			pathInfo: '/test2//foo//bar//',
		//			pathInfoParts: [
		//				'test2',
		//				'',
		//				'foo',
		//				'',
		//				'bar',
		//				''
		//			],
		//			pathRemainder: '/foo//bar//',
		//			requestURI: '/jazz/juno/test/testPaths/test2//foo//bar//',
		//			requestParentURI: '/jazz/juno/test/testPaths/test2//foo/',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2//foo//bar//',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '/foo//bar//',
		//			method: 2
		//		}
		url = URL + "/test2//foo//bar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//foo//bar//", r.getString("pathInfo"));
		assertEquals("['test2','','foo','','bar','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2//foo//bar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2//foo/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2//foo//bar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2/foo%2Fbar]
		//		{
		//			pathInfo: '/test2/foo//bar',
		//			pathInfoUndecoded: '/test2/foo%2F%2Fbar',
		//			pathInfoParts: [
		//				'test2',
		//				'foo//bar'
		//			],
		//			pathRemainder: 'foo//bar',
		//			pathRemainderUndecoded: 'foo%2F%2Fbar',
		//			requestURI: '/jazz/juno/test/testPaths/test2/foo%2F%2Fbar',
		//			requestParentURI: '/jazz/juno/test/testPaths/test2',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2/foo%2F%2Fbar',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: 'foo//bar',
		//			method: 2
		//		}
		url = URL + "/test2/foo%2F%2Fbar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo//bar", r.getString("pathInfo"));
		assertEquals("/test2/foo%2F%2Fbar", r.getString("pathInfoUndecoded"));
		assertEquals("['test2','foo//bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo//bar", r.getString("pathRemainder"));
		assertEquals("foo%2F%2Fbar", r.getString("pathRemainderUndecoded"));
		assertEquals("foo//bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/foo%2F%2Fbar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/foo%2F%2Fbar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/test2//foo%2Fbar//]
		//		{
		//			pathInfo: '/test2//foo//bar//',
		//			pathInfoUndecoded: '/test2//foo%2F%2Fbar//',
		//			pathInfoParts: [
		//				'test2',
		//				'',
		//				'foo//bar',
		//				''
		//			],
		//			pathRemainder: '/foo//bar//',
		//			pathRemainderUndecoded: '/foo%2F%2Fbar//',
		//			requestURI: '/jazz/juno/test/testPaths/test2//foo%2F%2Fbar//',
		//			requestParentURI: '/jazz/juno/test/testPaths/test2/',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/test2//foo%2F%2Fbar//',
		//			servletPath: '/juno/test/testPaths',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test',
		//			relativeServletURI: '/jazz/juno/test/testPaths',
		//			pathRemainder2: '/foo//bar//',
		//			method: 2
		//		}
		url = URL + "/test2//foo%2F%2Fbar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//foo//bar//", r.getString("pathInfo"));
		assertEquals("/test2//foo%2F%2Fbar//", r.getString("pathInfoUndecoded"));
		assertEquals("['test2','','foo//bar','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo%2F%2Fbar//", r.getString("pathRemainderUndecoded"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2//foo%2F%2Fbar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2//foo%2F%2Fbar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		// [/test/testPaths/a]
		//		{
		//			pathInfo: null,
		//			pathInfoParts: [
		//			],
		//			pathRemainder: null,
		//			requestURI: '/jazz/juno/test/testPaths/a',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: null,
		//			method: 3
		//		}
		url = URL + "/a";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertNull(r.getString("pathInfo"));
		assertEquals("[]", r.getObjectList("pathInfoParts").toString());
		assertNull(r.getString("pathRemainder"));
		assertNull(r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a/]
		//		{
		//			pathInfo: '/',
		//			pathInfoParts: [
		//			],
		//			pathRemainder: '',
		//			requestURI: '/jazz/juno/test/testPaths/a/',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '',
		//			method: 3
		//		}
		url = URL + "/a/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/", r.getString("pathInfo"));
		assertEquals("[]", r.getObjectList("pathInfoParts").toString());
		assertEquals("", r.getString("pathRemainder"));
		assertEquals("", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a//]
		//		{
		//			pathInfo: '//',
		//			pathInfoParts: [
		//				''
		//			],
		//			pathRemainder: '/',
		//			requestURI: '/jazz/juno/test/testPaths/a//',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a//',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '/',
		//			method: 3
		//		}
		url = URL + "/a//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//", r.getString("pathInfo"));
		assertEquals("['']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/", r.getString("pathRemainder"));
		assertEquals("/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a///]
		//		{
		//			pathInfo: '///',
		//			pathInfoParts: [
		//				'',
		//				''
		//			],
		//			pathRemainder: '//',
		//			requestURI: '/jazz/juno/test/testPaths/a///',
		//			requestParentURI: '/jazz/juno/test/testPaths',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a///',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '//',
		//			method: 3
		//		}
		url = URL + "/a///";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("///", r.getString("pathInfo"));
		assertEquals("['','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("//", r.getString("pathRemainder"));
		assertEquals("//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a///"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a///"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a/foo/bar]
		//		{
		//			pathInfo: '/foo/bar',
		//			pathInfoParts: [
		//				'foo',
		//				'bar'
		//			],
		//			pathRemainder: 'foo/bar',
		//			requestURI: '/jazz/juno/test/testPaths/a/foo/bar',
		//			requestParentURI: '/jazz/juno/test/testPaths/a/foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/foo/bar',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: 'foo/bar',
		//			method: 3
		//		}
		url = URL + "/a/foo/bar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo/bar", r.getString("pathInfo"));
		assertEquals("['foo','bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals("foo/bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/foo/bar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/foo/bar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a/foo/bar/]
		//		{
		//			pathInfo: '/foo/bar/',
		//			pathInfoParts: [
		//				'foo',
		//				'bar'
		//			],
		//			pathRemainder: 'foo/bar/',
		//			requestURI: '/jazz/juno/test/testPaths/a/foo/bar/',
		//			requestParentURI: '/jazz/juno/test/testPaths/a/foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/foo/bar/',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: 'foo/bar/',
		//			method: 3
		//		}
		url = URL + "/a/foo/bar/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo/bar/", r.getString("pathInfo"));
		assertEquals("['foo','bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo/bar/", r.getString("pathRemainder"));
		assertEquals("foo/bar/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/foo/bar/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/foo/bar/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a//foo//bar//]
		//		{
		//			pathInfo: '//foo//bar//',
		//			pathInfoParts: [
		//				'',
		//				'foo',
		//				'',
		//				'bar',
		//				''
		//			],
		//			pathRemainder: '/foo//bar//',
		//			requestURI: '/jazz/juno/test/testPaths/a//foo//bar//',
		//			requestParentURI: '/jazz/juno/test/testPaths/a//foo/',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a//foo//bar//',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '/foo//bar//',
		//			method: 3
		//		}
		url = URL + "/a//foo//bar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//foo//bar//", r.getString("pathInfo"));
		assertEquals("['','foo','','bar','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a//foo//bar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a//foo/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a//foo//bar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a/foo%2Fbar]
		//		{
		//			pathInfo: '/foo//bar',
		//			pathInfoUndecoded: '/foo%2F%2Fbar',
		//			pathInfoParts: [
		//				'foo//bar'
		//			],
		//			pathRemainder: 'foo//bar',
		//			pathRemainderUndecoded: 'foo%2F%2Fbar',
		//			requestURI: '/jazz/juno/test/testPaths/a/foo%2F%2Fbar',
		//			requestParentURI: '/jazz/juno/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/foo%2F%2Fbar',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: 'foo//bar',
		//			method: 3
		//		}
		url = URL + "/a/foo%2F%2Fbar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/foo//bar", r.getString("pathInfo"));
		assertEquals("/foo%2F%2Fbar", r.getString("pathInfoUndecoded"));
		assertEquals("['foo//bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo//bar", r.getString("pathRemainder"));
		assertEquals("foo%2F%2Fbar", r.getString("pathRemainderUndecoded"));
		assertEquals("foo//bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/foo%2F%2Fbar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/foo%2F%2Fbar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		// [/test/testPaths/a//foo%2Fbar//]
		//		{
		//			pathInfo: '//foo//bar//',
		//			pathInfoUndecoded: '//foo%2F%2Fbar//',
		//			pathInfoParts: [
		//				'',
		//				'foo//bar',
		//				''
		//			],
		//			pathRemainder: '/foo//bar//',
		//			pathRemainderUndecoded: '/foo%2F%2Fbar//',
		//			requestURI: '/jazz/juno/test/testPaths/a//foo%2F%2Fbar//',
		//			requestParentURI: '/jazz/juno/test/testPaths/a/',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a//foo%2F%2Fbar//',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '/foo//bar//',
		//			method: 3
		//		}
		url = URL + "/a//foo%2F%2Fbar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("//foo//bar//", r.getString("pathInfo"));
		assertEquals("//foo%2F%2Fbar//", r.getString("pathInfoUndecoded"));
		assertEquals("['','foo//bar','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo%2F%2Fbar//", r.getString("pathRemainderUndecoded"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a//foo%2F%2Fbar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a//foo%2F%2Fbar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));


		// [/test/testPaths/a/test2]
		//		{
		//			pathInfo: '/test2',
		//			pathInfoParts: [
		//				'test2'
		//			],
		//			pathRemainder: null,
		//			requestURI: '/jazz/juno/test/testPaths/a/test2',
		//			requestParentURI: '/jazz/juno/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: null,
		//			method: 4
		//		}
		url = URL + "/a/test2";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2", r.getString("pathInfo"));
		assertEquals("['test2']", r.getObjectList("pathInfoParts").toString());
		assertNull(r.getString("pathRemainder"));
		assertNull(r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2/]
		//		{
		//			pathInfo: '/test2/',
		//			pathInfoParts: [
		//				'test2'
		//			],
		//			pathRemainder: '',
		//			requestURI: '/jazz/juno/test/testPaths/a/test2/',
		//			requestParentURI: '/jazz/juno/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2/',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '',
		//			method: 4
		//		}
		url = URL + "/a/test2/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/", r.getString("pathInfo"));
		assertEquals("['test2']", r.getObjectList("pathInfoParts").toString());
		assertEquals("", r.getString("pathRemainder"));
		assertEquals("", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2//]
		//		{
		//			pathInfo: '/test2//',
		//			pathInfoParts: [
		//				'test2',
		//				''
		//			],
		//			pathRemainder: '/',
		//			requestURI: '/jazz/juno/test/testPaths/a/test2//',
		//			requestParentURI: '/jazz/juno/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2//',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '/',
		//			method: 4
		//		}
		url = URL + "/a/test2//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//", r.getString("pathInfo"));
		assertEquals("['test2','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/", r.getString("pathRemainder"));
		assertEquals("/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2///]
		//		{
		//			pathInfo: '/test2///',
		//			pathInfoParts: [
		//				'test2',
		//				'',
		//				''
		//			],
		//			pathRemainder: '//',
		//			requestURI: '/jazz/juno/test/testPaths/a/test2///',
		//			requestParentURI: '/jazz/juno/test/testPaths/a',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2///',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '//',
		//			method: 4
		//		}
		url = URL + "/a/test2///";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2///", r.getString("pathInfo"));
		assertEquals("['test2','','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("//", r.getString("pathRemainder"));
		assertEquals("//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2///"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2///"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2/foo/bar]
		//		{
		//			pathInfo: '/test2/foo/bar',
		//			pathInfoParts: [
		//				'test2',
		//				'foo',
		//				'bar'
		//			],
		//			pathRemainder: 'foo/bar',
		//			requestURI: '/jazz/juno/test/testPaths/a/test2/foo/bar',
		//			requestParentURI: '/jazz/juno/test/testPaths/a/test2/foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2/foo/bar',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: 'foo/bar',
		//			method: 4
		//		}
		url = URL + "/a/test2/foo/bar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo/bar", r.getString("pathInfo"));
		assertEquals("['test2','foo','bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo/bar", r.getString("pathRemainder"));
		assertEquals("foo/bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/foo/bar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/foo/bar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2/foo/bar/]
		//		{
		//			pathInfo: '/test2/foo/bar/',
		//			pathInfoParts: [
		//				'test2',
		//				'foo',
		//				'bar'
		//			],
		//			pathRemainder: 'foo/bar/',
		//			requestURI: '/jazz/juno/test/testPaths/a/test2/foo/bar/',
		//			requestParentURI: '/jazz/juno/test/testPaths/a/test2/foo',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2/foo/bar/',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: 'foo/bar/',
		//			method: 4
		//		}
		url = URL + "/a/test2/foo/bar/";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo/bar/", r.getString("pathInfo"));
		assertEquals("['test2','foo','bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo/bar/", r.getString("pathRemainder"));
		assertEquals("foo/bar/", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/foo/bar/"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2/foo"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/foo/bar/"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2//foo//bar//]
		//		{
		//			pathInfo: '/test2//foo//bar//',
		//			pathInfoParts: [
		//				'test2',
		//				'',
		//				'foo',
		//				'',
		//				'bar',
		//				''
		//			],
		//			pathRemainder: '/foo//bar//',
		//			requestURI: '/jazz/juno/test/testPaths/a/test2//foo//bar//',
		//			requestParentURI: '/jazz/juno/test/testPaths/a/test2//foo/',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2//foo//bar//',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '/foo//bar//',
		//			method: 4
		//		}
		url = URL + "/a/test2//foo//bar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//foo//bar//", r.getString("pathInfo"));
		assertEquals("['test2','','foo','','bar','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2//foo//bar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2//foo/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2//foo//bar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2/foo%2Fbar]
		//		{
		//			pathInfo: '/test2/foo//bar',
		//			pathInfoUndecoded: '/test2/foo%2F%2Fbar',
		//			pathInfoParts: [
		//				'test2',
		//				'foo//bar'
		//			],
		//			pathRemainder: 'foo//bar',
		//			pathRemainderUndecoded: 'foo%2F%2Fbar',
		//			requestURI: '/jazz/juno/test/testPaths/a/test2/foo%2F%2Fbar',
		//			requestParentURI: '/jazz/juno/test/testPaths/a/test2',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2/foo%2F%2Fbar',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: 'foo//bar',
		//			method: 4
		//		}
		url = URL + "/a/test2/foo%2F%2Fbar";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/foo//bar", r.getString("pathInfo"));
		assertEquals("/test2/foo%2F%2Fbar", r.getString("pathInfoUndecoded"));
		assertEquals("['test2','foo//bar']", r.getObjectList("pathInfoParts").toString());
		assertEquals("foo//bar", r.getString("pathRemainder"));
		assertEquals("foo%2F%2Fbar", r.getString("pathRemainderUndecoded"));
		assertEquals("foo//bar", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/foo%2F%2Fbar"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/foo%2F%2Fbar"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		// [/test/testPaths/a/test2//foo%2Fbar//]
		//		{
		//			pathInfo: '/test2//foo//bar//',
		//			pathInfoUndecoded: '/test2//foo%2F%2Fbar//',
		//			pathInfoParts: [
		//				'test2',
		//				'',
		//				'foo//bar',
		//				''
		//			],
		//			pathRemainder: '/foo//bar//',
		//			pathRemainderUndecoded: '/foo%2F%2Fbar//',
		//			requestURI: '/jazz/juno/test/testPaths/a/test2//foo%2F%2Fbar//',
		//			requestParentURI: '/jazz/juno/test/testPaths/a/test2/',
		//			requestURL: 'https://localhost:9443/jazz/juno/test/testPaths/a/test2//foo%2F%2Fbar//',
		//			servletPath: '/juno/test/testPaths/a',
		//			servletURI: 'https://localhost:9443/jazz/juno/test/testPaths/a',
		//			servletParentURI: 'https://localhost:9443/jazz/juno/test/testPaths',
		//			relativeServletURI: '/jazz/juno/test/testPaths/a',
		//			pathRemainder2: '/foo//bar//',
		//			method: 4
		//		}
		url = URL + "/a/test2//foo%2F%2Fbar//";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2//foo//bar//", r.getString("pathInfo"));
		assertEquals("/test2//foo%2F%2Fbar//", r.getString("pathInfoUndecoded"));
		assertEquals("['test2','','foo//bar','']", r.getObjectList("pathInfoParts").toString());
		assertEquals("/foo//bar//", r.getString("pathRemainder"));
		assertEquals("/foo%2F%2Fbar//", r.getString("pathRemainderUndecoded"));
		assertEquals("/foo//bar//", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2//foo%2F%2Fbar//"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2/"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2//foo%2F%2Fbar//"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		//--------------------------------------------------------------------------------
		// Spaces
		//--------------------------------------------------------------------------------
		url = URL + "/%20";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/ ", r.getString("pathInfo"));
		assertEquals("/%20", r.getString("pathInfoUndecoded"));
		assertEquals("[' ']", r.getObjectList("pathInfoParts").toString());
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("%20", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/%20"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/%20"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		url = URL + "/test2/%20";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/ ", r.getString("pathInfo"));
		assertEquals("/test2/%20", r.getString("pathInfoUndecoded"));
		assertEquals("['test2',' ']", r.getObjectList("pathInfoParts").toString());
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("%20", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/%20"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/%20"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		url = URL + "/a/%20";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/ ", r.getString("pathInfo"));
		assertEquals("/%20", r.getString("pathInfoUndecoded"));
		assertEquals("[' ']", r.getObjectList("pathInfoParts").toString());
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("%20", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/%20"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/%20"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		url = URL + "/a/test2/%20";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/ ", r.getString("pathInfo"));
		assertEquals("/test2/%20", r.getString("pathInfoUndecoded"));
		assertEquals("['test2',' ']", r.getObjectList("pathInfoParts").toString());
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("%20", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/%20"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/%20"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		url = URL + "/+";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/ ", r.getString("pathInfo"));
		assertEquals("/+", r.getString("pathInfoUndecoded"));
		assertEquals("[' ']", r.getObjectList("pathInfoParts").toString());
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("+", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/+"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/+"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(1, (int)r.getInt("method"));

		url = URL + "/test2/+";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/ ", r.getString("pathInfo"));
		assertEquals("/test2/+", r.getString("pathInfoUndecoded"));
		assertEquals("['test2',' ']", r.getObjectList("pathInfoParts").toString());
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("+", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/test2/+"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/test2/+"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths"));
		assertEquals(2, (int)r.getInt("method"));

		url = URL + "/a/+";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/ ", r.getString("pathInfo"));
		assertEquals("/+", r.getString("pathInfoUndecoded"));
		assertEquals("[' ']", r.getObjectList("pathInfoParts").toString());
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("+", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/+"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/+"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(3, (int)r.getInt("method"));

		url = URL + "/a/test2/+";
		r = client.doGet(url).getResponse(ObjectMap.class);
		assertEquals("/test2/ ", r.getString("pathInfo"));
		assertEquals("/test2/+", r.getString("pathInfoUndecoded"));
		assertEquals("['test2',' ']", r.getObjectList("pathInfoParts").toString());
		assertEquals(" ", r.getString("pathRemainder"));
		assertEquals("+", r.getString("pathRemainderUndecoded"));
		assertEquals(" ", r.getString("pathRemainder2"));
		assertTrue(r.getString("requestURI").endsWith("/testPaths/a/test2/+"));
		assertTrue(r.getString("requestParentURI").endsWith("/testPaths/a/test2"));
		assertTrue(r.getString("requestURL").endsWith("/testPaths/a/test2/+"));
		assertTrue(r.getString("servletPath").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletURI").endsWith("/testPaths/a"));
		assertTrue(r.getString("servletParentURI").endsWith("/testPaths"));
		assertTrue(r.getString("relativeServletURI").endsWith("/testPaths/a"));
		assertEquals(4, (int)r.getInt("method"));

		client.closeQuietly();
	}
}
