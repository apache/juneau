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
package org.apache.juneau.marshall;

import java.text.*;

/**
 * Subclass of runtime exceptions that take in a message and zero or more arguments.
 *
 * <p>
 * This class extends {@link org.apache.juneau.commons.BasicRuntimeException} in juneau-commons
 * for backward compatibility.
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S2176" // Intentional same-named backward-compat re-export of org.apache.juneau.commons.BasicRuntimeException; renaming this public class would break the API.
})
public class BasicRuntimeException extends org.apache.juneau.commons.BasicRuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BasicRuntimeException(String message, Object...args) {
		super(message, args);
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
		super(cause, message, args);
	}

	@Override /* Overridden from BasicRuntimeException */
	public BasicRuntimeException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}
