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

import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Specialized matcher for matching client versions.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestClientVersioning}
 * </ul>
 */
public class ClientVersionMatcher extends RestMatcher {

	private final String clientVersionHeader;
	private final VersionRange range;

	/**
	 * Constructor.
	 *
	 * @param clientVersionHeader
	 * 	The HTTP request header name containing the client version.
	 * 	If <jk>null</jk> or an empty string, uses <js>"X-Client-Version"</js>
	 * @param mi The version string that the client version must match.
	 */
	protected ClientVersionMatcher(String clientVersionHeader, MethodInfo mi) {
		this.clientVersionHeader = isEmpty(clientVersionHeader) ? "X-Client-Version" : clientVersionHeader;
		RestOp m = mi.getLastAnnotation(RestOp.class);
		range = new VersionRange(m.clientVersion());
	}

	@Override /* RestMatcher */
	public boolean matches(RestRequest req) {
		return range.matches(req.getHeader(clientVersionHeader));
	}

	@Override /* RestMatcher */
	public boolean required() {
		return true;
	}
}
