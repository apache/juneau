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
package org.apache.juneau.commons.inject;

/**
 * Post-bind extension seam for {@link ConfigPropertiesBinder}.
 *
 * <p>
 * The binder invokes {@link #validate(Object)} once on each fully-bound target. {@link #NO_OP} is the default and
 * performs no validation; supply a custom implementation to enforce constraints on a bound object without changing
 * {@link ConfigPropertiesBinder}'s public shape.
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface ConfigPropertiesValidator {

	/** The default: performs no validation. */
	ConfigPropertiesValidator NO_OP = target -> { /* No-op: no validation is performed. */ };

	/**
	 * Called once, after a target has been fully bound.
	 *
	 * <p>
	 * Under nesting, this is called once per bound object — innermost (most deeply nested) first, then each
	 * enclosing object in turn as the recursive bind unwinds.
	 *
	 * @param target The bound instance.
	 */
	void validate(Object target);
}
