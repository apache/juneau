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
package org.apache.juneau.rest.widget;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Defines an interface for defining resolvers for <js>"$W{...}"</js> string variables.
 * <p>
 * Widgets are associated with resources through the following
 * <ul>
 * 	<li>{@link RestResource#widgets() @RestResource.widgets}
 * 	<li>{@link RestMethod#widgets() @RestMethod.widgets}
 * 	<li>{@link RestConfig#addWidget(Class)}
 * </ul>
 */
public abstract class Widget {

	/**
	 * The widget key (i.e. The contents of the <js>"$W{...}"</js> variable).
	 * @return The widget key.
	 * 	Must not be <jk>null</jk>.
	 */
	public abstract String getName();

	/**
	 * Resolves the value for the variable.
	 * @param req The HTTP request object.
	 * @return The resolved value.
	 * @throws Exception
	 */
	public abstract String resolve(RestRequest req) throws Exception;
}
