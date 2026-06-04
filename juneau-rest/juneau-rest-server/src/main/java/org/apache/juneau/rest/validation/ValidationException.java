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
package org.apache.juneau.rest.validation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.http.response.*;

/**
 * Specialization of {@link BadRequest} thrown by {@link BeanValidator} when one or more Jakarta Bean Validation
 * constraints on a {@code @Content}, {@code @FormData}, or {@code @Request}-bound parameter are violated.
 *
 * <p>
 * Behaves like a plain {@code 400 Bad Request} from the wire's perspective &mdash; same status code, same reason
 * phrase, same default JSON envelope shape used by the rest of the framework's exception path &mdash; but adds a
 * structured, serialized {@link #getViolations() violations list} so the response body can include the list of
 * field-level errors that produced the failure.
 *
 * <h5 class='section'>Wire shape:</h5>
 * <p>
 * The response body depends on whether RFC 7807 problem-details mode is enabled on the resource:
 * <ul class='spaced-list'>
 * 	<li><b>Problem-details ON</b> &mdash; {@code application/problem+json} with the standard {@code status} /
 * 		{@code title} / {@code detail} members plus an {@code errors[]} extension array populated from
 * 		{@link #getViolations()}.
 * 	<li><b>Problem-details OFF (default)</b> &mdash; {@code application/json} with a simple
 * 		{@code { "status":400, "errors":[ ... ] }} envelope.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ValidationViolation}
 * 	<li class='jc'>{@link BeanValidator}
 * 	<li class='jc'>{@link BadRequest}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerValidation">REST Server &mdash; Jakarta Validation</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S110" // Deep inheritance inherent to the exception hierarchy
})
public class ValidationException extends BadRequest {

	private static final long serialVersionUID = 1L;

	/** The default detail message used when constructed without an explicit message. */
	public static final String DEFAULT_MESSAGE = "Request validation failed";

	private final transient List<ValidationViolation> violations;

	/**
	 * Constructor.
	 *
	 * @param violations The list of constraint violations. Must not be <jk>null</jk> (use an empty list to signal
	 * 	&quot;validation requested but no violations&quot; &mdash; though that case never actually throws in practice).
	 */
	public ValidationException(List<ValidationViolation> violations) {
		super(DEFAULT_MESSAGE);
		this.violations = List.copyOf(violations);
	}

	/**
	 * Constructor with a custom detail message.
	 *
	 * @param violations The list of constraint violations. Must not be <jk>null</jk>.
	 * @param msg The detail message. May be <jk>null</jk>.
	 *    Treated as a format pattern when {@code args} is non-empty.
	 * @param args Optional message arguments.
	 */
	public ValidationException(List<ValidationViolation> violations, String msg, Object...args) {
		super(msg, args);
		this.violations = List.copyOf(violations);
	}

	/**
	 * Returns the immutable list of constraint violations that produced this exception.
	 *
	 * @return The list of violations, never <jk>null</jk> and never modifiable.
	 */
	public List<ValidationViolation> getViolations() {
		return violations;
	}

	/**
	 * Returns a defensive, mutable copy of the violation list for callers that need to inspect / filter it
	 * without mutating the exception's own state.
	 *
	 * @return A new mutable copy. Never <jk>null</jk>.
	 */
	public List<ValidationViolation> copyViolations() {
		return copyOf(violations);
	}
}
