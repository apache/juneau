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
 * JSONL (JSON Lines) marshalling support.
 *
 * <p>
 * JSONL is a text format where each line is a valid JSON value, separated by newline characters.
 * It is the standard format for LLM fine-tuning datasets, streaming AI inference, log aggregation,
 * and bulk data pipelines.
 *
 * <p>
 * This package provides {@link org.apache.juneau.jsonl.JsonlSerializer} and
 * {@link org.apache.juneau.jsonl.JsonlParser} which extend the JSON serializer/parser to produce
 * and consume newline-delimited JSON.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of beans to JSONL</jc>
 * 	String <jv>jsonl</jv> = Jsonl.<jsm>of</jsm>(<jv>myList</jv>);
 *
 * 	<jc>// Parse JSONL back to a list</jc>
 * 	List&lt;MyBean&gt; <jv>list</jv> = Jsonl.<jsm>to</jsm>(<jv>jsonl</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://jsonlines.org/">JSON Lines Specification</a>
 * </ul>
 */
package org.apache.juneau.jsonl;
