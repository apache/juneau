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

/**
 * Denotes the default resolver.
 *
 * <p>
 * The default implementation simply instantiates the class using one of the following constructors:
 * <ul>
 * 	<li><code><jk>public</jk> T(RestConfig)</code>
 * 	<li><code><jk>public</jk> T()</code>
 * </ul>
 *
 * <p>
 * The former constructor can be used to get access to the {@link RestContextBuilder} object to get access to the
 * config file and initialization information or make programmatic modifications to the resource before
 * full initialization.
 *
 * <p>
 * Non-<code>RestServlet</code> classes can also add the following two methods to get access to the
 * {@link RestContextBuilder} and {@link RestContext} objects:
 * <ul>
 * 	<li><code><jk>public void</jk> init(RestConfig);</code>
 * 	<li><code><jk>public void</jk> init(RestContext);</code>
 * </ul>
 *
 */
public class RestResourceResolverSimple implements RestResourceResolver {

	@Override /* RestResourceResolver */
	public Object resolve(Class<?> c, RestContextBuilder builder) throws Exception {
		try {
			Constructor<?> c1 = findPublicConstructor(c, RestContextBuilder.class);
			if (c1 != null)
				return c1.newInstance(builder);
			c1 = findPublicConstructor(c);
			if (c1 != null)
				return c1.newInstance();
		} catch (Exception e) {
			throw new RestServletException("Could not instantiate resource class ''{0}''", c.getName()).initCause(e);
		}
		throw new RestServletException("Could not find public constructor for class ''{0}''.", c);
	}
}