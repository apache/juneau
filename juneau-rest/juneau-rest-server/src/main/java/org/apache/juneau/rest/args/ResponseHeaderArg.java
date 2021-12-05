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

import static java.util.Optional.*;
import static org.apache.juneau.http.annotation.HeaderAnnotation.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters annotated with {@link Header} of type {@link Value} representing response headers
 * on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value must be of type {@link Value} that accepts a value that is then set via:
 * <p class='bcode w800'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getResponse() getResponse}()
 * 		.{@link RestResponse#setHeader(String,String) setOutput}(<jv>name</jv>,<jv>value</jv>);
 * </p>
 */
public class ResponseHeaderArg implements RestOpArg {
	final ResponsePartMeta meta;
	final String name;
	private final Type type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @return A new {@link ResponseHeaderArg}, or <jk>null</jk> if the parameter is not annotated with {@link Header}.
	 */
	public static ResponseHeaderArg create(ParamInfo paramInfo, AnnotationWorkList annotations) {
		if (paramInfo.getParameterType().is(Value.class) && (paramInfo.hasAnnotation(Header.class) || paramInfo.getParameterType().hasAnnotation(Header.class)))
			return new ResponseHeaderArg(paramInfo, annotations);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param pi The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 */
	protected ResponseHeaderArg(ParamInfo pi, AnnotationWorkList annotations) {
		ClassInfo pt = pi.getParameterType();

		this.name = findName(pi.getAnnotations(Header.class), pt.getAnnotations(Header.class)).orElseThrow(() -> new ArgException(pi, "@Header used without name or value"));
		this.type = pi.getParameterType().innerType();
		HttpPartSchema schema = HttpPartSchema.create(Header.class, pi);
		this.meta = new ResponsePartMeta(HttpPartType.HEADER, schema, ofNullable(schema.getSerializer()).map(x -> HttpPartSerializer.creator().type(x).apply(annotations).create()).orElse(null));

		Class<?> c = type instanceof Class ? (Class<?>)type : type instanceof ParameterizedType ? (Class<?>)((ParameterizedType)type).getRawType() : null;
		if (c != Value.class)
			throw new ArgException(pi, "Type must be Value<?> on parameter annotated with @Header annotation");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override /* RestOpArg */
	public Object resolve(final RestOpSession opSession) throws Exception {
		Value<Object> v = new Value();
		v.listener(new ValueListener() {
			@Override
			public void onSet(Object o) {
				RestRequest req = opSession.getRequest();
				RestResponse res = opSession.getResponse();
				ResponsePartMeta rpm = req.getOpContext().getResponseHeaderMeta(o);
				if (rpm == null)
					rpm = ResponseHeaderArg.this.meta;
				HttpPartSerializerSession pss = rpm.getSerializer() == null ? req.getPartSerializerSession() : rpm.getSerializer().getPartSession();
				res.setHeader(new SerializedHeader(name, o, pss, rpm.getSchema(), false));
			}
		});
		return v;
	}
}
