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
package org.apache.juneau.rest.args;


import javax.servlet.http.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters of type {@link HttpServletRequest} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using <c><jv>opSession</jv>.{@link RestOpSession#getRestSession() getRestSession}().{@link RestSession#getRequest() getRequest}()</c>.
 */
public class HttpServletRequestArg extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link HttpServletRequestArg}, or <jk>null</jk> if the parameter type is not {@link HttpServletRequest}.
	 */
	public static HttpServletRequestArg create(ParamInfo paramInfo) {
		if (paramInfo.isType(HttpServletRequest.class))
			return new HttpServletRequestArg();
		return null;
	}

	/**
	 * Constructor.
	 */
	protected HttpServletRequestArg() {
		super((opSession)->opSession.getRestSession().getRequest());
	}
}
