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

import java.lang.reflect.*;

import org.apache.juneau.rest.annotation.*;

/**
 * REST java method parameter resolver.
 *
 * <p>
 * Used to resolve instances of classes being passed to Java REST methods.
 *
 * <p>
 * This class is associated with REST classes via the {@link RestResource#paramResolvers()} annotation and
 * {@link RestConfig#addParamResolvers(Class...)} method.
 */
public abstract class RestParam {

	final RestParamType paramType;
	final String name;
	final Type type;

	/**
	 * Constructor.
	 *
	 * @param paramType The Swagger parameter type.
	 * @param name
	 * 	The parameter name.
	 * 	Can be <jk>null</jk> if parameter doesn't have a name (e.g. the request body).
	 * @param type The object type to convert the parameter to.
	 */
	protected RestParam(RestParamType paramType, String name, Type type) {
		this.paramType = paramType;
		this.name = name;
		this.type = type;
	}

	/**
	 * Resolves the parameter object.
	 *
	 * @param req The rest request.
	 * @param res The rest response.
	 * @return The resolved object.
	 * @throws Exception
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
}
