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
package org.apache.juneau.rest;

import static javax.servlet.http.HttpServletResponse.*;

import java.util.*;

import javax.servlet.http.*;

/**
 * Represents a group of CallMethods on a REST resource that handle the same HTTP Method name but with different
 * paths/matchers/guards/etc...
 *
 * <p>
 * Incoming requests for a particular HTTP method type (e.g. <js>"GET"</js>) are handed off to this class and then
 * dispatched to the appropriate RestJavaMethod.
 */
public class RestCallRouter {
	private final RestJavaMethod[] restJavaMethods;

	RestCallRouter(RestJavaMethod[] callMethods) {
		this.restJavaMethods = callMethods;
	}

	/**
	 * Builder class.
	 */
	static final class Builder {
		private List<RestJavaMethod> childMethods = new ArrayList<>();
		private Set<String> collisions = new HashSet<>();
		private String httpMethodName;

		Builder(String httpMethodName) {
			this.httpMethodName = httpMethodName;
		}

		String getHttpMethodName() {
			return httpMethodName;
		}

		Builder add(RestJavaMethod m) throws RestServletException {
			if (! m.hasGuardsOrMatchers()) {
				String p = m.getHttpMethod() + ":" + m.getPathPattern();
				if (collisions.contains(p))
					throw new RestServletException("Duplicate Java methods assigned to the same method/pattern:  ''{0}''", p);
				collisions.add(p);
			}
			childMethods.add(m);
			return this;
		}

		RestCallRouter build() {
			Collections.sort(childMethods);
			return new RestCallRouter(childMethods.toArray(new RestJavaMethod[childMethods.size()]));
		}
	}

	/**
	 * Workhorse method.
	 *
	 * <p>
	 * Routes this request to one of the CallMethods.
	 *
	 * @param pathInfo The value of {@link HttpServletRequest#getPathInfo()} (sorta)
	 * @return The HTTP response code.
	 */
	int invoke(String pathInfo, RestRequest req, RestResponse res) throws Throwable {
		if (restJavaMethods.length == 1)
			return restJavaMethods[0].invoke(pathInfo, req, res);

		int maxRc = 0;
		for (RestJavaMethod m : restJavaMethods) {
			int rc = m.invoke(pathInfo, req, res);
			if (rc == SC_OK)
				return SC_OK;
			maxRc = Math.max(maxRc, rc);
		}
		return maxRc;
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder("RestCallRouter: [\n");
		for (RestJavaMethod sm : restJavaMethods)
			sb.append("\t" + sm + "\n");
		sb.append("]");
		return sb.toString();
	}
}