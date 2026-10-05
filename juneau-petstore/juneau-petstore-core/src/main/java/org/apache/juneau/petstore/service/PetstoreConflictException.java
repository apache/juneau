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
package org.apache.juneau.petstore.service;

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * Thrown when a petstore operation conflicts with the entity's current state (HTTP 409 at the console layer).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>if</jk> (<jv>pet</jv>.getStatus() == PetStatus.<jsf>SOLD</jsf>)
 * 		<jk>throw new</jk> PetstoreConflictException(<js>"Pet '%s' is already '%s'"</js>, <jv>pet</jv>.getId(), <jv>pet</jv>.getStatus());
 * </p>
 */
public class PetstoreConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message The message, a {@link String#format} pattern with {@code '%s'} placeholders.
	 * @param args The message arguments.
	 */
	public PetstoreConflictException(String message, Object...args) {
		super(f(message, args));
	}
}
