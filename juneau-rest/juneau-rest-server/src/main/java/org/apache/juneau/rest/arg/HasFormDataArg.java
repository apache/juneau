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

import static org.apache.juneau.common.utils.StringUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;

/**
 * Resolves method parameters annotated with {@link HasFormData} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getFormParams() getFormParams}()
 * 		.{@link RequestFormParams#contains(String) contains}(<jv>name</jv>);
 * </p>
 *
 * <p>
 * The parameter type can be a <jk>boolean</jk> or anything convertible from a <jk>boolean</jk>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class HasFormDataArg implements RestOpArg {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link HasFormDataArg}, or <jk>null</jk> if the parameter is not annotated with {@link HasFormData}.
	 */
	public static HasFormDataArg create(ParameterInfo paramInfo) {
		if (AP.has(HasFormData.class, paramInfo))
			return new HasFormDataArg(paramInfo);
		return null;
	}

	private static String getName(HasFormData x) {
		return firstNonEmpty(x.name(), x.value());
	}

	private static boolean hasName(HasFormData x) {
		return isAnyNotBlank(x.name(), x.value());
	}

	private final String name;
	private final Type type;

	/**
	 * Constructor.
	 *
	 * @param pi The Java method parameter being resolved.
	 */
	protected HasFormDataArg(ParameterInfo pi) {
		// @formatter:off
		this.name = AP.find(HasFormData.class, pi)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(HasFormDataArg::hasName)
			.findFirst()
			.map(HasFormDataArg::getName)
			.orElseThrow(() -> new ArgException(pi, "@HasFormData used without name or value"));
		// @formatter:on
		this.type = pi.getParameterType().innerType();
	}

	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		var req = opSession.getRequest();
		var bs = req.getBeanSession();
		return bs.convertToType(req.getFormParams().contains(name), bs.getClassMeta(type));
	}
}