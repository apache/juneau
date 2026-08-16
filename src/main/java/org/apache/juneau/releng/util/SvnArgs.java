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

package org.apache.juneau.releng.util;

/**
 * Shared {@code svn} CLI flag literals for the credential-passing convention every svn-invoking step
 * follows: the Apache LDAP availid goes on argv via {@link #USERNAME}, while the password is piped through
 * stdin via {@link #PASSWORD_FROM_STDIN} so it never appears on argv (visible in {@code ps}, shell history,
 * or logs).
 */
public final class SvnArgs {

	/** {@code svn} flag introducing the Apache LDAP availid that follows it on argv. */
	public static final String USERNAME = "--username";

	/** {@code svn} flag directing svn to read the password from stdin instead of argv. */
	public static final String PASSWORD_FROM_STDIN = "--password-from-stdin";

	private SvnArgs() {
	}
}
