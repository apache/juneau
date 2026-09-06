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
package org.apache.juneau.rest.server.widgets;

/**
 * Marker for a reusable widget primitive.
 *
 * <p>
 * Each concrete type owns a {@code public static final String CONTRACT_VERSION} (starting at {@code "1"}) and
 * implements {@link #validate()} as a fail-closed bean-level check.  Serving-path call sites (for example a
 * table emitter) must invoke {@code validate()} rather than treating it as documentation-only.
 *
 * <p>
 * The REST serving path now enforces this independently of any one call site: every classpath that carries this
 * module registers a {@code ResponseProcessor} (see
 * {@link org.apache.juneau.rest.server.widgets.WidgetsMixin.WidgetValidationProcessor
 * WidgetsMixin.WidgetValidationProcessor}, WORK-J0525) that calls {@link #validate()} on any REST response content
 * that is a {@code Widget}, fail-closed with a 500 on failure.  That closes the gap for the two implementations
 * that ride the wire as a JSON response body ({@code ModalDef}/{@code FormDef}) rather than being consumed by a
 * server-side emitter that already validates on the way in; a non-REST producer (a direct {@code Json.of(...)}
 * serialization, for example) is untouched by construction and still relies on an explicit {@code validate()} /
 * {@code checked()} call.
 *
 * @since 10.0.0
 */
public interface Widget {

	/**
	 * Fail-closed bean validation.
	 *
	 * @throws IllegalArgumentException If this widget is not well-formed.
	 */
	void validate();
}
