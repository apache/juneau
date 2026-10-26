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
package org.apache.juneau.http;

/**
 * A name/value pair representing a single query parameter, form field, or path variable.
 *
 * <p>
 * Implementations may supply their value eagerly or lazily (via a {@link java.util.function.Supplier}).
 * A {@code null} value signals that the part should be omitted from the request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">Next-Generation REST Client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public interface HttpPart {

	/**
	 * Returns the part name.
	 *
	 * @return The part name. Never <jk>null</jk>.
	 */
	String getName();

	/**
	 * Returns the part value, or <jk>null</jk> if the part should be omitted.
	 *
	 * @return The part value, possibly <jk>null</jk>.
	 */
	String getValue();
}
