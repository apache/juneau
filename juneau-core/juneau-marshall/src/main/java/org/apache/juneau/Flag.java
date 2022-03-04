// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.internal.ThrowableUtils.*;

import org.apache.juneau.assertions.*;

/**
 * A simple settable flag.
 */
public class Flag {

	private boolean value;

	/**
	 * Creates an unset flag.
	 *
	 * @return A new unset flag.
	 */
	public static Flag create() {
		return of(false);
	}

	/**
	 * Creates a flag with the specified initial state.
	 *
	 * @param value The initial state of the flag.
	 * @return A new flag.
	 */
	public static Flag of(boolean value) {
		return new Flag(value);
	}

	private Flag(boolean value) {
		this.value = value;
	}

	/**
	 * Runs a snippet of code if the flag is set.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>create</jsm>();
	 * 	...
	 * 	<jv>flag</jv>.ifSet(()-&gt;<jsm>doSomething</jsm>());
	 * </p>
	 *
	 * @param snippet The snippet of code to run.
	 * @return This object.
	 */
	public Flag ifSet(Snippet snippet) {
		if (value)
			runSnippet(snippet);
		return this;
	}

	/**
	 * Runs a snippet of code if the flag is not set.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	Flag <jv>flag</jv> = Flag.<jsm>create</jsm>();
	 * 	...
	 * 	<jv>flag</jv>.ifNotSet(()-&gt;<jsm>doSomething</jsm>());
	 * </p>
	 *
	 * @param snippet The snippet of code to run.
	 * @return This object.
	 */
	public Flag ifNotSet(Snippet snippet) {
		if (! value)
			runSnippet(snippet);
		return this;
	}

	private void runSnippet(Snippet snippet) {
		try {
			snippet.run();
		} catch (Error e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Sets the flag and returns the value before it was set.
	 *
	 * @return The previous value.
	 */
	public boolean getAndSet() {
		boolean b = value;
		value = true;
		return b;
	}

	/**
	 * Sets the flag.
	 *
	 * @return This object.
	 */
	public Flag set() {
		value = true;
		return this;
	}

	/**
	 * Unsets the flag.
	 *
	 * @return This object.
	 */
	public Flag unset() {
		value = false;
		return this;
	}
}
