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

import org.apache.juneau.common.internal.*;

/**
 * Subclass of non-runtime exceptions that take in a message and zero or more arguments.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public abstract class BasicException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicException(String message, Object...args) {
		super(format(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicException(Throwable causedBy, String message, Object...args) {
		this(message, args);
		initCause(causedBy);
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 */
	public BasicException(Throwable causedBy) {
		this(causedBy, causedBy.getLocalizedMessage());
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

	/**
	 * Returns the caused-by exception if there is one.
	 *
	 * @return The caused-by exception if there is one, or this exception if there isn't.
	 */
	public Throwable unwrap() {
		Throwable t = getCause();
		return t == null ? this : t;
	}
}
