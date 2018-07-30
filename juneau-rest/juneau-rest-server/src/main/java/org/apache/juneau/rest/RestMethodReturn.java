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

import static org.apache.juneau.internal.ReflectionUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;

/**
 * Contains metadata about the return type on a REST Java method.
 */
public class RestMethodReturn {

	private final Type type;
	private final int code;
	private final ObjectMap api;
	private final ResponseMeta responseMeta;

	RestMethodReturn(Method m, HttpPartSerializer partSerializer, PropertyStore ps) {
		this.responseMeta = ResponseMeta.create(m, ps);

		HttpPartSchema s = HttpPartSchema.DEFAULT;
		if (hasAnnotation(Response.class, m))
			s = HttpPartSchema.create(Response.class, m);

		this.type = m.getGenericReturnType();
		this.api = HttpPartSchema.getApiCodeMap(s, 200).unmodifiable();
		this.code = s.getCode(200);
	}

	/**
	 * Returns the return type of the Java method.
	 *
	 * @return The return type of the Java method.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the HTTP code code of the response.
	 *
	 * @return The HTTP code code of the response.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Returns the Swagger metadata associated with this return.
	 *
	 * @return A map of return metadata, never <jk>null</jk>.
	 */
	public ObjectMap getApi() {
		return api;
	}

	/**
	 * Returns metadata about the response object.
	 *
	 * @return
	 * 	Metadata about the response object.
	 * 	<br>Can be <jk>null</jk> if no {@link Response} annotation is associated with the return.
	 */
	public ResponseMeta getResponseMeta() {
		return responseMeta;
	}
}
