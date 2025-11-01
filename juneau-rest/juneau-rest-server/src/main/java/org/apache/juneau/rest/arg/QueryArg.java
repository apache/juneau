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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.annotation.QueryAnnotation.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;

/**
 * Resolves method parameters and parameter types annotated with {@link Query} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getQueryParams() getQueryParams}()
 * 		.{@link RequestQueryParams#get(String) get}(<jv>name</jv>)
 * 		.{@link RequestQueryParam#as(Class) as}(<jv>type</jv>);
 * </p>
 *
 * <p>
 * {@link HttpPartSchema schema} is derived from the {@link Query} annotation.
 *
 * <p>
 * If the {@link Schema#collectionFormat()} value is {@link HttpPartCollectionFormat#MULTI}, then the data type can be a {@link Collection} or array.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class QueryArg implements RestOpArg {
	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @return A new {@link QueryArg}, or <jk>null</jk> if the parameter is not annotated with {@link Query}.
	 */
	public static QueryArg create(ParameterInfo paramInfo, AnnotationWorkList annotations) {
		if (paramInfo.hasAnnotation(Query.class) || paramInfo.getParameterType().hasAnnotation(Query.class))
			return new QueryArg(paramInfo, annotations);
		return null;
	}

	/**
	 * Gets the merged @Query annotation combining class-level and parameter-level values.
	 *
	 * @param pi The parameter info.
	 * @param paramName The query parameter name.
	 * @return Merged annotation, or null if no class-level defaults exist.
	 */
	private static Query getMergedQuery(ParameterInfo pi, String paramName) {
		// Get the declaring class
		ClassInfo declaringClass = pi.getMethod().getDeclaringClass();
		if (declaringClass == null)
			return null;

		// Find @Rest annotation on the class
		Rest restAnnotation = declaringClass.getAnnotation(Rest.class);
		if (restAnnotation == null)
			return null;

		// Find matching @Query from class-level queryParams array
		Query classLevelQuery = null;
		for (var q : restAnnotation.queryParams()) {
			String qName = firstNonEmpty(q.name(), q.value());
			if (paramName.equals(qName)) {
				classLevelQuery = q;
				break;
			}
		}

		if (classLevelQuery == null)
			return null;

		// Get parameter-level @Query
		Query paramQuery = pi.getAnnotation(Query.class);
		if (paramQuery == null)
			paramQuery = pi.getParameterType().getAnnotation(Query.class);

		if (paramQuery == null) {
			// No parameter-level @Query, use class-level as-is
			return classLevelQuery;
		}

		// Merge the two annotations: parameter-level takes precedence
		return mergeAnnotations(classLevelQuery, paramQuery);
	}

	/**
	 * Merges two @Query annotations, with param-level taking precedence over class-level.
	 *
	 * @param classLevel The class-level default.
	 * @param paramLevel The parameter-level override.
	 * @return Merged annotation.
	 */
	private static Query mergeAnnotations(Query classLevel, Query paramLevel) {
		// @formatter:off
		return QueryAnnotation.create()
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

	private final boolean multi;

	private final HttpPartParser partParser;

	private final HttpPartSchema schema;

	private final String name, def;

	private final ClassInfo type;

	/**
	 * Constructor.
	 *
	 * @param pi The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 */
	protected QueryArg(ParameterInfo pi, AnnotationWorkList annotations) {
		// Get the query name from the parameter
		this.name = findName(pi).orElseThrow(() -> new ArgException(pi, "@Query used without name or value"));

		// Check for class-level defaults and merge if found
		Query mergedQuery = getMergedQuery(pi, name);

		// Use merged query annotation for all lookups
		this.def = nn(mergedQuery) && ! mergedQuery.def().isEmpty() ? mergedQuery.def() : findDef(pi).orElse(null);
		this.type = pi.getParameterType();
		this.schema = nn(mergedQuery) ? HttpPartSchema.create(mergedQuery) : HttpPartSchema.create(Query.class, pi);
		Class<? extends HttpPartParser> pp = schema.getParser();
		this.partParser = nn(pp) ? HttpPartParser.creator().type(pp).apply(annotations).create() : null;
		this.multi = schema.getCollectionFormat() == HttpPartCollectionFormat.MULTI;

		if (multi && ! type.isCollectionOrArray())
			throw new ArgException(pi, "Use of multipart flag on @Query parameter that is not an array or Collection");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		RestRequest req = opSession.getRequest();
		HttpPartParserSession ps = partParser == null ? req.getPartParserSession() : partParser.getPartSession();
		RequestQueryParams rh = req.getQueryParams();
		BeanSession bs = req.getBeanSession();
		ClassMeta<?> cm = bs.getClassMeta(type.innerType());

		if (multi) {
			Collection c = cm.isArray() ? list() : (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new JsonList());
			rh.getAll(name).stream().map(x -> x.parser(ps).schema(schema).as(cm.getElementType()).orElse(null)).forEach(x -> c.add(x));
			return cm.isArray() ? toArray(c, cm.getElementType().getInnerClass()) : c;
		}

		if (cm.isMapOrBean() && isOneOf(name, "*", "")) {
			var m = new JsonMap();
			rh.forEach(e -> m.put(e.getName(), e.parser(ps).schema(schema == null ? null : schema.getProperty(e.getName())).as(cm.getValueType()).orElse(null)));
			return req.getBeanSession().convertToType(m, cm);
		}

		return rh.getLast(name).parser(ps).schema(schema).def(def).as(type.innerType()).orElse(null);
	}
}