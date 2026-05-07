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
package org.apache.juneau.commons.inject;

/**
 * Unchecked exception thrown when a bean creator method is found but its invocation fails.
 *
 * <p>
 * Thrown by {@link BeanStore#createBeanFromMethod(Class, Object, java.util.function.Predicate, Object[])} when a matching
 * factory method is located but throws an exception during invocation.  Wrapping in an unchecked type makes
 * the exception compatible with {@link java.util.Optional} chaining and lambda contexts.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link BeanStore#createBeanFromMethod(Class, Object, java.util.function.Predicate, Object[])}
 * </ul>
 */
public class BeanCreationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The detail message.
	 */
	public BeanCreationException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 *
	 * @param message The detail message.
	 * @param cause The cause.
	 */
	public BeanCreationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.
	 */
	public BeanCreationException(Throwable cause) {
		super(cause);
	}
}
