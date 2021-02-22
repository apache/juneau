// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.config;

import static org.apache.juneau.config.Config.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.config.encode.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Builder for creating instances of {@link Config Configs}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
 * 	String setting = cf.getString(<js>"MySection/mysetting"</js>);
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-config}
 * </ul>
 */
@FluentSetters
public class ConfigBuilder extends ContextBuilder {

	/**
	 * Constructor, default settings.
	 */
	public ConfigBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param cp The initial configuration settings for this builder.
	 */
	public ConfigBuilder(ContextProperties cp) {
		super(cp);
	}

	@Override /* ContextBuilder */
	public Config build() {
		return build(Config.class);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Configuration name.
	 *
	 * <p>
	 * Specifies the configuration name.
	 * <br>This is typically the configuration file name, although
	 * the name can be anything identifiable by the {@link ConfigStore} used for retrieving and storing the configuration.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"Configuration"</js>.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder name(String value) {
		return set(CONFIG_name, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Configuration store.
	 *
	 * <p>
	 * The configuration store used for retrieving and storing configurations.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link ConfigFileStore#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder store(ConfigStore value) {
		return set(CONFIG_store, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Configuration store.
	 *
	 * <p>
	 * Convenience method for calling <code>store(ConfigMemoryStore.<jsf>DEFAULT</jsf>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder memStore() {
		return set(CONFIG_store, ConfigMemoryStore.DEFAULT);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  POJO serializer.
	 *
	 * <p>
	 * The serializer to use for serializing POJO values.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link SimpleJsonSerializer#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder serializer(WriterSerializer value) {
		return set(CONFIG_serializer, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  POJO serializer.
	 *
	 * <p>
	 * The serializer to use for serializing POJO values.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link SimpleJsonSerializer#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder serializer(Class<? extends WriterSerializer> value) {
		return set(CONFIG_serializer, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  POJO parser.
	 *
	 * <p>
	 * The parser to use for parsing values to POJOs.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder parser(ReaderParser value) {
		return set(CONFIG_parser, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  POJO parser.
	 *
	 * <p>
	 * The parser to use for parsing values to POJOs.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder parser(Class<? extends ReaderParser> value) {
		return set(CONFIG_parser, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Value encoder.
	 *
	 * <p>
	 * The encoder to use for encoding encoded configuration values.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link ConfigXorEncoder#INSTANCE}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder encoder(ConfigEncoder value) {
		return set(CONFIG_encoder, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Value encoder.
	 *
	 * <p>
	 * The encoder to use for encoding encoded configuration values.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link ConfigXorEncoder#INSTANCE}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder encoder(Class<? extends ConfigEncoder> value) {
		return set(CONFIG_encoder, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  SVL variable resolver.
	 *
	 * <p>
	 * The resolver to use for resolving SVL variables.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link VarResolver#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder varResolver(VarResolver value) {
		return set(CONFIG_varResolver, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  SVL variable resolver.
	 *
	 * <p>
	 * The resolver to use for resolving SVL variables.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link VarResolver#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder varResolver(Class<? extends VarResolver> value) {
		return set(CONFIG_varResolver, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Binary value line length.
	 *
	 * <p>
	 * When serializing binary values, lines will be split after this many characters.
	 * <br>Use <c>-1</c> to represent no line splitting.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <c>-1</c>.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder binaryLineLength(int value) {
		return set(CONFIG_binaryLineLength, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Binary value format.
	 *
	 * <p>
	 * The format to use when persisting byte arrays.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>{@link BinaryFormat#BASE64} - BASE64-encoded string.
	 * 	<li>{@link BinaryFormat#HEX} - Hexadecimal.
	 * 	<li>{@link BinaryFormat#SPACED_HEX} - Hexadecimal with spaces between bytes.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link BinaryFormat#BASE64}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder binaryFormat(BinaryFormat value) {
		return set(CONFIG_binaryFormat, value);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Multi-line values on separate lines.
	 *
	 * <p>
	 * When enabled, multi-line values will always be placed on a separate line from the key.
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder multiLineValuesOnSeparateLines() {
		return set(CONFIG_multiLineValuesOnSeparateLines);
	}

	/**
	 * <i><l>Config</l> configuration property:&emsp;</i>  Beans on separate lines.
	 *
	 * <p>
	 * When enabled, attempts to call any setters on this object will throw an {@link UnsupportedOperationException}.
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder readOnly() {
		return set(CONFIG_readOnly);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	// </FluentSetters>
}
