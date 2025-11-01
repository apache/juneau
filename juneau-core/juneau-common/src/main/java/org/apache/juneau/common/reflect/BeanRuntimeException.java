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
package org.apache.juneau.common.reflect;

import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.text.*;

/**
 * A {@link RuntimeException} wrapper around common reflection exceptions.
 *
 * <p>
 * This exception is used to wrap checked exceptions that commonly occur when using Java reflection APIs,
 * converting them into unchecked exceptions for easier handling in bean-processing code.
 *
 * <h5 class='section'>Wrapped Exceptions:</h5>
 * <ul>
 * 	<li>{@link InstantiationException}
 * 	<li>{@link IllegalAccessException}
 * 	<li>{@link IllegalArgumentException}
 * 	<li>{@link InvocationTargetException}
 * 	<li>{@link NoSuchMethodException}
 * 	<li>{@link SecurityException}
 * </ul>
 *
 * <p>
 * The exception message can optionally include the class name of the bean that caused the exception,
 * making it easier to identify the source of reflection errors in complex bean hierarchies.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> {
 * 		Constructor&lt;?&gt; <jv>c</jv> = MyBean.<jk>class</jk>.getConstructor();
 * 		<jv>c</jv>.newInstance();
 * 	} <jk>catch</jk> (Exception <jv>e</jv>) {
 * 		<jk>throw new</jk> BeanRuntimeException(<jv>e</jv>, MyBean.<jk>class</jk>, <js>"Failed to instantiate bean"</js>);
 * 	}
 * </p>
 */
public class BeanRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private static String getMessage(Throwable cause, Class<?> c, String msg, Object...args) {
		if (nn(msg))
			return (c == null ? "" : cn(c) + ": ") + f(msg, args);
		if (nn(cause))
			return (c == null ? "" : cn(c) + ": ") + cause.getMessage();
		return null;
	}

	/**
	 * Constructor.
	 *
	 * @param c The class name of the bean that caused the exception.
	 * @param message The error message.
	 * @param args Arguments passed in to the {@code String.format()} method.
	 */
	public BeanRuntimeException(Class<?> c, String message, Object...args) {
		this(null, c, message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 */
	public BeanRuntimeException(String message) {
		this((Throwable)null, null, message);
	}

	/**
	 * Constructor.
	 *
	 * @param message The error message.
	 * @param args Arguments passed in to the {@code String.format()} method.
	 */
	public BeanRuntimeException(String message, Object...args) {
		this(null, null, message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The initial cause of the exception.
	 */
	public BeanRuntimeException(Throwable cause) {
		this(cause, null, null);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param c The class name of the bean that caused the exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public BeanRuntimeException(Throwable cause, Class<?> c, String message, Object...args) {
		super(getMessage(cause, c, message, args), cause);
	}
}