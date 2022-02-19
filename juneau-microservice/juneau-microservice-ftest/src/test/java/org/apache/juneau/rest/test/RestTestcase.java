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

import static org.apache.juneau.internal.CollectionUtils.*;

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

	// Reusable RestClients keyed by label that live for the duration of a testcase class.
	private static Map<String,RestClient> clients = map();

	// Reusable object cache that lives for the duration of a testcase class.
	private static Map<String,Object> cache = map();

	@BeforeClass
	public static void setUp() {
		microserviceStarted = TestMicroservice.startMicroservice();
	}

	/**
	 * Creates a REST client against the test microservice using the specified serializer and parser.
	 * Client is automatically closed on tear-down.
	 */
	protected RestClient getClient(String label, Serializer serializer, Parser parser) {
		if (! clients.containsKey(label))
			clients.put(label, TestMicroservice.client(serializer, parser).build());
		return clients.get(label);
	}

	/**
	 * Same as {@link #getClient(String, Serializer, Parser)} but sets the debug flag on the client.
	 */
	protected RestClient getDebugClient(String label, Serializer serializer, Parser parser) {
		if (! clients.containsKey(label))
			clients.put(label, TestMicroservice.client(serializer, parser).debug().build());
		return clients.get(label);
	}

	protected void addClientToLifecycle(RestClient c) {
		clients.put(UUID.randomUUID().toString(), c);
	}

	@SuppressWarnings("unchecked")
	protected <T> T getCached(String label, Class<T> c) {
		return (T)cache.get(label);
	}

	protected void cache(String label, Object o) {
		cache.put(label, o);
	}

	@AfterClass
	public static void tearDown() {
		if (microserviceStarted)
			TestMicroservice.stopMicroservice();
		for (RestClient rc : clients.values()) {
			try {
				rc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		clients.clear();
		cache.clear();
	}
}
