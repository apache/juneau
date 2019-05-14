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
package org.apache.juneau.rest.mock;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.utils.*;

/**
 * Creates a mocked interface against a REST resource class.
 *
 * <p>
 * Allows you to test your REST resource classes without a running servlet container.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 *  <jk>public class</jk> MockTest {
 *
 *  	<jc>// Our REST resource to test.</jc>
 *  	<ja>@RestResource</ja>(serializers=JsonSerializer.Simple.<jk>class</jk>, parsers=JsonParser.<jk>class</jk>)
 *  	<jk>public static class</jk> MyRest {
 *
 *  		<ja>@RestMethod</ja>(name=<jsf>PUT</jsf>, path=<js>"/String"</js>)
 *  		<jk>public</jk> String echo(<ja>@Body</ja> String b) {
 *  			<jk>return</jk> b;
 *  		}
 *  	}
 *
 *  <ja>@Test</ja>
 *  <jk>public void</jk> testEcho() <jk>throws</jk> Exception {
 *  	MockRest
 *  		.<jsf>create</jsf>(MyRest.<jk>class</jk>)
 *  		.put(<js>"/String"</js>, <js>"'foo'"</js>)
 *  		.execute()
 *  		.assertStatus(200)
 *  		.assertBody(<js>"'foo'"</js>);
 *  }
 * </p>
 *
 * @deprecated Use <code>org.apache.juneau.rest.mock2</code>
 */
@Deprecated
public class MockRest implements MockHttpConnection {
	private static Map<Class<?>,RestContext> CONTEXTS = new ConcurrentHashMap<>();

	private final RestContext rc;

	private MockRest(Class<?> c, boolean debug) throws Exception {
		if (! CONTEXTS.containsKey(c)) {
			Object r = c.newInstance();
			RestContext rc = RestContext.create(r).logger(debug ? BasicRestLogger.class : NoOpRestLogger.class).build();
			if (r instanceof RestServlet) {
				((RestServlet)r).setContext(rc);
			} else {
				rc.postInit();
			}
			rc.postInitChildFirst();
			CONTEXTS.put(c, rc);
		}
		rc = CONTEXTS.get(c);
	}

	/**
	 * Create a new mock REST interface
	 *
	 * @param c The REST class.
	 * @return A new mock interface.
	 * @throws RuntimeException
	 * 	For testing conveniences, this method wraps all exceptions in a RuntimeException so that you can easily define mocks as reusable fields.
	 */
	public static MockRest create(Class<?> c) throws RuntimeException {
		return create(c, false);
	}

	/**
	 * Create a new mock REST interface
	 *
	 * @param c The REST class.
	 * @param debug
	 * 	If <jk>true</jk>, the REST interface will use the {@link BasicRestLogger} for logging.
	 * 	<br>Otherwise, uses {@link NoOpRestLogger}.
	 * @return A new mock interface.
	 * @throws RuntimeException
	 * 	For testing conveniences, this method wraps all exceptions in a RuntimeException so that you can easily define mocks as reusable fields.
	 */
	public static MockRest create(Class<?> c, boolean debug) throws RuntimeException {
		try {
			return new MockRest(c, debug);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @param body The body of the request.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	@Override /* MockHttpConnection */
	public MockServletRequest request(String method, String path, Object body) throws Exception {
		return MockServletRequest.create(method, path).body(body).restContext(rc);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest request(String method, String path) throws Exception {
		return request(method, path, null);
	}

	/**
	 * Perform a GET request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest get(String path) throws Exception {
		return request("GET", path, null);
	}

	/**
	 * Perform a PUT request.
	 *
	 * @param path The URI path.
	 * @param body The body of the request.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest put(String path, Object body) throws Exception {
		return request("PUT", path, body);
	}

	/**
	 * Perform a POST request.
	 *
	 * @param path The URI path.
	 * @param body The body of the request.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest post(String path, Object body) throws Exception {
		return request("POST", path, body);
	}

	/**
	 * Perform a DELETE request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest delete(String path) throws Exception {
		return request("DELETE", path, null);
	}

	/**
	 * Perform an OPTIONS request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest options(String path) throws Exception {
		return request("OPTIONS", path, null);
	}
}
