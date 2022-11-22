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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.http.annotation.HeaderAnnotation.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;

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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class HeaderArg implements RestOpArg {
	private final HttpPartParser partParser;
	private final HttpPartSchema schema;
	private final boolean multi;
	private final String name, def;
	private final ClassInfo type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @return A new {@link HeaderArg}, or <jk>null</jk> if the parameter is not annotated with {@link Header}.
	 */
	public static HeaderArg create(ParamInfo paramInfo, AnnotationWorkList annotations) {
		if ((!paramInfo.getParameterType().is(Value.class)) && (paramInfo.hasAnnotation(Header.class) || paramInfo.getParameterType().hasAnnotation(Header.class)))
			return new HeaderArg(paramInfo, annotations);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param pi The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 */
	protected HeaderArg(ParamInfo pi, AnnotationWorkList annotations) {
		this.name = findName(pi).orElseThrow(() -> new ArgException(pi, "@Header used without name or value"));
		this.def = findDef(pi).orElse(null);
		this.type = pi.getParameterType();
		this.schema = HttpPartSchema.create(Header.class, pi);
		Class<? extends HttpPartParser> pp = schema.getParser();
		this.partParser = pp != null ? HttpPartParser.creator().type(pp).apply(annotations).create() : null;
		this.multi = schema.getCollectionFormat() == HttpPartCollectionFormat.MULTI;

		if (multi && ! type.isCollectionOrArray())
			throw new ArgException(pi, "Use of multipart flag on @Header parameter that is not an array or Collection");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override /* RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		RestRequest req = opSession.getRequest();
		HttpPartParserSession ps = partParser == null ? req.getPartParserSession() : partParser.getPartSession();
		RequestHeaders rh = req.getHeaders();
		BeanSession bs = req.getBeanSession();
		ClassMeta<?> cm = bs.getClassMeta(type.innerType());

		if (multi) {
			Collection c = cm.isArray() ? list() : (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new JsonList());
			rh.stream(name).map(x -> x.parser(ps).schema(schema).as(cm.getElementType()).orElse(null)).forEach(x -> c.add(x));
			return cm.isArray() ? ArrayUtils.toArray(c, cm.getElementType().getInnerClass()) : c;
		}

		if (cm.isMapOrBean() && isOneOf(name, "*", "")) {
			JsonMap m = new JsonMap();
			rh.forEach(x -> m.put(x.getName(), x.parser(ps).schema(schema == null ? null : schema.getProperty(x.getName())).as(cm.getValueType()).orElse(null)));
			return req.getBeanSession().convertToType(m, cm);
		}

		return rh.getLast(name).parser(ps).schema(schema).def(def).as(type.innerType()).orElse(null);
	}
}
