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

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.annotation.HeaderAnnotation.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;

/**
 * Resolves method parameters annotated with {@link Header} of type {@link Value} representing response headers
 * on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value must be of type {@link Value} that accepts a value that is then set via:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getResponse() getResponse}()
 * 		.{@link RestResponse#setHeader(String,String) setOutput}(<jv>name</jv>,<jv>value</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class ResponseHeaderArg implements RestOpArg {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @return A new {@link ResponseHeaderArg}, or <jk>null</jk> if the parameter is not annotated with {@link Header}.
	 */
	public static ResponseHeaderArg create(ParameterInfo paramInfo, AnnotationWorkList annotations) {
		if (paramInfo.getParameterType().is(Value.class) && AP.has(Header.class, paramInfo))
			return new ResponseHeaderArg(paramInfo, annotations);
		return null;
	}

	final ResponsePartMeta meta;
	final String name;

	private final Type type;

	/**
	 * Constructor.
	 *
	 * @param pi The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 */
	protected ResponseHeaderArg(ParameterInfo pi, AnnotationWorkList annotations) {
		this.name = findName(pi).orElseThrow(() -> new ArgException(pi, "@Header used without name or value"));
		this.type = pi.getParameterType().innerType();
		var schema = HttpPartSchema.create(Header.class, pi);

		var ps = schema.getSerializer();
		this.meta = new ResponsePartMeta(HttpPartType.HEADER, schema, nn(ps) ? HttpPartSerializer.creator().type(ps).apply(annotations).create() : null);

		var c = type instanceof Class ? (Class<?>)type : type instanceof ParameterizedType ? (Class<?>)((ParameterizedType)type).getRawType() : null;
		if (c != Value.class)
			throw new ArgException(pi, "Type must be Value<?> on parameter annotated with @Header annotation");
	}

	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		var v = new Value<>();
		v.listener(o -> {
			var req = opSession.getRequest();
			var res = opSession.getResponse();
			var rpm = req.getOpContext().getResponseHeaderMeta(o);
			if (rpm == null)
				rpm = ResponseHeaderArg.this.meta;
			var pss = rpm.getSerializer() == null ? req.getPartSerializerSession() : rpm.getSerializer().getPartSession();
			res.setHeader(new SerializedHeader(name, o, pss, rpm.getSchema(), false));
		});
		return v;
	}
}