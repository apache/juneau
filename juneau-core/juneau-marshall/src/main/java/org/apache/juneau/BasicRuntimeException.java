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
package org.apache.juneau;

import static org.apache.juneau.commons.utils.Utils.*;

import java.text.*;

/**
 * Subclass of runtime exceptions that take in a message and zero or more arguments.
 *
 *
 * @serial exclude
 */
public class BasicRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings({
		"java:S1104" // Field may be set via setUnmodifiable() method
	})
	boolean unmodifiable;
	@SuppressWarnings({
		"java:S1104" // Field reassigned via setMessage() method, cannot be final
	})
	String message;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicRuntimeException(String message, Object...args) {
		super(f(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 */
	public BasicRuntimeException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicRuntimeException(Throwable cause, String message, Object...args) {
		super(f(message, args), cause);
	}

	@Override /* Overridden from Throwable */
	public String getMessage() {
		return nn(message) ? message : super.getMessage();
	}

	/**
	 * Sets the detail message on this exception.
	 *
	 * @param message The message.
	 * @param args The message args.
	 * @return This object.
	 */
	public BasicRuntimeException setMessage(String message, Object...args) {
		this.message = f(message, args);
		return this;
	}
}