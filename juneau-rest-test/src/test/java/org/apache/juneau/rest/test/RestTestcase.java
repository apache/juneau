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

import java.io.*;
import java.util.*;

import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Superclass of REST testcases that start up the REST test microservice before running the tests locally.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class RestTestcase {

	private static boolean microserviceStarted;
	private static List<RestClient> clients = new ArrayList<RestClient>();

	@BeforeClass
	public static void setUp() {
		microserviceStarted = TestMicroservice.startMicroservice();
	}

	/**
	 * Creates a REST client against the test microservice using the specified serializer and parser.
	 * Client is automatically closed on tear-down.
	 */
	protected RestClient getClient(Serializer serializer, Parser parser) {
		RestClient rc = TestMicroservice.client(serializer, parser).build();
		clients.add(rc);
		return rc;
	}

	/**
	 * Same as {@link #getClient(Serializer, Parser)} but sets the debug flag on the client.
	 */
	protected RestClient getDebugClient(Serializer serializer, Parser parser) {
		RestClient rc = TestMicroservice.client(serializer, parser).debug(true).build();
		clients.add(rc);
		return rc;
	}

	@AfterClass
	public static void tearDown() {
		if (microserviceStarted)
			TestMicroservice.stopMicroservice();
		for (RestClient rc : clients) {
			try {
				rc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
