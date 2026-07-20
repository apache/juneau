/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.svl;

import org.apache.juneau.commons.*;

/**
 * Exception that occurs during a var resolver session.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallSimpleVariableLanguage">Simple Variable Language Basics</a>
 * </ul>
 *
 * @serial exclude
 */
public class VarResolverException extends BasicRuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link String#format(String, Object...) String.format}-style message (<c>%s</c> placeholders).
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments.
	 */
	public VarResolverException(String message, Object...args) {
		this(null, message, args);
	}

	/**
	 * Constructor.
	 *
	 * @param cause The cause of this exception.
	 * @param message The {@link String#format(String, Object...) String.format}-style message (<c>%s</c> placeholders).
	 * @param args Optional {@link String#format(String, Object...) String.format}-style arguments.
	 */
	public VarResolverException(Throwable cause, String message, Object...args) {
		super(cause, message, args);
	}

	@Override /* Overridden from BasicRuntimeException */
	public VarResolverException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}
