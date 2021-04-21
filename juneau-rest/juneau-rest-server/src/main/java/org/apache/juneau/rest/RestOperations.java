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

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Encapsulates the set of {@link RestOp}-annotated methods within a single {@link Rest}-annotated object.
 */
public class RestOperations {

	/**
	 * Represents a null value for the {@link Rest#restOperationsClass()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public final class Null extends RestOperations {
		public Null(RestOperationsBuilder builder) throws Exception {
			super(builder);
		}
	}

	private final Map<String,List<RestOperationContext>> map;
	private List<RestOperationContext> list;

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static RestOperationsBuilder create() {
		return new RestOperationsBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this object.
	 */
	public RestOperations(RestOperationsBuilder builder) {
		AMap<String,List<RestOperationContext>> m = AMap.create();
		for (Map.Entry<String,TreeSet<RestOperationContext>> e : builder.map.entrySet())
			m.put(e.getKey(), AList.of(e.getValue()));
		this.map = m;
		this.list = AList.of(builder.set);
	}

	/**
	 * Finds the method that should handle the specified call.
	 *
	 * @param call The HTTP call.
	 * @return The method that should handle the specified call.
	 * @throws MethodNotAllowed If no methods implement the requested HTTP method.
	 * @throws PreconditionFailed At least one method was found but it didn't match one or more matchers.
	 * @throws NotFound HTTP method match was found but matching path was not.
	 */
	public RestOperationContext findOperation(RestCall call) throws MethodNotAllowed, PreconditionFailed, NotFound {
		String m = call.getMethod();

		int rc = 0;
		if (map.containsKey(m)) {
			for (RestOperationContext oc : map.get(m)) {
				int mrc = oc.match(call);
				if (mrc == 2)
					return oc;
				rc = Math.max(rc, mrc);
			}
		}

		if (map.containsKey("*")) {
			for (RestOperationContext oc : map.get("*")) {
				int mrc = oc.match(call);
				if (mrc == 2)
					return oc;
				rc = Math.max(rc, mrc);
			}
		}

		// If no paths matched, see if the path matches any other methods.
		// Note that we don't want to match against "/*" patterns such as getOptions().
		if (rc == 0) {
			for (RestOperationContext oc : list) {
				if (! oc.getPathPattern().endsWith("/*")) {
					int orc = oc.match(call);
					if (orc == 2)
						throw new MethodNotAllowed();
				}
			}
		}

		if (rc == 1)
			throw new PreconditionFailed("Method ''{0}'' not found on resource on path ''{1}'' with matching matcher.", m, call.getPathInfo());

		throw new NotFound("Java method matching path ''{0}'' not found on resource ''{1}''.", call.getPathInfo(), className(call.getResource()));
	}


	/**
	 * Returns the list of method contexts in this object.
	 *
	 * @return An unmodifiable list of method contexts in this object.
	 */
	public List<RestOperationContext> getOperationContexts() {
		return Collections.unmodifiableList(list);
	}
}
