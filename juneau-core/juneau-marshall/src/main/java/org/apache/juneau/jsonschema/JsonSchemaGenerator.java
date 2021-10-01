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
package org.apache.juneau.jsonschema;

import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Collections.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;

/**
 * Generates JSON-schema metadata about POJOs.
 * {@review}
 */
public class JsonSchemaGenerator extends BeanTraverseContext implements JsonSchemaMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSchemaGenerator DEFAULT = new JsonSchemaGenerator(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean useBeanDefs, allowNestedExamples, allowNestedDescriptions;
	final Set<TypeCategory> addExamplesTo, addDescriptionsTo;
	final Class<? extends BeanDefMapper> beanDefMapper;
	final Set<String> ignoreTypes;

	private final BeanDefMapper beanDefMapperBean;
	final JsonSerializer jsonSerializer;
	final JsonParser jsonParser;
	private final Set<Pattern> ignoreTypePatterns;
	private final Map<ClassMeta<?>,JsonSchemaClassMeta> jsonSchemaClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonSchemaBeanPropertyMeta> jsonSchemaBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonSchemaGenerator(JsonSchemaGeneratorBuilder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		useBeanDefs = builder.useBeanDefs;
		allowNestedExamples = builder.allowNestedExamples;
		allowNestedDescriptions = builder.allowNestedDescriptions;
		beanDefMapper = builder.beanDefMapper;
		addExamplesTo = builder.addExamplesTo == null ? emptySet() : new TreeSet<>(builder.addExamplesTo);
		addDescriptionsTo = builder.addDescriptionsTo == null ? emptySet() : new TreeSet<>(builder.addDescriptionsTo);
		ignoreTypes = builder.ignoreTypes == null ? emptySet() : new TreeSet<>(builder.ignoreTypes);

		Set<Pattern> ignoreTypePatterns = new LinkedHashSet<>();
		for (String s : ignoreTypes)
			for (String s2 : split(s))
				ignoreTypePatterns.add(Pattern.compile(s2.replace(".", "\\.").replace("*", ".*")));
		this.ignoreTypePatterns = ignoreTypePatterns;

		try {
			beanDefMapperBean = beanDefMapper.newInstance();
		} catch (Exception e) {
			throw runtimeException(e);
		}

		jsonSerializer = builder.jsonSerializerBuilder.build();
		jsonParser = (JsonParser) builder.jsonParserBuilder.beanContext(getBeanContext()).build();
	}

	@Override /* Context */
	public JsonSchemaGeneratorBuilder copy() {
		return new JsonSchemaGeneratorBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link JsonSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> JsonSerializerBuilder()</code>.
	 *
	 * @return A new {@link JsonSerializerBuilder} object.
	 */
	public static JsonSchemaGeneratorBuilder create() {
		return new JsonSchemaGeneratorBuilder();
	}

	@Override /* Context */
	public JsonSchemaGeneratorSession createSession() {
		return createSession(defaultArgs());
	}

	@Override
	public JsonSchemaGeneratorSession createSession(BeanSessionArgs args) {
		return new JsonSchemaGeneratorSession(this, args);
	}

	JsonSerializer getJsonSerializer() {
		return jsonSerializer;
	}

	JsonParser getJsonParser() {
		return jsonParser;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add descriptions to types.
	 *
	 * @see JsonSchemaGeneratorBuilder#addDescriptionsTo(TypeCategory...)
	 * @return
	 * 	Set of categories of types that descriptions should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddDescriptionsTo() {
		return addDescriptionsTo;
	}

	/**
	 * Add examples.
	 *
	 * @see JsonSchemaGeneratorBuilder#addExamplesTo(TypeCategory...)
	 * @return
	 * 	Set of categories of types that examples should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddExamplesTo() {
		return addExamplesTo;
	}

	/**
	 * Allow nested descriptions.
	 *
	 * @see JsonSchemaGeneratorBuilder#allowNestedDescriptions()
	 * @return
	 * 	<jk>true</jk> if nested descriptions are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedDescriptions() {
		return allowNestedDescriptions;
	}

	/**
	 * Allow nested examples.
	 *
	 * @see JsonSchemaGeneratorBuilder#allowNestedExamples()
	 * @return
	 * 	<jk>true</jk> if nested examples are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedExamples() {
		return allowNestedExamples;
	}

	/**
	 * Bean schema definition mapper.
	 *
	 * @see JsonSchemaGeneratorBuilder#beanDefMapper(Class)
	 * @return
	 * 	Interface to use for converting Bean classes to definition IDs and URIs.
	 */
	protected final BeanDefMapper getBeanDefMapper() {
		return beanDefMapperBean;
	}

	/**
	 * Ignore types from schema definitions.
	 *
	 * @see JsonSchemaGeneratorBuilder#ignoreTypes(String...)
	 * @return
	 * 	Custom schema information for particular class types.
	 */
	public Set<Pattern> getIgnoreTypes() {
		return ignoreTypePatterns;
	}

	/**
	 * Use bean definitions.
	 *
	 * @see JsonSchemaGeneratorBuilder#useBeanDefs()
	 * @return
	 * 	<jk>true</jk> if schemas on beans will be serialized with <js>'$ref'</js> tags.
	 */
	protected final boolean isUseBeanDefs() {
		return useBeanDefs;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	public JsonSchemaClassMeta getJsonSchemaClassMeta(ClassMeta<?> cm) {
		JsonSchemaClassMeta m = jsonSchemaClassMetas.get(cm);
		if (m == null) {
			m = new JsonSchemaClassMeta(cm, this);
			jsonSchemaClassMetas.put(cm, m);
		}
		return m;
	}

	@Override
	public JsonSchemaBeanPropertyMeta getJsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		JsonSchemaBeanPropertyMeta m = jsonSchemaBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new JsonSchemaBeanPropertyMeta(bpm, this);
			jsonSchemaBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if the specified type is ignored.
	 *
	 * <p>
	 * The type is ignored if it's specified in the {@link JsonSchemaGeneratorBuilder#ignoreTypes(String...)} setting.
	 * <br>Ignored types return <jk>null</jk> on the call to {@link JsonSchemaGeneratorSession#getSchema(ClassMeta)}.
	 *
	 * @param cm The type to check.
	 * @return <jk>true</jk> if the specified type is ignored.
	 */
	public boolean isIgnoredType(ClassMeta<?> cm) {
		for (Pattern p : ignoreTypePatterns)
			if (p.matcher(cm.getSimpleName()).matches() || p.matcher(cm.getName()).matches())
				return true;
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"JsonSchemaGenerator",
				OMap
					.create()
					.filtered()
					.a("useBeanDefs", useBeanDefs)
					.a("allowNestedExamples", allowNestedExamples)
					.a("allowNestedDescriptions", allowNestedDescriptions)
					.a("beanDefMapper", beanDefMapper)
					.a("addExamplesTo", addExamplesTo)
					.a("addDescriptionsTo", addDescriptionsTo)
					.a("ignoreTypes", ignoreTypes)
				)
				.a("JsonSerializer", jsonSerializer.toMap()
			);
	}
}
