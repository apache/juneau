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

import java.lang.reflect.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.stats.*;

/**
 * A specialized invoker for methods that are called during a servlet request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.RestContext">RestContext</a>
 * </ul>
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
	 * @throws Throwable If an error occurred during either parameter resolution or method invocation.
	 */
	public void invoke(RestOpSession opSession) throws Throwable {
		Object[] args = new Object[opArgs.length];
		for (int i = 0; i < opArgs.length; i++) {
			ParamInfo pi = inner().getParam(i);
			try {
				args[i] = opArgs[i].resolve(opSession);
			} catch (BasicHttpException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequest(e, "Could not resolve parameter {0} of type ''{1}'' on method ''{2}''.", i, pi.getParameterType(), getFullName());
			}
		}
		try {
			RestSession session = opSession.getRestSession();
			RestRequest req = opSession.getRequest();
			RestResponse res = opSession.getResponse();

			Object output = super.invoke(session.getResource(), args);

			// Handle manual call to req.setDebug().
			Boolean debug = req.getAttribute("Debug").as(Boolean.class).orElse(null);
			if (debug == Boolean.TRUE) {
				session.debug(true);
			} else if (debug == Boolean.FALSE) {
				session.debug(false);
			}

			if (! inner().hasReturnType(Void.TYPE))
				if (output != null || ! res.getOutputStreamCalled())
					res.setContent(output);

		} catch (IllegalAccessException|IllegalArgumentException e) {
			throw new InternalServerError(e, "Error occurred invoking method ''{0}''.", inner().getFullName());
		} catch (InvocationTargetException e) {
			RestResponse res = opSession.getResponse();
			Throwable e2 = e.getTargetException();
			res.setStatus(500);  // May be overridden later.
			res.setContent(opSession.getRestContext().convertThrowable(e2));
		}
	}
}
