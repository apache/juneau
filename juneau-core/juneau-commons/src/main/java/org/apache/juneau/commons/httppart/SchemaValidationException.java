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
package org.apache.juneau.commons.httppart;

import java.text.*;

import org.apache.juneau.commons.BasicRuntimeException;

/**
 * Exception thrown when an HTTP part fails schema validation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 *
 * @serial exclude
 */
public class SchemaValidationException extends BasicRuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The {@link MessageFormat}-style message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public SchemaValidationException(String message, Object...args) {
		super(message, args);
	}

	/**
	 * Returns the root cause of this exception.
	 *
	 * @return The root cause of this exception.
	 */
	public SchemaValidationException getRootCause() {
		var t = this;
		while (t.getCause() instanceof SchemaValidationException t2) {
			t = t2;
		}
		return t;
	}

	@Override /* Overridden from BasicRuntimeException */
	public SchemaValidationException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}
