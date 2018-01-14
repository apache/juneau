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
import org.apache.juneau.parser.*;

/**
 * Interface used to convert HTTP headers, query parameters, form-data parameters, and URI path variables to POJOs
 * 
 * <p>
 * The following default implementations are provided:
 * <ul class='doctree'>
 * 	<li class='jc'>{@link org.apache.juneau.httppart.UonPartParser} - Parts encoded in UON notation.
 * 	<li class='jc'>{@link org.apache.juneau.httppart.SimplePartParser} - Parts encoded in plain text.
 * </ul>
 * 
 * <p>
 * Implementations must include either a public no-args constructor or a public constructor that takes in a single
 * {@link PropertyStore} object.
 */
public interface HttpPartParser {

	/**
	 * Represent "no" part parser.
	 * 
	 * <li>
	 * Used to represent the absence of a part parser in annotations.
	 */
	public static interface Null extends HttpPartParser {}
	
	/**
	 * Converts the specified input to the specified class type.
	 * 
	 * @param partType The part type being parsed.
	 * @param in The input being parsed.
	 * @param type The category of value being parsed.
	 * @return The parsed value.
	 * @throws ParseException
	 */
	public <T> T parse(HttpPartType partType, String in, ClassMeta<T> type) throws ParseException;
}
