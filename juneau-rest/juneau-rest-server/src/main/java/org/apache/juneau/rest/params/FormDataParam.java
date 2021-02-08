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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters and parameter types annotated with {@link FormData} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using <c><jv>call</jv>.{@link RestCall#getRestRequest() getRestRequest}().{@link RestRequest#getFormData() getFormData}().{@link RequestFormData#get(HttpPartParserSession,HttpPartSchema,String,Type,Type...) get}(<jv>parserSession<jv>, <jv>schema</jv>, <jv>name</jv>, <jv>type</jv>)</c>
 * with a {@link HttpPartSchema schema} derived from the {@link FormData} annotation.
 *
 * <p>
 * If the {@link FormData#multi()} flag is set, then the data type can be a {@link Collection} or array.
 */
public class FormDataParam implements RestOperationParam {
	private final boolean multi;
	private final HttpPartParser partParser;
	private final HttpPartSchema schema;
	private final String name;
	private final ClassInfo type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param cp The configuration properties of the {@link RestContext}.
	 * @return A new {@link FormDataParam}, or <jk>null</jk> if the parameter is not annotated with {@link FormData}.
	 */
	public static FormDataParam create(ParamInfo paramInfo, ContextProperties cp) {
		if (paramInfo.hasAnnotation(FormData.class) || paramInfo.getParameterType().hasAnnotation(FormData.class))
			return new FormDataParam(paramInfo, cp);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param cp The configuration properties of the {@link RestContext}.
	 */
	protected FormDataParam(ParamInfo paramInfo, ContextProperties cp) {
		this.name = getName(paramInfo);
		this.type = paramInfo.getParameterType();
		this.schema = HttpPartSchema.create(FormData.class, paramInfo);
		this.partParser = castOrCreate(HttpPartParser.class, schema.getParser(), true, cp);
		this.multi = getMulti(paramInfo) || schema.getCollectionFormat() == HttpPartCollectionFormat.MULTI;

		if (multi && ! type.isCollectionOrArray())
			throw new ParameterException(paramInfo, "Use of multipart flag on @FormData parameter that is not an array or Collection");
	}

	private String getName(ParamInfo paramInfo) {
		String n = null;
		for (FormData h : paramInfo.getAnnotations(FormData.class))
			n = firstNonEmpty(h.name(), h.n(), h.value(), n);
		for (FormData h : paramInfo.getParameterType().getAnnotations(FormData.class))
			n = firstNonEmpty(h.name(), h.n(), h.value(), n);
		if (n == null)
			throw new ParameterException(paramInfo, "@FormData used without name or value");
		return n;
	}

	private boolean getMulti(ParamInfo paramInfo) {
		for (FormData f : paramInfo.getAnnotations(FormData.class))
			if (f.multi())
				return true;
		for (FormData f : paramInfo.getParameterType().getAnnotations(FormData.class))
			if (f.multi())
				return true;
		return false;
	}

	@Override /* RestOperationParam */
	public Object resolve(RestCall call) throws Exception {
		RestRequest req = call.getRestRequest();
		HttpPartParserSession ps = partParser == null ? req.getPartParser() : partParser.createPartSession(req.getParserSessionArgs());
		RequestFormData fd = req.getFormData();
		return multi ? fd.getAll(ps, schema, name, type.innerType()) : fd.get(ps, schema, name, type.innerType());
	}
}
