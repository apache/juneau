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
/**
 * Optional <a class="doclink" href="https://jakarta.ee/specifications/bean-validation/">Jakarta Bean Validation 3.x</a>
 * integration for {@code juneau-rest-server} &mdash; declarative validation of request beans via
 * {@code jakarta.validation.constraints.*} annotations.
 *
 * <h5 class='topic'>Off by default &mdash; opt in per parameter</h5>
 * <p>
 * Validation is <b>off by default and never runs automatically</b>. A {@link org.apache.juneau.rest.validation.BeanValidator}
 * is only constructed when an arg resolver sees a {@code @jakarta.validation.Valid} (or Spring
 * {@code @org.springframework.validation.annotation.Validated}) annotation on a {@code @Content}, {@code @FormData},
 * or {@code @Request}-bound parameter. Without that annotation:
 * <ul class='spaced-list'>
 * 	<li>No {@code jakarta.validation.Validator} is created.
 * 	<li>No constraint check is invoked.
 * 	<li>No runtime cost is paid &mdash; the request bean flows through the unmodified Juneau pipeline as if
 * 		Jakarta Validation were not on the classpath at all.
 * </ul>
 * <p>
 * There is no global &quot;turn on validation for the whole resource&quot; switch &mdash; opt-in is per-parameter
 * via {@code @Valid}. This is a deliberate behavior difference from Spring MVC (which auto-validates beans whose
 * type carries constraint annotations) and from the standard Jakarta cascade rules.
 *
 * <h5 class='topic'>Dependency stance</h5>
 * <p>
 * {@code juneau-rest-server} declares {@code jakarta.validation:jakarta.validation-api} in {@code provided}
 * scope. A concrete provider (e.g. {@code org.hibernate.validator:hibernate-validator}) is <b>not</b> bundled
 * and must be added by the consumer when they want validation to actually run. When the provider is missing at
 * runtime, the integration degrades to a no-op &mdash; the request bean is returned unmodified and a warning is
 * logged on first attempted use.
 *
 * <h5 class='topic'>Failure handling</h5>
 * <p>
 * A constraint violation throws {@link org.apache.juneau.rest.validation.ValidationException} (a 400 Bad Request
 * subclass) carrying the list of {@link org.apache.juneau.rest.validation.ValidationViolation} details. The
 * response body shape depends on whether RFC 7807 problem-details mode is enabled on the resource:
 * <ul class='spaced-list'>
 * 	<li><b>Problem-details ON</b> &mdash; {@code application/problem+json} with the standard {@code status} /
 * 		{@code title} / {@code detail} members plus an {@code errors[]} extension array.
 * 	<li><b>Problem-details OFF (default)</b> &mdash; {@code application/json} with a simple
 * 		{@code { "status":400, "errors":[ ... ] }} envelope.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerValidation">REST Server &mdash; Jakarta Validation</a>
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.validation;
