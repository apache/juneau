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
package org.apache.juneau.jsonschema.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.parser.*;

/**
 * Annotation for specifying config properties defined in {@link JsonSchemaGenerator}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.JsonSchemaDetails">JSON-Schema Support</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply(JsonSchemaConfigAnnotation.Apply.class)
public @interface JsonSchemaConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// JsonSchemaGenerator
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Add descriptions to types.
	 *
	 * <p>
	 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
	 *
	 * <p>
	 * The description is the result of calling {@link ClassMeta#getFullName()}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a comma-delimited list of any of the following values:
	 * 		<ul class='doctree'>
	 * 			<li><js>"BEAN"</js>
	 * 			<li><js>"COLLECTION"</js>
	 * 			<li><js>"ARRAY"</js>
	 * 			<li><js>"MAP"</js>
	 * 			<li><js>"STRING"</js>
	 * 			<li><js>"NUMBER"</js>
	 * 			<li><js>"BOOLEAN"</js>
	 * 			<li><js>"ANY"</js>
	 * 			<li><js>"OTHER"</js>
	 * 		</ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addDescriptionsTo() default "";

	/**
	 * Add examples.
	 *
	 * <p>
	 * Identifies which categories of types that examples should be automatically added to generated schemas.
	 * <p>
	 * The examples come from calling {@link ClassMeta#getExample(BeanSession,JsonParserSession)} which in turn gets examples
	 * from the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Example}
	 * 	<li class='ja'>{@link Marshalled#example() Marshalled(example)}
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a comma-delimited list of any of the following values:
	 * 		<ul class='doctree'>
	 * 			<li><js>"BEAN"</js>
	 * 			<li><js>"COLLECTION"</js>
	 * 			<li><js>"ARRAY"</js>
	 * 			<li><js>"MAP"</js>
	 * 			<li><js>"STRING"</js>
	 * 			<li><js>"NUMBER"</js>
	 * 			<li><js>"BOOLEAN"</js>
	 * 			<li><js>"ANY"</js>
	 * 			<li><js>"OTHER"</js>
	 * 		</ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addExamplesTo() default "";

	/**
	 * Allow nested descriptions.
	 *
	 * <p>
	 * Identifies whether nested descriptions are allowed in schema definitions.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedDescriptions()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String allowNestedDescriptions() default "";

	/**
	 * Allow nested examples.
	 *
	 * <p>
	 * Identifies whether nested examples are allowed in schema definitions.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedExamples()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String allowNestedExamples() default "";

	/**
	 * Bean schema definition mapper.
	 *
	 * <p>
	 * Interface to use for converting Bean classes to definition IDs and URIs.
	 *
	 * <p>
	 * Used primarily for defining common definition sections for beans in Swagger JSON.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		This setting is ignored if {@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#useBeanDefs()} is not enabled.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#beanDefMapper(Class)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends BeanDefMapper> beanDefMapper() default BeanDefMapper.Void.class;

	/**
	 * Ignore types from schema definitions.
	 *
	 * <p>
	 * Defines class name patterns that should be ignored when generating schema definitions in the generated
	 * Swagger documentation.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: Comma-delimited list of patterns
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#ignoreTypes(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreTypes() default "";

	/**
	 * Use bean definitions.
	 *
	 * <p>
	 * When enabled, schemas on beans will be serialized as the following:
	 * <p class='bjson'>
	 * 	{
	 * 		type: <js>'object'</js>,
	 * 		<js>'$ref'</js>: <js>'#/definitions/TypeId'</js>
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The definitions can then be retrieved from the session using {@link JsonSchemaGeneratorSession#getBeanDefs()}.
	 *
	 * <p>
	 * Definitions can also be added programmatically using {@link JsonSchemaGeneratorSession#addBeanDef(String, JsonMap)}.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#useBeanDefs()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useBeanDefs() default "";

	//-----------------------------------------------------------------------------------------------------------------
	// BeanTraverseContext
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Automatically detect POJO recursions.
	 *
	 * <p>
	 * Specifies that recursions should be checked for during traversal.
	 *
	 * <p>
	 * Recursions can occur when traversing models that aren't true trees but rather contain loops.
	 * <br>In general, unchecked recursions cause stack-overflow-errors.
	 * <br>These show up as {@link ParseException ParseExceptions} with the message <js>"Depth too deep.  Stack overflow occurred."</js>.
	 *
	 * <p>
	 * The behavior when recursions are detected depends on the value for {@link org.apache.juneau.BeanTraverseContext.Builder#ignoreRecursions()}.
	 *
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * 	the following when <jsf>BEANTRAVERSE_ignoreRecursions</jsf> is <jk>true</jk>...
	 *
	 * <p class='bjson'>
	 * 	{A:{B:{C:<jk>null</jk>}}}
	 * </p>
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='warn'>
	 * 		Checking for recursion can cause a small performance penalty.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#detectRecursions()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String detectRecursions() default "";

	/**
	 * Ignore recursion errors.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanTraverseContext.Builder#detectRecursions()}.
	 * <br>Setting is ignored if <jsf>BEANTRAVERSE_detectRecursions</jsf> is <js>"false"</js>.
	 *
	 * <p>
	 * If <js>"true"</js>, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * <br>Otherwise, a {@link BeanRecursionException} is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#ignoreRecursions()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreRecursions() default "";

	/**
	 * Initial depth.
	 *
	 * <p>
	 * The initial indentation level at the root.
	 * <br>Useful when constructing document fragments that need to be indented at a certain level.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: integer
	 *	<li class='note'>
	 * 		Default value: <js>"0"</js>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#initialDepth(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String initialDepth() default "";

	/**
	 * Max traversal depth.
	 *
	 * <p>
	 * Abort traversal if specified depth is reached in the POJO tree.
	 * <br>If this depth is exceeded, an exception is thrown.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Format: integer
	 * 	<li class='note'>
	 * 		Default value: <js>"100"</js>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanTraverseContext.Builder#maxDepth(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String maxDepth() default "";
}
