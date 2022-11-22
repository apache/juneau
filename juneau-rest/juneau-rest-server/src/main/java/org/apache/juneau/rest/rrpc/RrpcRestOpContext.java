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
package org.apache.juneau.rest.rrpc;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;

/**
 * A specialized {@link RestOpContext} for handling <js>"RRPC"</js> HTTP methods.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a>
 * </ul>
 */
public class RrpcRestOpContext extends RestOpContext {

	private final RrpcInterfaceMeta meta;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this method context.
	 * @throws ServletException Problem with metadata was detected.
	 */
	protected RrpcRestOpContext(RestOpContext.Builder builder) throws ServletException {
		super(builder);

		ClassMeta<?> interfaceClass = getBeanContext().getClassMeta(getJavaMethod().getGenericReturnType());
		meta = new RrpcInterfaceMeta(interfaceClass.getInnerClass(), null);
		if (meta.getMethodsByPath().isEmpty())
			throw new InternalServerError("Method {0} returns an interface {1} that doesn't define any remote methods.", getJavaMethod().getName(), interfaceClass.getFullName());

	}

	@Override
	public RrpcRestOpSession.Builder createSession(RestSession session) {
		return RrpcRestOpSession.create(this, session);
	}

	/**
	 * Returns the metadata about the RRPC Java method.
	 *
	 * @return The metadata about the RRPC Java method.
	 */
	protected RrpcInterfaceMeta getMeta() {
		return meta;
	}
}
