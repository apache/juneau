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
package org.apache.juneau.rest;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.MessageBundle;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.Date;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;

/**
 * REST java method parameter resolver.
 *
 * <p>
 * Used to resolve instances of classes being passed to Java REST methods.
 *
 * <p>
 * By default, the following parameter types can be passed into Java methods in any order:
 *
 * <h5 class='topic'>Standard top-level objects</h5>
 * <ul>
 * 	<li><b>Standard top-level objects</b>
 * 	<ul>
 * 		<li class='jc'>{@link HttpServletRequest}
 * 		<li class='jc'>{@link RestRequest}
 * 		<li class='jc'>{@link HttpServletResponse}
 * 		<li class='jc'>{@link RestResponse}
 * 	</ul>
 * 	<li><b>Headers</b>
 * 	<ul>
 * 		<li class='jc'>{@link Accept}
 * 		<li class='jc'>{@link AcceptCharset}
 * 		<li class='jc'>{@link AcceptEncoding}
 * 		<li class='jc'>{@link AcceptLanguage}
 * 		<li class='jc'>{@link Authorization}
 * 		<li class='jc'>{@link CacheControl}
 * 		<li class='jc'>{@link Connection}
 * 		<li class='jc'>{@link ContentLength}
 * 		<li class='jc'>{@link ContentType}
 * 		<li class='jc'>{@link Date}
 * 		<li class='jc'>{@link Expect}
 * 		<li class='jc'>{@link From}
 * 		<li class='jc'>{@link Host}
 * 		<li class='jc'>{@link IfMatch}
 * 		<li class='jc'>{@link IfModifiedSince}
 * 		<li class='jc'>{@link IfNoneMatch}
 * 		<li class='jc'>{@link IfRange}
 * 		<li class='jc'>{@link IfUnmodifiedSince}
 * 		<li class='jc'>{@link MaxForwards}
 * 		<li class='jc'>{@link Pragma}
 * 		<li class='jc'>{@link ProxyAuthorization}
 * 		<li class='jc'>{@link Range}
 * 		<li class='jc'>{@link Referer}
 * 		<li class='jc'>{@link TE}
 * 		<li class='jc'>{@link TimeZone}
 * 		<li class='jc'>{@link UserAgent}
 * 		<li class='jc'>{@link Upgrade}
 * 		<li class='jc'>{@link Via}
 * 		<li class='jc'>{@link Warning}
 * 	</ul>
 * 	<li><b>Other objects</b>
 * 	<ul>
 * 		<li class='jc'>{@link Config}
 * 		<li class='jc'>{@link InputStream}
 * 		<li class='jc'>{@link Locale}
 * 		<li class='jc'>{@link MessageBundle}
 * 		<li class='jc'>{@link OutputStream}
 * 		<li class='jc'>{@link Parser}
 * 		<li class='jc'>{@link Reader}
 * 		<li class='jc'>{@link RequestBody}
 * 		<li class='jc'>{@link RequestFormData}
 * 		<li class='jc'>{@link RequestHeaders}
 * 		<li class='jc'>{@link RequestAttributes}
 * 		<li class='jc'>{@link RequestPath}
 * 		<li class='jc'>{@link RequestQuery}
 * 		<li class='jc'>{@link ResourceBundle}
 * 		<li class='jc'>{@link RestContext}
 * 		<li class='jc'>{@link ServletInputStream}
 * 		<li class='jc'>{@link ServletOutputStream}
 * 		<li class='jc'>{@link Swagger}
 * 		<li class='jc'>{@link UriContext}
 * 		<li class='jc'>{@link UriResolver}
 * 		<li class='jc'>{@link Writer}
 *  	</ul>
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='jf'>{@link RestContext#REST_paramResolvers}
 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.MethodParameters}
 * </ul>
 */
public abstract class RestMethodParam {

	final RestParamType paramType;
	final ParamInfo mpi;
	final String name;
	final Type type;
	final Class<?> c;

	/**
	 * Constructor.
	 *
	 * @param paramType The Swagger parameter type.
	 * @param mpi The method parameter.
	 * @param name
	 * 	The parameter name.
	 * 	Can be <jk>null</jk> if parameter doesn't have a name (e.g. the request body).
	 * @param type The object type to convert the parameter to.
	 */
	protected RestMethodParam(RestParamType paramType, ParamInfo mpi, String name, Type type) {
		this.paramType = paramType;
		this.mpi = mpi;
		this.name = name;
		this.type = type;
		this.c = type instanceof Class ? (Class<?>)type : type instanceof ParameterizedType ? (Class<?>)((ParameterizedType)type).getRawType() : null;
	}

	/**
	 * Constructor.
	 *
	 * @param paramType The Swagger parameter type.
	 * @param mpi The method parameter.
	 * @param name
	 * 	The parameter name.
	 * 	Can be <jk>null</jk> if parameter doesn't have a name (e.g. the request body).
	 */
	protected RestMethodParam(RestParamType paramType, ParamInfo mpi, String name) {
		this(paramType, mpi, name, mpi.getParameterType().innerType());
	}

	/**
	 * Constructor.
	 *
	 * @param paramType The Swagger parameter type.
	 * @param mpi The method parameter.
	 */
	protected RestMethodParam(RestParamType paramType, ParamInfo mpi) {
		this(paramType, mpi, null, mpi.getParameterType().innerType());
	}

	/**
	 * Constructor.
	 *
	 * @param paramType The Swagger parameter type.
	 * @param type The object type to convert the parameter to.
	 */
	protected RestMethodParam(RestParamType paramType, Type type) {
		this(paramType, null, null, type);
	}

	/**
	 * Constructor.
	 *
	 * @param paramType The Swagger parameter type.
	 * @param type The object type to convert the parameter to.
	 */
	protected RestMethodParam(RestParamType paramType, ClassInfo type) {
		this(paramType, null, null, type.innerType());
	}

	/**
	 * Constructor.
	 *
	 * @param paramType The Swagger parameter type.
	 * @param name
	 * 	The parameter name.
	 * 	Can be <jk>null</jk> if parameter doesn't have a name (e.g. the request body).
	 * @param type The object type to convert the parameter to.
	 */
	protected RestMethodParam(RestParamType paramType, String name, Type type) {
		this(paramType, null, name, type);
	}

	/**
	 * Resolves the parameter object.
	 *
	 * @param req The rest request.
	 * @param res The rest response.
	 * @return The resolved object.
	 * @throws Exception Generic error occurred.
	 */
	public abstract Object resolve(RestRequest req, RestResponse res) throws Exception;

	/**
	 * Returns the parameter class type that this parameter resolver is meant for.
	 *
	 * @return The parameter class type, or <jk>null</jk> if the type passed in isn't an instance of {@link Class}.
	 */
	protected Class<?> forClass() {
		if (type instanceof Class)
			return (Class<?>)type;
		return null;
	}

	/**
	 * Returns the swagger parameter type for this parameter as shown in the Swagger doc.
	 *
	 * @return the swagger parameter type for this parameter.
	 */
	protected RestParamType getParamType() {
		return paramType;
	}

	/**
	 * Returns the parameter info.
	 *
	 * @return The parameter info.
	 */
	public ParamInfo getMethodParamInfo() {
		return mpi;
	}

	/**
	 * Returns the parameter name for this parameter as shown in the Swagger doc.
	 *
	 * @return the parameter name for this parameter.
	 */
	protected String getName() {
		return name;
	}

	/**
	 * Returns the parameter class type.
	 *
	 * @return the parameter class type.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the parameter class type.
	 *
	 * @return the parameter class type.
	 */
	public Class<?> getTypeClass() {
		return c;
	}
}
