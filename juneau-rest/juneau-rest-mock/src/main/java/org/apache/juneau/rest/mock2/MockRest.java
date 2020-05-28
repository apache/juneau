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
package org.apache.juneau.rest.mock2;

import static org.apache.juneau.rest.util.RestUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

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
 *  	<ja>@Rest</ja>(serializers=JsonSerializer.Simple.<jk>class</jk>, parsers=JsonParser.<jk>class</jk>)
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
 *  		.<jsm>create</jsm>(MyRest.<jk>class</jk>)
 *  		.json()
 *  		.build()
 *  		.put(<js>"/String"</js>, <js>"'foo'"</js>)
 *  		.execute()
 *  		.assertStatus(200)
 *  		.assertBody(<js>"'foo'"</js>);
 *  }
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-mock.MockRest}
 * </ul>
 */
public class MockRest implements MockHttpConnection {
	private static Map<Class<?>,RestContext> CONTEXTS_DEBUG = new ConcurrentHashMap<>(), CONTEXTS_NORMAL = new ConcurrentHashMap<>();

	private final RestContext ctx;

	/** Debug mode enabled. */
	private final boolean debug;

	final String contextPath, servletPath, rootUrl;

	/**
	 * Constructor.
	 *
	 * @param impl The {@link Rest @Rest} annotated servlet.
	 * @param contextPath The context path of the servlet.
	 * @param servletPath The servlet path of the servlet.
	 * @param debug Enable debug mode on the servlet side.
	 */
	protected MockRest(Object impl, String contextPath, String servletPath, boolean debug) {
		try {
			this.debug = debug;
			Class<?> c = impl instanceof Class ? (Class<?>)impl : impl.getClass();
			Map<Class<?>,RestContext> contexts = debug ? CONTEXTS_DEBUG : CONTEXTS_NORMAL;
			if (! contexts.containsKey(c)) {
				Object o = impl instanceof Class ? ((Class<?>)impl).newInstance() : impl;
				RestContextBuilder rcb = RestContext.create(o);
				if (debug) {
					rcb.debug(Enablement.TRUE);
					rcb.callLoggerConfig(RestCallLoggerConfig.DEFAULT_DEBUG);
				}
				RestContext rc = rcb.build();
				if (o instanceof RestServlet) {
					((RestServlet)o).setContext(rc);
				} else {
					rc.postInit();
				}
				rc.postInitChildFirst();
				contexts.put(c, rc);
			}
			ctx = contexts.get(c);
			this.contextPath = contextPath;
			if (servletPath.isEmpty())
				servletPath = toValidContextPath(ctx.getPath());
			this.servletPath = servletPath;
			rootUrl = new StringBuilder().append(contextPath).append(servletPath).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param uri The request URI.
	 * @param headers Optional headers to include in the request.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <c>toString()</c> method.
	 * @return A new servlet request.
	 */
	@Override /* MockHttpConnection */
	public MockServletRequest request(String method, String uri, Header[] headers, Object body) {
		MockPathResolver pr = new MockPathResolver(null, contextPath, servletPath == null ? ctx.getPath() : servletPath, uri, null);
		if (pr.getError() != null)
			throw new RuntimeException(pr.getError());

		MockServletRequest r = MockServletRequest.create(method, pr.getURI())
			.contextPath(pr.getContextPath())
			.servletPath(pr.getServletPath())
			.body(body)
			.debug(debug)
			.restContext(ctx);

		if (headers != null) {
			for (Header h : headers) {
				if (h.getName().equals("X-Roles")) {
					r.roles(StringUtils.split(h.getValue().toString(), ','));
				} else {
					r.header(h.getName(), h.getValue());
				}
			}
		}

		return r;
	}
}
