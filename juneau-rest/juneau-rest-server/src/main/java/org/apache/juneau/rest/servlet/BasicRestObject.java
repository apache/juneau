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
package org.apache.juneau.rest.servlet;

import java.util.*;

import jakarta.servlet.http.*;

import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.stats.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.response.*;

/**
 * Identical to {@link BasicRestServlet} but doesn't extend from {@link HttpServlet}.
 *
 * <p>
 * Meant as a base class for child REST resources in servlet containers and Spring Boot environments.
 *
 * <p>
 * Provides support for JSON, XML, HTML, URL-Encoding, UON, XML, OpenAPI, and MessagePack.  See {@link BasicUniversalConfig}
 * for details.
 *
 * <p>
 * Implements the basic REST endpoints defined in {@link BasicRestOperations}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
@Rest
public abstract class BasicRestObject extends RestObject implements BasicRestOperations, BasicUniversalConfig {

	//-----------------------------------------------------------------------------------------------------------------
	// BasicRestConfig methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* BasicRestOperations */
	public Swagger getSwagger(RestRequest req) {
		return req.getSwagger().orElseThrow(NotFound::new);
	}

	@Override /* BasicRestOperations */
	public HttpResource getHtdoc(@Path("/*") String path, Locale locale) throws NotFound {
		return getContext().getStaticFiles().resolve(path, locale).orElseThrow(NotFound::new);
	}

	@Override /* BasicRestOperations */
	public HttpResource getFavIcon() {
		String favIcon = getContext().getConfig().get("REST/favicon").orElse("images/juneau.png");
		return getHtdoc(favIcon, null);
	}

	@Override /* BasicRestOperations */
	public void error() {}

	@Override /* BasicRestOperations */
	public RestContextStats getStats(RestRequest req) {
		return req.getContext().getStats();
	}
}
