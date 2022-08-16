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
package org.apache.juneau.examples.rest;

import java.net.*;
import java.util.*;

import org.apache.juneau.microservice.jetty.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;

/**
 * Utility class for starting up the examples microservice.
 */
public class SamplesMicroservice {
	static volatile JettyMicroservice microservice;
	static URI microserviceURI;

	// Reusable HTTP clients that get created and shut down with the microservice.
	public static RestClient DEFAULT_CLIENT;
	public static RestClient DEFAULT_CLIENT_PLAINTEXT;

	/**
	 * Starts the microservice.
	 * @return <jk>true</jk> if the service started, <jk>false</jk> if it's already started.
	 * If this returns <jk>false</jk> then don't call stopMicroservice()!.
	 */
	public synchronized static boolean startMicroservice() {
		if (microservice != null)
			return false;
		try {
			Locale.setDefault(Locale.US);
			microservice = JettyMicroservice.create().workingDir("../juneau-examples-rest-jetty").configName("juneau-examples-rest-jetty.cfg").servlet(RootResources.class).build();
			microserviceURI = microservice.start().getURI();
			DEFAULT_CLIENT = client().json().build();
			DEFAULT_CLIENT_PLAINTEXT = client().plainText().build();
			return true;
		} catch (Throwable e) {
			// Probably already started.
			e.printStackTrace();
			System.err.println(e); // NOT DEBUG
			return false;
		}
	}

	/**
	 * Returns the URI of the microservice.
	 * @return The URI of the microservice.
	 */
	public static URI getURI() {
		if (microservice == null)
			startMicroservice();
		return microserviceURI;
	}

	/**
	 * Stops the microservice.
	 */
	public synchronized static void stopMicroservice() {
		try {
			microservice.stop();
			microservice = null;
			DEFAULT_CLIENT.closeQuietly();
			DEFAULT_CLIENT_PLAINTEXT.closeQuietly();
		} catch (Exception e) {
			System.err.println(e); // NOT DEBUG
		}
	}

	/**
	 * Create a new HTTP client.
	 */
	public static RestClient.Builder client() {
		try {
			return RestClient.create().rootUrl(microserviceURI);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a new HTTP client using the specified serializer and parser.
	 */
	public static RestClient.Builder client(Serializer s, Parser p) {
		return client().serializer(s).parser(p);
	}

	/**
	 * Create a new HTTP client using the specified serializer and parser.
	 */
	public static RestClient.Builder client(Class<? extends Serializer> s, Class<? extends Parser> p) {
		return client().serializer(s).parser(p);
	}
}
