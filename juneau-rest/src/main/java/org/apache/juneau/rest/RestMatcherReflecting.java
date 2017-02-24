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

import java.lang.reflect.*;

/**
 * Subclass of {@link RestMatcher} that gives access to the servlet and Java method it's applied to.
 * <p>
 * Essentially the same as {@link RestMatcher} except has a constructor where the
 * 	Java method is passed in so that you can access annotations defined on it to tailor
 * 	the behavior of the matcher.
 */
public abstract class RestMatcherReflecting extends RestMatcher {

	/**
	 * Constructor.
	 *
	 * @param servlet The REST servlet.
	 * @param javaMethod The Java method that this rest matcher is defined on.
	 */
	protected RestMatcherReflecting(RestServlet servlet, Method javaMethod) {}
}
