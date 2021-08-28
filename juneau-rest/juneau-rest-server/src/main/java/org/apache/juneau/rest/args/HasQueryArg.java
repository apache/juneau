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

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters annotated with {@link HasQuery} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using <c><jv>call</jv>.{@link RestCall#getRestRequest() getRestRequest}().{@link RestRequest#getQueryParams() getQueryParams}().{@link RequestQueryParams#contains(String...) contains}(<jv>name</jv>)</c>
 *
 * <p>
 * The parameter type can be a <jk>boolean</jk> or anything convertible from a <jk>boolean</jk>.
 */
public class HasQueryArg implements RestOpArg {

	private final String name;
	private final Type type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link HasQueryArg}, or <jk>null</jk> if the parameter is not annotated with {@link HasQuery}.
	 */
	public static HasQueryArg create(ParamInfo paramInfo) {
		if (paramInfo.hasAnnotation(HasQuery.class))
			return new HasQueryArg(paramInfo);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 */
	protected HasQueryArg(ParamInfo paramInfo) {
		this.name = getName(paramInfo);
		this.type = paramInfo.getParameterType().innerType();
	}

	private String getName(ParamInfo paramInfo) {
		String n = null;
		for (HasQuery h : paramInfo.getAnnotations(HasQuery.class))
			n = firstNonEmpty(h.name(), h.n(), h.value(), n);
		if (n == null)
			throw new ArgException(paramInfo, "@HasQuery used without name or value");
		return n;
	}

	@Override /* RestOpArg */
	public Object resolve(RestCall call) throws Exception {
		RestRequest req = call.getRestRequest();
		BeanSession bs = req.getBeanSession();
		return bs.convertToType(req.getQueryParams().contains(name), bs.getClassMeta(type));
	}
}
