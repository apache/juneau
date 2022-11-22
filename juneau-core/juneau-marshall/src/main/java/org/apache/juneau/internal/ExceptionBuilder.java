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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

/**
 * Builder class for {@link Exception} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The exception class.
 */
@FluentSetters
public class ExceptionBuilder<T extends Throwable> {

	private final Class<T> type;
	private String message;
	private Throwable causedBy;

	/**
	 * Default constructor.
	 * @param type The exception type to create.
	 */
	public ExceptionBuilder(Class<T> type) {
		this.type = type;
	}

	/**
	 * Specifies the exception message.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return This object.
	 */
	@FluentSetter
	public ExceptionBuilder<T> message(String msg, Object...args) {
		message = format(msg, args);
		return this;
	}

	/**
	 * Specifies the caused-by exception.
	 *
	 * @param value The caused-by exception.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@FluentSetter
	public ExceptionBuilder<T> causedBy(Throwable value) {
		causedBy = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	/**
	 * Creates the exception.
	 *
	 * @return The exception.
	 */
	public T build() {
		try {
			return type.getConstructor(String.class, Throwable.class).newInstance(message, causedBy);
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}
}
