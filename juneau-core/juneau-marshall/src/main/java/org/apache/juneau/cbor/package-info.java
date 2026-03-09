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
/**
 * CBOR (Concise Binary Object Representation) marshalling support per RFC 8949.
 *
 * <p>
 * CBOR is a binary data format designed for extremely small code size, very small message size,
 * and extensibility. It is widely used in IoT, constrained environments, COSE (CBOR Object Signing
 * and Encryption), and WebAuthn.
 *
 * <p>
 * CBOR shares the same data model as JSON: maps, arrays, strings, numbers, booleans, and null.
 * The {@link org.apache.juneau.cbor.CborSerializer} and {@link org.apache.juneau.cbor.CborParser}
 * provide binary encoding/decoding with compact integer representation and native byte strings.
 *
 * <p>
 * Type mapping:
 * <ul>
 * 	<li>Java beans / Map → CBOR map (major type 5)
 * 	<li>Collection / array → CBOR array (major type 4)
 * 	<li>String / Enum → CBOR text string (major type 3)
 * 	<li>Integer types → CBOR unsigned/negative integer (major types 0/1)
 * 	<li>Float / Double → CBOR IEEE 754 float (major type 7)
 * 	<li>Boolean → CBOR simple values 20/21
 * 	<li>null → CBOR simple value 22
 * 	<li>byte[] → CBOR byte string (major type 2)
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/CborBasics">CBOR Basics</a>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8949.html">RFC 8949</a>
 * </ul>
 */
package org.apache.juneau.cbor;
