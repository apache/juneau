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

import java.lang.reflect.Method;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.annotation.*;

/**
 * REST resource information provider.
 *
 * <p>
 * Provides localized Swagger documentation and other related information about a REST resource.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
 * 	<li class='link'>{@doc juneau-rest-server.Swagger}
 * </ul>
 */
public interface RestInfoProvider {

	/**
	 * Represents no RestInfoProvider.
	 *
	 * <p>
	 * Used on annotation to indicate that the value should be inherited from the parent class, and
	 * ultimately {@link BasicRestInfoProvider} if not specified at any level.
	 */
	public interface Null extends RestInfoProvider {}

	/**
	 * Returns the localized swagger for the REST resource.
	 *
	 * <p>
	 * This object is made available through the following:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getSwagger()}
	 * </ul>
	 *
	 * @param req The incoming HTTP request.
	 * @return
	 * 	A new {@link Swagger} instance.
	 * 	<br>Never <jk>null</jk>.
	 * @throws Exception
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public Swagger getSwagger(RestRequest req) throws Exception;

	/**
	 * Returns the localized site name of the REST resource.
	 *
	 * <p>
	 * This object is made available through the following:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getSiteName()}
	 * 	<li><c>$RI{siteName}</c> variable.
	 * 	<li><c>$R{siteName}</c> variable.
	 * </ul>
	 *
	 * @param req The current request.
	 * @return The localized site name of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getSiteName(RestRequest req) throws Exception;

	/**
	 * Returns the localized title of the REST resource.
	 *
	 * <p>
	 * This object is made available through the following:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getResourceTitle()}
	 * 	<li><c>$RI{title}</c> variable.
	 * 	<li><c>$R{resourceTitle}</c> variable.
	 * </ul>
	 *
	 * @param req The current request.
	 * @return The localized title of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getTitle(RestRequest req) throws Exception;

	/**
	 * Returns the localized description of the REST resource.
	 *
	 * <p>
	 * This object is made available through the following:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getResourceDescription()}
	 * 	<li><c>$RI{description}</c> variable.
	 * 	<li><c>$R{resourceDescription}</c> variable.
	 * </ul>
	 *
	 * @param req The current request.
	 * @return The localized description of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getDescription(RestRequest req) throws Exception;

	/**
	 * Returns the localized summary of the specified java method.
	 *
	 * <p>
	 * This object is made available through the following:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getMethodSummary()}
	 * 	<li><c>$RI{methodSummary}</c> variable.
	 * 	<li><c>$R{methodSummary}</c> variable.
	 * </ul>
	 *
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized summary of the method, or <jk>null</jk> if none was found.
	 * @throws Exception
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getMethodSummary(Method method, RestRequest req) throws Exception;

	/**
	 * Returns the localized description of the specified java method on this servlet.
	 *
	 * <p>
	 * This object is made available through the following:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getMethodDescription()}
	 * 	<li><c>$RI{methodDescription}</c> variable.
	 * 	<li><c>$R{methodDescription}</c> variable.
	 * </ul>
	 *
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized description of the method, or <jk>null</jk> if none was was found.
	 * @throws Exception
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getMethodDescription(Method method, RestRequest req) throws Exception;
}
