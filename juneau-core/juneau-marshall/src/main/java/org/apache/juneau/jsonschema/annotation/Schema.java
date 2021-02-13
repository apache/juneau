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

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.oapi.*;

/**
 * Swagger schema annotation.
 *
 * <p>
 * The Schema Object allows the definition of input and output data types.
 * These types can be objects, but also primitives and arrays.
 * This object is based on the JSON Schema Specification Draft 4 and uses a predefined subset of it.
 * On top of this subset, there are extensions provided by this specification to allow for more complete documentation.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 * <br>Also used to define OpenAPI schema information for POJOs serialized through {@link OpenApiSerializer} and parsed through {@link OpenApiParser}.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// A response object thats a hex-encoded string</jc>
 * 	<ja>@Response</ja>(
 * 		schema=<ja>@Schema</ja>(
 * 			type=<js>"string"</js>,
 * 			format=<js>"binary"</js>
 * 		)
 * 	)
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Free-form</jc>
 * 	<ja>@Response</ja>(
 * 		schema=<ja>@Schema</ja>({
 * 			<js>"type:'string',"</js>,
 * 			<js>"format:'binary'"</js>
 * 		})
 * 	)
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// A request body consisting of an array of arrays, the internal array being of type integer, numbers must be between 0 and 63 (inclusive)</jc>
</jc>
 * 	<ja>@Body</ja>(
 * 		schema=<ja>@Schema</ja>(
 * 			items=<ja>@Items</ja>(
 * 				type=<js>"array"</js>,
 * 				items=<ja>@SubItems</ja>(
 * 					type=<js>"integer"</js>,
 * 					minimum=<js>"0"</js>,
 * 					maximum=<js>"63"</js>
 * 				)
 *			)
 * 		)
 * 	)
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerSchemaObject}
 * </ul>
 */
@Documented
@Retention(RUNTIME)
@Repeatable(SchemaAnnotation.Array.class)
@ContextPropertiesApply(SchemaAnnotation.Apply.class)
public @interface Schema {

	/**
	 * <mk>default</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is any {@doc SimplifiedJson}.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] _default() default {};

	/**
	 * <mk>enum</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} array or comma-delimited list.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] _enum() default {};

	/**
	 * <mk>$ref</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <p>
	 * 	A JSON reference to the schema definition.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a <a href='https://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03'>JSON Reference</a>.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String $ref() default "";

	/**
	 * <mk>additionalProperties</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] additionalProperties() default {};

	/**
	 * <mk>allOf</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] allOf() default {};

	/**
	 * Synonym for {@link #collectionFormat()}.
	 */
	String cf() default "";

	/**
	 * <mk>collectionFormat</mk> field.
	 *
	 * <p>
	 * Note that this field isn't part of the Swagger 2.0 specification, but the specification does not specify how
	 * items are supposed to be represented.
	 *
	 * <p>
	 * Determines the format of the array if <c>type</c> <js>"array"</js> is used.
	 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
	 *
	 * <br>Possible values are:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"csv"</js> (default) - Comma-separated values (e.g. <js>"foo,bar"</js>).
	 * 	<li>
	 * 		<js>"ssv"</js> - Space-separated values (e.g. <js>"foo bar"</js>).
	 * 	<li>
	 * 		<js>"tsv"</js> - Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 * 	<li>
	 * 		<js>"pipes</js> - Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 * 	<li>
	 * 		<js>"multi"</js> - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 * 	<li>
	 * 		<js>"uon"</js> - UON notation (e.g. <js>"@(foo,bar)"</js>).
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link CollectionFormatType}.
	 *
	 * <p>
	 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 */
	String collectionFormat() default "";

	/**
	 * Synonym for {@link #description()}.
	 */
	String[] d() default {};

	/**
	 * <mk>description</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <p>
	 * A brief description of the body. This could contain examples of use.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(description=<js>"Pet object to add to the store"</js>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(Pet <jv>input</jv>) {...}
	 *
	 * 	<ja>@Body</ja>(description=<js>"Pet object to add to the store"</js>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * Synonym for {@link #_default()}.
	 */
	String[] df() default {};

	/**
	 * <mk>discriminator</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String discriminator() default "";

	/**
	 * Synonym for {@link #_enum()}.
	 */
	String[] e() default {};

	/**
	 * Synonym for {@link #exclusiveMaximum()}.
	 */
	boolean emax() default false;

	/**
	 * Synonym for {@link #exclusiveMinimum()}.
	 */
	boolean emin() default false;

	/**
	 * Synonym for {@link #readOnly()}.
	 */
	String[] ex() default {};

	/**
	 * <mk>example</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <p>
	 * A free-form property to include an example of an instance for this schema.
	 *
	 * <p>
	 * This attribute defines a JSON representation of the body value that is used by <c>BasicRestInfoProvider</c> to construct
	 * media-type-based examples of the body of the request.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object or plain text string.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] example() default {};

	/**
	 * <mk>examples</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <p>
	 * This is a JSON object whose keys are media types and values are string representations of that value.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] examples() default {};

	/**
	 * <mk>exclusiveMaximum</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	boolean exclusiveMaximum() default false;

	/**
	 * <mk>exclusiveMinimum</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	boolean exclusiveMinimum() default false;

	/**
	 * Synonym for {@link #examples()}.
	 */
	String[] exs() default {};

	/**
	 * <mk>externalDocs</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	ExternalDocs externalDocs() default @ExternalDocs;

	/**
	 * Synonym for {@link #format()}.
	 */
	String f() default "";

	/**
	 * <mk>format</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPut</ja>
	 * 	<jk>public void</jk> setAge(
	 * 		<ja>@Body</ja>(type=<js>"integer"</js>, format=<js>"int32"</js>) String <jv>input</jv>
	 * 	) {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='extlink'>{@doc ExtSwaggerDataTypeFormats}
	 * </ul>
	 */
	String format() default "";

	/**
	 * Specifies that schema information for this part should not be shown in the generated Swagger documentation.
	 */
	boolean ignore() default false;

	/**
	 * <mk>items</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	Items items() default @Items;

	/**
	 * Synonym for {@link #maximum()}.
	 */
	String max() default "";

	/**
	 * Synonym for {@link #maxItems()}.
	 */
	long maxi() default -1;

	/**
	 * <mk>maximum</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String maximum() default "";

	/**
	 * <mk>maxItems</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	long maxItems() default -1;

	/**
	 * Synonym for {@link #maxLength()}.
	 */
	long maxl() default -1;

	/**
	 * <mk>maxLength</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	long maxLength() default -1;

	/**
	 * Synonym for {@link #maxProperties()}.
	 */
	long maxp() default -1;

	/**
	 * <mk>maxProperties</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	long maxProperties() default -1;

	/**
	 * Synonym for {@link #minimum()}.
	 */
	String min() default "";

	/**
	 * Synonym for {@link #minItems()}.
	 */
	long mini() default -1;

	/**
	 * <mk>minimum</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String minimum() default "";

	/**
	 * <mk>minItems</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	long minItems() default -1;

	/**
	 * Synonym for {@link #minLength()}.
	 */
	long minl() default -1;

	/**
	 * <mk>minLength</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	long minLength() default -1;

	/**
	 * Synonym for {@link #minProperties()}.
	 */
	long minp() default -1;

	/**
	 * <mk>minProperties</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	long minProperties() default -1;

	/**
	 * Synonym for {@link #multipleOf()}.
	 */
	String mo() default "";

	/**
	 * <mk>multipleOf</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String multipleOf() default "";

	/**
	 * Dynamically apply this annotation to the specified classes/methods/fields.
	 *
	 * <p>
	 * Used in conjunction with {@link BeanContextBuilder#applyAnnotations(Class...)} to dynamically apply an annotation to an existing class/method/field.
	 * It is ignored when the annotation is applied directly to classes/methods/fields.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
	 *  <li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass"</js>
	 * 				</ul>
	 * 			<li>Fully qualified inner class:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass"</js>
	 * 				</ul>
	 * 			<li>Simple inner:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2"</js>
	 * 					<li><js>"Inner1$Inner2"</js>
	 * 					<li><js>"Inner2"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple with args:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner2.myMethod"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner2.myField"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	Class<?>[] onClass() default {};

	/**
	 * Synonym for {@link #pattern()}.
	 */
	String p() default "";

	/**
	 * <mk>pattern</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestPut</ja>
	 * 	<jk>public void</jk> doPut(<ja>@Body</ja>(format=<js>"/\\w+\\.\\d+/"</js>) String <jv>input<jv>) {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		This string SHOULD be a valid regular expression.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String pattern() default "";

	/**
	 * <mk>properties</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] properties() default {};

	/**
	 * Synonym for {@link #required()}.
	 */
	boolean r() default false;

	/**
	 * <mk>readOnly</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	boolean readOnly() default false;

	/**
	 * <mk>required</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <p>
	 * 	Determines whether this parameter is mandatory.
	 *  <br>The property MAY be included and its default value is false.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(required=<jk>true</jk>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(Pet <jv>input</jv>) {...}
	 *
	 * 	<ja>@Body</ja>(required=<jk>true</jk>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is boolean.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	boolean required() default false;

	/**
	 * Synonym for {@link #readOnly()}.
	 */
	boolean ro() default false;

	/**
	 * Synonym for {@link #type()}.
	 */
	String t() default "";

	/**
	 * <mk>title</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String title() default "";

	/**
	 * <mk>type</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(type=<js>"object"</js>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(Pet <jv>input</jv>) {...}
	 *
	 * 	<ja>@Body</ja>(type=<js>"object"</js>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		The possible values are:
	 * 		<ul>
	 * 			<li><js>"object"</js>
	 * 			<li><js>"string"</js>
	 * 			<li><js>"number"</js>
	 * 			<li><js>"integer"</js>
	 * 			<li><js>"boolean"</js>
	 * 			<li><js>"array"</js>
	 * 			<li><js>"file"</js>
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='extlink'>{@doc ExtSwaggerDataTypes}
	 * </ul>
	 *
	 */
	String type() default "";

	/**
	 * Synonym for {@link #uniqueItems()}.
	 */
	boolean ui() default false;

	/**
	 * <mk>uniqueItems</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is boolean.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	boolean uniqueItems() default false;

	/**
	 * Free-form value for the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <p>
	 * This is a JSON object that makes up the swagger information for this field.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of a Schema object:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Schema</ja>(
	 * 		type=<js>"array"</js>,
	 * 		items=<ja>@Items</ja>(
	 * 			$ref=<js>"#/definitions/Pet"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Schema</ja>(<js>"type:'array',items:{$ref:'#/definitions/Pet'}"</js>)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form using variables</jc>
	 * 	<ja>@Schema</ja>(<js>"$L{petArraySwagger}"</js>)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>petArraySwagger</mk> = <mv>{ type: "array", items: { $ref: "#/definitions/Pet" } }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this field from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Schema</ja>(<js>"{type: 'array'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Schema</ja>(<js>"type: 'array'"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] value() default {};

	/**
	 * <mk>xml</mk> field of the {@doc ExtSwaggerSchemaObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] xml() default {};
}
