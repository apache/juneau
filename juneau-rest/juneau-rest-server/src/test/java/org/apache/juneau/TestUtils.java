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
package org.apache.juneau;

/**
 * Minimal module-local test utilities for <c>juneau-rest-server</c>.
 *
 * <p>
 * Inherits the shared pure-JDK/commons helpers (such as {@code assertThrowsWithMessage} and {@code r(Object)}) from
 * {@link BasicTestUtils}, which lives in the marshall-free <c>juneau-test-utils</c> module.
 *
 * <p>
 * This class is intentionally self-contained: it must NOT reference {@code org.apache.juneau.rest.mock.*} or any type
 * that would pull a higher module into <c>juneau-rest-server</c>'s test scope and introduce a Maven reactor cycle. It is
 * deliberately NOT a copy of the full cross-module {@code TestUtils} that lives in the integration-test residual.
 */
public class TestUtils extends BasicTestUtils {
}
