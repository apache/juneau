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
import static org.apache.juneau.http.annotation.PathRemainderAnnotation.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.util.*;

/**
 * Resolves method parameters annotated with {@link PathRemainder} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * This is a specialized version of {@link PathArg} for the path remainder (the part matched by {@code /*}).
 * It's functionally equivalent to using {@code @Path("/*")}, but provides a more intuitive annotation name.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getPathParams() getPathParams}()
 * 		.{@link RequestPathParams#get(String) get}(<js>"/*"</js>)
 * 		.{@link RequestPathParam#as(Class) as}(<jv>type</jv>);
 * </p>
 *
 * <p>
 * {@link HttpPartSchema schema} is derived from the {@link PathRemainder} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * 	<li class='ja'>{@link PathRemainder}
 * 	<li class='jc'>{@link PathArg}
 * </ul>
 *
 * @since 9.2.0
 */
public class PathRemainderArg implements RestOpArg {
	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @param pathMatcher Path matcher for the specified method (not used, but included for BeanStore compatibility).
	 * @return A new {@link PathRemainderArg}, or <jk>null</jk> if the parameter is not annotated with {@link PathRemainder}.
	 */
	public static PathRemainderArg create(ParamInfo paramInfo, AnnotationWorkList annotations, UrlPathMatcher pathMatcher) {
		if (paramInfo.hasAnnotation(PathRemainder.class) || paramInfo.getParameterType().hasAnnotation(PathRemainder.class))
			return new PathRemainderArg(paramInfo, annotations);
		return null;
	}

	private final HttpPartParser partParser;
	private final HttpPartSchema schema;
	private final String def;

	private final Type type;

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 */
	protected PathRemainderArg(ParamInfo paramInfo, AnnotationWorkList annotations) {
		this.def = findDef(paramInfo).orElse(null);
		this.type = paramInfo.getParameterType().innerType();
		this.schema = HttpPartSchema.create(PathRemainder.class, paramInfo);
		Class<? extends HttpPartParser> pp = schema.getParser();
		this.partParser = nn(pp) ? HttpPartParser.creator().type(pp).apply(annotations).create() : null;
	}

	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		RestRequest req = opSession.getRequest();
		HttpPartParserSession ps = partParser == null ? req.getPartParserSession() : partParser.getPartSession();
		// The path remainder is stored under the name "/*"
		return req.getPathParams().get("/*").parser(ps).schema(schema).def(def).as(type).orElse(null);
	}
}