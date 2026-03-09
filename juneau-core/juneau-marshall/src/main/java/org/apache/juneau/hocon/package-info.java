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
 * HOCON (Human-Optimized Config Object Notation) format serializer and parser for Apache Juneau.
 *
 * <p>
 * HOCON is a superset of JSON used extensively in the JVM ecosystem (Akka, Play, sbt, Spark, Kafka) for configuration.
 * It extends JSON with:
 * </p>
 * <ul class='spaced-list'>
 * 	<li>Path expressions — <c>a.b.c = value</c> creates nested objects
 * 	<li>Equals sign separator — <c>key = value</c> (HOCON convention)
 * 	<li>Unquoted strings — keys and values without quotes when unambiguous
 * 	<li>Object merging — duplicate keys merge instead of overwrite
 * 	<li>Substitutions — <c>${var}</c> and <c>${?var}</c> references (parse only)
 * 	<li>Triple-quoted strings — <c>"""..."""</c> with no escape processing
 * 	<li>Comments — <c>#</c> and <c>//</c>
 * 	<li>Optional root braces — top-level without <c>{}</c>
 * </ul>
 *
 * <h5 class='section'>Bean mapping</h5>
 * <p>
 * Round-trip fidelity matches JSON: beans, maps, lists, primitives, dates (ISO 8601), and Duration all round-trip.
 * Substitutions and value concatenation are resolved during parse; the serializer always writes concrete values.
 * </p>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	String <jv>hocon</jv> = HoconSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 * 	MyBean <jv>bean</jv> = HoconParser.<jsf>DEFAULT</jsf>.parse(<jv>hocon</jv>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Or use the marshaller:</jc>
 * 	String <jv>hocon</jv> = Hocon.<jsf>of</jsf>(<jv>myBean</jv>);
 * 	MyBean <jv>bean</jv> = Hocon.<jsf>to</jsf>(<jv>hocon</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HoconBasics">Hocon Basics</a>
 * 	<li class='link'><a class="doclink" href="https://github.com/lightbend/config/blob/main/HOCON.md">HOCON Specification</a>
 * </ul>
 */
package org.apache.juneau.hocon;
