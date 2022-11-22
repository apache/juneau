package org.apache.juneau.rest.springboot;
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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Specialized subclass of {@link BasicSpringRestServlet} for showing "group" pages.
 *
 * <p>
 * Meant as a base class for top-level REST resources in Spring Boot environments.
 *
 * <p>
 * Provides support for JSON, XML, HTML, URL-Encoding, UON, XML, OpenAPI, and MessagePack.  See {@link BasicUniversalConfig}
 * for details.
 *
 * <p>
 * Implements the basic REST endpoints defined in {@link BasicRestOperations}.
 *
 * <p>
 * Children are attached to this resource using the {@link Rest#children() @Rest(children)} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-server-springboot">juneau-rest-server-springboot</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-server.jrs.AnnotatedClasses">@Rest-Annotated Classes</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest
public abstract class BasicSpringRestServletGroup extends BasicSpringRestServlet implements BasicGroupOperations {
	private static final long serialVersionUID = 1L;


	@Override /* BasicGroupOperations */
	public ChildResourceDescriptions getChildren(RestRequest req) {
		return ChildResourceDescriptions.of(req);
	}
}

