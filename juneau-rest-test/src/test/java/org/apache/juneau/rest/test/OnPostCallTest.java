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

import java.io.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

public class OnPostCallTest extends RestTestcase {

	private static String URL = "/testOnPostCall";

	//====================================================================================================
	// Properties overridden via properties annotation.
	//====================================================================================================
	@Test
	public void testPropertiesOverridenByAnnotation() throws Exception {
		RestClient client = new TestRestClient().setAccept("text/s1");
		String url = URL + "/testPropertiesOverridenByAnnotation";
		String r;
		RestCall rc;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s1", r);

		r = client.doPut(url, new StringReader("")).setHeader("Override-Accept", "text/s2").getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2", r);

		rc = client.doPut(url, new StringReader("")).setHeader("Override-Content-Type", "text/s3").connect();
		r = rc.getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s1", r);
		assertTrue(rc.getResponse().getFirstHeader("Content-Type").getValue().startsWith("text/s3"));

		client.closeQuietly();
	}

	//====================================================================================================
	// Properties overridden via properties annotation.  Default Accept header.
	//====================================================================================================
	@Test
	public void testPropertiesOverridenByAnnotationDefaultAccept() throws Exception {
		RestClient client = new TestRestClient().setAccept("");
		String url = URL + "/testPropertiesOverridenByAnnotation";
		String r;
		RestCall rc;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2", r);

		r = client.doPut(url, new StringReader("")).setHeader("Override-Accept", "text/s3").getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s3", r);

		rc = client.doPut(url, new StringReader("")).setHeader("Override-Content-Type", "text/s3").connect();
		r = rc.getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/s2", r);
		assertTrue(rc.getResponse().getFirstHeader("Content-Type").getValue().startsWith("text/s3"));

		client.closeQuietly();
	}

	//====================================================================================================
	// Properties overridden programmatically.
	//====================================================================================================
	@Test
	public void testPropertiesOverriddenProgramatically() throws Exception {
		RestClient client = new TestRestClient().setAccept("text/s1");
		String url = URL + "/testPropertiesOverriddenProgramatically";
		String r;
		RestCall rc;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s1", r);

		r = client.doPut(url, new StringReader("")).setHeader("Override-Accept", "text/s2").getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2", r);

		rc = client.doPut(url, new StringReader("")).setHeader("Override-Content-Type", "text/s3").connect();
		r = rc.getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s1", r);
		assertTrue(rc.getResponse().getFirstHeader("Content-Type").getValue().startsWith("text/s3"));

		client.closeQuietly();
	}

	//====================================================================================================
	// Properties overridden programmatically.  Default Accept header.
	//====================================================================================================
	@Test
	public void testPropertiesOverriddenProgramaticallyDefaultAccept() throws Exception {
		RestClient client = new TestRestClient().setAccept("");
		String url = URL + "/testPropertiesOverriddenProgramatically";
		String r;
		RestCall rc;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2", r);

		r = client.doPut(url, new StringReader("")).setHeader("Override-Accept", "text/s3").getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s3", r);

		rc = client.doPut(url, new StringReader("")).setHeader("Override-Content-Type", "text/s3").connect();
		r = rc.getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=xp4,p5=xp5,contentType=text/s2", r);
		assertTrue(rc.getResponse().getFirstHeader("Content-Type").getValue().startsWith("text/s3"));

		client.closeQuietly();
	}
}
