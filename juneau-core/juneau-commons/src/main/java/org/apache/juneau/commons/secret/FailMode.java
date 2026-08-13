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
package org.apache.juneau.commons.secret;

/**
 * The policy a network- or OS-backed {@link SecretStore} applies when its backing store cannot answer &mdash; for
 * example when the OS keychain tool is missing or a remote vault is unreachable.
 *
 * <p>
 * Modeled on the {@code FailMode} of {@link org.apache.juneau.commons.concurrent.ReplayCache}: the policy is a
 * consumer decision rather than a fixed SPI behavior.  The built-in {@link InMemorySecretStore} and
 * {@link EnvVarSecretStore} never encounter an unavailable backend, so this enum is only meaningful for the
 * out-of-commons implementations that reach an external secret backend.
 *
 * <p>
 * Note this is orthogonal to a key simply being <i>absent</i> &mdash; a store that successfully answers "no secret
 * under this key" is not a failure and is never resolved through a {@link FailMode}.
 *
 * @since 10.0.0
 */
public enum FailMode {

	/** Treat a backend failure as a missing secret: {@code find} returns empty, {@code exists}/{@code delete} return <jk>false</jk> (availability-preserving). */
	FAIL_OPEN,

	/** Treat a backend failure as an error and propagate it, so the operation fails loudly rather than silently reporting a secret as absent (the safe default). */
	FAIL_CLOSED
}
