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
package org.apache.juneau.rest.server.validation;

import org.apache.juneau.marshall.*;

/**
 * Wire-friendly snapshot of a single Jakarta Bean Validation constraint violation, suitable for inclusion in an
 * HTTP error response body.
 *
 * <p>
 * Produced by {@link BeanValidator} from a {@code jakarta.validation.ConstraintViolation}, with deliberately
 * narrow surface so the bean is decoupled from the optional {@code jakarta.validation} runtime dependency. The
 * {@link #invalidValue} field is omitted by default to avoid echoing potentially sensitive request data back to
 * the client; callers can opt back in via {@link #setInvalidValue(String)}.
 *
 * <h5 class='section'>JSON shape:</h5>
 * <p class='bjson'>
 * 	{
 * 		<jok>"path"</jok>: <jov>"name"</jov>,
 * 		<jok>"message"</jok>: <jov>"must not be blank"</jov>,
 * 		<jok>"constraint"</jok>: <jov>"NotBlank"</jov>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ValidationException}
 * 	<li class='jc'>{@link BeanValidator}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerValidation">REST Server &mdash; Jakarta Validation</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Marshalled
public class ValidationViolation {

	private String path;
	private String message;
	private String constraint;
	private String invalidValue;

	/**
	 * Default constructor.
	 */
	public ValidationViolation() {}

	/**
	 * Convenience constructor populating the three non-sensitive fields.
	 *
	 * @param path The property path string (e.g. {@code "name"} or {@code "items[2].sku"}). May be <jk>null</jk>.
	 * @param message The localized violation message produced by the constraint validator. May be <jk>null</jk>.
	 * @param constraint The simple name of the constraint annotation (e.g. {@code "NotBlank"}). May be <jk>null</jk>.
	 */
	public ValidationViolation(String path, String message, String constraint) {
		this.path = path;
		this.message = message;
		this.constraint = constraint;
	}

	/**
	 * The Jakarta-Validation property path string &mdash; the dotted/indexed path from the validated root bean
	 * down to the violating field (e.g. {@code "name"} or {@code "items[2].sku"}).
	 *
	 * @return The property path, or <jk>null</jk> if not set.
	 */
	public String getPath() { return path; }

	/**
	 * Sets the property path.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ValidationViolation setPath(String value) {
		path = value;
		return this;
	}

	/**
	 * The localized violation message from the constraint validator (e.g. {@code "must not be blank"}).
	 *
	 * @return The message, or <jk>null</jk> if not set.
	 */
	public String getMessage() { return message; }

	/**
	 * Sets the message.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ValidationViolation setMessage(String value) {
		message = value;
		return this;
	}

	/**
	 * The simple name of the constraint annotation that was violated (e.g. {@code "NotBlank"}, {@code "Size"}).
	 *
	 * @return The constraint name, or <jk>null</jk> if not set.
	 */
	public String getConstraint() { return constraint; }

	/**
	 * Sets the constraint.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ValidationViolation setConstraint(String value) {
		constraint = value;
		return this;
	}

	/**
	 * The (string-rendered) invalid value that triggered the violation.
	 *
	 * <p>
	 * Omitted by default to avoid echoing potentially sensitive request data back to the client; populated only
	 * when the caller explicitly opts in via {@link #setInvalidValue(String)} (typically from a
	 * {@code BeanValidator} configured with {@code includeInvalidValue(true)}).
	 *
	 * @return The invalid value, or <jk>null</jk> if not set.
	 */
	public String getInvalidValue() { return invalidValue; }

	/**
	 * Sets the invalid value.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ValidationViolation setInvalidValue(String value) {
		invalidValue = value;
		return this;
	}
}
