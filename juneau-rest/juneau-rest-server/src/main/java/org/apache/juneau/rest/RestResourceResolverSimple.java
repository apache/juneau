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

import org.apache.juneau.internal.*;

/**
 * Denotes the default resolver for child resources.
 *
 * The default implementation simply instantiates the class using one of the following constructors:
 * <ul>
 * 	<li><code><jk>public</jk> T(RestContextBuilder)</code>
 * 	<li><code><jk>public</jk> T()</code>
 * </ul>
 *
 * <p>
 * The former constructor can be used to get access to the {@link RestContextBuilder} object to get access to the
 * config file and initialization information or make programmatic modifications to the resource before
 * full initialization.
 * 
 * <p>
 * Child classes can also be defined as inner-classes of the parent resource class.
 *
 * <p>
 * Non-<code>RestServlet</code> classes can also add the following method to get access to the {@link RestContextBuilder} 
 * object:
 * <ul>
 * 	<li><code><jk>public void</jk> init(RestContextBuilder);</code>
 * </ul>
 *
 */
public class RestResourceResolverSimple implements RestResourceResolver {

	@Override /* RestResourceResolver */
	public Object resolve(Object parent, Class<?> c, RestContextBuilder builder) throws Exception {
		try {
			Object r = ClassUtils.newInstanceFromOuter(parent, Object.class, c, true, builder);
			if (r != null)
				return r;
		} catch (Exception e) {
			throw new RestServletException("Could not instantiate resource class ''{0}''", c.getName()).initCause(e);
		}
		throw new RestServletException("Could not find public constructor for class ''{0}''.", c);
	}
}