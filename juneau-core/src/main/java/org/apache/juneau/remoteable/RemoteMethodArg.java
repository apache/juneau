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
package org.apache.juneau.remoteable;

/**
 * Represents the metadata about an annotated argument of a method on a remote proxy interface.
 */
public class RemoteMethodArg {

	/** The argument name.  Can be blank. */
	public final String name;

	/** The zero-based index of the argument on the Java method. */
	public final int index;

	/** The value is skipped if it's null/empty. */
	public final boolean skipIfNE;

	/**
	 * Constructor.
	 *
	 * @param name The argument name.  Can be blank.
	 * @param index The zero-based index of the argument on the Java method.
	 * @param skipIfNE The value is skipped if it's null/empty.
	 */
	protected RemoteMethodArg(String name, int index, boolean skipIfNE) {
		this.name = name;
		this.index = index;
		this.skipIfNE = skipIfNE;
	}
}
