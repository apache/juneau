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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;

/**
 * Resolves method parameters annotated with {@link HasQuery} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getQueryParams() getQueryParams}()
 * 		.{@link RequestQueryParams#contains(String) contains}(<jv>name</jv>);
 * </p>
 *
 * <p>
 * The parameter type can be a <jk>boolean</jk> or anything convertible from a <jk>boolean</jk>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.JavaMethodParameters">Java Method Parameters</a>
 * </ul>
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
	 * @param pi The Java method parameter being resolved.
	 */
	protected HasQueryArg(ParamInfo pi) {
		Value<String> _name = Value.empty();
		pi.forEachAnnotation(HasQuery.class, x -> hasName(x), x -> _name.set(getName(x)));
		this.name = _name.orElseThrow(() -> new ArgException(pi, "@HasQuery used without name or value"));
		this.type = pi.getParameterType().innerType();
	}

	private static boolean hasName(HasQuery x) {
		return isNotEmpty(x.name()) || isNotEmpty(x.value());
	}

	private static String getName(HasQuery x) {
		return firstNonEmpty(x.name(), x.value());
	}

	@Override /* RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		RestRequest req = opSession.getRequest();
		BeanSession bs = req.getBeanSession();
		return bs.convertToType(req.getQueryParams().contains(name), bs.getClassMeta(type));
	}
}
