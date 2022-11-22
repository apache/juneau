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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.text.*;

/**
 * An extension of {@link AssertionError} with helper constructors for messages with message-style arguments.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public class BasicAssertionError extends AssertionError {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicAssertionError(String message, Object...args) {
		super(format(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicAssertionError(Throwable cause, String message, Object...args) {
		this(getMessage(cause, message, null), args);
		initCause(cause);
	}

	/**
	 * Finds the message.
	 *
	 * @param cause The cause.
	 * @param msg The message.
	 * @param def The default value if both above are <jk>null</jk>.
	 * @return The resolved message.
	 */
	protected static final String getMessage(Throwable cause, String msg, String def) {
		if (msg != null)
			return msg;
		if (cause != null)
			return cause.getMessage();
		return def;
	}
}
