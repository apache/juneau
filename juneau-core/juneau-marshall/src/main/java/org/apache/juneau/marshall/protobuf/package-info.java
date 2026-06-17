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
 * Protocol Buffers <b>binary wire-format</b> marshalling support (bean/reflection-driven, no <c>.proto</c> file required).
 *
 * <p>
 * Unlike the text-format {@link org.apache.juneau.marshall.proto} package (<c>text/protobuf</c>), this package emits and
 * parses the compact, non-self-describing protobuf <b>binary</b> wire format (<c>application/protobuf</c>).  Because the
 * binary wire carries neither field names nor specific scalar types, both serialization and parsing consult a per-bean
 * <b>field-number &hArr; property + wire-scalar-type</b> table derived from Juneau bean metadata and cached on
 * {@link org.apache.juneau.marshall.protobuf.ProtobufClassMeta}.
 *
 * <p>
 * The codec is lossless for Juneau&hArr;Juneau round-trips out of the box, and upgrades field-by-field to true external
 * <c>protoc</c> interop when the user supplies explicit field numbers and scalar types via
 * {@link org.apache.juneau.marshall.protobuf.Protobuf @Protobuf}.
 *
 * <p>
 * Default Java&rarr;proto scalar mapping:
 * <ul>
 * 	<li>{@code boolean}/{@code Boolean} &rarr; bool (varint)
 * 	<li>{@code byte}/{@code short}/{@code int}/{@code Integer} &rarr; int32 (varint)
 * 	<li>{@code long}/{@code Long} &rarr; int64 (varint)
 * 	<li>{@code float}/{@code Float} &rarr; float (I32)
 * 	<li>{@code double}/{@code Double} &rarr; double (I64)
 * 	<li>{@link java.lang.String} &rarr; string (length-delimited UTF-8)
 * 	<li>{@code byte[]} &rarr; bytes (length-delimited)
 * 	<li>{@link java.lang.Enum} &rarr; int32 ordinal (varint)
 * 	<li>{@link java.math.BigInteger}/{@link java.math.BigDecimal}/{@code char} &rarr; string (lossless)
 * 	<li>Date/time types &rarr; ISO-8601 string (lossless)
 * 	<li>Nested bean &rarr; length-delimited embedded message
 * 	<li>{@link java.util.Map} &rarr; repeated <c>entry { key=1; value=2 }</c> messages
 * 	<li>Repeated scalars &rarr; packed; repeated string/bytes/message &rarr; tagged entries
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBinaryBasics">Protobuf Binary Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/programming-guides/encoding/">Protocol Buffers Encoding</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.marshall.protobuf;
