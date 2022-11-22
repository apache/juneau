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

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Resolves method parameters on {@link RestOp}-annotated Java methods of types found on the {@link RestRequest} object.
 *
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HttpServletRequest}
 * 	<li class='jc'>{@link HttpPartParserSession}
 * 	<li class='jc'>{@link HttpPartSerializerSession}
 * 	<li class='jc'>{@link InputStream}
 * 	<li class='jc'>{@link Locale}
 * 	<li class='jc'>{@link Messages}
 * 	<li class='jc'>{@link Reader}
 * 	<li class='jc'>{@link RequestAttributes}
 * 	<li class='jc'>{@link RequestContent}
 * 	<li class='jc'>{@link RequestFormParams}
 * 	<li class='jc'>{@link RequestHeaders}
 * 	<li class='jc'>{@link RequestPathParams}
 * 	<li class='jc'>{@link RequestQueryParams}
 * 	<li class='jc'>{@link ResourceBundle}
 * 	<li class='jc'>{@link RestRequest}
 * 	<li class='jc'>{@link ServletInputStream}
 * 	<li class='jc'>{@link Swagger}
 * 	<li class='jc'>{@link TimeZone}
 * 	<li class='jc'>{@link UriContext}
 * 	<li class='jc'>{@link UriResolver}
 * 	<li class='jc'>{@link VarResolverSession}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 */
public class RestRequestArgs extends SimpleRestOperationArg {

	/**
	 * Static creator.
	 *
	 * @param paramInfo The Java method parameter being resolved.
	 * @return A new arg, or <jk>null</jk> if the parameter type is not one of the supported types.
	 */
	public static RestRequestArgs create(ParamInfo paramInfo) {
		if (paramInfo.isType(HttpPartParserSession.class))
			return new RestRequestArgs(x->x.getPartParserSession());
		if (paramInfo.isType(HttpPartSerializerSession.class))
			return new RestRequestArgs(x->x.getPartSerializerSession());
		if (paramInfo.isType(InputStream.class))
			return new RestRequestArgs(x->x.getInputStream());
		if (paramInfo.isType(Locale.class))
			return new RestRequestArgs(x->x.getLocale());
		if (paramInfo.isType(Messages.class))
			return new RestRequestArgs(x->x.getMessages());
		if (paramInfo.isType(Reader.class))
			return new RestRequestArgs(x->x.getReader());
		if (paramInfo.isType(RequestAttributes.class))
			return new RestRequestArgs(x->x.getAttributes());
		if (paramInfo.isType(RequestContent.class))
			return new RestRequestArgs(x->x.getContent());
		if (paramInfo.isType(RequestFormParams.class))
			return new RestRequestArgs(x->x.getFormParams());
		if (paramInfo.isType(RequestHeaders.class))
			return new RestRequestArgs(x->x.getHeaders());
		if (paramInfo.isType(RequestPathParams.class))
			return new RestRequestArgs(x->x.getPathParams());
		if (paramInfo.isType(RequestQueryParams.class))
			return new RestRequestArgs(x->x.getQueryParams());
		if (paramInfo.isType(ResourceBundle.class))
			return new RestRequestArgs(x->x.getMessages());
		if (paramInfo.isType(RestRequest.class))
			return new RestRequestArgs(x->x);
		if (paramInfo.isType(ServletInputStream.class))
			return new RestRequestArgs(x->x.getInputStream());
		if (paramInfo.isType(Swagger.class))
			return new RestRequestArgs(x->x.getSwagger().orElse(null));
		if (paramInfo.isType(TimeZone.class))
			return new RestRequestArgs(x->x.getTimeZone().orElse(null));
		if (paramInfo.isType(UriContext.class))
			return new RestRequestArgs(x->x.getUriContext());
		if (paramInfo.isType(UriResolver.class))
			return new RestRequestArgs(x->x.getUriResolver());
		if (paramInfo.isType(VarResolverSession.class))
			return new RestRequestArgs(x->x.getVarResolverSession());
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param <T> The function return type.
	 * @param function The function for finding the arg.
	 */
	protected <T> RestRequestArgs(ThrowingFunction<RestRequest,T> function) {
		super((session)->function.apply(session.getRequest()));
	}
}
