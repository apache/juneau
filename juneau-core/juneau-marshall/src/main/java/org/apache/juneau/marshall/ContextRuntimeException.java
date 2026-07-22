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

/**
 * General runtime operation exception that can occur in any of the context classes.
 *
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S110" // Deep inheritance inherent to the exception hierarchy
})
public class ContextRuntimeException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 * @param args Arguments passed in to the {@code String.format()} method.
	 */
	public ContextRuntimeException(String message, Object...args) {
		super(message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The initial cause of the exception.  Can be <jk>null</jk> (no cause is set).
	 */
	public ContextRuntimeException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.  Can be <jk>null</jk> (no cause is set).
	 * @param message The {@link String#format(String, Object...) String.format}-style message (<c>%s</c> placeholders).
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments.
	 */
	public ContextRuntimeException(Throwable cause, String message, Object...args) {
		super(cause, message, args);
	}

	@Override /* Overridden from BasicRuntimeException */
	public ContextRuntimeException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}