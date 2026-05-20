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
 * Collections classes.
 *
 * <h5 class='section'>Type hierarchy:</h5>
 * <ul>
 * 	<li>{@link org.apache.juneau.collections.MarshalledMap} / {@link org.apache.juneau.collections.MarshalledList}
 * 		&mdash; neutral, marshaller-agnostic base classes carrying typed accessors, fluent setters,
 * 		bean integration, and {@link org.apache.juneau.objecttools.ObjectRest}-based path navigation.
 * 		Their default {@link java.lang.Object#toString()} is the inherited {@link java.util.LinkedHashMap}
 * 		/ {@link java.util.LinkedList} form.
 * 	<li>{@link org.apache.juneau.collections.JsonMap} / {@link org.apache.juneau.collections.JsonList}
 * 		&mdash; strict-JSON-flavored subclasses. Their {@code toString()} renders as strict
 * 		<a href="https://www.rfc-editor.org/rfc/rfc8259" target="_blank">RFC&nbsp;8259</a> JSON via
 * 		{@link org.apache.juneau.marshaller.Json#of(Object)}, and their {@code (CharSequence)} /
 * 		{@code (Reader)} constructors and {@code ofString(...)} factories default to
 * 		{@link org.apache.juneau.json.JsonParser#DEFAULT}.
 * 	<li>{@link org.apache.juneau.json5.Json5Map} / {@link org.apache.juneau.json5.Json5List}
 * 		(in {@link org.apache.juneau.json5}) &mdash; JSON5-flavored siblings of
 * 		{@code JsonMap} / {@code JsonList}. Their {@code toString()} renders as JSON5 via
 * 		{@link org.apache.juneau.marshaller.Json5#of(Object)}, and their {@code (CharSequence)} /
 * 		{@code (Reader)} constructors and {@code ofString(...)} factories default to
 * 		{@link org.apache.juneau.json5.Json5Parser#DEFAULT}. These are the drop-in replacement
 * 		for callers that previously relied on {@code JsonMap.toString()} producing JSON5 or on
 * 		{@code new JsonMap("{unquoted:'json5'}")} parsing JSON5.
 * 	<li>{@link org.apache.juneau.collections.ResolvingMarshalledMap}
 * 		&mdash; a {@code MarshalledMap} subclass that resolves SVL variables on every value lookup.
 * 		Renamed from {@code ResolvingJsonMap} and re-parented from {@code JsonMap} in 9.5 since
 * 		SVL resolution is language-agnostic.
 * </ul>
 *
 * <p>Per-marshaller flavored {@code XMap} / {@code XList} pairs (e.g. {@code XmlMap}, {@code YamlMap},
 * {@code UonMap}) for the remaining languages are planned as a follow-on and are not delivered in 9.5;
 * parser sessions other than {@link org.apache.juneau.json.JsonParserSession} and
 * {@link org.apache.juneau.json5.Json5ParserSession} currently produce the neutral
 * {@link org.apache.juneau.collections.MarshalledMap} / {@link org.apache.juneau.collections.MarshalledList}
 * when parsing into unbound {@code Object} / {@code Map<String,Object>} / {@code Collection<Object>}
 * targets.
 */
package org.apache.juneau.collections;
