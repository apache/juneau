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

import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.*;
import org.apache.juneau.microservice.jetty.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;

/**
 * Utility class for starting up the tests microservice.
 * @author james.bognar
 */
public class TestMicroservice {

	static JettyMicroservice microservice;
	static URI microserviceURI;

	// Reusable HTTP clients that get created and shut down with the microservice.
	public static RestClient DEFAULT_CLIENT, DEFAULT_CLIENT_DEBUG;
	public static RestClient DEFAULT_CLIENT_PLAINTEXT;

	/**
	 * Starts the microservice.
	 * @return <jk>true</jk> if the service started, <jk>false</jk> if it's already started.
	 * If this returns <jk>false</jk> then don't call stopMicroservice()!.
	 */
	public static boolean startMicroservice() {
		if (microservice != null)
			return false;
		try {
			Locale.setDefault(Locale.US);
			microservice = JettyMicroservice
				.create()
				.configName("juneau-microservice-test.cfg")
				.manifest(
					new StringReader("Test-Entry: test-value\n")
				)
				.servlet(Root.class)
				.build()
			;
			microserviceURI = microservice.start().getURI();
			DEFAULT_CLIENT = client().json().build();
			DEFAULT_CLIENT_DEBUG = client().json().debug().build();
			DEFAULT_CLIENT_PLAINTEXT = client().plainText().build();
			return true;
		} catch (Throwable e) {
			e.printStackTrace(); // NOT DEBUG
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
	public static void stopMicroservice() {
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
			final RequestLine[] currentRequest = new RequestLine[1];
			final StatusLine[] currentResponse = new StatusLine[1];
			RestClient.Builder rc = RestClient.create()
				.json()
				.rootUrl(microserviceURI)
				.noTrace();
			rc.retryHandler(
					new HttpRequestRetryHandler() {
						@Override
						public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
							System.err.println("*** ERROR ***");  // NOT DEBUG
							TestMicroservice.jettyDump(currentRequest[0], currentResponse[0]);
							return (executionCount < 3);
						}
					}
				)
				.addInterceptorFirst(new org.apache.http.HttpRequestInterceptor() {
					@Override
					public void process(HttpRequest r, HttpContext c) throws HttpException, IOException {
						currentRequest[0] = r.getRequestLine();
					}
				})
				.addInterceptorFirst(new org.apache.http.HttpResponseInterceptor() {
					@Override
					public void process(HttpResponse r, HttpContext c) throws HttpException, IOException {
						currentResponse[0] = r.getStatusLine();
					}
				});
			return rc;
		} catch (Exception e) {
			throw asRuntimeException(e);
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

	public static CloseableHttpClient createHttpClient() {
		try {
			return HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}

	private static int dumpCount = 0;

	public static void jettyDump(RequestLine rl, StatusLine sl) {
		try (FileWriter fw = new FileWriter(microservice.getConfig().get("Logging/logDir").orElse(".") + "/jetty-thread-dump-"+(dumpCount++)+".log")) {
			String dump = microservice.getServer().dump();
			fw.append("RequestLine = [" + rl + "]\n");
			fw.append("StatusLine = [" + sl + "]\n");
			fw.append(dump);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
