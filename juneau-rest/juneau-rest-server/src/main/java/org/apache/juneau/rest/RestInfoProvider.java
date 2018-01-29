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
import java.util.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.annotation.*;

/**
 * REST resource information provider.
 * 
 * <p>
 * Provides localized Swagger documentation and other related information about a REST resource.
 * 
 * 
 * <h5 class='topic'>Additional Information</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
 * </ul>
 * 
 * 
 * <h5 class='section'>Documentation:</h5>
 * <ul>
 * 	<li><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.OptionsPages">Overview &gt; OPTIONS Pages</a>
 * </ul>
 */
public interface RestInfoProvider {
	
	/**
	 * Represents no RestInfoProvider.
	 * 
	 * <p>
	 * Used on annotation to indicate that the value should be inherited from the parent class, and
	 * ultimately {@link RestInfoProviderDefault} if not specified at any level.
	 */
	public interface Null extends RestInfoProvider {}

	/**
	 * Returns the contents of the localized swagger file for the specified request.
	 * 
	 * @param req The incoming HTTP request.
	 * @return 
	 * 	The contents of the parsed swagger file, or <jk>null</jk> if a swagger file could not be found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public Swagger getSwaggerFromFile(RestRequest req) throws Exception;

	/**
	 * Returns the localized swagger for the REST resource.
	 * 
	 * <p>
	 * If {@link #getSwaggerFromFile(RestRequest)} returns a non-<jk>null</jk> value, then 
	 * that swagger is returned by this method.
	 * <br>Otherwise, a new swagger object is return with information gathered via the other methods defined on this class.
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
	 * Returns the localized operation ID of the specified java method.
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized operation ID of the method, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getMethodOperationId(Method method, RestRequest req) throws Exception;

	/**
	 * Returns the localized summary of the specified java method.
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
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized description of the method, or <jk>null</jk> if none was was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getMethodDescription(Method method, RestRequest req) throws Exception;

	/**
	 * Returns the localized tags of the specified java method on this servlet.
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized tags of the method, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public List<String> getMethodTags(Method method, RestRequest req) throws Exception;
	
	/**
	 * Returns the localized external documentation of the specified java method on this servlet.
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized external documentation of the method, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public ExternalDocumentation getMethodExternalDocs(Method method, RestRequest req) throws Exception;
	
	/**
	 * Returns the localized parameter info for the specified java method.
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized parameter info of the method, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public List<ParameterInfo> getMethodParameters(Method method, RestRequest req) throws Exception;

	/**
	 * Returns the localized Swagger response information about the specified Java method.
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The localized response information of the method, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public Map<Integer,ResponseInfo> getMethodResponses(Method method, RestRequest req) throws Exception;
	
	/**
	 * Returns the supported <code>Accept</code> types the specified Java method.
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The supported <code>Accept</code> types of the method, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public List<MediaType> getMethodProduces(Method method, RestRequest req) throws Exception;
	
	/**
	 * Returns the supported <code>Content-Type</code> types the specified Java method.
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return The supported <code>Content-Type</code> types of the method, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public List<MediaType> getMethodConsumes(Method method, RestRequest req) throws Exception;

	/**
	 * Returns whether the specified method is deprecated
	 * 
	 * @param method The Java method annotated with {@link RestMethod @RestMethod}.
	 * @param req The current request.
	 * @return <jk>true</jk> if the method is deprecated.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public boolean isDeprecated(Method method, RestRequest req) throws Exception;

	/**
	 * Returns the localized site name of the REST resource.
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
	 * @param req The current request.
	 * @return The localized description of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getDescription(RestRequest req) throws Exception;

	/**
	 * Returns the localized contact information of the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public Contact getContact(RestRequest req) throws Exception;

	/**
	 * Returns the localized license information of the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized license information of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public License getLicense(RestRequest req) throws Exception;

	/**
	 * Returns the terms-of-service iof the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized terms-of-service of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getTermsOfService(RestRequest req) throws Exception;

	/**
	 * Returns the localized version of the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized version of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public String getVersion(RestRequest req) throws Exception;

	/**
	 * Returns the supported <code>Content-Type</code> request headers for the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The supported <code>Content-Type</code> request headers of the REST resource, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public List<MediaType> getConsumes(RestRequest req) throws Exception;
	
	/**
	 * Returns the supported <code>Accept</code> request headers for the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The supported <code>Accept</code> request headers of the REST resource, or <jk>null</jk> if none were found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public List<MediaType> getProduces(RestRequest req) throws Exception;
	
	/**
	 * Returns the localized tags of the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized tags of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public List<Tag> getTags(RestRequest req) throws Exception;

	/**
	 * Returns the external documentation of the REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized external documentation of the REST resource, or <jk>null</jk> if none was found.
	 * @throws Exception 
	 * 	Throw a {@link RestException} with a specific HTTP error status or any other exception 
	 * 	to cause a <jsf>SC_INTERNAL_SERVER_ERROR</jsf>.
	 */
	public ExternalDocumentation getExternalDocs(RestRequest req) throws Exception;
}
