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
package org.apache.juneau.rest.test;

import static org.apache.juneau.rest.test.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class HeadersTest extends RestTestcase {

	RestClient client = TestMicroservice.DEFAULT_CLIENT;

	private static String URL = "/testHeaders";

	//====================================================================================================
	// Basic tests
	//====================================================================================================

	@Test
	public void accept() throws Exception {
		assertEquals("text/foo", client.doGet(URL + "/accept").accept("text/foo").getResponseAsString());
		assertEquals("text/foo+bar", client.doGet(URL + "/accept").accept("text/foo+bar").getResponseAsString());
		assertEquals("text/*", client.doGet(URL + "/accept").accept("text/*").getResponseAsString());
		assertEquals("*/foo", client.doGet(URL + "/accept").accept("*/foo").getResponseAsString());

		assertEquals("text/foo", client.doGet(URL + "/accept").accept("text/foo;q=1.0").getResponseAsString());
		assertEquals("text/foo;q=0.9", client.doGet(URL + "/accept").accept("text/foo;q=0.9").getResponseAsString());
		assertEquals("text/foo;x=X;q=0.9;y=Y", client.doGet(URL + "/accept").accept("text/foo;x=X;q=0.9;y=Y").getResponseAsString());

		assertEquals("text/foo", client.doGet(URL + "/accept").query("Accept", "text/foo").getResponseAsString());
	}

	@Test
	public void acceptCharset() throws Exception {
		assertEquals("UTF-8", client.doGet(URL + "/acceptCharset").acceptCharset("UTF-8").getResponseAsString());
		assertEquals("UTF-8", client.doGet(URL + "/acceptCharset").query("Accept-Charset", "UTF-8").getResponseAsString());
	}

	@Test
	public void acceptEncoding() throws Exception {
		assertEquals("foo", client.doGet(URL + "/acceptEncoding").acceptEncoding("foo").getResponseAsString());
		assertEquals("*", client.doGet(URL + "/acceptEncoding").acceptEncoding("*").getResponseAsString());
		assertEquals("*", client.doGet(URL + "/acceptEncoding").query("Accept-Encoding", "*").getResponseAsString());
	}

	@Test
	public void acceptLanguage() throws Exception {
		assertEquals("foo", client.doGet(URL + "/acceptLanguage").acceptLanguage("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/acceptLanguage").query("Accept-Language", "foo").getResponseAsString());
	}

	@Test
	public void authorization() throws Exception {
		assertEquals("foo", client.doGet(URL + "/authorization").authorization("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/authorization").query("Authorization", "foo").getResponseAsString());
	}

	@Test
	public void cacheControl() throws Exception {
		assertEquals("foo", client.doGet(URL + "/cacheControl").cacheControl("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/cacheControl").query("Cache-Control", "foo").getResponseAsString());
	}

	@Test
	public void connection() throws Exception {
		assertEquals("foo", client.doGet(URL + "/connection").connection("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/connection").query("Connection", "foo").getResponseAsString());
	}

	@Test
	public void contentLength() throws Exception {
		assertEquals("0", client.doGet(URL + "/contentLength").contentLength(0).getResponseAsString());
		assertEquals("0", client.doGet(URL + "/contentLength").query("Content-Length", 0).getResponseAsString());
	}

	@Test
	public void contentType() throws Exception {
		assertEquals("text/foo", client.doGet(URL + "/contentType").contentType("text/foo").getResponseAsString());
		assertEquals("text/foo", client.doGet(URL + "/contentType").query("Content-Type", "text/foo").getResponseAsString());
	}

	@Test
	public void date() throws Exception {
		assertEquals("foo", client.doGet(URL + "/date").date("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/date").query("Date", "foo").getResponseAsString());
	}

	@Test
	public void expect() throws Exception {
		assertEquals("100-continue", client.doGet(URL + "/expect").expect("100-continue").getResponseAsString());
		assertEquals("100-continue", client.doGet(URL + "/expect").query("Expect", "100-continue").getResponseAsString());
	}

	@Test
	public void from() throws Exception {
		assertEquals("foo", client.doGet(URL + "/from").from("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/from").query("From", "foo").getResponseAsString());
	}

	@Test
	public void host() throws Exception {
		assertTrue(client.doGet(URL + "/host").host("localhost").getResponseAsString().startsWith("localhost"));
		assertTrue(client.doGet(URL + "/host").query("Host", "localhost").getResponseAsString().startsWith("localhost"));
	}

	@Test
	public void ifMatch() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		assertEquals("\"foo\"", client.doGet(URL + "/ifMatch").ifMatch("foo").getResponseAsString());
		assertEquals("\"foo\"", client.doGet(URL + "/ifMatch").ifMatch("\"foo\"").getResponseAsString());
		assertEquals("W/\"foo\"", client.doGet(URL + "/ifMatch").ifMatch("W/\"foo\"").getResponseAsString());
		assertEquals("W/\"foo\", \"bar\"", client.doGet(URL + "/ifMatch").ifMatch("W/\"foo\",\"bar\"").getResponseAsString());
		assertEquals("\"foo\"", client.doGet(URL + "/ifMatch").query("If-Match", "foo").getResponseAsString());
	}

	@Test
	public void ifModifiedSince() throws Exception {
		assertEquals("foo", client.doGet(URL + "/ifModifiedSince").ifModifiedSince("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/ifModifiedSince").query("If-Modified-Since", "foo").getResponseAsString());
	}

	@Test
	public void ifNoneMatch() throws Exception {
		assertEquals("\"foo\"", client.doGet(URL + "/ifNoneMatch").ifNoneMatch("foo").getResponseAsString());
		assertEquals("\"foo\"", client.doGet(URL + "/ifNoneMatch").ifNoneMatch("\"foo\"").getResponseAsString());
		assertEquals("W/\"foo\"", client.doGet(URL + "/ifNoneMatch").ifNoneMatch("W/\"foo\"").getResponseAsString());
		assertEquals("W/\"foo\", \"bar\"", client.doGet(URL + "/ifNoneMatch").ifNoneMatch("W/\"foo\",\"bar\"").getResponseAsString());
		assertEquals("\"foo\"", client.doGet(URL + "/ifNoneMatch").query("If-None-Match", "foo").getResponseAsString());
	}

	@Test
	public void ifRange() throws Exception {
		assertEquals("foo", client.doGet(URL + "/ifRange").ifRange("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/ifRange").query("If-Range", "foo").getResponseAsString());
	}

	@Test
	public void ifUnmodifiedSince() throws Exception {
		assertEquals("foo", client.doGet(URL + "/ifUnmodifiedSince").ifUnmodifiedSince("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/ifUnmodifiedSince").query("If-Unmodified-Since", "foo").getResponseAsString());
	}

	@Test
	public void maxForwards() throws Exception {
		assertEquals("123", client.doGet(URL + "/maxForwards").maxForwards(123).getResponseAsString());
		assertEquals("123", client.doGet(URL + "/maxForwards").query("Max-Forwards", 123).getResponseAsString());
	}

	@Test
	public void pragma() throws Exception {
		assertEquals("foo", client.doGet(URL + "/pragma").pragma("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/pragma").query("Pragma", "foo").getResponseAsString());
	}

	@Test
	public void proxyAuthorization() throws Exception {
		assertEquals("foo", client.doGet(URL + "/proxyAuthorization").proxyAuthorization("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/proxyAuthorization").query("Proxy-Authorization", "foo").getResponseAsString());
	}

	@Test
	public void range() throws Exception {
		assertEquals("foo", client.doGet(URL + "/range").range("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/range").query("Range", "foo").getResponseAsString());
	}

	@Test
	public void referer() throws Exception {
		assertEquals("foo", client.doGet(URL + "/referer").referer("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/referer").query("Referer", "foo").getResponseAsString());
	}

	@Test
	public void te() throws Exception {
		assertEquals("foo", client.doGet(URL + "/te").te("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/te").query("TE", "foo").getResponseAsString());
	}

	@Test
	public void upgrade() throws Exception {
		assertEquals("foo", client.doGet(URL + "/upgrade").upgrade("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/upgrade").query("Upgrade", "foo").getResponseAsString());
	}

	@Test
	public void userAgent() throws Exception {
		assertEquals("foo", client.doGet(URL + "/userAgent").userAgent("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/userAgent").query("User-Agent", "foo").getResponseAsString());
	}

	@Test
	public void warning() throws Exception {
		assertEquals("foo", client.doGet(URL + "/warning").warning("foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/warning").query("Warning", "foo").getResponseAsString());
	}

	@Test
	public void customHeader() throws Exception {
		assertEquals("foo", client.doGet(URL + "/customHeader").header("Custom", "foo").getResponseAsString());
		assertEquals("foo", client.doGet(URL + "/customHeader").query("Custom", "foo").getResponseAsString());
	}

	//====================================================================================================
	// Default values.
	//====================================================================================================

	@Test
	public void defaultRequestHeaders() throws Exception {
		assertObjectEquals("{h1:'1',h2:'2',h3:'3'}", client.doGet(URL + "/defaultRequestHeaders").getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/defaultRequestHeaders").header("H1",4).header("H2",5).header("H3",6).getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/defaultRequestHeaders").header("h1",4).header("h2",5).header("h3",6).getResponse(ObjectMap.class));
	}

	@Test
	public void defaultRequestHeadersCaseInsensitive() throws Exception {
		assertObjectEquals("{h1:'1',h2:'2',h3:'3'}", client.doGet(URL + "/defaultRequestHeadersCaseInsensitive").getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/defaultRequestHeadersCaseInsensitive").header("H1",4).header("H2",5).header("H3",6).getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/defaultRequestHeadersCaseInsensitive").header("h1",4).header("h2",5).header("h3",6).getResponse(ObjectMap.class));
	}

	@Test
	public void annotatedHeaders() throws Exception {
		assertObjectEquals("{h1:null,h2:null,h3:null}", client.doGet(URL + "/annotatedHeaders").getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/annotatedHeaders").header("H1",4).header("H2",5).header("H3",6).getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/annotatedHeaders").header("h1",4).header("h2",5).header("h3",6).getResponse(ObjectMap.class));
	}

	@Test
	public void annotatedHeadersCaseInsensitive() throws Exception {
		assertObjectEquals("{h1:null,h2:null,h3:null}", client.doGet(URL + "/annotatedHeadersCaseInsensitive").getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/annotatedHeadersCaseInsensitive").header("H1",4).header("H2",5).header("H3",6).getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/annotatedHeadersCaseInsensitive").header("h1",4).header("h2",5).header("h3",6).getResponse(ObjectMap.class));
	}

	@Test
	public void annotatedHeadersDefault() throws Exception {
		assertObjectEquals("{h1:'1',h2:'2',h3:'3'}", client.doGet(URL + "/annotatedHeadersDefault").getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/annotatedHeadersDefault").header("H1",4).header("H2",5).header("H3",6).getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/annotatedHeadersDefault").header("h1",4).header("h2",5).header("h3",6).getResponse(ObjectMap.class));
	}

	@Test
	public void annotatedAndDefaultHeaders() throws Exception {
		assertObjectEquals("{h1:'4',h2:'5',h3:'6'}", client.doGet(URL + "/annotatedAndDefaultHeaders").getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'7',h2:'8',h3:'9'}", client.doGet(URL + "/annotatedAndDefaultHeaders").header("H1",7).header("H2",8).header("H3",9).getResponse(ObjectMap.class));
		assertObjectEquals("{h1:'7',h2:'8',h3:'9'}", client.doGet(URL + "/annotatedAndDefaultHeaders").header("h1",7).header("h2",8).header("h3",9).getResponse(ObjectMap.class));
	}
}
