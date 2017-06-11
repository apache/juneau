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
package org.apache.juneau.parser;

import org.apache.juneau.*;
import org.apache.juneau.urlencoding.*;

/**
 * Interface used to convert HTTP headers, query parameters, form-data parameters, and URI
 * path variables to POJOs
 * <p>
 * By default, the {@link UrlEncodingParser} class implements this interface so that it can be used to parse
 * these HTTP parts.
 * However, the interface is provided to allow custom parsing of these objects by providing your own implementation
 * class.
 * <p>
 * Implementations must include a no-arg constructor.
 */
public interface PartParser {

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param partType The part type being parsed.
	 * @param in The input being parsed.
	 * @param type The category of value being parsed.
	 * @return The parsed value.
	 * @throws ParseException
	 */
	public <T> T parse(PartType partType, String in, ClassMeta<T> type) throws ParseException;
}
