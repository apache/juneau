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
package org.apache.juneau.rest.vars;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;

/**
 * Resolver for resolving URL variables <js>"$U{name}"</js>.
 * <p>
 * The advantage of using this variable is that you can resolve URLs with special protocols such as <js>"servlet:/xxx"</js>.
 * <p>
 * See {@link UriResolver} for the kinds of URIs that can be resolved.
 * <ul>
 * 	<li>{@link RestResource#widgets() @RestResource.widgets()}
 * 	<li>{@link RestMethod#widgets() @RestMethod.widgets()}
 * 	<li>{@link RestConfig#addWidget(Class)}
 * </ul>
 * <p>
 * Uses the URI resolver returned by {@link RestRequest#getUriResolver(UriResolution, UriRelativity)} with resolution
 * 	{@link UriResolution#ROOT_RELATIVE} and relativity {@link UriRelativity#RESOURCE}.
 *
 * @see org.apache.juneau.svl
 */
public class UrlVar extends SimpleVar {

	/**
	 * The name of the session or context object that identifies the {@link RestRequest} object.
	 */
	public static final String SESSION_req = "req";

	/** The name of this variable. */
	public static final String NAME = "U";

	/**
	 * Constructor.
	 */
	public UrlVar() {
		super(NAME);
	}

	@Override /* Parameter */
	public String resolve(VarResolverSession session, String key) {
		RestRequest req = session.getSessionObject(RestRequest.class, SESSION_req);
		return req.getUriResolver(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE).resolve(key);
	}
}