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

import static org.apache.juneau.http.annotation.HasFormDataAnnotation.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters annotated with {@link HasFormData} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bcode w800'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getFormParams() getFormParams}()
 * 		.{@link RequestFormParams#contains(String...) contains}(<jv>name</jv>);
 * </p>
 *
 * <p>
 * The parameter type can be a <jk>boolean</jk> or anything convertible from a <jk>boolean</jk>.
 */
public class HasFormDataArg implements RestOpArg {

	private final String name;
	private final Type type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link HasFormDataArg}, or <jk>null</jk> if the parameter is not annotated with {@link HasFormData}.
	 */
	public static HasFormDataArg create(ParamInfo paramInfo) {
		if (paramInfo.hasAnnotation(HasFormData.class))
			return new HasFormDataArg(paramInfo);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param pi The Java method parameter being resolved.
	 */
	protected HasFormDataArg(ParamInfo pi) {
		this.name = findName(pi.getAnnotations(HasFormData.class)).orElseThrow(() -> new ArgException(pi, "@HasFormData used without name or value"));
		this.type = pi.getParameterType().innerType();
	}

	@Override /* RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		RestRequest req = opSession.getRequest();
		BeanSession bs = req.getBeanSession();
		return bs.convertToType(req.getFormParams().contains(name), bs.getClassMeta(type));
	}
}
