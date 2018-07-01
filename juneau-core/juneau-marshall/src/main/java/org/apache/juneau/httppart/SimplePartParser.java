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
package org.apache.juneau.httppart;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * An implementation of {@link HttpPartParser} that takes in the strings and tries to convert them to POJOs using constructors and static create methods.
 *
 * <p>
 * The class being created must be one of the following in order to convert it from a string:
 *
 * <ul>
 * 	<li>
 * 		An <jk>enum</jk>.
 * 	<li>
 * 		Have a public constructor with a single <code>String</code> parameter.
 * 	<li>
 * 		Have one of the following public static methods that takes in a single <code>String</code> parameter:
 * 		<ul>
 * 			<li><code>fromString</code>
 * 			<li><code>fromValue</code>
 * 			<li><code>valueOf</code>
 * 			<li><code>parse</code>
 * 			<li><code>parseString</code>
 * 			<li><code>forName</code>
 * 			<li><code>forString</code>
 * 	</ul>
 * </ul>
 */
public class SimplePartParser implements HttpPartParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link SimplePartParser}, all default settings. */
	public static final SimplePartParser DEFAULT = new SimplePartParser();


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	@Override
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) {
		return ClassUtils.fromString(type.getInnerClass(), in);
	}
}
