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
package org.apache.juneau.commons.reflect;

import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.text.*;

/**
 * General exception that occurs when trying to execute a constructor, method, or field using reflection.
 *
 *
 * @serial exclude
 */
public class ExecutableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ExecutableException(String message, Object...args) {
		super(f(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 */
	public ExecutableException(Throwable causedBy) {
		super(causedBy);
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ExecutableException(Throwable causedBy, String message, Object...args) {
		super(f(message, args), causedBy);
	}

	/**
	 * If the thrown exception was an {@link InvocationTargetException} returns the target exception.
	 * Otherwise returns the inner exception which is typically {@link IllegalArgumentException} or {@link IllegalAccessException}.
	 *
	 * @return The inner throwable.
	 */
	public Throwable getTargetException() {
		Throwable c = this.getCause();
		return c instanceof InvocationTargetException c2 ? c2.getTargetException() : c;
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