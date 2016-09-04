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
package org.apache.juneau.server;

import java.text.*;

import javax.servlet.*;

/**
 * General exception thrown from {@link RestServlet} during construction or initialization.
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class RestServletException extends ServletException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The detailed message.
	 * @param args Optional message arguments.
	 */
	public RestServletException(String message, Object...args) {
		super(args.length == 0 ? message : MessageFormat.format(message, args));
	}

	/**
	 * Sets the inner cause for this exception.
	 *
	 * @param cause The inner cause.
	 * @return This object (for method chaining).
	 */
	@Override /* Throwable */
	public synchronized RestServletException initCause(Throwable cause) {
		super.initCause(cause);
		return this;
	}
}
