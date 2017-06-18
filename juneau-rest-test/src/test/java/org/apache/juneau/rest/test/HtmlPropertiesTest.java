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

import static org.junit.Assert.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

/**
 * Tests HTML page titles, text, and links.
 */
public class HtmlPropertiesTest extends RestTestcase {

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(pageX).
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalTest1() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/Normal/test1").accept("text/html").getResponseAsString();
		assertTrue(s.contains("Normal-title"));
		assertTrue(s.contains("Normal-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(pageX), overridden by @RestMethod(pageX) annotations.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalTest2() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/Normal/test2").accept("text/html").getResponseAsString();
		assertTrue(s.contains("Normal.test2-title"));
		assertTrue(s.contains("Normal.test2-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(pageX), overridden by RestResponse.setPageX() methods.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalTest3() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/Normal/test3").accept("text/html").getResponseAsString();
		assertTrue(s.contains("Normal.test3-title"));
		assertTrue(s.contains("Normal.test3-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(pageX), overridden by RestResponse.setProperty() method.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalTest4() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/Normal/test4").accept("text/html").getResponseAsString();
		assertTrue(s.contains("Normal.test4-title"));
		assertTrue(s.contains("Normal.test4-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from RestConfig.setX() methods.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalInitTest1() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalInit/test1").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalInit-title"));
		assertTrue(s.contains("NormalInit-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from RestConfig.setX() methods, overridden by @RestMethod(pageX) annotations.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalInitTest2() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalInit/test2").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalInit.test2-title"));
		assertTrue(s.contains("NormalInit.test2-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from RestConfig.setX() methods, overridden by RestResponse.setPageX() methods.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalInitTest3() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalInit/test3").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalInit.test3-title"));
		assertTrue(s.contains("NormalInit.test3-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from RestConfig.setX() methods, overridden by RestResponse.setProperty() method.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalInitTest4() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalInit/test4").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalInit.test4-title"));
		assertTrue(s.contains("NormalInit.test4-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(path/title).
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalDefaultingTest1() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalDefaulting/test1").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalDefaulting-title"));
		assertTrue(s.contains("NormalDefaulting-description"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(path/title), overridden by @RestMethod(pageX) annotations.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalDefaultingTest2() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalDefaulting/test2").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalDefaulting-title"));
		assertTrue(s.contains("NormalDefaulting.test2-summary"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(path/title), overridden by RestResponse.setPageX() methods.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalDefaultingTest3() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalDefaulting/test3").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalDefaulting.test3-title"));
		assertTrue(s.contains("NormalDefaulting.test3-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(path/title), overridden by RestResponse.setProperty() method.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalDefaultingTest4() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalDefaulting/test4").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalDefaulting.test4-title"));
		assertTrue(s.contains("NormalDefaulting.test4-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from parent @RestResource(path/title).
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalSubclassed1Test1() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalSubclassed1/test1").accept("text/html").getResponseAsString();
		assertTrue(s.contains("Normal-title"));
		assertTrue(s.contains("Normal-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from parent @RestResource(path/title), overridden by @RestMethod(pageX) annotations.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalSubclassed1Test2() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalSubclassed1/test2").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalSubclassed1.test2-title"));
		assertTrue(s.contains("NormalSubclassed1.test2-text"));

	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from parent @RestResource(path/title), overridden by child @RestResource(pageTitle/pageText).
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalSubclassed2Test1() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalSubclassed2/test1").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalSubclassed2-title"));
		assertTrue(s.contains("NormalSubclassed2-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from parent @RestResource(path/title), overridden by @RestMethod(pageX).
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testNormalSubclassed2Test2() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/NormalSubclassed2/test2").accept("text/html").getResponseAsString();
		assertTrue(s.contains("NormalSubclassed2.test2-title"));
		assertTrue(s.contains("NormalSubclassed2.test2-text"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(pageX) with $L variables.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testLocalizedExplicitTest1() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/LocalizedExplicit/test1").accept("text/html").getResponseAsString();
		assertTrue(s.contains("LocalizedExplicit.nls.pageTitle"));
		assertTrue(s.contains("LocalizedExplicit.nls.pageText"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(pageX) with $L variables, overridden by @RestMethod(pageX) with $L variables.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testLocalizedExplicitTest2() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/LocalizedExplicit/test2").accept("text/html").getResponseAsString();
		assertTrue(s.contains("LocalizedExplicit.test2.nls.pageTitle"));
		assertTrue(s.contains("LocalizedExplicit.test2.nls.pageText"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(pageX) with $L variables, overridden by RestResponse.setPageX() with $L variables.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testLocalizedExplicitTest3() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/LocalizedExplicit/test3").accept("text/html").getResponseAsString();
		assertTrue(s.contains("LocalizedExplicit.test3.nls.pageTitle"));
		assertTrue(s.contains("LocalizedExplicit.test3.nls.pageText"));
	}

	//----------------------------------------------------------------------------------------------------
	// Values pulled from @RestResource(pageX) with $L variables, overridden by RestResponse.setProperty() with $L variables.
	//----------------------------------------------------------------------------------------------------
	@Test
	public void testLocalizedExplicitTest4() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String s = client.doGet("/testHtmlProperties/LocalizedExplicit/test4").accept("text/html").getResponseAsString();
		assertTrue(s.contains("LocalizedExplicit.test4.nls.pageTitle"));
		assertTrue(s.contains("LocalizedExplicit.test4.nls.pageText"));
	}
}
