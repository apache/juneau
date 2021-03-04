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

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.reflect.*;

/**
 * A {@link RuntimeException} meant to wrap a non-{@link RuntimeException}.
 */
public final class HttpRuntimeException extends BasicRuntimeException {
	private static final long serialVersionUID = 1L;

	final Throwable t;

	/**
	 * Constructor.
	 *
	 * @param t Wrapped exception.
	 */
	public HttpRuntimeException(Throwable t) {
		super(t, t == null ? "" : t.getMessage());
		this.t = t;
	}

	/**
	 * Returns the wrapped throwable.
	 *
	 * @return The wrapped throwable.
	 */
	public Throwable getInner() {
		return t;
	}

	/**
	 * Takes in an arbitrary {@link Throwable} and converts it to an appropriate runtime exception for producing an
	 * HTTP response.
	 *
	 * @param t The throwable to wrap.
	 * @param ec The exception class to create if the specified throwable cannot produce a valid HTTP response.
	 * @return RuntimeException The new exception to throw.
	 */
	public static RuntimeException toHttpException(Throwable t, Class<?> ec) {
		return toHttpException(t, ec, null);
	}

	/**
	 * Takes in an arbitrary {@link Throwable} and converts it to an appropriate runtime exception for producing an
	 * HTTP response.
	 *
	 * @param t The throwable to wrap.
	 * @param ec The exception class to create if the specified throwable cannot produce a valid HTTP response.
	 * @param msg The message text to pass to the ec class constructor.
	 * @param args The message arguments to pass to the ec class constructor.
	 * @return RuntimeException The new exception to throw.
	 */
	public static RuntimeException toHttpException(Throwable t, Class<?> ec, String msg, Object...args) {
		ClassInfo ci = ClassInfo.ofc(t);

		// If it's any RuntimeException annotated with @Response, it can be rethrown.
		if (ci.isRuntimeException()) {
			if (ci.hasAnnotation(Response.class))
				return (RuntimeException)t;
			if (ci.isChildOf(BasicHttpException.class))
				return (RuntimeException)t;
		}

		// If it's a non-RuntimeException but annotated with @Response, it can be wrapped and rethrown.
		if (ci.hasAnnotation(Response.class))
			return new HttpRuntimeException(t);

		if (ci.is(InvocationTargetException.class))
			return new HttpRuntimeException(((InvocationTargetException)t).getCause());

		if (ec == null)
			ec = InternalServerError.class;
		ClassInfo eci = ClassInfo.ofc(ec);

		try {
			ConstructorInfo cci = eci.getPublicConstructor(Throwable.class, String.class, Object[].class);
			if (cci != null)
	 			return toHttpException((Throwable)cci.invoke(t, msg, args), InternalServerError.class);

			cci = eci.getPublicConstructor(Throwable.class);
			if (cci != null)
				return toHttpException((Throwable)cci.invoke(t), InternalServerError.class);

			System.err.println("WARNING:  Class '"+ec+"' does not have a public constructor that takes in valid arguments.");
			return new InternalServerError(t);
		} catch (ExecutableException e) {
			throw new InternalServerError(e.getCause());
		}
	}
}
