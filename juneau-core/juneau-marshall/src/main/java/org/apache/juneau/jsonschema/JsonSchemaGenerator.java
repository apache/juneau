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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
	 * <p>
	 * The description is the result of calling {@link ClassMeta#getFullName()}.
	 * <p>
	 * The format is a comma-delimited list of any of the following values:
	 *
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link TypeCategory#BEAN BEAN}
	 * 	<li class='jf'>{@link TypeCategory#COLLECTION COLLECTION}
	 * 	<li class='jf'>{@link TypeCategory#ARRAY ARRAY}
	 * 	<li class='jf'>{@link TypeCategory#MAP MAP}
	 * 	<li class='jf'>{@link TypeCategory#STRING STRING}
	 * 	<li class='jf'>{@link TypeCategory#NUMBER NUMBER}
	 * 	<li class='jf'>{@link TypeCategory#BOOLEAN BOOLEAN}
	 * 	<li class='jf'>{@link TypeCategory#ANY ANY}
	 * 	<li class='jf'>{@link TypeCategory#OTHER OTHER}
	 * </ul>
	 */
	public static final String JSONSCHEMA_addDescriptionsTo = PREFIX + ".addDescriptionsTo.s";

	/**
	 * Configuration property:  Add examples.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies which categories of types that examples should be automatically added to generated schemas.
	 * <p>
	 * The examples come from calling {@link ClassMeta#getExample(BeanSession)} which in turn gets examples
	 * from the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Example}
	 * 	<li class='jf'>{@link BeanContext#BEAN_examples}
	 * </ul>
	 *
	 * <p>
	 * The format is a comma-delimited list of any of the following values:
	 *
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link TypeCategory#BEAN BEAN}
	 * 	<li class='jf'>{@link TypeCategory#COLLECTION COLLECTION}
	 * 	<li class='jf'>{@link TypeCategory#ARRAY ARRAY}
	 * 	<li class='jf'>{@link TypeCategory#MAP MAP}
	 * 	<li class='jf'>{@link TypeCategory#STRING STRING}
	 * 	<li class='jf'>{@link TypeCategory#NUMBER NUMBER}
	 * 	<li class='jf'>{@link TypeCategory#BOOLEAN BOOLEAN}
	 * 	<li class='jf'>{@link TypeCategory#ANY ANY}
	 * 	<li class='jf'>{@link TypeCategory#OTHER OTHER}
	 * </ul>
	 */
	public static final String JSONSCHEMA_addExamplesTo = PREFIX + ".addExamplesTo.s";

	/**
	 * Configuration property:  Allow nested descriptions.
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
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#allowNestedDescriptions(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#allowNestedDescriptions()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies whether nested descriptions are allowed in schema definitions.
	 */
	public static final String JSONSCHEMA_allowNestedDescriptions = PREFIX + ".allowNestedDescriptions.b";

	/**
	 * Configuration property:  Allow nested examples.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies whether nested examples are allowed in schema definitions.
	 */
	public static final String JSONSCHEMA_allowNestedExamples = PREFIX + ".allowNestedExamples.b";

	/**
	 * Configuration property:  Bean schema definition mapper.
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
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Interface to use for converting Bean classes to definition IDs and URIs.
	 * <p>
	 * Used primarily for defining common definition sections for beans in Swagger JSON.
	 * <p>
	 * This setting is ignored if {@link #JSONSCHEMA_useBeanDefs} is not enabled.
	 */
	public static final String JSONSCHEMA_beanDefMapper = PREFIX + ".beanDefMapper.o";

	/**
	 * Configuration property:  Default schemas.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jsonschema.JsonSchemaGenerator#JSONSCHEMA_defaultSchemas JSONSCHEMA_defaultSchemas}
	 * 	<li><b>Name:</b>  <js>"JsonSchemaGenerator.defaultSchema.smo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,{@link org.apache.juneau.collections.OMap}&gt;</c>
	 * 	<li><b>System property:</b>  <c>JsonSchemaGenerator.defaultSchema</c>
	 * 	<li><b>Environment variable:</b>  <c>JSONSCHEMAGENERATOR_DEFAULTSCHEMA</c>
	 * 	<li><b>Default:</b>  Empty map.
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jsonschema.annotation.JsonSchemaConfig#defaultSchemas()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#defaultSchema(Class,OMap)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to override or provide custom schema information for particular class types.
	 * <p>
	 * Keys are full class names.
	 */
	public static final String JSONSCHEMA_defaultSchemas = PREFIX + ".defaultSchemas.smo";

	/**
	 * Configuration property:  Ignore types from schema definitions.
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
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines class name patterns that should be ignored when generating schema definitions in the generated
	 * Swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Don't generate schema for any prototype packages or the class named 'Swagger'.</jc>
	 * 	<ja>@Rest</ja>(
	 * 			properties={
	 * 				<ja>@Property</ja>(name=<jsf>JSONSCHEMA_ignoreTypes</jsf>, value=<js>"Swagger,*.proto.*"</js>)
	 * 			}
	 * 	<jk>public class</jk> MyResource {...}
	 * </p>
	 */
	public static final String JSONSCHEMA_ignoreTypes = PREFIX + ".ignoreTypes.s";

	/**
	 * Configuration property:  Use bean definitions.
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
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#useBeanDefs(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGeneratorBuilder#useBeanDefs()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, schemas on beans will be serialized as the following:
	 * <p class='bcode w800'>
	 * 	{
	 * 		type: <js>'object'</js>,
	 * 		<js>'$ref'</js>: <js>'#/definitions/TypeId'</js>
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The definitions can then be retrieved from the session using {@link JsonSchemaGeneratorSession#getBeanDefs()}.
	 * <p>
	 * Definitions can also be added programmatically using {@link JsonSchemaGeneratorSession#addBeanDef(String, OMap)}.
	 */
	public static final String JSONSCHEMA_useBeanDefs = PREFIX + ".useBeanDefs.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings.*/
	public static final JsonSchemaGenerator DEFAULT = new JsonSchemaGenerator(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean useBeanDefs, allowNestedExamples, allowNestedDescriptions;
	private final BeanDefMapper beanDefMapper;
	private final Set<TypeCategory> addExamplesTo, addDescriptionsTo;
	private final Map<String,OMap> defaultSchemas;
	private final JsonSerializer jsonSerializer;
	private final Set<Pattern> ignoreTypes;
	private final Map<ClassMeta<?>,JsonSchemaClassMeta> jsonSchemaClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,JsonSchemaBeanPropertyMeta> jsonSchemaBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps Initialize with the specified config property store.
	 */
	public JsonSchemaGenerator(PropertyStore ps) {
		super(ps.builder().setDefault(BEANTRAVERSE_detectRecursions, true).setDefault(BEANTRAVERSE_ignoreRecursions, true).build());

		useBeanDefs = getBooleanProperty(JSONSCHEMA_useBeanDefs, false);
		allowNestedExamples = getBooleanProperty(JSONSCHEMA_allowNestedExamples, false);
		allowNestedDescriptions = getBooleanProperty(JSONSCHEMA_allowNestedDescriptions, false);
		beanDefMapper = getInstanceProperty(JSONSCHEMA_beanDefMapper, BeanDefMapper.class, BasicBeanDefMapper.class);
		addExamplesTo = TypeCategory.parse(getStringProperty(JSONSCHEMA_addExamplesTo, null));
		addDescriptionsTo = TypeCategory.parse(getStringProperty(JSONSCHEMA_addDescriptionsTo, null));
		defaultSchemas = getMapProperty(JSONSCHEMA_defaultSchemas, OMap.class);

		Set<Pattern> ignoreTypes = new LinkedHashSet<>();
		for (String s : split(ps.getProperty(JSONSCHEMA_ignoreTypes, String.class, "")))
			ignoreTypes.add(Pattern.compile(s.replace(".", "\\.").replace("*", ".*")));
		this.ignoreTypes = ignoreTypes;

		jsonSerializer = new JsonSerializer(ps);
	}

	@Override /* Context */
	public JsonSchemaGeneratorBuilder builder() {
		return new JsonSchemaGeneratorBuilder(getPropertyStore());
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
	 * Configuration property:  Add descriptions to types.
	 *
	 * @see #JSONSCHEMA_addDescriptionsTo
	 * @return
	 * 	Set of categories of types that descriptions should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddDescriptionsTo() {
		return addDescriptionsTo;
	}

	/**
	 * Configuration property:  Add examples.
	 *
	 * @see #JSONSCHEMA_addExamplesTo
	 * @return
	 * 	Set of categories of types that examples should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddExamplesTo() {
		return addExamplesTo;
	}

	/**
	 * Configuration property:  Allow nested descriptions.
	 *
	 * @see #JSONSCHEMA_allowNestedDescriptions
	 * @return
	 * 	<jk>true</jk> if nested descriptions are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedDescriptions() {
		return allowNestedDescriptions;
	}

	/**
	 * Configuration property:  Allow nested examples.
	 *
	 * @see #JSONSCHEMA_allowNestedExamples
	 * @return
	 * 	<jk>true</jk> if nested examples are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedExamples() {
		return allowNestedExamples;
	}

	/**
	 * Configuration property:  Bean schema definition mapper.
	 *
	 * @see #JSONSCHEMA_beanDefMapper
	 * @return
	 * 	Interface to use for converting Bean classes to definition IDs and URIs.
	 */
	protected final BeanDefMapper getBeanDefMapper() {
		return beanDefMapper;
	}

	/**
	 * Configuration property:  Default schemas.
	 *
	 * @see #JSONSCHEMA_defaultSchemas
	 * @return
	 * 	Custom schema information for particular class types.
	 */
	protected final Map<String,OMap> getDefaultSchemas() {
		return defaultSchemas;
	}

	/**
	 * Configuration property:  Ignore types from schema definitions.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_ignoreTypes
	 * @return
	 * 	Custom schema information for particular class types.
	 */
	public Set<Pattern> getIgnoreTypes() {
		return ignoreTypes;
	}

	/**
	 * Configuration property:  Use bean definitions.
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
			.a("JsonSchemaGenerator", new DefaultFilteringOMap()
				.a("useBeanDefs", useBeanDefs)
				.a("allowNestedExamples", allowNestedExamples)
				.a("allowNestedDescriptions", allowNestedDescriptions)
				.a("beanDefMapper", beanDefMapper)
				.a("addExamplesTo", addExamplesTo)
				.a("addDescriptionsTo", addDescriptionsTo)
				.a("defaultSchemas", defaultSchemas)
				.a("ignoreTypes", ignoreTypes)
			);
	}
}
