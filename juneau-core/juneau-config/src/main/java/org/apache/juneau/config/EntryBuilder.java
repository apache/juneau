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
package org.apache.juneau.config;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;

import org.apache.juneau.marshall.serializer.*;

/**
 * A fluent builder for adding or updating a single {@link Config} entry.
 *
 * <p>
 * This is an ergonomic, self-documenting alternative to the multi-argument
 * {@link Config#set(String, Object, Serializer, String, String, List)} overload.  That overload packs several distinct
 * "leave-unchanged versus clear" sentinel conventions into one call (a <jk>null</jk> comment leaves the existing comment
 * untouched while a blank comment clears it; a <jk>null</jk> pre-lines list leaves the existing pre-lines untouched while
 * an empty list clears them).  This builder replaces those sentinels with explicit methods:
 * <ul>
 * 	<li><b>Not calling</b> a facet method leaves that facet untouched (no sentinel needed).
 * 	<li>An explicit setter ({@link #comment(String)}, {@link #preLines(List)}, {@link #serializer(Serializer)},
 * 		{@link #modifiers(String)}) sets that facet.
 * 	<li>An explicit clear ({@link #clearComment()}, {@link #clearPreLines()}) intentionally clears that facet rather than
 * 		relying on a magic blank string or empty list.
 * </ul>
 *
 * <p>
 * The write is performed by the terminal {@link #set(Object)} or {@link #set()} method.  The result is identical to the
 * equivalent {@link Config#set(String, Object, Serializer, String, String, List)} call.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Add an entry with a comment and pre-lines.</jc>
 * 	<jv>config</jv>.entry(<js>"MySection/myKey"</js>)
 * 		.comment(<js>"My comment"</js>)
 * 		.preLines(List.<jsm>of</jsm>(<js>"# A pre-line"</js>))
 * 		.set(<js>"My value"</js>);
 *
 * 	<jc>// Update only the comment on an existing entry, leaving the value and pre-lines untouched.</jc>
 * 	<jv>config</jv>.entry(<js>"MySection/myKey"</js>)
 * 		.comment(<js>"Updated comment"</js>)
 * 		.set();
 *
 * 	<jc>// Clear the comment on an existing entry.</jc>
 * 	<jv>config</jv>.entry(<js>"MySection/myKey"</js>)
 * 		.clearComment()
 * 		.set();
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This builder is not thread safe and is intended to be used and discarded within a single statement.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauConfig">juneau-config Basics</a>
 * </ul>
 */
public class EntryBuilder {

	private final Config config;
	private final String key;

	private Object value;
	private Serializer serializer;
	private String modifiers;
	private String comment;
	private List<String> preLines;

	/**
	 * Constructor.
	 *
	 * @param config The config that the entry belongs to.
	 * @param key
	 * 	The key of the entry to add or update.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	protected EntryBuilder(Config config, String key) {
		assertArgNotNull("key", key);
		this.config = config;
		this.key = key;
	}

	/**
	 * Specifies the value to store on the entry.
	 *
	 * <p>
	 * Equivalent to passing the value to the terminal {@link #set(Object)} method.
	 *
	 * @param value
	 * 	The new value POJO.
	 * 	<br>Serialized to a string using the registered (or {@link #serializer(Serializer) specified}) serializer.
	 * @return This object.
	 */
	public EntryBuilder value(Object value) {
		this.value = value;
		return this;
	}

	/**
	 * Specifies the serializer to use to serialize the value.
	 *
	 * <p>
	 * If not called, the serializer registered on the config is used.
	 *
	 * @param value The serializer to use for serializing the value.
	 * @return This object.
	 */
	public EntryBuilder serializer(Serializer value) {
		this.serializer = value;
		return this;
	}

	/**
	 * Specifies the modifiers to apply to the value (e.g. <js>"*"</js> for encoded values).
	 *
	 * <p>
	 * If not called, the modifiers on any existing entry are left untouched.
	 *
	 * @param value The modifiers to apply to the value.
	 * @return This object.
	 */
	public EntryBuilder modifiers(String value) {
		this.modifiers = value;
		return this;
	}

	/**
	 * Specifies the same-line comment to add to the entry.
	 *
	 * <p>
	 * If not called, the comment on any existing entry is left untouched.
	 * <br>Use {@link #clearComment()} to explicitly remove an existing comment.
	 *
	 * @param value The same-line comment to add to the entry.  Must not be <jk>null</jk> (use {@link #clearComment()} instead).
	 * @return This object.
	 */
	public EntryBuilder comment(String value) {
		assertArgNotNull("value", value);
		this.comment = value;
		return this;
	}

	/**
	 * Explicitly clears the same-line comment on the entry.
	 *
	 * @return This object.
	 */
	public EntryBuilder clearComment() {
		this.comment = "";
		return this;
	}

	/**
	 * Specifies the comment or blank lines to add before the entry.
	 *
	 * <p>
	 * If not called, the pre-lines on any existing entry are left untouched.
	 * <br>Use {@link #clearPreLines()} to explicitly remove existing pre-lines.
	 *
	 * @param value The comment or blank lines to add before the entry.  Must not be <jk>null</jk> (use {@link #clearPreLines()} instead).
	 * @return This object.
	 */
	public EntryBuilder preLines(List<String> value) {
		assertArgNotNull("value", value);
		this.preLines = value;
		return this;
	}

	/**
	 * Explicitly clears the pre-lines on the entry.
	 *
	 * @return This object.
	 */
	public EntryBuilder clearPreLines() {
		this.preLines = Collections.emptyList();
		return this;
	}

	/**
	 * Adds or updates the entry using the value specified via {@link #value(Object)}.
	 *
	 * @return The config that this builder belongs to.
	 * @throws SerializeException
	 * 	If serializer could not serialize the value or if a serializer is not registered with this config file.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config set() throws SerializeException {
		return config.set(key, value, serializer, modifiers, comment, preLines);
	}

	/**
	 * Adds or updates the entry using the specified value.
	 *
	 * @param value
	 * 	The new value POJO.
	 * 	<br>Serialized to a string using the registered (or {@link #serializer(Serializer) specified}) serializer.
	 * @return The config that this builder belongs to.
	 * @throws SerializeException
	 * 	If serializer could not serialize the value or if a serializer is not registered with this config file.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config set(Object value) throws SerializeException {
		this.value = value;
		return set();
	}
}
