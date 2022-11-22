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
import org.apache.juneau.internal.*;

/**
 * Subclass of runtime exceptions that take in a message and zero or more arguments.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
@FluentSetters
public class BasicRuntimeException extends RuntimeException {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	boolean unmodifiable;
	String message;

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicRuntimeException(Throwable cause, String message, Object...args) {
		super(format(message, args), cause);
	}

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicRuntimeException(String message, Object...args) {
		super(format(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 */
	public BasicRuntimeException(Throwable cause) {
		super(cause);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies whether this bean should be unmodifiable.
	 * <p>
	 * When enabled, attempting to set any properties on this bean will cause an {@link UnsupportedOperationException}.
	 *
	 * @return This object.
	 */
	@FluentSetter
	protected BasicRuntimeException setUnmodifiable() {
		unmodifiable = true;
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this bean is unmodifiable.
	 *
	 * @return <jk>true</jk> if this bean is unmodifiable.
	 */
	public boolean isUnmodifiable() {
		return unmodifiable;
	}

	/**
	 * Throws an {@link UnsupportedOperationException} if the unmodifiable flag is set on this bean.
	 */
	protected final void assertModifiable() {
		if (unmodifiable)
			throw new UnsupportedOperationException("Bean is read-only");
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
	 * Sets the detail message on this exception.
	 *
	 * @param message The message.
	 * @param args The message args.
	 * @return This object.
	 */
	@FluentSetter
	public BasicRuntimeException setMessage(String message, Object...args) {
		assertModifiable();
		this.message = format(message, args);
		return this;
	}

	@Override /* Throwable */
	public String getMessage() {
		if (message != null)
			return message;
		String m = super.getMessage();
		if (m == null && getCause() != null)
			m = getCause().getMessage();
		return m;
	}

	@Override /* Throwable */
	public synchronized Throwable fillInStackTrace() {
		assertModifiable();
		return super.fillInStackTrace();
	}

	@Override /* Throwable */
	public synchronized Throwable initCause(Throwable cause) {
		assertModifiable();
		return super.initCause(cause);
	}

	@Override /* Throwable */
	public void setStackTrace(StackTraceElement[] stackTrace) {
		assertModifiable();
		super.setStackTrace(stackTrace);
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

	// <FluentSetters>

	// </FluentSetters>
}
