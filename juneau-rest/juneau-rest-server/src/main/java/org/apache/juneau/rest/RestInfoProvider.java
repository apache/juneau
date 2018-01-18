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

import java.util.*;

import org.apache.juneau.dto.swagger.*;

/**
 * Class that provides documentation and other related information about a REST resource.
 * 
 * 
 * <h5 class='topic'>Additional Information</h5>
 * <ul>
 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
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
	 * 	The contents of the parsed swagger file.
	 * 	Returns <jk>null</jk> if a swagger file could not be found.
	 * @throws RestException
	 */
	public Swagger getSwaggerFromFile(RestRequest req) throws RestException;

	/**
	 * Returns the localized swagger for this REST resource.
	 * 
	 * <p>
	 * If {@link #getSwaggerFromFile(RestRequest)} returns a non-<jk>null</jk> value, then 
	 * that swagger is returned by this method.
	 * <br>Otherwise, a new swagger object is return with information gathered via various means.
	 * 
	 * @param req The incoming HTTP request.
	 * @return A new Swagger instance.
	 * @throws RestException
	 */
	public Swagger getSwagger(RestRequest req) throws RestException;


	/**
	 * Returns the localized summary of the specified java method on this servlet.
	 * 
	 * @param javaMethodName The name of the Java method whose description we're retrieving.
	 * @param req The current request.
	 * @return The localized summary of the method, or a blank string if no summary was found.
	 */
	public String getMethodSummary(String javaMethodName, RestRequest req);

	/**
	 * Returns the localized summary of the java method invoked on the specified request.
	 * 
	 * @param req The current request.
	 * @return The localized summary of the method, or a blank string if no summary was found.
	 */
	public String getMethodSummary(RestRequest req);

	/**
	 * Returns the localized description of the specified java method on this servlet.
	 * 
	 * @param javaMethodName The name of the Java method whose description we're retrieving.
	 * @param req The current request.
	 * @return The localized description of the method, or a blank string if no description was found.
	 */
	public String getMethodDescription(String javaMethodName, RestRequest req);

	/**
	 * Returns the localized description of the invoked java method on the specified request.
	 * 
	 * @param req The current request.
	 * @return The localized description of the method, or a blank string if no description was found.
	 */
	public String getMethodDescription(RestRequest req);

	/**
	 * Returns the localized site name of this REST resource.
	 * 
	 * @param req The current request.
	 * @return The localized description of this REST resource, or <jk>null</jk> if no resource description was found.
	 */
	public String getSiteName(RestRequest req);

	/**
	 * Returns the localized title of this REST resource.
	 * 
	 * @param req The current request.
	 * @return The localized description of this REST resource, or <jk>null</jk> if no resource description was found.
	 */
	public String getTitle(RestRequest req);

	/**
	 * Returns the localized description of this REST resource.
	 * 
	 * @param req The current request.
	 * @return The localized description of this REST resource, or <jk>null</jk> if no resource description was found.
	 */
	public String getDescription(RestRequest req);

	/**
	 * Returns the localized contact information of this REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public Contact getContact(RestRequest req) ;

	/**
	 * Returns the localized license information of this REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public License getLicense(RestRequest req);

	/**
	 * Returns the terms-of-service information of this REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public String getTermsOfService(RestRequest req);

	/**
	 * Returns the version information of this REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public String getVersion(RestRequest req);

	/**
	 * Returns the version information of this REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public List<Tag> getTags(RestRequest req);

	/**
	 * Returns the external documentation of this REST resource.
	 * 
	 * @param req The current request.
	 * @return
	 * 	The localized contact information of this REST resource, or <jk>null</jk> if no contact information was found.
	 */
	public ExternalDocumentation getExternalDocs(RestRequest req);
}
