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

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;

/**
 * Generates JSON-schema metadata about POJOs.
 * {@review}
 */
@ConfigurableContext
public class JsonSchemaGenerator extends BeanTraverseContext implements JsonSchemaMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "JsonSchemaGenerator";

	/**
	 * Configuration property:  Add descriptions to types.
	 *
	 * <p>
	 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
	 * The description is the result of calling {@link ClassMeta#getFullName()}.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jsonschema.JsonSchemaGenerator#JSONSCHEMA_addDescriptionsTo JSONSCHEMA_addDescriptionsTo}
	 * 	<li><b>Name:</b>  <js>"JsonSchemaGenerator.addDescriptionsTo.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>JsonSchemaGenerator.addDescriptionsTo</c>
	 * 	<li><b>Environment variable:</b>  <c>JSONSCHEMAGENERATOR_ADDDESCRIPTIONSTO</c>
	 * 	<li><b>Default:</b>  Empty string.
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.JsonSchemaConfig#addDescriptionsTo()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#addDescriptionsTo(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String JSONSCHEMA_addDescriptionsTo = PREFIX + ".addDescriptionsTo.s";

	/**
	 * Configuration property:  Add examples.
	 *
	 * <p>
	 * Identifies which categories of types that examples should be automatically added to generated schemas.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jsonschema.JsonSchemaGenerator#JSONSCHEMA_addExamplesTo JSONSCHEMA_addExamplesTo}
	 * 	<li><b>Name:</b>  <js>"JsonSchemaGenerator.addExamplesTo.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>JsonSchemaGenerator.addExamplesTo</c>
	 * 	<li><b>Environment variable:</b>  <c>JSONSCHEMAGENERATOR_ADDEXAMPLESTO</c>
	 * 	<li><b>Default:</b>  Empty string.
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.JsonSchemaConfig#addExamplesTo()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#addExamplesTo(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String JSONSCHEMA_addExamplesTo = PREFIX + ".addExamplesTo.s";

	/**
	 * Configuration property:  Allow nested descriptions.
	 *
	 * <p>
	 * Identifies whether nested descriptions are allowed in schema definitions.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jsonschema.JsonSchemaGenerator#JSONSCHEMA_allowNestedDescriptions JSONSCHEMA_allowNestedDescriptions}
	 * 	<li><b>Name:</b>  <js>"JsonSchemaGenerator.allowNestedDescriptions.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>JsonSchemaGenerator.allowNestedDescriptions</c>
	 * 	<li><b>Environment variable:</b>  <c>JSONSCHEMAGENERATOR_ALLOWNESTEDDESCRIPTIONS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.JsonSchemaConfig#allowNestedDescriptions()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#allowNestedDescriptions()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String JSONSCHEMA_allowNestedDescriptions = PREFIX + ".allowNestedDescriptions.b";

	/**
	 * Configuration property:  Allow nested examples.
	 *
	 * <p>
	 * Identifies whether nested examples are allowed in schema definitions.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jsonschema.JsonSchemaGenerator#JSONSCHEMA_allowNestedExamples JSONSCHEMA_allowNestedExamples}
	 * 	<li><b>Name:</b>  <js>"JsonSchemaGenerator.allowNestedExamples.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>JsonSchemaGenerator.allowNestedExamples</c>
	 * 	<li><b>Environment variable:</b>  <c>JSONSCHEMAGENERATOR_ALLOWNESTEDEXAMPLES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.JsonSchemaConfig#allowNestedExamples()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#allowNestedExamples()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String JSONSCHEMA_allowNestedExamples = PREFIX + ".allowNestedExamples.b";

	/**
	 * Configuration property:  Bean schema definition mapper.
	 *
	 * <p>
	 * Interface to use for converting Bean classes to definition IDs and URIs.
	 * Used primarily for defining common definition sections for beans in Swagger JSON.
	 * This setting is ignored if {@link #JSONSCHEMA_useBeanDefs} is not enabled.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jsonschema.JsonSchemaGenerator#JSONSCHEMA_beanDefMapper JSONSCHEMA_beanDefMapper}
	 * 	<li><b>Name:</b>  <js>"JsonSchemaGenerator.beanDefMapper.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.jsonschema.BeanDefMapper}
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.jsonschema.BasicBeanDefMapper}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.JsonSchemaConfig#beanDefMapper()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#beanDefMapper(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#beanDefMapper(BeanDefMapper)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String JSONSCHEMA_beanDefMapper = PREFIX + ".beanDefMapper.o";

	/**
	 * Configuration property:  Ignore types from schema definitions.
	 *
	 * <p>
	 * Defines class name patterns that should be ignored when generating schema definitions in the generated
	 * Swagger documentation.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jsonschema.JsonSchemaGenerator#JSONSCHEMA_ignoreTypes JSONSCHEMA_ignoreTypes}
	 * 	<li><b>Name:</b>  <js>"JsonSchemaGenerator.ignoreTypes.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c> (comma-delimited)
	 * 	<li><b>System property:</b>  <c>JsonSchemaGenerator.ignoreTypes</c>
	 * 	<li><b>Environment variable:</b>  <c>JSONSCHEMAGENERATOR_IGNORETYPES</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>.
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.JsonSchemaConfig#ignoreTypes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#ignoreTypes(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String JSONSCHEMA_ignoreTypes = PREFIX + ".ignoreTypes.s";

	/**
	 * Configuration property:  Use bean definitions.
	 *
	 * <p>
	 * When enabled, schemas on beans will be serialized as the following:
	 * <p class='bcode w800'>
	 * 	{
	 * 		type: <js>'object'</js>,
	 * 		<js>'$ref'</js>: <js>'#/definitions/TypeId'</js>
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jsonschema.JsonSchemaGenerator#JSONSCHEMA_useBeanDefs JSONSCHEMA_useBeanDefs}
	 * 	<li><b>Name:</b>  <js>"JsonSchemaGenerator.useBeanDefs.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>JsonSchemaGenerator.useBeanDefs</c>
	 * 	<li><b>Environment variable:</b>  <c>JSONSCHEMAGENERATOR_USEBEANDEFS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.JsonSchemaConfig#useBeanDefs()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#useBeanDefs()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String JSONSCHEMA_useBeanDefs = PREFIX + ".useBeanDefs.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSchemaGenerator DEFAULT = new JsonSchemaGenerator(ContextProperties.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean useBeanDefs, allowNestedExamples, allowNestedDescriptions;
	private final BeanDefMapper beanDefMapper;
	private final Set<TypeCategory> addExamplesTo, addDescriptionsTo;
	private final JsonSerializer jsonSerializer;
	private final Set<Pattern> ignoreTypes;
	private final Map<ClassMeta<?>,JsonSchemaClassMeta> jsonSchemaClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonSchemaBeanPropertyMeta> jsonSchemaBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param cp Initialize with the specified config property store.
	 */
	public JsonSchemaGenerator(ContextProperties cp) {
		super(cp.copy().setDefault(BEANTRAVERSE_detectRecursions, true).setDefault(BEANTRAVERSE_ignoreRecursions, true).build());

		useBeanDefs = cp.getBoolean(JSONSCHEMA_useBeanDefs).orElse(false);
		allowNestedExamples = cp.getBoolean(JSONSCHEMA_allowNestedExamples).orElse(false);
		allowNestedDescriptions = cp.getBoolean(JSONSCHEMA_allowNestedDescriptions).orElse(false);
		beanDefMapper = cp.getInstance(JSONSCHEMA_beanDefMapper, BeanDefMapper.class).orElseGet(BasicBeanDefMapper::new);
		addExamplesTo = TypeCategory.parse(cp.getString(JSONSCHEMA_addExamplesTo).orElse(null));
		addDescriptionsTo = TypeCategory.parse(cp.getString(JSONSCHEMA_addDescriptionsTo).orElse(null));

		Set<Pattern> ignoreTypes = new LinkedHashSet<>();
		for (String s : split(cp.get(JSONSCHEMA_ignoreTypes, String.class).orElse("")))
			ignoreTypes.add(Pattern.compile(s.replace(".", "\\.").replace("*", ".*")));
		this.ignoreTypes = ignoreTypes;

		jsonSerializer = new JsonSerializer(cp);
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
		return createSession(createDefaultSessionArgs());
	}

	@Override
	public JsonSchemaGeneratorSession createSession(BeanSessionArgs args) {
		return new JsonSchemaGeneratorSession(this, args);
	}

	JsonSerializer getJsonSerializer() {
		return jsonSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add descriptions to types.
	 *
	 * @see #JSONSCHEMA_addDescriptionsTo
	 * @return
	 * 	Set of categories of types that descriptions should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddDescriptionsTo() {
		return addDescriptionsTo;
	}

	/**
	 * Add examples.
	 *
	 * @see #JSONSCHEMA_addExamplesTo
	 * @return
	 * 	Set of categories of types that examples should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddExamplesTo() {
		return addExamplesTo;
	}

	/**
	 * Allow nested descriptions.
	 *
	 * @see #JSONSCHEMA_allowNestedDescriptions
	 * @return
	 * 	<jk>true</jk> if nested descriptions are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedDescriptions() {
		return allowNestedDescriptions;
	}

	/**
	 * Allow nested examples.
	 *
	 * @see #JSONSCHEMA_allowNestedExamples
	 * @return
	 * 	<jk>true</jk> if nested examples are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedExamples() {
		return allowNestedExamples;
	}

	/**
	 * Bean schema definition mapper.
	 *
	 * @see #JSONSCHEMA_beanDefMapper
	 * @return
	 * 	Interface to use for converting Bean classes to definition IDs and URIs.
	 */
	protected final BeanDefMapper getBeanDefMapper() {
		return beanDefMapper;
	}

	/**
	 * Ignore types from schema definitions.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_ignoreTypes
	 * @return
	 * 	Custom schema information for particular class types.
	 */
	public Set<Pattern> getIgnoreTypes() {
		return ignoreTypes;
	}

	/**
	 * Use bean definitions.
	 *
	 * @see #JSONSCHEMA_useBeanDefs
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
	 * The type is ignored if it's specified in the {@link #JSONSCHEMA_ignoreTypes} setting.
	 * <br>Ignored types return <jk>null</jk> on the call to {@link JsonSchemaGeneratorSession#getSchema(ClassMeta)}.
	 *
	 * @param cm The type to check.
	 * @return <jk>true</jk> if the specified type is ignored.
	 */
	public boolean isIgnoredType(ClassMeta<?> cm) {
		for (Pattern p : ignoreTypes)
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
			);
	}
}
