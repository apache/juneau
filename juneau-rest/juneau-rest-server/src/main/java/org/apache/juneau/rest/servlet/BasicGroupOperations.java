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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;

/**
 * Basic REST group operation methods.
 *
 * <p>
 * 	Defines 1 special use REST operation endpoint:
 * </p>
 *
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(path=<js>"/*"</js></js>)
 * 	<jk>public</jk> {@link ChildResourceDescriptions} {@link #getChildren(RestRequest) getChildren}({@link RestRequest} <jv>req</jv>);
 * </p>
 *
 * <p>
 * 	Implementations provided by the following classes:
 * </p>
 * <ul class='javatreec'>
 * 	<li class='jac'>{@link BasicRestServletGroup}
 * 	<li class='jac'>{@link BasicRestObjectGroup}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.AnnotatedClasses">@Rest-Annotated Classes</a>
 * </ul>
 */
public interface BasicGroupOperations {

	/**
	 * [GET /] - Get child resources.
	 *
	 * <p>
	 * Returns a bean that lists and allows navigation to child resources.
	 *
	 * @param req The HTTP request.
	 * @return The bean containing links to the child resources.
	 */
	@RestGet(path="/", summary="Navigation page")
	public ChildResourceDescriptions getChildren(RestRequest req);
}
