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
package org.apache.juneau.ini;

/**
 * API for defining a string encoding/decoding mechanism for entries in {@link ConfigFile}.
 */
public interface Encoder {

	/**
	 * Encode a string.
	 * 
	 * @param fieldName The field name being encoded.
	 * @param in The unencoded input string.
	 * @return The encoded output string.
	 */
	public String encode(String fieldName, String in);

	/**
	 * Decode a string.
	 * 
	 * @param fieldName The field name being decoded.
	 * @param in The encoded input string.
	 * @return The decoded output string.
	 */
	public String decode(String fieldName, String in);
}
