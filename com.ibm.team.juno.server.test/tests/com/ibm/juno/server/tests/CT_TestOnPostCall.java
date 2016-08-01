/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import com.ibm.juno.client.*;

public class CT_TestOnPostCall {

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
