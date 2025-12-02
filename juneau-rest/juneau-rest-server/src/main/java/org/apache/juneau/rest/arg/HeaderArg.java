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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.http.annotation.HeaderAnnotation.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Resolves method parameters and parameter types annotated with {@link Header} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * This includes any of the following predefined request header types:
 *
 * <ul class='javatree condensed'>
 * 	<li class='jc'>{@link Accept}
 * 	<li class='jc'>{@link AcceptCharset}
 * 	<li class='jc'>{@link AcceptEncoding}
 * 	<li class='jc'>{@link AcceptLanguage}
 * 	<li class='jc'>{@link AcceptRanges}
 * 	<li class='jc'>{@link Authorization}
 * 	<li class='jc'>{@link CacheControl}
 * 	<li class='jc'>{@link ClientVersion}
 * 	<li class='jc'>{@link Connection}
 * 	<li class='jc'>{@link ContentDisposition}
 * 	<li class='jc'>{@link ContentEncoding}
 * 	<li class='jc'>{@link ContentLength}
 * 	<li class='jc'>{@link ContentType}
 * 	<li class='jc'>{@link org.apache.juneau.http.header.Date}
 * 	<li class='jc'>{@link Debug}
 * 	<li class='jc'>{@link Expect}
 * 	<li class='jc'>{@link Forwarded}
 * 	<li class='jc'>{@link From}
 * 	<li class='jc'>{@link Host}
 * 	<li class='jc'>{@link IfMatch}
 * 	<li class='jc'>{@link IfModifiedSince}
 * 	<li class='jc'>{@link IfNoneMatch}
 * 	<li class='jc'>{@link IfRange}
 * 	<li class='jc'>{@link IfUnmodifiedSince}
 * 	<li class='jc'>{@link MaxForwards}
 * 	<li class='jc'>{@link NoTrace}
 * 	<li class='jc'>{@link Origin}
 * 	<li class='jc'>{@link Pragma}
 * 	<li class='jc'>{@link ProxyAuthorization}
 * 	<li class='jc'>{@link Range}
 * 	<li class='jc'>{@link Referer}
 * 	<li class='jc'>{@link TE}
 * 	<li class='jc'>{@link Thrown}
 * 	<li class='jc'>{@link Upgrade}
 * 	<li class='jc'>{@link UserAgent}
 * 	<li class='jc'>{@link Warning}
 * </ul>
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getHeaders() getHeaders}();
 * </p>
 *
 * <p>
 * {@link HttpPartSchema schema} is derived from the {@link Header} annotation.
 *
 * <p>
 * If the {@link Schema#collectionFormat()} value is {@link HttpPartCollectionFormat#MULTI}, then the data type can be a {@link Collection} or array.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class HeaderArg implements RestOpArg {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @return A new {@link HeaderArg}, or <jk>null</jk> if the parameter is not annotated with {@link Header}.
	 */
	public static HeaderArg create(ParameterInfo paramInfo, AnnotationWorkList annotations) {
		if ((! paramInfo.getParameterType().is(Value.class)) && AP.has(Header.class, paramInfo))
			return new HeaderArg(paramInfo, annotations);
		return null;
	}

	/**
	 * Gets the merged @Header annotation combining class-level and parameter-level values.
	 *
	 * @param pi The parameter info.
	 * @param paramName The header name.
	 * @return Merged annotation, or null if no class-level defaults exist.
	 */
	private static Header getMergedHeader(ParameterInfo pi, String paramName) {
		// Get the declaring class
		var declaringClass = pi.getMethod().getDeclaringClass();
		if (declaringClass == null)
			return null;

		// Find @Rest annotation on the class
		var restAnnotation = declaringClass.getAnnotations(Rest.class).findFirst().map(AnnotationInfo::inner).orElse(null);
		if (restAnnotation == null)
			return null;

		// Find matching @Header from class-level headerParams array
		var classLevelHeader = (Header)null;
		for (var h : restAnnotation.headerParams()) {
			var hName = firstNonEmpty(h.name(), h.value());
			if (paramName.equals(hName)) {
				classLevelHeader = h;
				break;
			}
		}

		if (classLevelHeader == null)
			return null;

		// Get parameter-level @Header
		var paramHeader = AP.find(Header.class, pi).stream().findFirst().map(AnnotationInfo::inner).orElse(null);

		if (paramHeader == null) {
			// No parameter-level @Header, use class-level as-is
			return classLevelHeader;
		}

		// Merge the two annotations: parameter-level takes precedence
		return mergeAnnotations(classLevelHeader, paramHeader);
	}

	/**
	 * Merges two @Header annotations, with param-level taking precedence over class-level.
	 *
	 * @param classLevel The class-level default.
	 * @param paramLevel The parameter-level override.
	 * @return Merged annotation.
	 */
	private static Header mergeAnnotations(Header classLevel, Header paramLevel) {
		// @formatter:off
		return HeaderAnnotation.create()
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

	private final boolean multi;

	private final String name, def;

	private final ClassInfo type;

	/**
	 * Constructor.
	 *
	 * @param pi The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 */
	protected HeaderArg(ParameterInfo pi, AnnotationWorkList annotations) {
		// Get the header name from the parameter
		this.name = findName(pi).orElseThrow(() -> new ArgException(pi, "@Header used without name or value"));

		// Check for class-level defaults and merge if found
		var mergedHeader = getMergedHeader(pi, name);

		// Use merged header annotation for all lookups
		this.def = nn(mergedHeader) && ! mergedHeader.def().isEmpty() ? mergedHeader.def() : findDef(pi).orElse(null);
		this.type = pi.getParameterType();
		this.schema = nn(mergedHeader) ? HttpPartSchema.create(mergedHeader) : HttpPartSchema.create(Header.class, pi);
		var pp = schema.getParser();
		this.partParser = nn(pp) ? HttpPartParser.creator().type(pp).apply(annotations).create() : null;
		this.multi = schema.getCollectionFormat() == HttpPartCollectionFormat.MULTI;

		if (multi && ! type.isCollectionOrArray())
			throw new ArgException(pi, "Use of multipart flag on @Header parameter that is not an array or Collection");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		var req = opSession.getRequest();
		var ps = partParser == null ? req.getPartParserSession() : partParser.getPartSession();
		var rh = req.getHeaders();
		var bs = req.getBeanSession();
		var cm = bs.getClassMeta(type.innerType());

		if (multi) {
			var c = cm.isArray() ? list() : (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new JsonList());
			rh.stream(name).map(x -> x.parser(ps).schema(schema).as(cm.getElementType()).orElse(null)).forEach(x -> c.add(x));
			return cm.isArray() ? toArray(c, cm.getElementType().getInnerClass()) : c;
		}

		if (cm.isMapOrBean() && isOneOf(name, "*", "")) {
			var m = new JsonMap();
			rh.forEach(x -> m.put(x.getName(), x.parser(ps).schema(schema == null ? null : schema.getProperty(x.getName())).as(cm.getValueType()).orElse(null)));
			return req.getBeanSession().convertToType(m, cm);
		}

		return rh.getLast(name).parser(ps).schema(schema).def(def).as(type.innerType()).orElse(null);
	}
}