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
 * BSON (Binary JSON) marshalling support.
 *
 * <p>
 * BSON is a binary serialization format used by MongoDB. It extends JSON with additional types
 * (datetime, binary, 32-bit/64-bit integers, decimal128) and uses little-endian byte order.
 *
 * <p>
 * Juneau provides {@link org.apache.juneau.bson.BsonSerializer} and {@link org.apache.juneau.bson.BsonParser} for
 * serialization and parsing, plus {@link org.apache.juneau.marshaller.Bson} for a combined API.
 *
 * <h5 class='section'>Type mapping (summary)</h5>
 * <ul class='spaced-list'>
 * 	<li>Beans/maps → BSON document (0x03)
 * 	<li>Collections/arrays → BSON array (0x04)
 * 	<li>Strings/enums → BSON string (0x02)
 * 	<li>Numbers → int32 (0x10), int64 (0x12), double (0x01), or decimal128 (0x13)
 * 	<li>Dates → BSON datetime (0x09) or ISO 8601 string
 * 	<li>{@code byte[]} → BSON binary (0x05)
 * </ul>
 *
 * <p>
 * Compared to MsgPack: BSON is length-prefixed and little-endian; supports typed integers (int32/int64),
 * native datetime, and decimal128. Compared to JSON: BSON is binary and more compact for typed data.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * 	<li class='link'><a class="doclink" href="https://bsonspec.org/spec.html">BSON Specification</a>
 * </ul>
 */
package org.apache.juneau.bson;
