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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.parser.*;

/**
 * A specialized {@link RestOperationContext} for handling <js>"RRPC"</js> HTTP methods.
 */
public class RrpcRestOperationContext extends RestOperationContext {

	private final RrpcInterfaceMeta meta;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this method context.
	 * @throws ServletException Problem with metadata was detected.
	 */
	public RrpcRestOperationContext(RestOperationContextBuilder builder) throws ServletException {
		super(builder);

		ClassMeta<?> interfaceClass = getClassMeta(getJavaMethod().getGenericReturnType());
		meta = new RrpcInterfaceMeta(interfaceClass.getInnerClass(), null);
		if (meta.getMethodsByPath().isEmpty())
			throw new InternalServerError("Method {0} returns an interface {1} that doesn't define any remote methods.", getJavaMethod().getName(), interfaceClass.getFullName());

	}

	@Override
	public void invoke(RestCall call) throws Throwable {

		super.invoke(call);

		Optional<Optional<Object>> x = call.getOutput();
		final Object o = x.isPresent() ? x.get().orElse(null) : null;

		if ("GET".equals(call.getMethod())) {
			call.output(meta.getMethodsByPath().keySet());
			return;

		} else if ("POST".equals(call.getMethod())) {
			String pip = call.getUrlPath().getPath();
			if (pip.indexOf('/') != -1)
				pip = pip.substring(pip.lastIndexOf('/')+1);
			pip = urlDecode(pip);
			RrpcInterfaceMethodMeta rmm = meta.getMethodMetaByPath(pip);
			if (rmm != null) {
				Method m = rmm.getJavaMethod();
				try {
					RestRequest req = call.getRestRequest();
					// Parse the args and invoke the method.
					Parser p = req.getBody().getParser();
					Object[] args = null;
					if (m.getGenericParameterTypes().length == 0)
						args = new Object[0];
					else {
						try (Closeable in = p.isReaderParser() ? req.getReader() : req.getInputStream()) {
							args = p.parseArgs(in, m.getGenericParameterTypes());
						}
					}
					call.output(m.invoke(o, args));
					return;
				} catch (Exception e) {
					throw toHttpException(e, InternalServerError.class);
				}
			}
		}
		throw new NotFound();
	}
}
