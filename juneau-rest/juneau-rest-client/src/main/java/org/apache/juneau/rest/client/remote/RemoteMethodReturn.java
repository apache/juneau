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
package org.apache.juneau.rest.client.remote;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.reflect.*;

/**
 * Represents the metadata about the returned object of a method on a remote proxy interface.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-rest-client.RestProxies}
 * </ul>
 */
public final class RemoteMethodReturn {

	private final Type returnType;
	private final RemoteReturn returnValue;
	private final ResponseBeanMeta meta;

	RemoteMethodReturn(MethodInfo m) {
		RemoteMethod rm = m.getAnnotation(RemoteMethod.class);
		ClassInfo rt = m.getReturnType();
		RemoteReturn rv = rt.is(void.class) ? RemoteReturn.NONE : rm == null ? RemoteReturn.BODY : rm.returns();
		if (rt.hasAnnotation(Response.class) && rt.isInterface()) {
			this.meta = ResponseBeanMeta.create(m, PropertyStore.DEFAULT);
			rv = RemoteReturn.BEAN;
		} else {
			this.meta = null;
		}
		this.returnType = m.getReturnType().innerType();
		this.returnValue = rv;
	}

	/**
	 * Returns schema information about the HTTP part.
	 *
	 * @return Schema information about the HTTP part, or <jk>null</jk> if not found.
	 */
	public ResponseBeanMeta getResponseBeanMeta() {
		return meta;
	}

	/**
	 * Returns the class type of the method return.
	 *
	 * @return The class type of the method return.
	 */
	public Type getReturnType() {
		return returnType;
	}

	/**
	 * Specifies whether the return value is the body of the request or the HTTP status.
	 *
	 * @return The type of value returned.
	 */
	public RemoteReturn getReturnValue() {
		return returnValue;
	}
}
