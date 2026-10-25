/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.examples.guides.secureanendpoint;

// region resource
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * An endpoint that only answers requests carrying a valid bearer token.
 */
@Rest(title="Orders API")
public class SecureResource extends RestServlet implements BasicJsonConfig {

	private static final long serialVersionUID = 1L;

	private final transient TokenValidator validator;

	public SecureResource(TokenValidator validator) {
		this.validator = validator;
	}

	// Every request must pass this guard before any @RestOp method runs.
	@Bean
	public RestGuardList guards(BeanStore beanStore) {
		return RestGuardList.create(beanStore)
			.append(BearerTokenGuard.create().realm("orders").validator(validator).build())
			.build();
	}

	@RestGet(path="/orders")
	public String orders(@Auth ClaimsPrincipal caller) {
		if (! hasScope(caller, "orders:read"))
			throw new Forbidden("Missing scope orders:read");
		return "orders for " + caller.getName();
	}

	private static boolean hasScope(ClaimsPrincipal caller, String wanted) {
		// A JWT carries "scope" as a space-separated string; an introspection result carries it as a list.
		var scope = caller.getClaims().get("scope");
		if (scope instanceof Collection<?> c)
			return c.contains(wanted);
		return scope != null && Arrays.asList(scope.toString().split(" ")).contains(wanted);
	}
}
// endregion resource
