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

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.helper.*;

/**
 * Specialized subclass of {@link BasicRestServlet} for showing "group" pages.
 *
 * <p>
 * Group pages consist of simple lists of child resource URLs and their labels.
 * They're meant to be used as jumping-off points for child resources.
 *
 * <p>
 * Child resources are specified using the {@link Rest#children() @Rest(children)} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc BasicRestServletGroup}
 * </ul>
 */
@Rest
public abstract class BasicRestServletGroup extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * [GET /] - Get child resources.
	 *
	 * @param req The HTTP request.
	 * @return The bean containing links to the child resources.
	 */
	@RestMethod(method=GET, path="/", summary="Navigation page")
	public ChildResourceDescriptions getChildren(RestRequest req) {
		return new ChildResourceDescriptions(req);
	}
}

