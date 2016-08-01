/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;

public class CT_TestCallbackStrings {

	//====================================================================================================
	// Basic tests using &Content parameter
	//====================================================================================================
	@Test
	public void test() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/json+simple");
		String r;

		r = c.doCallback("GET /testCallback").getResponseAsString();
		assertEquals("{method:'GET',headers:{},content:''}", r);

		r = c.doCallback("GET /testCallback some sample content").getResponseAsString();
		assertEquals("{method:'GET',headers:{},content:'some sample content'}", r);

		r = c.doCallback("GET {Foo-X:123,Foo-Y:'abc'} /testCallback").getResponseAsString();
		assertEquals("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:''}", r);

		r = c.doCallback("GET  { Foo-X : 123, Foo-Y : 'abc' } /testCallback").getResponseAsString();
		assertEquals("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:''}", r);

		r = c.doCallback("GET {Foo-X:123,Foo-Y:'abc'} /testCallback   some sample content  ").getResponseAsString();
		assertEquals("{method:'GET',headers:{'Foo-X':'123','Foo-Y':'abc'},content:'some sample content'}", r);

		r = c.doCallback("PUT {Foo-X:123,Foo-Y:'abc'} /testCallback   some sample content  ").getResponseAsString();
		assertEquals("{method:'PUT',headers:{'Foo-X':'123','Foo-Y':'abc'},content:'some sample content'}", r);

		c.closeQuietly();
	}
}
