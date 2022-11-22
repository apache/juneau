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

import static org.apache.juneau.http.remote.RemoteUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.http.remote.RemoteReturn;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.reflect.*;

/**
 * Represents the metadata about the returned object of a method on a remote proxy interface.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#jrc.Proxies">REST Proxies</a>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public final class RemoteOperationReturn {

	private final Type returnType;
	private final RemoteReturn returnValue;
	private final ResponseBeanMeta meta;
	private boolean isFuture, isCompletableFuture;

	RemoteOperationReturn(MethodInfo m) {
		ClassInfo rt = m.getReturnType();

		AnnotationList al = m.getAnnotationList(REMOTE_OP_GROUP);
		if (al.isEmpty())
			al = m.getReturnType().unwrap(Value.class,Optional.class).getAnnotationList(REMOTE_OP_GROUP);

		RemoteReturn rv = null;

		if (rt.is(Future.class)) {
			isFuture = true;
			rt = ClassInfo.of(((ParameterizedType)rt.innerType()).getActualTypeArguments()[0]);
		} else if (rt.is(CompletableFuture.class)) {
			isCompletableFuture = true;
			rt = ClassInfo.of(((ParameterizedType)rt.innerType()).getActualTypeArguments()[0]);
		}

		if (rt.is(void.class) || rt.is(Void.class)) {
			rv = RemoteReturn.NONE;
		} else {
			Value<RemoteReturn> v = Value.of(RemoteReturn.BODY);
			al.forEachValue(RemoteReturn.class, "returns", x -> true, x -> v.set(x));
			rv = v.get();
		}

		if (rt.hasAnnotation(Response.class) && rt.isInterface()) {
			this.meta = ResponseBeanMeta.create(m, AnnotationWorkList.create());
			rv = RemoteReturn.BEAN;
		} else {
			this.meta = null;
		}

		this.returnType = rt.innerType();
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
	 * Returns <jk>true</jk> if the return is wrapped in a {@link Future}.
	 *
	 * @return <jk>true</jk> if the return is wrapped in a {@link Future}.
	 */
	public boolean isFuture() {
		return isFuture;
	}

	/**
	 * Returns <jk>true</jk> if the return is wrapped in a {@link CompletableFuture}.
	 *
	 * @return <jk>true</jk> if the return is wrapped in a {@link CompletableFuture}.
	 */
	public boolean isCompletableFuture() {
		return isCompletableFuture;
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
