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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;

import javax.servlet.*;

import org.apache.juneau.internal.*;

/**
 * General exception thrown from {@link RestServlet} during construction or initialization.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc TODO}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class RestServletException extends ServletException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The detailed message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RestServletException(String message, Object...args) {
		super(format(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.
	 * @param message The detailed message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public RestServletException(Throwable cause, String message, Object...args) {
		super(format(message, args), cause);
	}

	/**
	 * Similar to {@link #getCause()} but searches until it finds the throwable of the specified type.
	 *
	 * @param <T> The throwable type.
	 * @param c The throwable type.
	 * @return The cause of the specified type, or <jk>null</jk> of not found.
	 */
	public <T extends Throwable> T getCause(Class<T> c) {
		return ThrowableUtils.getCause(c, this);
	}
}
