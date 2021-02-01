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

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters annotated with {@link Attr} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using <c><jv>call</jv>.{@link RestCall#getRestRequest() getRestRequest}().{@link RestRequest#getAttributes() getAttributes}().{@link RequestAttributes#get(String,Type,Type...) get}(<jv>name</jv>,<jv>type</jv>)</c>.
 */
public class AttributeParam implements RestOperationParam {

	private final String name;
	private final Type type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link AttributeParam}, or <jk>null</jk> if the parameter is not annotated with {@link Attr}.
	 */
	public static AttributeParam create(ParamInfo paramInfo) {
		if (paramInfo.hasAnnotation(Attr.class) || paramInfo.getParameterType().hasAnnotation(Attr.class))
			return new AttributeParam(paramInfo);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 */
	protected AttributeParam(ParamInfo paramInfo) {
		this.name = getName(paramInfo);
		this.type = paramInfo.getParameterType().innerType();
	}

	private String getName(ParamInfo paramInfo) {
		String n = null;
		for (Attr h : paramInfo.getAnnotations(Attr.class))
			n = firstNonEmpty(h.name(), h.value(), n);
		for (Attr h : paramInfo.getParameterType().getAnnotations(Attr.class))
			n = firstNonEmpty(h.name(), h.value(), n);
		if (n == null)
			throw new ParameterException(paramInfo, "@Attr used without name or value");
		return n;
	}

	@Override /* RestOperationParam */
	public Object resolve(RestCall call) throws Exception {
		return call.getRestRequest().getAttributes().get(name, type);
	}
}
