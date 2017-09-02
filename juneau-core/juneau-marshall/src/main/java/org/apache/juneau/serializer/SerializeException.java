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
package org.apache.juneau.serializer;

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;

/**
 * General exception thrown whenever an error occurs during serialization.
 */
public final class SerializeException extends FormattedException {

	private static final long serialVersionUID = 1L;

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
	 * @param session The serializer session to extract information from.
	 * @param causedBy The inner exception.
	 */
	public SerializeException(SerializerSession session, Exception causedBy) {
		super(causedBy, getMessage(session, causedBy.getMessage()));
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The inner exception.
	 */
	public SerializeException(Exception causedBy) {
		super(causedBy, getMessage(null, causedBy.getMessage()));
	}

	private static String getMessage(SerializerSession session, String msg, Object... args) {
		msg = format(msg, args);
		if (session != null) {
			Map<String,Object> m = session.getLastLocation();
			if (m != null && ! m.isEmpty())
				msg = "Serialize exception occurred at " + JsonSerializer.DEFAULT_LAX.toString(m) + ".  " + msg;
		}
		return msg;
	}

	/**
	 * Returns the highest-level <code>ParseException</code> in the stack trace.
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
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized SerializeException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}