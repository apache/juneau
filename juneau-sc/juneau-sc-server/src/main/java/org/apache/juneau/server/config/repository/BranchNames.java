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
package org.apache.juneau.server.config.repository;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.regex.*;

/**
 * Validation of branch names accepted by the configuration repository commands.
 *
 * <p>
 * A branch name is only ever passed to git as a ref name, so names such as <c>feature/foo</c> are allowed, but a
 * leading dash, <c>HEAD</c>, parent-directory sequences, backslashes, NUL and characters outside
 * <c>[A-Za-z0-9._/-]</c> are not.
 */
final class BranchNames {

	private static final Pattern BRANCH_CHARS = Pattern.compile("[A-Za-z0-9._/-]+");

	private BranchNames() {}

	/**
	 * Returns <jk>true</jk> if the specified value is an acceptable branch name.
	 *
	 * @param value The branch name.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if valid.
	 */
	static boolean isValid(String value) {
		return value != null
			&& ! value.isEmpty()
			&& ! value.startsWith("-")
			&& neq(value, "HEAD")
			&& BRANCH_CHARS.matcher(value).matches()
			&& ! value.contains("..");
	}

	/**
	 * Throws if the specified value is not an acceptable branch name.
	 *
	 * @param value The branch name.  Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If invalid.
	 */
	static void validate(String value) {
		if (! isValid(value))
			throw new IllegalArgumentException("Invalid branch value.");
	}
}
