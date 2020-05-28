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
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * An implementation of a shim that allows HTTP clients to make direct calls to <ja>@Rest</ja>-annotated resources.
 */
public class MockHttpConnectionImpl implements MockHttpConnection {
	private static Map<Class<?>,RestContext>
		CONTEXTS_DEBUG = new ConcurrentHashMap<>(),
		CONTEXTS_NORMAL = new ConcurrentHashMap<>();

	private final RestContext restBeanCtx;
	private final boolean debug;
	private final String contextPath, servletPath;

	/**
	 * Constructor.
	 *
	 * @param restBean The {@link Rest @Rest} annotated servlet class or instance
	 * @param contextPath The context path of the servlet.
	 * @param servletPath The servlet path of the servlet.
	 * @param debug Enable debug mode on the servlet side.
	 */
	protected MockHttpConnectionImpl(Object restBean, String contextPath, String servletPath, boolean debug) {
		try {
			this.debug = debug;
			Class<?> c = restBean instanceof Class ? (Class<?>)restBean : restBean.getClass();
			Map<Class<?>,RestContext> contexts = debug ? CONTEXTS_DEBUG : CONTEXTS_NORMAL;
			if (! contexts.containsKey(c)) {
				Object o = restBean instanceof Class ? ((Class<?>)restBean).newInstance() : restBean;
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
			restBeanCtx = contexts.get(c);
			this.contextPath = contextPath;
			if (servletPath.isEmpty())
				servletPath = toValidContextPath(restBeanCtx.getPath());
			this.servletPath = servletPath;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	String getResourcePath() {
		return new StringBuilder().append(contextPath).append(servletPath).toString();
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param uri The request URI.
	 * @param headers Headers to include in the request.  Not <jk>null</jk>.
	 * @return A new servlet request.
	 */
	@Override /* MockHttpConnection */
	public MockServletRequest request(String method, String uri, Header[] headers) {

		MockPathResolver pr = new MockPathResolver(null, contextPath, servletPath, uri, null);
		if (pr.getError() != null)
			throw new RuntimeException(pr.getError());

		MockServletRequest r = MockServletRequest
			.create(method, pr.getURI())
			.contextPath(pr.getContextPath())
			.servletPath(pr.getServletPath())
			.debug(debug)
			.restContext(restBeanCtx);

		for (Header h : headers) {
			String n = h.getName(), v = h.getValue();
			if (n.equals("X-Roles")) {
				r.roles(split(v, ','));
			} else {
				r.header(n, v);
			}
		}

		return r;
	}
}
