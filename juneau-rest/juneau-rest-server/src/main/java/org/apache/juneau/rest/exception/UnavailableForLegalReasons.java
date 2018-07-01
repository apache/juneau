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

import static org.apache.juneau.rest.exception.UnavailableForLegalReasons.*;

import java.text.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;

/**
 * Exception representing an HTTP 451 (Unavailable For Legal Reasons).
 * 
 * <p>
 * A server operator has received a legal demand to deny access to a resource or to a set of resources that includes the requested resource.
 */
@Response(
	code=CODE,
	description=MESSAGE
)
public class UnavailableForLegalReasons extends RestException {
	private static final long serialVersionUID = 1L;
	
	/** Default message */
	public static final String MESSAGE = "Unavailable For Legal Reasons";
	
	/** HTTP status code */
	public static final int CODE = 451;

	/**
	 * Constructor.
	 * 
	 * @param cause The cause.  Can be <jk>null</jk>. 
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public UnavailableForLegalReasons(Throwable cause, String msg, Object...args) {
		super(cause, CODE, getMessage(cause, msg, MESSAGE), args);
	}
	
	/**
	 * Constructor.
	 */
	public UnavailableForLegalReasons() {
		this((Throwable)null, MESSAGE);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public UnavailableForLegalReasons(String msg, Object...args) {
		this(null, msg, args);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param cause The cause.  Can be <jk>null</jk>. 
	 */
	public UnavailableForLegalReasons(Throwable cause) {
		this(cause, null);
	}
}