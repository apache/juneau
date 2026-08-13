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
 * A small, JDK-only SPI for storing and retrieving secrets by key &mdash; the secure, mutable sibling of
 * {@link org.apache.juneau.commons.settings.PropertySource}.
 *
 * <p>
 * The interface + built-in implementations in this package have zero third-party runtime dependencies.
 * Sensitivity is intrinsic: secret values are held as {@code char[]} (never {@link java.lang.String String}) and are never
 * {@code toString()}'d, logged, or dumped.  Backends that reach an OS keychain, a remote vault, or a cloud
 * secret manager live in separate opt-in modules, never here.
 *
 * <h5 class='section'>Key types:</h5>
 * <ul>
 * 	<li>{@link org.apache.juneau.commons.secret.SecretStore} &mdash; the SPI (store / find / exists / delete).
 * 	<li>{@link org.apache.juneau.commons.secret.InMemorySecretStore} &mdash; the zero-config, process-local default.
 * 	<li>{@link org.apache.juneau.commons.secret.EnvVarSecretStore} &mdash; a read-only, environment-variable-backed source.
 * 	<li>{@link org.apache.juneau.commons.secret.SecretStoreProvider} &mdash; {@code ServiceLoader} discovery.
 * 	<li>{@link org.apache.juneau.commons.secret.SecretStores} &mdash; {@code BeanStore} resolution (default {@code InMemorySecretStore}).
 * 	<li>{@link org.apache.juneau.commons.secret.SecretStorePropertySource} &mdash; an opt-in bridge exposing a store as a redacting {@code PropertySource}.
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.commons.secret;
