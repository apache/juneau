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
package org.apache.juneau.reflect;

/**
 * Identifies possible modifiers on classes, methods, fields, and constructors.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public enum ReflectFlags {

	/** PUBLIC */
	PUBLIC,

	/** NOT_PUBLIC */
	NOT_PUBLIC,

	/** PROTECTED */
	PROTECTED,

	/** NOT_PROTECTED */
	NOT_PROTECTED,

	/** STATIC */
	STATIC,

	/** NOT_STATIC */
	NOT_STATIC,

	/** MEMBER */
	MEMBER,

	/** NOT_MEMBER */
	NOT_MEMBER,

	/** INTERFACE */
	INTERFACE,

	/** CLASS */
	CLASS,

	/** HAS_PARAMS */
	HAS_PARAMS,

	/** HAS_NO_PARAMS */
	HAS_NO_PARAMS,

	/** DEPRECATED */
	DEPRECATED,

	/** NOT_DEPRECATED */
	NOT_DEPRECATED,

	/** ABSTRACT */
	ABSTRACT,

	/** NOT_ABSTRACT */
	NOT_ABSTRACT,

	/** TRANSIENT */
	TRANSIENT,

	/** NOT_TRANSIENT */
	NOT_TRANSIENT
}
