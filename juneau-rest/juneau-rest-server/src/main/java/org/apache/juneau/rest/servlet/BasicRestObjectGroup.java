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

import jakarta.servlet.http.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.config.*;

/**
 * Identical to {@link BasicRestServletGroup} but doesn't extend from {@link HttpServlet}.
 *
 * <p>
 * Meant as a base class for child REST resources in servlet containers or Spring Boot environments.
 *
 * <p>
 * Provides support for JSON, XML, HTML, URL-Encoding, UON, XML, OpenAPI, and MessagePack.  See {@link BasicUniversalConfig}
 * for details.
 *
 * <p>
 * Implements the basic REST endpoints defined in {@link BasicRestOperations} and {@link BasicGroupOperations}.
 *
 * <p>
 * Children are attached to this resource using the {@link Rest#children() @Rest(children)} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.AnnotatedClasses">@Rest-Annotated Classes</a>
 * </ul>
 */
@Rest
public abstract class BasicRestObjectGroup extends BasicRestObject implements BasicGroupOperations {

	@Override /* BasicGroupOperations */
	public ChildResourceDescriptions getChildren(RestRequest req) {
		return ChildResourceDescriptions.of(req);
	}
}