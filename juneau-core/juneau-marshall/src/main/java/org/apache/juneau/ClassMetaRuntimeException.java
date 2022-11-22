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

import java.text.*;

/**
 * General class metadata runtime operation exception.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public final class ClassMetaRuntimeException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param c The class name of the bean that caused the exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ClassMetaRuntimeException(Throwable cause, Class<?> c, String message, Object... args) {
		super(cause, getMessage(cause, c, message), args);
	}

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 */
	public ClassMetaRuntimeException(String message) {
		this((Throwable)null, null, message);
	}

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 * @param args Arguments passed in to the {@code String.format()} method.
	 */
	public ClassMetaRuntimeException(String message, Object...args) {
		this(null, null, message, args);
	}

	/**
	 * Shortcut for calling <code><jk>new</jk> ClassMetaRuntimeException(String.format(c.getName() + <js>": "</js> + message, args));</code>
	 *
	 * @param c The class name of the bean that caused the exception.
	 * @param message The error message.
	 * @param args Arguments passed in to the {@code String.format()} method.
	 */
	public ClassMetaRuntimeException(Class<?> c, String message, Object... args) {
		this(null, c, message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The initial cause of the exception.
	 */
	public ClassMetaRuntimeException(Throwable cause) {
		this(cause, null, null);
	}

	private static String getMessage(Throwable cause, Class<?> c, String msg) {
		if (msg != null)
			return (c == null ? "" : c.getName() + ": ") + msg;
		if (cause != null)
			return (c == null ? "" : c.getName() + ": ") + cause.getMessage();
		return null;
	}
}
