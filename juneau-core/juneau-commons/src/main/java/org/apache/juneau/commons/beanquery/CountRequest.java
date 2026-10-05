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
package org.apache.juneau.commons.beanquery;

/**
 * What a caller asks {@link BeanQuery.Builder#counts(CountRequest)} to write onto a {@link BeanQuery}'s
 * {@code opts} (design §5).
 *
 * <p>
 * This is the builder-facing counterpart of the session-side {@link CountPolicy}: {@code CountPolicy}
 * decides whether a session honors a count request at all; {@code CountRequest} is what the request asks for.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	BeanQuery <jv>q</jv> = BeanQuery.<jsm>create</jsm>().counts(CountRequest.<jsf>BOTH</jsf>).build();
 * </p>
 *
 * @since 10.0.0
 */
public enum CountRequest {

	/** Request no counts.  Writing this removes the {@code counts} opt if one was set. */
	NONE,

	/** Request only the matched-row count ({@code counts=matched}). */
	MATCHED,

	/** Request both the total and matched counts ({@code counts=both}). */
	BOTH
}
