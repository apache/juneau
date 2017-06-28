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

import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;

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
 * 	<li>{@link RestConfig#setResourceResolver(Class)}/{@link RestConfig#setResourceResolver(RestResourceResolver)}
 * 		methods.
 * </ul>
 *
 * <p>
 * The default implementation simply instantiates the class using one of the following constructors:
 * <ul>
 * 	<li><code><jk>public</jk> T(RestConfig)</code>
 * 	<li><code><jk>public</jk> T()</code>
 * </ul>
 *
 * <p>
 * The former constructor can be used to get access to the {@link RestConfig} object to get access to the
 * config file and initialization information or make programmatic modifications to the resource before
 * full initialization.
 *
 * <p>
 * Non-<code>RestServlet</code> classes can also add the following two methods to get access to the
 * {@link RestConfig} and {@link RestContext} objects:
 * <ul>
 * 	<li><code><jk>public void</jk> init(RestConfig);</code>
 * 	<li><code><jk>public void</jk> init(RestContext);</code>
 * </ul>
 */
public class RestResourceResolver {

	/**
	 * Denotes the default resolver.
	 */
	public static final class DEFAULT extends RestResourceResolver {}

	/**
	 * Resolves the specified class to a resource object.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own custom resolution.
	 *
	 * <p>
	 * The default implementation simply creates a new class instance using {@link Class#newInstance()}.
	 *
	 * @param c The class to resolve.
	 * @param config The initialization configuration for the resource.
	 * @return The instance of that class.
	 * @throws RestServletException If class could not be resolved.
	 */
	public Object resolve(Class<?> c, RestConfig config) throws RestServletException {
		try {
			Constructor<?> c1 = findPublicConstructor(c, RestConfig.class);
			if (c1 != null)
				return c1.newInstance(config);
			c1 = findPublicConstructor(c);
			if (c1 != null)
				return c1.newInstance();
		} catch (Exception e) {
			throw new RestServletException("Could not instantiate resource class ''{0}''", c.getName()).initCause(e);
		}
		throw new RestServletException("Could not find public constructor for class ''{0}''.", c);
	}
}
