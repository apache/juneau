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

import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.mstat.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;

/**
 * A specialized invoker for methods that are called during a servlet request.
 */
public class RestOpInvoker extends MethodInvoker {

	private final RestOpArg[] opArgs;

	/**
	 * Constructor.
	 *
	 * @param m The method being wrapped.
	 * @param opArgs The parameter resolvers.
	 * @param stats The instrumentor.
	 */
	public RestOpInvoker(Method m, RestOpArg[] opArgs, MethodExecStats stats) {
		super(m, stats);
		this.opArgs = opArgs;
	}

	/**
	 * Invokes this method from the specified {@link RestSession}.
	 *
	 * @param opSession The REST call.
	 * @param resource The REST resource object.
	 * @return The results of the call.
	 * @throws BasicHttpException If an error occurred during either parameter resolution or method invocation.
	 */
	public Object invokeFromCall(RestOpSession opSession, Object resource) throws BasicHttpException {
		Object[] args = new Object[opArgs.length];
		for (int i = 0; i < opArgs.length; i++) {
			ParamInfo pi = inner().getParam(i);
			try {
				args[i] = opArgs[i].resolve(opSession);
			} catch (Exception e) {
				throw toHttpException(e, BadRequest.class, "Could not resolve parameter {0} of type ''{1}'' on method ''{2}''.", i, pi.getParameterType(), getFullName());
			}
		}
		try {
			return invoke(resource, args);
		} catch (ExecutableException e) {
			throw toHttpException(e.unwrap(), InternalServerError.class, "Method ''{0}'' threw an unexpected exception.", getFullName());
		}
	}
}
