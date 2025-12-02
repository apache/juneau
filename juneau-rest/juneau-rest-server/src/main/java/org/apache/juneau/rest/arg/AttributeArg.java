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

import static org.apache.juneau.commons.utils.StringUtils.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;

/**
 * Resolves method parameters and parameter types annotated with {@link Attr} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getAttributes() getAttributes}()
 * 		.{@link RequestAttributes#get(String) get}(<jv>name</jv>)
 * 		.{@link RequestAttribute#as(Class) as}(<jv>type</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class AttributeArg implements RestOpArg {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link AttributeArg}, or <jk>null</jk> if the parameter is not annotated with {@link Attr}.
	 */
	public static AttributeArg create(ParameterInfo paramInfo) {
		if (AP.has(Attr.class, paramInfo))
			return new AttributeArg(paramInfo);
		return null;
	}

	private final String name;

	private final Class<?> type;

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 */
	protected AttributeArg(ParameterInfo paramInfo) {
		this.name = getName(paramInfo);
		this.type = paramInfo.getParameterType().inner();
	}

	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		return opSession.getRequest().getAttribute(name).as(type).orElse(null);
	}

	private static String getName(ParameterInfo paramInfo) {
		// @formatter:off
		return AP.find(Attr.class, paramInfo)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> isAnyNotBlank(x.value(), x.name()))
			.findFirst()
			.map(x -> firstNonBlank(x.name(), x.value()))
			.orElseThrow(() -> new ArgException(paramInfo, "@Attr used without name or value"));
		// @formatter:on
	}
}