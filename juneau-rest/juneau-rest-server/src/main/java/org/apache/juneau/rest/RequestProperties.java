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

import org.apache.juneau.svl.*;

/**
 * Encapsulates request-level properties.
 *
 * <p>
 * These are properties specified for a single HTTP request that extends the properties defined on {@link RestMethodProperties}
 * and are accessible and modifiable through the following:
 * <ul>
 * 	<li class='jm'>{@link RestRequest#getProperties()}
 * 	<li class='jm'>{@link RestRequest#prop(String, Object)}
 * 	<li class='jm'>{@link RestResponse#getProperties()}
 * 	<li class='jm'>{@link RestResponse#prop(String, Object)}
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.ConfigurableProperties}
 * </ul>
 * @deprecated Use {@link RequestAttributes}
 */
@SuppressWarnings("serial")
@Deprecated
public class RequestProperties extends ResolvingObjectMap {

	/**
	 * Constructor
	 *
	 * @param varResolver The request variable resolver session.
	 * @param inner The inner properties defined on the resource context.
	 */
	public RequestProperties(VarResolverSession varResolver, RestMethodProperties inner) {
		super(varResolver);
		setInner(inner);
	}
}
