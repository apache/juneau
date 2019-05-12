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

import javax.servlet.http.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Identical to {@link BasicRestServlet} but doesn't extend from {@link HttpServlet}
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.Instantiation.BasicRest}
 * </ul>
 */
@RestResource(
	// Allow OPTIONS requests to be simulated using ?method=OPTIONS query parameter.
	allowedMethodParams="OPTIONS"
)
@HtmlDocConfig(
	// Basic page navigation links.
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS"
	}
)
public abstract class BasicRest implements BasicRestConfig {

	/**
	 * [OPTIONS /*] - Show resource options.
	 *
	 * @param req The HTTP request.
	 * @return A bean containing the contents for the OPTIONS page.
	 */
	@Override /* BasicRestConfig */
	public Swagger getOptions(RestRequest req) {
		// Localized Swagger for this resource is available through the RestRequest object.
		return req.getSwagger();
	}
}
