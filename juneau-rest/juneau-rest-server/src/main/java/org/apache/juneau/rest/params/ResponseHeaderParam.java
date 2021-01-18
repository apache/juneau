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
package org.apache.juneau.rest.params;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Resolves method parameters annotated with {@link ResponseHeader} on {@link RestMethod}-annotated Java methods.
 *
 * <p>
 * The parameter value must be of type {@link Value} that accepts a value that is then set via
 * <c><jv>call</jv>.{@link RestCall#getRestResponse() getRestResponse}().{@link RestResponse#setHeader(String,String) setOutput}(<jv>name</jv>,<jv>value</jv>)</c>.
 */
public class ResponseHeaderParam implements RestParam {
	final ResponsePartMeta meta;
	final String name;
	private final Type type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param ps The configuration properties of the {@link RestContext}.
	 * @return A new {@link ResponseHeaderParam}, or <jk>null</jk> if the parameter is not annotated with {@link ResponseHeader}.
	 */
	public static ResponseHeaderParam create(ParamInfo paramInfo, PropertyStore ps) {
		if (paramInfo.hasAnnotation(ResponseHeader.class) || paramInfo.getParameterType().hasAnnotation(ResponseHeader.class))
			return new ResponseHeaderParam(paramInfo, ps);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param ps The configuration properties of the {@link RestContext}.
	 */
	protected ResponseHeaderParam(ParamInfo paramInfo, PropertyStore ps) {
		this.name = getName(paramInfo);
		this.type = paramInfo.getParameterType().innerType();
		HttpPartSchema schema = HttpPartSchema.create(ResponseHeader.class, paramInfo);
		this.meta = new ResponsePartMeta(HttpPartType.HEADER, schema, castOrCreate(HttpPartSerializer.class, schema.getSerializer(), true, ps));

		Class<?> c = type instanceof Class ? (Class<?>)type : type instanceof ParameterizedType ? (Class<?>)((ParameterizedType)type).getRawType() : null;
		if (c != Value.class)
			throw new ParameterException(paramInfo, "Type must be Value<?> on parameter annotated with @ResponseHeader annotation");
	}

	private static String getName(ParamInfo paramInfo) {
		String n = null;
		for (ResponseHeader h : paramInfo.getAnnotations(ResponseHeader.class))
			n = firstNonEmpty(h.name(), h.n(), h.value(), n);
		for (ResponseHeader h : paramInfo.getParameterType().getAnnotations(ResponseHeader.class))
			n = firstNonEmpty(h.name(), h.n(), h.value(), n);
		if (n == null)
			throw new ParameterException(paramInfo, "@ResponseHeader used without name or value");
		return n;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override /* RestMethodParam */
	public Object resolve(final RestCall call) throws Exception {
		Value<Object> v = new Value();
		v.listener(new ValueListener() {
			@Override
			public void onSet(Object o) {
				try {
					RestRequest req = call.getRestRequest();
					RestResponse res = call.getRestResponse();
					ResponsePartMeta rpm = req.getResponseHeaderMeta(o);
					if (rpm == null)
						rpm = ResponseHeaderParam.this.meta;
					HttpPartSerializerSession pss = rpm.getSerializer() == null ? req.getPartSerializer() : rpm.getSerializer().createPartSession(req.getSerializerSessionArgs());
					res.setHeader(new HttpPart(name, HttpPartType.HEADER, rpm.getSchema(), pss, o));
				} catch (SerializeException | SchemaValidationException e) {
					throw new RuntimeException(e);
				}
			}
		});
		return v;
	}
}
