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

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters and parameter types annotated with {@link Response} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value must be of type {@link Value} that accepts a value that is then set via
 * <c><jv>call</jv>.{@link RestCall#getRestResponse() getRestResponse}().{@link RestResponse#setOutput(Object) setOutput}(<jv>value</jv>)</c>.
 */
public class ResponseBeanArg implements RestOperationArg {
	final ResponseBeanMeta meta;
	private final Type type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param cp The configuration properties of the {@link RestContext}.
	 * @return A new {@link ResponseBeanArg}, or <jk>null</jk> if the parameter is not annotated with {@link Response}.
	 */
	public static ResponseBeanArg create(ParamInfo paramInfo, ContextProperties cp) {
		if (paramInfo.hasAnnotation(Response.class) || paramInfo.getParameterType().hasAnnotation(Response.class))
			return new ResponseBeanArg(paramInfo, cp);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param cp The configuration properties of the {@link RestContext}.
	 */
	protected ResponseBeanArg(ParamInfo paramInfo, ContextProperties cp) {
		this.type = paramInfo.getParameterType().innerType();
		this.meta = ResponseBeanMeta.create(paramInfo, cp);
		Class<?> c = type instanceof Class ? (Class<?>)type : type instanceof ParameterizedType ? (Class<?>)((ParameterizedType)type).getRawType() : null;
		if (c != Value.class)
			throw new ArgException(paramInfo, "Type must be Value<?> on parameter annotated with @Response annotation");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override /* RestOperationArg */
	public Object resolve(final RestCall call) throws Exception {
		Value<Object> v = new Value();
		v.listener(new ValueListener() {
			@Override
			public void onSet(Object o) {
				RestRequest req = call.getRestRequest();
				RestResponse res = call.getRestResponse();
				ResponseBeanMeta meta = req.getOpContext().getResponseBeanMeta(o);
				if (meta == null)
					meta = ResponseBeanArg.this.meta;
				res.setResponseBeanMeta(meta);
				res.setOutput(o);
			}
		});
		return v;
	}
}
