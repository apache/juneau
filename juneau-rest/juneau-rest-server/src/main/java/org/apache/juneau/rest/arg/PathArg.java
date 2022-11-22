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

import static org.apache.juneau.http.annotation.PathAnnotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class PathArg implements RestOpArg {
	private final HttpPartParser partParser;
	private final HttpPartSchema schema;
	private final String name, def;
	private final Type type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @param pathMatcher Path matcher for the specified method.
	 * @return A new {@link PathArg}, or <jk>null</jk> if the parameter is not annotated with {@link Path}.
	 */
	public static PathArg create(ParamInfo paramInfo, AnnotationWorkList annotations, UrlPathMatcher pathMatcher) {
		if (paramInfo.hasAnnotation(Path.class) || paramInfo.getParameterType().hasAnnotation(Path.class))
			return new PathArg(paramInfo, annotations, pathMatcher);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @param annotations The annotations to apply to any new part parsers.
	 * @param pathMatcher Path matcher for the specified method.
	 */
	protected PathArg(ParamInfo paramInfo, AnnotationWorkList annotations, UrlPathMatcher pathMatcher) {
		this.name = getName(paramInfo, pathMatcher);
		this.def = findDef(paramInfo).orElse(null);
		this.type = paramInfo.getParameterType().innerType();
		this.schema = HttpPartSchema.create(Path.class, paramInfo);
		Class<? extends HttpPartParser> pp = schema.getParser();
		this.partParser = pp != null ? HttpPartParser.creator().type(pp).apply(annotations).create() : null;
	}

	private String getName(ParamInfo pi, UrlPathMatcher pathMatcher) {
		String p = findName(pi).orElse(null);
		if (p != null)
			return p;
		if (pathMatcher != null) {
			int idx = 0;
			int i = pi.getIndex();
			MethodInfo mi = pi.getMethod();

			for (int j = 0; j < i; j++)
				if (mi.getParam(i).getAnnotation(Path.class) != null)
					idx++;

			String[] vars = pathMatcher.getVars();
			if (vars.length <= idx)
				throw new ArgException(pi, "Number of attribute parameters exceeds the number of URL pattern variables");

			// Check for {#} variables.
			String idxs = String.valueOf(idx);
			for (int j = 0; j < vars.length; j++)
				if (StringUtils.isNumeric(vars[j]) && vars[j].equals(idxs))
					return vars[j];

			return pathMatcher.getVars()[idx];
		}
		throw new ArgException(pi, "@Path used without name or value");
	}

	@Override /* RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		RestRequest req = opSession.getRequest();
		if (name.equals("*")) {
			JsonMap m = new JsonMap();
			req.getPathParams().stream().forEach(x -> m.put(x.getName(), x.getValue()));
			return req.getBeanSession().convertToType(m, type);
		}
		HttpPartParserSession ps = partParser == null ? req.getPartParserSession() : partParser.getPartSession();
		return req.getPathParams().get(name).parser(ps).schema(schema).def(def).as(type).orElse(null);
	}
}
