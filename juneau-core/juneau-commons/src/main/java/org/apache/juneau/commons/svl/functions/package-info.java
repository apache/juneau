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
 * Built-in {@link org.apache.juneau.commons.svl.VarFunction} catalog for
 * {@code #{name(args...)}} script-evaluation syntax.
 *
 * <p>
 * Functions are organized by category with one Java file per category;
 * each individual function is a static nested class extending
 * {@link org.apache.juneau.commons.svl.TypedFunction}.
 *
 * <h5 class='section'>Categories:</h5>
 * <ul>
 * 	<li>{@link org.apache.juneau.commons.svl.functions.StringFunctions} — substring, upper,
 * 		lower, trim, replace, contains, format, split, join, pathToken, etc.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.TypeConversionFunctions} — toInt, toLong,
 * 		toDouble, toBool.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.ArithmeticFunctions} — add, subtract,
 * 		multiply, divide, modulo, min, max, abs.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.BooleanFunctions} — and, or, not, xor,
 * 		eq, neq, lt, lte, gt, gte.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.ConditionalFunctions} — if, switch,
 * 		coalesce, notEmpty.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.RegexFunctions} — match, extract,
 * 		replaceRegex.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.EncodingFunctions} — base64Encode/Decode,
 * 		urlEncode/Decode, htmlEscape/Unescape.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.DateFunctions} — now, parseDate,
 * 		formatDate.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.RandomFunctions} — rand, randInt,
 * 		randLong, randString, randChoice, uuid.
 * 	<li>{@link org.apache.juneau.commons.svl.functions.JsonFunctions} — jsonPath, get, keys,
 * 		values, size.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallSimpleVariableLanguage">Simple Variable Language Basics</a>
 * </ul>
 */
package org.apache.juneau.commons.svl.functions;
