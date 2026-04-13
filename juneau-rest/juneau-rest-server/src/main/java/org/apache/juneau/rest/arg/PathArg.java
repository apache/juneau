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

import static org.apache.juneau.Constants.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.util.*;

/**
 * Resolves method parameters and parameter types annotated with {@link Path} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getPathParams() getPathParams}()
 * 		.{@link RequestPathParams#get(String) get}(<jv>name</jv>)
 * 		.{@link RequestPathParam#as(Class) as}(<jv>type</jv>);
 * </p>
 *
 * <p>
 * {@link HttpPartSchema schema} is derived from the {@link Path} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class PathArg implements RestOpArg {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @param pathMatcher Path matcher for the specified method.
	 * @return A new {@link PathArg}, or <jk>null</jk> if the parameter is not annotated with {@link Path}.
	 */
	public static PathArg create(ParameterInfo paramInfo, AnnotationWorkList annotations, UrlPathMatcher pathMatcher) {
		if (AP.has(Path.class, paramInfo))
			return new PathArg(paramInfo, annotations, pathMatcher);
		return null;
	}

	private static Optional<String> findName(ParameterInfo pi) {
		// @formatter:off
		return AP.find(Path.class, pi)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> isAnyNotBlank(x.value(), x.name()))
			.findFirst()
			.map(x -> firstNonBlank(x.name(), x.value()));
		// @formatter:on
	}

	private static Optional<String> findDef(ParameterInfo pi) {
		// @formatter:off
		return AP.find(Path.class, pi)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> ne(x.def()) && neq(NONE, x.def()))
			.findFirst()
			.map(Path::def);
		// @formatter:on
	}

	private static Path getClassLevelPath(ParameterInfo pi, String paramName) {
		var declaringClass = pi.getMethod().getDeclaringClass();
		if (declaringClass == null)
			return null;
		var restAnnotation = declaringClass.getAnnotations(Rest.class).findFirst().map(AnnotationInfo::inner).orElse(null);
		if (restAnnotation == null)
			return null;
		for (var p : restAnnotation.pathParams()) {
			var pName = firstNonEmpty(p.name(), p.value());
			if (paramName.equals(pName))
				return p;
		}
		return null;
	}

	private final HttpPartParser partParser;
	private final HttpPartSchema schema;
	private final String name;
	private final String def;
	private final Type type;

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @param pathMatcher Path matcher for the specified method.
	 */
	protected PathArg(ParameterInfo paramInfo, AnnotationWorkList annotations, UrlPathMatcher pathMatcher) {
		this.name = getName(paramInfo, pathMatcher);

		var classLevelPath = getClassLevelPath(paramInfo, name);

		var schemaBuilder = HttpPartSchema.create();
		if (classLevelPath != null)
			schemaBuilder.apply(classLevelPath);
		schemaBuilder.applyAll(Path.class, paramInfo);
		this.schema = schemaBuilder.build();

		this.def = findDef(paramInfo).or(() -> Optional.ofNullable(classLevelPath).filter(p -> ne(p.def()) && neq(NONE, p.def())).map(Path::def)).orElse(null);
		this.type = paramInfo.getParameterType().innerType();
		var pp = schema.getParser();
		this.partParser = nn(pp) ? HttpPartParser.creator().type(pp).apply(annotations).create() : null;
	}

	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		var req = opSession.getRequest();
		if (name.equals("*")) {
			var m = new JsonMap();
			req.getPathParams().stream().forEach(x -> m.put(x.getName(), x.getValue()));
			return req.getBeanSession().convertToType(m, type);
		}
		var ps = partParser == null ? req.getPartParserSession() : partParser.getPartSession();
		return req.getPathParams().get(name).parser(ps).schema(schema).def(def).as(type).orElse(null);
	}

	private static String getName(ParameterInfo pi, UrlPathMatcher pathMatcher) {
		var p = findName(pi).orElse(null);
		if (nn(p))
			return p;
		if (nn(pathMatcher)) {
			var idx = 0;
			var i = pi.getIndex();
			var mi = pi.getMethod();

			for (var j = 0; j < i; j++) {
				var hasAnnotation = AP.has(Path.class, mi.getParameter(j));
				if (hasAnnotation)
					idx++;
			}

			var vars = pathMatcher.getVars();
			if (vars.length <= idx)
				throw new ArgException(pi, "Number of attribute parameters exceeds the number of URL pattern variables.  vars.length={0}, idx={1}", vars.length, idx);

			var idxs = String.valueOf(idx);
			for (var v : vars)
				if (isNumeric(v) && v.equals(idxs))
					return v;

			return pathMatcher.getVars()[idx];
		}
		throw new ArgException(pi, "@Path used without name or value");
	}
}
