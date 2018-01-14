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

import org.apache.juneau.rest.annotation.*;

/**
 * Class used to resolve {@link Class} objects to instances.
 * 
 * <p>
 * Used to convert classes defined via {@link RestResource#children() @RestResource.children()} into child instances.
 * 
 * <p>
 * Subclasses can be created to provide customized resource resolution.
 * These can be associated with REST resources in one of the following ways:
 * <ul>
 * 	<li>{@link RestResource#resourceResolver() @RestResource.resourceResolver()} annotation.
 * 	<li>{@link RestContextBuilder#resourceResolver(Class)}/{@link RestContextBuilder#resourceResolver(RestResourceResolver)}
 * 		methods.
 * </ul>
 * 
 * Implementations must provide one of the following public constructors:
 * <ul>
 * 	<li>RestResourceResolver()
 * 	<li>RestResourceResolver(RestContext)
 * </ul>
 */
public interface RestResourceResolver {

	/**
	 * Resolves the specified class to a resource object.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own custom resolution.
	 * 
	 * <p>
	 * The default implementation simply creates a new class instance using {@link Class#newInstance()}.
	 * 
	 * @param parent 
	 * 	The parent resource (i.e. the instance whose class has the {@link RestResource#children() @RestResource.children()} annotation.
	 * @param c The class to resolve.
	 * @param builder The initialization configuration for the resource.
	 * @return The instance of that class.
	 * @throws Exception If class could not be resolved.
	 */
	Object resolve(Object parent, Class<?> c, RestContextBuilder builder) throws Exception;
}
