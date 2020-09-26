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

import static org.apache.juneau.http.HttpMethod.*;

import javax.servlet.http.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.helper.*;

/**
 * Identical to {@link BasicRestServletGroup} but doesn't extend from {@link HttpServlet}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc BasicRestGroup}
 * </ul>
 */
@Rest
public abstract class BasicRestGroup extends BasicRest {

	/**
	 * [GET /] - Get child resources.
	 *
	 * @param req The HTTP request.
	 * @return The bean containing links to the child resources.
	 */
	@RestMethod(name=GET, path="/", summary="Navigation page")
	public ChildResourceDescriptions getChildren(RestRequest req) {
		return new ChildResourceDescriptions(req);
	}
}

