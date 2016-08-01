/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau;


/**
 * General bean runtime operation exception.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class BeanRuntimeException extends FormattedRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 */
	public BeanRuntimeException(String message) {
		super(message);
	}

	/**
	 * Shortcut for calling <code><jk>new</jk> BeanRuntimeException(String.format(c.getName() + <js>": "</js> + message, args));</code>
	 *
	 * @param c The class name of the bean that caused the exception.
	 * @param message The error message.
	 * @param args Arguments passed in to the {@code String.format()} method.
	 */
	public BeanRuntimeException(Class<?> c, String message, Object... args) {
		super(c.getName() + ": " + message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The initial cause of the exception.
	 */
	public BeanRuntimeException(Throwable cause) {
		super(cause == null ? null : cause.getLocalizedMessage());
		initCause(cause);
	}

	/**
	 * Sets the inner cause for this exception.
	 *
	 * @param cause The inner cause.
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized BeanRuntimeException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}
