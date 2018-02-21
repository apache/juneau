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
package org.apache.juneau.config.proto;

import static org.apache.juneau.config.proto.Config.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.config.encode.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Builder for creating instances of {@link Config Configs}.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	Config cf = Config.<jsm>create</jsm>().build(<js>"MyConfig.cfg"</js>);
 * 	String setting = cf.get(<js>"MySection/mysetting"</js>);
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-config'>Overview &gt; juneau-config</a>
 * </ul>
 */
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
	 * @param ps The initial configuration settings for this builder.
	 */
	public ConfigBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public Config build() {
		return build(Config.class);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Configuration property:  Configuration name.
	 * 
	 * <p>
	 * Specifies the configuration name.
	 * <br>This is typically the configuration file name minus the file extension, although
	 * the name can be anything identifiable by the {@link Store} used for retrieving and storing the configuration.
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
	 * Configuration property:  Configuration store.
	 * 
	 * <p>
	 * The configuration store used for retrieving and storing configurations.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link FileStore#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder store(Store value) {
		return set(CONFIG_store, value);
	}

	/**
	 * Configuration property:  POJO serializer.
	 * 
	 * <p>
	 * The serializer to use for serializing POJO values.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link JsonSerializer#DEFAULT_LAX}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder serializer(WriterSerializer value) {
		return set(CONFIG_serializer, value);
	}

	/**
	 * Configuration property:  POJO serializer.
	 * 
	 * <p>
	 * The serializer to use for serializing POJO values.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link JsonSerializer#DEFAULT_LAX}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder serializer(Class<? extends WriterSerializer> value) {
		return set(CONFIG_serializer, value);
	}

	/**
	 * Configuration property:  POJO parser.
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
	 * Configuration property:  POJO parser.
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
	 * Configuration property:  Value encoder.
	 * 
	 * <p>
	 * The encoder to use for encoding encoded configuration values.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link XorEncoder#INSTANCE}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder encoder(Encoder value) {
		return set(CONFIG_encoder, value);
	}

	/**
	 * Configuration property:  Value encoder.
	 * 
	 * <p>
	 * The encoder to use for encoding encoded configuration values.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is {@link XorEncoder#INSTANCE}.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder encoder(Class<? extends Encoder> value) {
		return set(CONFIG_encoder, value);
	}
	
	/**
	 * Configuration property:  SVL variable resolver.
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
	 * Configuration property:  SVL variable resolver.
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
	 * Configuration property:  Binary value line length.
	 * 
	 * <p>
	 * When serializing binary values, lines will be split after this many characters.
	 * <br>Use <code>-1</code> to represent no line splitting.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <code>-1</code>.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder binaryLineLength(int value) {
		return set(CONFIG_binaryLineLength, value);
	}
	
	/**
	 * Configuration property:  Binary value format.
	 * 
	 * <p>
	 * The format to use when persisting byte arrays.
	 * 
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li><js>"BASE64"</js> - BASE64-encoded string.
	 * 	<li><js>"HEX"</js> - Hexadecimal.
	 * 	<li><js>"SPACED_HEX"</js> - Hexadecimal with spaces between bytes.
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <js>"BASE64"</js>.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder binaryFormat(String value) {
		return set(CONFIG_binaryFormat, value);
	}

	/**
	 * Configuration property:  Beans on separate lines.
	 * 
	 * <p>
	 * When enabled, serialized POJOs will be placed on a separate line from the key.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public ConfigBuilder beansOnSeparateLines(boolean value) {
		return set(CONFIG_beansOnSeparateLines, value);
	}
	
	
	@Override /* ContextBuilder */
	public ConfigBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ConfigBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ConfigBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public ConfigBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public ConfigBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ConfigBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ConfigBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ConfigBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}
