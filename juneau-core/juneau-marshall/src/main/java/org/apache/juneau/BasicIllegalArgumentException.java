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
package org.apache.juneau;

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Subclass of illegal-argument exceptions that take in a message and zero or more arguments.
 */
public class BasicIllegalArgumentException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicIllegalArgumentException(String message, Object...args) {
		super(format(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicIllegalArgumentException(Throwable causedBy, String message, Object...args) {
		this(message, args);
		initCause(causedBy);
	}

	/**
	 * Throws a {@link BasicIllegalArgumentException} if the specified method does not only contain args of the specified types.
	 *
	 * @param m The method to check.
	 * @param args The allowed args.
	 */
	public static void assertArgsOnlyOfType(MethodInfo m, Class<?>...args) {
		if (! m.argsOnlyOfType(args))
			throw new BasicIllegalArgumentException("Invalid arguments passed to method {0}.  Only arguments of type {1} are allowed.", m, args);
	}

	/**
	 * Same as {@link #getCause()} but searches the throwable chain for an exception of the specified type.
	 *
	 * @param c The throwable type to search for.
	 * @param <T> The throwable type to search for.
	 * @return The exception, or <jk>null</jk> if not found.
	 */
	public <T extends Throwable> T getCause(Class<T> c) {
		return ThrowableUtils.getCause(c, this);
	}
}
