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
package org.apache.juneau.objecttools;

import java.net.*;
import java.text.*;

import org.apache.juneau.*;

/**
 * Generic exception thrown from the {@link ObjectRest} class.
 *
 * <p>
 * Typically, this is a user-error, such as trying to address a non-existent node in the tree.
 *
 * <p>
 * The status code is an HTTP-equivalent code.  It will be one of the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link HttpURLConnection#HTTP_BAD_REQUEST HTTP_BAD_REQUEST}
 * 		- Attempting to do something impossible.
 * 	<li>
 * 		{@link HttpURLConnection#HTTP_NOT_FOUND HTTP_NOT_FOUND}
 * 		- Attempting to access a non-existent node in the tree.
 * 	<li>
 * 		{@link HttpURLConnection#HTTP_FORBIDDEN HTTP_FORBIDDEN}
 * 		- Attempting to overwrite the root object.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public final class ObjectRestException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	private int status;

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param status HTTP status code.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ObjectRestException(Throwable cause, int status, String message, Object... args) {
		super(cause, message, args);
		this.status = status;
	}

	/**
	 * Constructor.
	 *
	 * @param status The HTTP-equivalent status code.
	 * @param message The detailed message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public ObjectRestException(int status, String message, Object...args) {
		this(null, status, message, args);
	}

	/**
	 * The HTTP-equivalent status code.
	 *
	 * <p>
	 * See above for details.
	 *
	 * @return The HTTP-equivalent status code.
	 */
	public int getStatus() {
		return status;
	}
}
