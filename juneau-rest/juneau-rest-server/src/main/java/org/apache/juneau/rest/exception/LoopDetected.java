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
package org.apache.juneau.rest.exception;

import static org.apache.juneau.rest.exception.LoopDetected.*;

import java.text.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;

/**
 * Exception representing an HTTP 508 (Loop Detected).
 *
 * <p>
 * The server detected an infinite loop while processing the request (sent in lieu of 208 Already Reported).
 *
 * @deprecated Use {@link org.apache.juneau.http.exception.LoopDetected}
 */
@Response(code=CODE, description=MESSAGE)
@Deprecated
public class LoopDetected extends RestException {
	private static final long serialVersionUID = 1L;

	/** HTTP status code */
	public static final int CODE = 508;

	/** Default message */
	public static final String MESSAGE = "Loop Detected";

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public LoopDetected(Throwable cause, String msg, Object...args) {
		super(cause, CODE, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 */
	public LoopDetected(String msg) {
		super(msg);
		setStatus(CODE);
	}

	/**
	 * Constructor.
	 */
	public LoopDetected() {
		this((Throwable)null, MESSAGE);
	}

	/**
	 * Constructor.
	 *
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public LoopDetected(String msg, Object...args) {
		this(null, msg, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause.  Can be <jk>null</jk>.
	 */
	public LoopDetected(Throwable cause) {
		this(cause, null);
	}
}