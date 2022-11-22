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

import java.lang.reflect.*;
import java.text.MessageFormat;

/**
 * General exception that occurs when trying to execute a constructor, method, or field using reflection.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public class ExecutableException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ExecutableException(Throwable causedBy, String message, Object...args) {
		super(causedBy, message, args);
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
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ExecutableException(String message, Object...args) {
		super(message, args);
	}

	/**
	 * If the thrown exception was an {@link InvocationTargetException} returns the target exception.
	 * Otherwise returns the inner exception which is typically {@link IllegalArgumentException} or {@link IllegalAccessException}.
	 *
	 * @return The inner throwable.
	 */
	public Throwable getTargetException() {
		Throwable c = this.getCause();
		return c instanceof InvocationTargetException ? ((InvocationTargetException)c).getTargetException() : c;
	}
}
