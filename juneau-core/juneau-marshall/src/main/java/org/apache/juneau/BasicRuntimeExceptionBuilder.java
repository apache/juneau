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
package org.apache.juneau;

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.internal.*;

/**
 * Builder for {@link BasicRuntimeException} beans.
 */
@FluentSetters
public class BasicRuntimeExceptionBuilder {

	String message;
	Throwable causedBy;
	boolean unmodifiable;

	/**
	 * Copies the values from the specified exception.
	 *
	 * @param value The exception to copy from.
	 * @return This object (for method chaining).
	 */
	public BasicRuntimeExceptionBuilder copyFrom(BasicRuntimeException value) {
		message = value.getMessage();
		causedBy = value.getCause();
		unmodifiable = value.unmodifiable;
		return this;
	}

	/**
	 * Specifies the exception message.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicRuntimeExceptionBuilder message(String msg, Object...args) {
		message = format(msg, args);
		return this;
	}

	/**
	 * Specifies the caused-by exception.
	 *
	 * @param value The caused-by exception.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicRuntimeExceptionBuilder causedBy(Throwable value) {
		causedBy = value;
		return this;
	}

	/**
	 * Specifies whether this exception should be unmodifiable after creation.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicRuntimeExceptionBuilder unmodifiable() {
		unmodifiable = true;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
