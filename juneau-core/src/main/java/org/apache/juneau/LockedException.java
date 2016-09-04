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

/**
 * Exception that gets thrown when trying to modify settings on a locked {@link Lockable} object.
 * <p>
 * A locked exception indicates a programming error.
 * Certain objects that are meant for reuse, such as serializers and parsers, provide
 * the ability to lock the current settings so that they cannot be later changed.
 * This exception indicates that a setting change was attempted on a previously locked object.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class LockedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	LockedException() {
		super("Object is locked and object settings cannot be modified.");
	}
}
