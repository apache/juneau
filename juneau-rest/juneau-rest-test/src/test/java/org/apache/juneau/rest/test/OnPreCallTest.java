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

public class OnPreCallTest extends RestTestcase {

	private static String URL = "/testOnPreCall";

	//====================================================================================================
	// Properties overridden via properties annotation.
	//====================================================================================================
	@Test
	public void testPropertiesOverriddenByAnnotation() throws Exception {
		RestClient client = TestMicroservice.client().contentType("text/a1").accept("text/plain").build();
		String url = URL + "/testPropertiesOverriddenByAnnotation";
		String r;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/a1", r);

		r = client.doPut(url, new StringReader("")).header("Override-Content-Type", "text/a2").getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=mp3,p4=xp4,p5=xp5,contentType=text/a2", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Properties overridden programmatically.
	//====================================================================================================
	@Test
	public void testPropertiesOverriddenProgrammatically() throws Exception {
		RestClient client = TestMicroservice.client().contentType("text/a1").accept("text/plain").build();
		String url = URL + "/testPropertiesOverriddenProgrammatically";
		String r;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5,contentType=text/a1", r);

		r = client.doPut(url, new StringReader("")).header("Override-Content-Type", "text/a2").getResponseAsString();
		assertEquals("p1=sp1,p2=xp2,p3=pp3,p4=pp4,p5=xp5,contentType=text/a2", r);

		client.closeQuietly();
	}
}
