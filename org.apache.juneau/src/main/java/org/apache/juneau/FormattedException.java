/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau;

import java.text.*;

/**
 * Subclass of non-runtime exceptions that take in a message and zero or more arguments.
 * <p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class FormattedException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args The arguments in the message.
	 */
	public FormattedException(String message, Object...args) {
		super(args.length == 0 ? message : MessageFormat.format(message, args));
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 * @param message The {@link MessageFormat}-style message.
	 * @param args The arguments in the message.
	 */
	public FormattedException(Throwable causedBy, String message, Object...args) {
		this(message, args);
		initCause(causedBy);
	}

	/**
	 * Constructor.
	 *
	 * @param causedBy The cause of this exception.
	 */
	public FormattedException(Throwable causedBy) {
		this(causedBy, causedBy.getLocalizedMessage());
	}
}
