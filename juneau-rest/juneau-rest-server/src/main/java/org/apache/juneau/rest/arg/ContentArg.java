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

import java.lang.reflect.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;

/**
 * Resolves method parameters and parameter types annotated with {@link Content} on {@link RestOp}-annotated Java methods.
 *
 * <p>
 * The parameter value is resolved using:
 * <p class='bjava'>
 * 	<jv>opSession</jv>
 * 		.{@link RestOpSession#getRequest() getRequest}()
 * 		.{@link RestRequest#getContent() getContent}()
 * 		.{@link RequestContent#setSchema(HttpPartSchema) setSchema}(<jv>schema</jv>)
 * 		.{@link RequestContent#as(Type,Type...) as}(<jv>type</jv>);
 * </p>
 *
 * <p>
 * {@link HttpPartSchema schema} is derived from the {@link Content} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class ContentArg implements RestOpArg {

	private final HttpPartSchema schema;
	private final Type type;

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new {@link ContentArg}, or <jk>null</jk> if the parameter is not annotated with {@link Content}.
	 */
	public static ContentArg create(ParamInfo paramInfo) {
		if (paramInfo.hasAnnotation(Content.class) || paramInfo.getParameterType().hasAnnotation(Content.class))
			return new ContentArg(paramInfo);
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 */
	protected ContentArg(ParamInfo paramInfo) {
		this.type = paramInfo.getParameterType().innerType();
		this.schema = HttpPartSchema.create(Content.class, paramInfo);
	}

	@Override /* RestOpArg */
	public Object resolve(RestOpSession opSession) throws Exception {
		return opSession.getRequest().getContent().setSchema(schema).as(type);
	}
}
