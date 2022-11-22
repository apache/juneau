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
package org.apache.juneau.rest.arg;

import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters annotated with {@link Method} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getMethod() getMethod}();
 * </p>
 *
 * <p>
 * The parameter type must be {@link String}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class MethodArg implements RestOpArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link MethodArg}, or <jk>null</jk> if the parameter isn't annotated with {@link Method}.
	 */
	public static MethodArg create(ParamInfo paramInfo) {
		if (paramInfo.hasAnnotation(Method.class))
			return new MethodArg();
		return null;
	}

	/**
	 * Constructor.
	 */
	protected MethodArg() {
	}

	@Override /* RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		return opSession.getRequest().getMethod();
	}
}
