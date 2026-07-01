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
package org.apache.juneau.test.junit;

/**
 * Lifecycle scope for a {@link TestBean @TestBean}-declared override.
 *
 * <p>
 * Selects whether the override is rebuilt on every test method ({@link #METHOD}) or shared across all test
 * methods in a single test class ({@link #CLASS}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link TestBean} - The annotation that carries this scope.
 * 	<li class='jc'>{@link JuneauBeanStoreExtension} - The JUnit 5 extension that honors this scope.
 * </ul>
 *
 * @since 10.0.0
 */
public enum Scope {

	/**
	 * Per-test scope &mdash; the override is rebuilt for every {@code @Test} method.
	 *
	 * <p>
	 * Default scope.  Discovered at {@code beforeEach} and dropped at {@code afterEach}.
	 */
	METHOD,

	/**
	 * Per-class scope &mdash; the override is built once and shared by every {@code @Test} method in the class.
	 *
	 * <p>
	 * Discovered at {@code beforeAll} and dropped at {@code afterAll}.  Only {@code static} fields and methods may
	 * declare {@code CLASS}-scope overrides; instance members are not visible at {@code beforeAll} time.
	 */
	CLASS
}
