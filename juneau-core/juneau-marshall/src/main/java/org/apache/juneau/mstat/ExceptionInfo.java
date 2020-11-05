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
package org.apache.juneau.mstat;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Stores information about an exception.
 */
@Bean(properties="hash,exceptionClass,message,stackTrace,causedBy", fluentSetters=true)
public class ExceptionInfo implements Cloneable {

	private String hash;
	private String exceptionClass;
	private String message;
	private List<StackTraceElement> stackTrace;
	private ExceptionInfo causedBy;

	/**
	 * TODO
	 *
	 * @return TODO
	 */
	public static ExceptionInfo create() {
		return new ExceptionInfo();
	}

	/**
	 * Returns an 8-byte hash of the stack trace.
	 *
	 * @return An 8-byte hash of the stack trace.
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * TODO
	 *
	 * @param value TODO
	 * @return This object (for method chaining).
	 */
	public ExceptionInfo hash(String value) {
		this.hash = value;
		return this;
	}

	/**
	 * Returns the simple class name of the exception.
	 *
	 * @return The simple class name of the exception, or <jk>null</jk> if not specified.
	 */
	public String getExceptionClass() {
		return exceptionClass;
	}

	/**
	 * TODO
	 *
	 * @param value TODO
	 * @return This object (for method chaining).
	 */
	public ExceptionInfo exceptionClass(String value) {
		this.exceptionClass = value;
		return this;
	}

	/**
	 * TODO
	 *
	 * @return TODO
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * TODO
	 *
	 * @param value TODO
	 * @return This object (for method chaining).
	 */
	public ExceptionInfo message(String value) {
		this.message = value;
		return this;
	}


	/**
	 * TODO
	 *
	 * @return TODO
	 */
	public List<StackTraceElement> getStackTrace() {
		return stackTrace;
	}

	/**
	 * TODO
	 *
	 * @param value TODO
	 * @return This object (for method chaining).
	 */
	public ExceptionInfo stackTrace(List<StackTraceElement> value) {
		this.stackTrace = value;
		return this;
	}


	/**
	 * TODO
	 *
	 * @return TODO
	 */
	public ExceptionInfo getCausedBy() {
		return causedBy;
	}

	/**
	 * TODO
	 *
	 * @param value TODO
	 * @return This object (for method chaining).
	 */
	public ExceptionInfo causedBy(ExceptionInfo value) {
		this.causedBy = value;
		return this;
	}
}
