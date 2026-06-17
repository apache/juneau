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
 * JSON5L (JSON5 Lines) marshalling support.
 *
 * <p>
 * JSON5L combines the relaxed JSON5 dialect (comments, unquoted/single-quoted keys, trailing
 * commas, relaxed numbers) with JSONL's newline-delimited framing (one document per line).
 *
 * <p>
 * This package provides {@link org.apache.juneau.marshall.json5l.Json5lSerializer} and
 * {@link org.apache.juneau.marshall.json5l.Json5lParser}.  The serializer emits strict
 * RFC-8259 JSON per line by default (byte-identical to {@link org.apache.juneau.marshall.jsonl.JsonlSerializer}),
 * with an opt-in for JSON5 sugar.  The parser accepts the full JSON5 dialect per line — and,
 * because JSON5 is a strict superset of JSON, plain JSONL input as well.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of beans to JSON5L</jc>
 * 	String <jv>json5l</jv> = Json5l.<jsm>of</jsm>(<jv>myList</jv>);
 *
 * 	<jc>// Parse JSON5L back to a list</jc>
 * 	List&lt;MyBean&gt; <jv>list</jv> = Json5l.<jsm>to</jsm>(<jv>json5l</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://jsonlines.org/">JSON Lines Specification</a>
 * 	<li class='link'><a class="doclink" href="https://json5.org/">JSON5 Specification</a>
 * </ul>
 */
package org.apache.juneau.marshall.json5l;
