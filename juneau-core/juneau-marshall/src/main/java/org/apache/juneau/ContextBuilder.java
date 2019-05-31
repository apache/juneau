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
package org.apache.juneau;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.csv.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.jso.annotation.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.msgpack.annotation.*;
import org.apache.juneau.oapi.annotation.*;
import org.apache.juneau.parser.annotation.*;
import org.apache.juneau.plaintext.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.soap.annotation.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.annotation.*;
import org.apache.juneau.urlencoding.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Builder class for building instances of serializers and parsers.
 */
public abstract class ContextBuilder {

	/** Contains all the modifiable settings for the implementation class. */
	protected final PropertyStoreBuilder psb;

	/**
	 * Constructor.
	 * Default settings.
	 */
	public ContextBuilder() {
		this.psb = PropertyStore.create();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public ContextBuilder(PropertyStore ps) {
		if (ps == null)
			ps = PropertyStore.DEFAULT;
		this.psb = ps.builder();
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Used in cases where multiple context builder are sharing the same property store builder.
	 * <br>(e.g. <code>HtlmlDocBuilder</code>)
	 *
	 * @param psb The property store builder to use.
	 */
	protected ContextBuilder(PropertyStoreBuilder psb) {
		this.psb = psb;
	}

	/**
	 * Returns access to the inner property store builder.
	 *
	 * <p>
	 * Used in conjunction with {@link #ContextBuilder(PropertyStoreBuilder)} when builders share property store builders.
	 *
	 * @return The inner property store builder.
	 */
	protected PropertyStoreBuilder getPropertyStoreBuilder() {
		return psb;
	}

	/**
	 * Build the object.
	 *
	 * @return The built object.
	 * Subsequent calls to this method will create new instances.
	 */
	public abstract Context build();

	/**
	 * Copies the settings from the specified property store into this builder.
	 *
	 * @param copyFrom The factory whose settings are being copied.
	 * @return This object (for method chaining).
	 */
	public ContextBuilder apply(PropertyStore copyFrom) {
		this.psb.apply(copyFrom);
		return this;
	}

	/**
	 * Applies a set of annotations to this property store.
	 *
	 * @param al The list of all annotations annotated with {@link PropertyStoreApply}.
	 * @param r The string resolver for resolving variables in annotation values.
	 * @return This object (for method chaining).
	 */
	public ContextBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		this.psb.applyAnnotations(al, r);
		return this;
	}

	/**
	 * Applies any of the various <ja>@XConfig</ja> annotations on the specified class to this context.
	 *
	 * <p>
	 * Applies any of the following annotations:
	 * <ul class='doctree'>
	 * 	<li class ='ja'>{@link BeanConfig}
	 * 	<li class ='ja'>{@link CsvConfig}
	 * 	<li class ='ja'>{@link HtmlConfig}
	 * 	<li class ='ja'>{@link HtmlDocConfig}
	 * 	<li class ='ja'>{@link JsoConfig}
	 * 	<li class ='ja'>{@link JsonConfig}
	 * 	<li class ='ja'>{@link JsonSchemaConfig}
	 * 	<li class ='ja'>{@link MsgPackConfig}
	 * 	<li class ='ja'>{@link OpenApiConfig}
	 * 	<li class ='ja'>{@link ParserConfig}
	 * 	<li class ='ja'>{@link PlainTextConfig}
	 * 	<li class ='ja'>{@link SerializerConfig}
	 * 	<li class ='ja'>{@link SoapXmlConfig}
	 * 	<li class ='ja'>{@link UonConfig}
	 * 	<li class ='ja'>{@link UrlEncodingConfig}
	 * 	<li class ='ja'>{@link XmlConfig}
	 * 	<li class ='ja'><code>RdfConfig</code>
	 * </ul>
	 *
	 * <p>
	 * Annotations are appended in the following order:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param fromClass The class on which the annotations are defined.
	 * @return This object (for method chaining).
	 */
	public ContextBuilder applyAnnotations(Class<?> fromClass) {
		applyAnnotations(ClassInfo.of(fromClass).getAnnotationListParentFirst(ConfigAnnotationFilter.INSTANCE), VarResolver.DEFAULT.createSession());
		return this;
	}

	/**
	 * Applies any of the various <ja>@XConfig</ja> annotations on the specified method to this context.
	 *
	 * <p>
	 * Applies any of the following annotations:
	 * <ul class='doctree'>
	 * 	<li class ='ja'>{@link BeanConfig}
	 * 	<li class ='ja'>{@link CsvConfig}
	 * 	<li class ='ja'>{@link HtmlConfig}
	 * 	<li class ='ja'>{@link HtmlDocConfig}
	 * 	<li class ='ja'>{@link JsoConfig}
	 * 	<li class ='ja'>{@link JsonConfig}
	 * 	<li class ='ja'>{@link JsonSchemaConfig}
	 * 	<li class ='ja'>{@link MsgPackConfig}
	 * 	<li class ='ja'>{@link OpenApiConfig}
	 * 	<li class ='ja'>{@link ParserConfig}
	 * 	<li class ='ja'>{@link PlainTextConfig}
	 * 	<li class ='ja'>{@link SerializerConfig}
	 * 	<li class ='ja'>{@link SoapXmlConfig}
	 * 	<li class ='ja'>{@link UonConfig}
	 * 	<li class ='ja'>{@link UrlEncodingConfig}
	 * 	<li class ='ja'>{@link XmlConfig}
	 * 	<li class ='ja'><code>RdfConfig</code>
	 * </ul>
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of the method class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On the method class.
	 * 	<li>On this method and matching methods ordered parent-to-child.
	 * </ol>
	 *
	 * @param fromMethod The method on which the annotations are defined.
	 * @return This object (for method chaining).
	 */
	public ContextBuilder applyAnnotations(Method fromMethod) {
		applyAnnotations(MethodInfo.of(fromMethod).getAnnotationListParentFirst(ConfigAnnotationFilter.INSTANCE), VarResolver.DEFAULT.createSession());
		return this;
	}

	/**
	 * Build a new instance of the specified object.
	 *
	 * @param c The subclass of {@link Context} to instantiate.
	 * @return A new object using the settings defined in this builder.
	 */

	public <T extends Context> T build(Class<T> c) {
		return ContextCache.INSTANCE.create(c, getPropertyStore());
	}

	/**
	 * Returns a read-only snapshot of the current property store on this builder.
	 *
	 * @return A property store object.
	 */
	public PropertyStore getPropertyStore() {
		return psb.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets a configuration property on this object.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#set(String, Object)
	 */
	public ContextBuilder set(String name, Object value) {
		psb.set(name, value);
		return this;
	}

	/**
	 * Peeks at a configuration property on this object.
	 *
	 * @param key The property name.
	 * @return This object (for method chaining).
	 */
	public Object peek(String key) {
		return psb.peek(key);
	}

	/**
	 * Peeks at a configuration property on this object.
	 *
	 * @param c The type to convert to.
	 * @param key The property name.
	 * @return This object (for method chaining).
	 * @param <T>
	 */
	public <T> T peek(Class<T> c, String key) {
		return psb.peek(c, key);
	}

	/**
	 * Sets multiple configuration properties on this object.
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#set(java.util.Map)
	 */
	public ContextBuilder set(Map<String,Object> properties) {
		psb.set(properties);
		return this;
	}

	/**
	 * Adds multiple configuration properties on this object.
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#add(java.util.Map)
	 */
	public ContextBuilder add(Map<String,Object> properties) {
		psb.add(properties);
		return this;
	}

	/**
	 * Adds a value to a SET or LIST property.
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public ContextBuilder addTo(String name, Object value) {
		psb.addTo(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a SET, LIST, or MAP property.
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public ContextBuilder addTo(String name, String key, Object value) {
		psb.addTo(name, key, value);
		return this;
	}

	/**
	 * Removes a value from a SET, LIST, or MAP property.
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public ContextBuilder removeFrom(String name, Object value) {
		psb.removeFrom(name, value);
		return this;
	}
}