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
package org.apache.juneau.common.utils;

/**
 * Identical to {@link Runnable} but the run method can throw stuff.
 *
 * <p>
 * Allows you to pass in arbitrary snippets of code in fluent interfaces.
 *
 * <p>
 * See <c>Assertions.<jsm>assertThrown</jsm>(Snippet)</c> for an example.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#ja.Overview">Fluent Assertions</a>
 * </ul>
 */
public interface Snippet {

	/**
	 * Run arbitrary code and optionally throw an exception.
	 *
	 * @throws Throwable Any throwable.
	 */
	void run() throws Throwable;
}