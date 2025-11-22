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
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.annotation.PathAnnotation.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.common.reflect.*;
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

	private static AnnotationProvider AP = AnnotationProvider.INSTANCE;

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

	/**
	 * Gets the merged @Path annotation combining class-level and parameter-level values.
	 *
	 * @param pi The parameter info.
	 * @param paramName The path parameter name.
	 * @return Merged annotation, or null if no class-level defaults exist.
	 */
	private static Path getMergedPath(ParameterInfo pi, String paramName) {
		// Get the declaring class
		var declaringClass = pi.getMethod().getDeclaringClass();
		if (declaringClass == null)
			return null;

		// Find @Rest annotation on the class
		var restAnnotation = declaringClass.getAnnotations(Rest.class).findFirst().map(AnnotationInfo::inner).orElse(null);
		if (restAnnotation == null)
			return null;

		// Find matching @Path from class-level pathParams array
		Path classLevelPath = null;
		for (var p : restAnnotation.pathParams()) {
			String pName = firstNonEmpty(p.name(), p.value());
			if (paramName.equals(pName)) {
				classLevelPath = p;
				break;
			}
		}

		if (classLevelPath == null)
			return null;

		// Get parameter-level @Path
		var paramPath = AP.find(Path.class, pi).stream().findFirst().map(AnnotationInfo::inner).orElse(null);

		if (paramPath == null) {
			// No parameter-level @Path, use class-level as-is
			return classLevelPath;
		}

		// Merge the two annotations: parameter-level takes precedence
		return mergeAnnotations(classLevelPath, paramPath);
	}

	/**
	 * Merges two @Path annotations, with param-level taking precedence over class-level.
	 *
	 * @param classLevel The class-level default.
	 * @param paramLevel The parameter-level override.
	 * @return Merged annotation.
	 */
	private static Path mergeAnnotations(Path classLevel, Path paramLevel) {
		// @formatter:off
		return PathAnnotation.create()
			.name(firstNonEmpty(paramLevel.name(), paramLevel.value(), classLevel.name(), classLevel.value()))
			.value(firstNonEmpty(paramLevel.value(), paramLevel.name(), classLevel.value(), classLevel.name()))
			.def(firstNonEmpty(paramLevel.def(), classLevel.def()))
			.description(paramLevel.description().length > 0 ? paramLevel.description() : classLevel.description())
			.parser(paramLevel.parser() != HttpPartParser.Void.class ? paramLevel.parser() : classLevel.parser())
			.serializer(paramLevel.serializer() != HttpPartSerializer.Void.class ? paramLevel.serializer() : classLevel.serializer())
			.schema(mergeSchemas(classLevel.schema(), paramLevel.schema()))
			.build();
		// @formatter:on
	}

	/**
	 * Merges two @Schema annotations, with param-level taking precedence.
	 *
	 * @param classLevel The class-level default.
	 * @param paramLevel The parameter-level override.
	 * @return Merged annotation.
	 */
	private static Schema mergeSchemas(Schema classLevel, Schema paramLevel) {
		// If parameter has a non-default schema, use it; otherwise use class-level
		if (! SchemaAnnotation.empty(paramLevel))
			return paramLevel;
		return classLevel;
	}

	private final HttpPartParser partParser;
	private final HttpPartSchema schema;
	private final String name, def;
	private final Type type;

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @param pathMatcher Path matcher for the specified method.
	 */
	protected PathArg(ParameterInfo paramInfo, AnnotationWorkList annotations, UrlPathMatcher pathMatcher) {
		// Get the path parameter name
		this.name = getName(paramInfo, pathMatcher);

		// Check for class-level defaults and merge if found
		var mergedPath = getMergedPath(paramInfo, name);

		// Use merged path annotation for all lookups
		var pathDef = nn(mergedPath) ? mergedPath.def() : null;
		this.def = nn(pathDef) && ne(NONE, pathDef) ? pathDef : findDef(paramInfo).orElse(null);
		this.type = paramInfo.getParameterType().innerType();
		this.schema = nn(mergedPath) ? HttpPartSchema.create(mergedPath) : HttpPartSchema.create(Path.class, paramInfo);
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
			int idx = 0;
			int i = pi.getIndex();
			var mi = pi.getMethod();

			for (int j = 0; j < i; j++) {
				var hasAnnotation = AnnotationProvider.INSTANCE.has(Path.class, mi.getParameter(j));
				if (hasAnnotation)
					idx++;
			}

			var vars = pathMatcher.getVars();
			if (vars.length <= idx)
				throw new ArgException(pi, "Number of attribute parameters exceeds the number of URL pattern variables.  vars.length={0}, idx={1}", vars.length, idx);

			// Check for {#} variables.
			var idxs = String.valueOf(idx);
			for (var var : vars)
				if (isNumeric(var) && var.equals(idxs))
					return var;

			return pathMatcher.getVars()[idx];
		}
		throw new ArgException(pi, "@Path used without name or value");
	}
}