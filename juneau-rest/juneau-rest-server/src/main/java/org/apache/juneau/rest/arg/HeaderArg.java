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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.commons.lang.*;
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

	private static Optional<String> findName(ParameterInfo pi) {
		// @formatter:off
		return AP.find(Header.class, pi)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> isAnyNotBlank(x.value(), x.name()))
			.findFirst()
			.map(x -> firstNonBlank(x.name(), x.value()));
		// @formatter:on
	}

	private static Optional<String> findDef(ParameterInfo pi) {
		// @formatter:off
		return AP.find(Header.class, pi)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> ne(x.def()))
			.findFirst()
			.map(Header::def);
		// @formatter:on
	}

	private static Header getClassLevelHeader(ParameterInfo pi, String paramName) {
		var declaringClass = pi.getMethod().getDeclaringClass();
		if (declaringClass == null)
			return null;
		var restAnnotation = declaringClass.getAnnotations(Rest.class).findFirst().map(AnnotationInfo::inner).orElse(null);
		if (restAnnotation == null)
			return null;
		for (var h : restAnnotation.headerParams()) {
			var hName = firstNonEmpty(h.name(), h.value());
			if (paramName.equals(hName))
				return h;
		}
		return null;
	}

	private final HttpPartParser partParser;
	private final HttpPartSchema schema;
	private final boolean multi;
	private final String name;
	private final String def;
	private final ClassInfo type;

	/**
	 * Constructor.
	 *
	 * @param pi The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 */
	protected HeaderArg(ParameterInfo pi, AnnotationWorkList annotations) {
		this.name = findName(pi).orElseThrow(() -> new ArgException(pi, "@Header used without name or value"));

		var classLevelHeader = getClassLevelHeader(pi, name);

		var schemaBuilder = HttpPartSchema.create();
		if (classLevelHeader != null)
			schemaBuilder.apply(classLevelHeader);
		schemaBuilder.applyAll(Header.class, pi);
		this.schema = schemaBuilder.build();

		this.def = findDef(pi).or(() -> Optional.ofNullable(classLevelHeader).filter(h -> ne(h.def())).map(Header::def)).orElse(null);
		this.type = pi.getParameterType();
		var pp = schema.getParser();
		this.partParser = nn(pp) ? HttpPartParser.creator().type(pp).apply(annotations).create() : null;
		this.multi = schema.getCollectionFormat() == HttpPartCollectionFormat.MULTI;

		if (multi && ! type.isCollectionOrArray())
			throw new ArgException(pi, "Use of multipart flag on @Header parameter that is not an array or Collection");
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for REST argument resolution with generic types
		"unchecked", // Type erasure requires unchecked casts in REST argument parsing
	})
	@Override /* Overridden from RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		var req = opSession.getRequest();
		var ps = partParser == null ? req.getPartParserSession() : partParser.getPartSession();
		var rh = req.getHeaders();
		var bs = req.getBeanSession();
		var cm = bs.getClassMeta(type.innerType());

		if (multi) {
			Collection c;
			if (cm.isArray()) {
				c = list();
			} else if (cm.canCreateNewInstance()) {
				c = (Collection)cm.newInstance();
			} else {
				c = new JsonList();
			}
			rh.stream(name).map(x -> x.parser(ps).schema(schema).as(cm.getElementType()).orElse(null)).forEach(c::add);
			return cm.isArray() ? toArray(c, cm.getElementType().inner()) : c;
		}

		if (cm.isMapOrBean() && isOneOf(name, "*", "")) {
			var m = new JsonMap();
			rh.forEach(x -> m.put(x.getName(), x.parser(ps).schema(schema == null ? null : schema.getProperty(x.getName())).as(cm.getValueType()).orElse(null)));
			return req.getBeanSession().convertToType(m, cm);
		}

		return rh.getLast(name).parser(ps).schema(schema).def(def).as(type.innerType()).orElse(null);
	}
}
