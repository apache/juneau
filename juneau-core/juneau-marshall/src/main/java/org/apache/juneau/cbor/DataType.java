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
package org.apache.juneau.cbor;

/**
 * Constants for the CBOR format (RFC 8949).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/CborBasics">CBOR Basics</a>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8949.html">RFC 8949 - CBOR</a>
 * </ul>
 */
enum DataType {
	UINT,        // Major type 0: unsigned integer
	NINT,        // Major type 1: negative integer
	BINARY,      // Major type 2: byte string
	STRING,      // Major type 3: text string
	ARRAY,       // Major type 4: array
	MAP,         // Major type 5: map
	TAG,         // Major type 6: semantic tag
	FLOAT,       // Major type 7: float (half/single/double)
	BOOLEAN,     // Major type 7: simple value 20/21
	NULL,        // Major type 7: simple value 22
	UNDEFINED,   // Major type 7: simple value 23
	BREAK,       // Major type 7: additional info 31
	SIMPLE;      // Major type 7: other simple values

	boolean isOneOf(DataType...dataTypes) {
		for (var dt : dataTypes)
			if (this == dt)
				return true;
		return false;
	}
}
