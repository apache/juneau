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

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * An exception that typically occurs when trying to perform an invalid operation on a configuration property.
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S110" // Deep inheritance inherent to the exception hierarchy
})
public class ConfigException extends BasicRuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 *
	 * @param message The error message.
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments.
	 */
	public ConfigException(String message, Object...args) {
		this(null, message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.  Can be <jk>null</jk> (no cause is set).
	 * @param message The {@link String#format(String, Object...) String.format}-style message (<c>%s</c> placeholders).
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments.
	 */
	public ConfigException(Throwable cause, String message, Object...args) {
		super(cause, message, args);
	}

	@Override
	public String getMessage() {
		Throwable t = getCause();
		if (t == null)
			return super.getMessage();
		var sb = new StringBuilder(super.getMessage());
		while (nn(t)) {
			sb.append("  ").append(t.getMessage());
			t = t.getCause();
		}
		return sb.toString();
	}

	@Override /* Overridden from BasicRuntimeException */
	public ConfigException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}