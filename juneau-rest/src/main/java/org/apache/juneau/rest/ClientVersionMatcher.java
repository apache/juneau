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

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Specialized matcher for matching client versions.
 * <p>
 * See {@link RestResource#clientVersionHeader} and {@link RestMethod#clientVersion} for more info.
 */
public class ClientVersionMatcher extends RestMatcherReflecting {

	private final String clientVersionHeader;
	private final VersionRange range;

	/**
	 * Constructor.
	 *
	 * @param servlet The servlet.
	 * @param javaMethod The version string that the client version must match.
	 */
	protected ClientVersionMatcher(RestServlet servlet, java.lang.reflect.Method javaMethod) {
		super(servlet, javaMethod);
		this.clientVersionHeader = servlet.getClientVersionHeader();
		RestMethod m = javaMethod.getAnnotation(RestMethod.class);
		range = new VersionRange(m.clientVersion());
	}

	@Override /* RestMatcher */
	public boolean matches(RestRequest req) {
		return range.matches(req.getHeader(clientVersionHeader));
	}

	@Override /* RestMatcher */
	public boolean mustMatch() {
		return true;
	}
}
