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
package org.apache.juneau.internal;

import java.io.*;

/**
 * Utility class for creating exceptions.
 */
public class ExceptionUtils {

	/**
	 * Creates a new builder for {@link RuntimeException} objects.
	 *
	 * @return A new builder for {@link RuntimeException} objects.
	 */
	public static ExceptionBuilder<RuntimeException> runtimeException() {
		return new ExceptionBuilder<>(RuntimeException.class);
	}

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link RuntimeException}.
	 */
	public static RuntimeException runtimeException(String msg, Object...args) {
		return runtimeException().message(msg, args).build();
	}

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param cause The caused-by exception.
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link RuntimeException}.
	 */
	public static RuntimeException runtimeException(Throwable cause, String msg, Object...args) {
		return runtimeException().message(msg, args).causedBy(cause).build();
	}

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link RuntimeException}, or the same exception if it's already of that type.
	 */
	public static RuntimeException runtimeException(Throwable cause) {
		return cause instanceof RuntimeException ? (RuntimeException)cause : runtimeException().causedBy(cause).build();
	}

	/**
	 * Creates a new builder for {@link IOException} objects.
	 *
	 * @return A new builder for {@link IOException} objects.
	 */
	public static ExceptionBuilder<IOException> ioException() {
		return new ExceptionBuilder<>(IOException.class);
	}

	/**
	 * Creates a new {@link IOException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link IOException}.
	 */
	public static IOException ioException(String msg, Object...args) {
		return ioException().message(msg, args).build();
	}

	/**
	 * Creates a new {@link IOException}.
	 *
	 * @param cause The caused-by exception.
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link IOException}.
	 */
	public static IOException ioException(Throwable cause, String msg, Object...args) {
		return ioException().message(msg, args).causedBy(cause).build();
	}

	/**
	 * Creates a new {@link IOException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link IOException}, or the same exception if it's already of that type.
	 */
	public static IOException ioException(Throwable cause) {
		return cause instanceof IOException ? (IOException)cause : ioException().causedBy(cause).build();
	}

	/**
	 * Creates a new builder for {@link IOException} objects.
	 *
	 * @return A new builder for {@link IOException} objects.
	 */
	public static ExceptionBuilder<UnsupportedOperationException> unsupportedOperationException() {
		return new ExceptionBuilder<>(UnsupportedOperationException.class);
	}

	/**
	 * Creates a new {@link UnsupportedOperationException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link UnsupportedOperationException}.
	 */
	public static UnsupportedOperationException unsupportedOperationException(String msg, Object...args) {
		return unsupportedOperationException().message(msg, args).build();
	}

	/**
	 * Creates a new builder for {@link IllegalArgumentException} objects.
	 *
	 * @return A new builder for {@link IllegalArgumentException} objects.
	 */
	public static ExceptionBuilder<IllegalArgumentException> illegalArgumentException() {
		return new ExceptionBuilder<>(IllegalArgumentException.class);
	}

	/**
	 * Creates a new {@link UnsupportedOperationException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link UnsupportedOperationException}.
	 */
	public static IllegalArgumentException illegalArgumentException(String msg, Object...args) {
		return illegalArgumentException().message(msg, args).build();
	}
}
