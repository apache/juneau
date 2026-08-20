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
