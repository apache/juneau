/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.arg;

import static org.apache.juneau.commons.utils.ClassUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters annotated with {@link StatusCode} on {@link RestOp}-annotated Java methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class ResponseCodeArg implements RestOpArg {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link ResponseCodeArg}, or <jk>null</jk> if the parameter is not annotated with {@link StatusCode}.
	 */
	public static ResponseCodeArg create(ParameterInfo paramInfo) {
		if (AP.has(StatusCode.class, paramInfo))
			return new ResponseCodeArg(paramInfo);
		return null;
	}

	private final Type type;

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 */
	protected ResponseCodeArg(ParameterInfo paramInfo) {
		this.type = paramInfo.getParameterType().innerType();
		var c = type instanceof Class ? (Class<?>)type : type instanceof ParameterizedType ? (Class<?>)((ParameterizedType)type).getRawType() : null;
		if (c != Value.class || Value.getParameterType(type) != Integer.class)
			throw new ArgException(paramInfo, "Type must be Value<Integer> on parameter annotated with @StatusCode annotation");
	}

	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		var v = new Value<>();
		v.listener(o -> opSession.getResponse().setStatus(Integer.parseInt(o.toString())));
		return v;
	}
}