/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.serializer;

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;

/**
 * General exception thrown whenever an error occurs during serialization.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>

 * </ul>
 *
 * @serial exclude
 */
public class SerializeException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creator method.
	 *
	 * <p>
	 * If the throwable is already a {@link SerializeException}, we simply return that exception as-is.
	 * If the throwable is an {@link InvocationTargetException}, we unwrap the thrown exception.
	 * Otherwise we create a new {@link SerializeException}.
	 *
	 * @param e The exception being wrapped or unwrapped.
	 * @return A new {@link SerializeException}.
	 */
	public static SerializeException create(Throwable e) {
		if (e instanceof InvocationTargetException e2)
			e = e2.getCause();
		if (e instanceof SerializeException e2)
			return e2;
		return new SerializeException(e);
	}

	private static String getMessage(SerializerSession session, String msg, Object...args) {
		msg = f(msg, args);
		if (nn(session)) {
			Map<String,Object> m = session.getLastLocation();
			if (nn(m) && ! m.isEmpty())
				msg = "Serialize exception occurred at " + Json5Serializer.DEFAULT.toString(m) + ".  " + msg;
		}
		return msg;
	}

	/**
	 * Constructor.
	 *
	 * @param session The serializer session to extract information from.
	 * @param causedBy The inner exception.
	 */
	public SerializeException(SerializerSession session, Exception causedBy) {
		super(causedBy, getMessage(session, causedBy.getMessage()));
	}

	/**
	 * Constructor.
	 *
	 * @param session The serializer session to extract information from.
	 * @param message The exception message containing {@link MessageFormat}-style arguments.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public SerializeException(SerializerSession session, String message, Object...args) {
		super(getMessage(session, message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param message The exception message containing {@link MessageFormat}-style arguments.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public SerializeException(String message, Object...args) {
		super(getMessage(null, message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The inner exception.
	 */
	public SerializeException(Throwable causedBy) {
		super(causedBy, getMessage(null, causedBy.getMessage()));
	}

	/**
	 * Returns the highest-level <c>ParseException</c> in the stack trace.
	 * Useful for JUnit testing of error conditions.
	 *
	 * @return The root parse exception, or this exception if there isn't one.
	 */
	public SerializeException getRootCause() {
		SerializeException t = this;
		while (! (t.getCause() == null || ! (t.getCause() instanceof SerializeException)))
			t = (SerializeException)t.getCause();
		return t;
	}

	/**
	 * Sets the inner cause for this exception.
	 *
	 * @param cause The inner cause.
	 * @return This object.
	 */
	@Override /* Overridden from Throwable */
	public synchronized SerializeException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}

	@Override /* Overridden from BasicRuntimeException */
	public SerializeException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}