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
package org.apache.juneau.annotation;

/**
 * Specifies the marshalling strategy for a type annotated with {@link Marshalled @Marshalled}.
 */
public enum MarshalledAs {

	/**
	 * Auto-detect the marshalling strategy (default).
	 *
	 * <p>
	 * The marshalling engine inspects the type and selects the best strategy
	 * (bean, map, collection, string swap, etc.).
	 */
	DETECT,

	/**
	 * Serialize using {@link Object#toString()} and deserialize using a {@code fromString(String)},
	 * {@code valueOf(String)}, or single-{@code String}-argument constructor.
	 *
	 * <p>
	 * Replaces the common {@code BeanStringSwap} use case without requiring a separate swap class.
	 */
	STRING
}
