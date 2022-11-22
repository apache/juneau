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

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters annotated with {@link Request} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getRequest(RequestBeanMeta) getRequest}(<jv>meta</jv>);
 * </p>
 *
 * <p>
 * {@link RequestBeanMeta meta} is derived from the {@link Request} annotation and context configuration.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RequestBeans">@Request Beans</a>
 * </ul>
 */
public class RequestBeanArg implements RestOpArg {
	private final RequestBeanMeta meta;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @return A new {@link RequestBeanArg}, or <jk>null</jk> if the parameter is not annotated with {@link Request}.
	 */
	public static RequestBeanArg create(ParamInfo paramInfo, AnnotationWorkList annotations) {
		if (paramInfo.hasAnnotation(Request.class))
			return new RequestBeanArg(paramInfo, annotations);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 */
	protected RequestBeanArg(ParamInfo paramInfo, AnnotationWorkList annotations) {
		this.meta = RequestBeanMeta.create(paramInfo, annotations);
	}

	@Override /* RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		return opSession.getRequest().getRequest(meta);
	}
}
