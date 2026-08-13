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

import org.apache.juneau.commons.settings.*;

/**
 * Marker specialization of {@link PropertySource} whose values are sensitive and must be redacted anywhere they
 * would otherwise be surfaced &mdash; a {@code toString()}, a settings dump, or a log line.
 *
 * <p>
 * This marker is the seam that keeps the opt-in {@link SecretStorePropertySource} bridge from leaking bridged
 * secrets: a dump/log path that iterates {@link PropertySource}s should test
 * {@code src instanceof SensitivePropertySource} and redact rather than print resolved values.  The marker is scoped
 * to the bridge on purpose &mdash; it is not forced across all of {@code Settings}.
 *
 * @since 10.0.0
 */
public interface SensitivePropertySource extends PropertySource {}
