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
 * Runtime / process-entry data carriers.
 *
 * <p>
 * Lean, {@link java.util.Optional}-returning value types for inputs supplied to a running process from outside the
 * application: command-line arguments ({@link org.apache.juneau.commons.runtime.Args}) and Jar manifest contents
 * ({@link org.apache.juneau.commons.runtime.ManifestFile}).
 *
 * <p>
 * These classes intentionally avoid extending {@code JsonMap} or any marshall-side type so they can be consumed from
 * {@code juneau-commons} and dependents without pulling in the full serialization stack.
 */
package org.apache.juneau.commons.runtime;
