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
 * JSON Schema Data Transfer Objects
 *
 * <h5 class='topic'>Overview</h5>
 *
 * <p>
 * Juneau supports serializing and parsing of JSON Schema documents through the use of beans defined in the
 * <c>org.apache.juneau.bean.jsonschema</c> package.
 * These beans are used with the existing {@link org.apache.juneau.json.JsonSerializer} and
 * {@link org.apache.juneau.json.JsonParser} classes to produce and consume JSON Schema documents.
 * </p>
 *
 * <p>
 * <b>NOTE:</b> This implementation follows the JSON Schema Draft 2020-12 specification.
 * For backward compatibility, deprecated properties from earlier drafts (Draft 04) are still supported.
 * </p>
 *
 * <h5 class='topic'>Bean Classes</h5>
 *
 * <p>
 * The bean classes that make up the model are as follows:
 * </p>
 * <ul class='spaced-list'>
 * 	<li>{@link org.apache.juneau.bean.jsonschema.JsonSchema} - Top level schema object.
 * 	<li>{@link org.apache.juneau.bean.jsonschema.JsonSchemaProperty} - A subclass of <c>JsonSchema</c> for
 * 		representing properties.
 * 	<li>{@link org.apache.juneau.bean.jsonschema.JsonSchemaPropertySimpleArray} - A convenience subclass of
 * 		<c>JsonSchemaProperty</c> for representing properties of simple array types.
 * 	<li>{@link org.apache.juneau.bean.jsonschema.JsonSchemaRef} - Represents a URI reference to another schema.
 * 	<li>{@link org.apache.juneau.bean.jsonschema.JsonSchemaArray} - An array of <c>JsonSchema</c> objects.
 * 	<li>{@link org.apache.juneau.bean.jsonschema.JsonType} - An enum of possible JSON data types.
 * 	<li>{@link org.apache.juneau.bean.jsonschema.JsonTypeArray} - An array of <c>JsonType</c> objects.
 * </ul>
 *
 * <h5 class='topic'>Creating JSON Schema Documents</h5>
 *
 * <p>
 * JSON Schema documents can be constructed using the Juneau JSON Schema beans as a document model object.
 * These beans are defined with fluent-style setters to make constructing documents as easy as possible.
 * </p>
 *
 * <p>
 * The following is an example JSON Schema document:
 * </p>
 * <p class='bjson'>
 * 	{
 * 		<js>"title"</js>: <js>"Example Schema"</js>,
 * 		<js>"type"</js>: <js>"object"</js>,
 * 		<js>"properties"</js>: {
 * 			<js>"firstName"</js>: {
 * 				<js>"type"</js>: <js>"string"</js>
 * 			},
 * 			<js>"lastName"</js>: {
 * 				<js>"type"</js>: <js>"string"</js>
 * 			},
 * 			<js>"age"</js>: {
 * 				<js>"description"</js>: <js>"Age in years"</js>,
 * 				<js>"type"</js>: <js>"integer"</js>,
 * 				<js>"minimum"</js>: 0
 * 			}
 * 		},
 * 		<js>"required"</js>: [<js>"firstName"</js>, <js>"lastName"</js>]
 * 	}
 * </p>
 *
 * <p>
 * This document can be constructed using the following code:
 * </p>
 * <p class='bjava'>
 * 	<jc>// Create the document object model</jc>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setTitle(<js>"Example Schema"</js>)
 * 		.setType(JsonType.<jsf>OBJECT</jsf>)
 * 		.addProperties(
 * 			<jk>new</jk> JsonSchemaProperty(<js>"firstName"</js>, JsonType.<jsf>STRING</jsf>),
 * 			<jk>new</jk> JsonSchemaProperty(<js>"lastName"</js>, JsonType.<jsf>STRING</jsf>),
 * 			<jk>new</jk> JsonSchemaProperty(<js>"age"</js>, JsonType.<jsf>INTEGER</jsf>)
 * 				.setDescription(<js>"Age in years"</js>)
 * 				.setMinimum(0)
 * 		)
 * 		.addRequired(<js>"firstName"</js>, <js>"lastName"</js>);
 *
 * 	<jc>// Serialize to JSON</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT_READABLE</jsf>.serialize(<jv>schema</jv>);
 * </p>
 *
 * <p>
 * The following is a more-complex example showing various kinds of constraints:
 * </p>
 * <p class='bjson'>
 * 	{
 * 		<js>"$id"</js>: <js>"http://some.site.somewhere/entry-schema#"</js>,
 * 		<js>"$schema"</js>: <js>"https://json-schema.org/draft/2020-12/schema"</js>,
 * 		<js>"description"</js>: <js>"schema for an fstab entry"</js>,
 * 		<js>"type"</js>: <js>"object"</js>,
 * 		<js>"required"</js>: [ <js>"storage"</js> ],
 * 		<js>"properties"</js>: {
 * 			<js>"storage"</js>: {
 * 				<js>"type"</js>: <js>"object"</js>,
 * 				<js>"oneOf"</js>: [
 * 					{ <js>"$ref"</js>: <js>"#/definitions/diskDevice"</js> },
 * 					{ <js>"$ref"</js>: <js>"#/definitions/diskUUID"</js> },
 * 					{ <js>"$ref"</js>: <js>"#/definitions/nfs"</js> },
 * 					{ <js>"$ref"</js>: <js>"#/definitions/tmpfs"</js> }
 * 				]
 * 			},
 * 			<js>"fstype"</js>: {
 * 				<js>"enum"</js>: [ <js>"ext3"</js>, <js>"ext4"</js>, <js>"btrfs"</js> ]
 * 			},
 * 			<js>"options"</js>: {
 * 				<js>"type"</js>: <js>"array"</js>,
 * 				<js>"minItems"</js>: 1,
 * 				<js>"items"</js>: { <js>"type"</js>: <js>"string"</js> },
 * 				<js>"uniqueItems"</js>: <jk>true</jk>
 * 			},
 * 			<js>"readonly"</js>: { <js>"type"</js>: <js>"boolean"</js> }
 * 		},
 * 		<js>"definitions"</js>: {
 * 			<js>"diskDevice"</js>: {},
 * 			<js>"diskUUID"</js>: {},
 * 			<js>"nfs"</js>: {},
 * 			<js>"tmpfs"</js>: {}
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * This document can be constructed using the following code:
 * </p>
 * <p class='bjava'>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setIdUri(<js>"http://some.site.somewhere/entry-schema#"</js>)
 * 		.setSchemaVersionUri(<js>"https://json-schema.org/draft/2020-12/schema"</js>)
 * 		.setDescription(<js>"schema for an fstab entry"</js>)
 * 		.setType(JsonType.<jsf>OBJECT</jsf>)
 * 		.addRequired(<js>"storage"</js>)
 * 		.addProperties(
 * 			<jk>new</jk> JsonSchemaProperty(<js>"storage"</js>)
 * 				.setType(JsonType.<jsf>OBJECT</jsf>)
 * 				.addOneOf(
 * 					<jk>new</jk> JsonSchemaRef(<js>"#/definitions/diskDevice"</js>),
 * 					<jk>new</jk> JsonSchemaRef(<js>"#/definitions/diskUUID"</js>),
 * 					<jk>new</jk> JsonSchemaRef(<js>"#/definitions/nsf"</js>),
 * 					<jk>new</jk> JsonSchemaRef(<js>"#/definitions/tmpfs"</js>)
 * 				),
 * 			<jk>new</jk> JsonSchemaProperty(<js>"fstype"</js>)
 * 				.addEnum(<js>"ext3"</js>, <js>"ext4"</js>, <js>"btrfs"</js>),
 * 			<jk>new</jk> JsonSchemaPropertySimpleArray(<js>"options"</js>, JsonType.<jsf>STRING</jsf>)
 * 				.setMinItems(1)
 * 				.setUniqueItems(<jk>true</jk>),
 * 			<jk>new</jk> JsonSchemaProperty(<js>"readonly"</js>)
 * 				.setType(JsonType.<jsf>BOOLEAN</jsf>)
 * 		)
 * 		.addDefinition(<js>"diskDevice"</js>, <jk>new</jk> JsonSchema())
 * 		.addDefinition(<js>"diskUUID"</js>, <jk>new</jk> JsonSchema())
 * 		.addDefinition(<js>"nfs"</js>, <jk>new</jk> JsonSchema())
 * 		.addDefinition(<js>"tmpfs"</js>, <jk>new</jk> JsonSchema());
 *
 * 	<jc>// Serialize to JSON</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT_READABLE</jsf>.serialize(<jv>schema</jv>);
 * </p>
 *
 * <h5 class='topic'>Serializing to Other Data Types</h5>
 *
 * <p>
 * Since the JSON Schema DTOs are simple beans, they can be used to serialize to a variety of other
 * language types as well as JSON.
 * This also allows JSON Schema documents to be easily served up using the Juneau REST API.
 * </p>
 *
 * <h5 class='topic'>Parsing JSON Schema Documents</h5>
 *
 * <p>
 * Use the {@link org.apache.juneau.json.JsonParser} to parse JSON Schema documents into DTOs:
 * </p>
 * <p class='bjava'>
 * 	<jc>// Use parser to load JSON Schema document into JSON Schema DTOs</jc>
 * 	JsonSchema <jv>schema</jv> = JsonParser.<jsf>DEFAULT</jsf>.parse(<jv>json</jv>, JsonSchema.<jk>class</jk>);
 * </p>
 *
 * <p>
 * Schema objects can also be constructed from other media types using the appropriate parsers.
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a href="https://json-schema.org/draft/2020-12/json-schema-core.html">JSON Schema 2020-12 Core</a>
 * 	<li class='link'><a href="https://json-schema.org/draft/2020-12/json-schema-validation.html">JSON Schema 2020-12 Validation</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonSchema">juneau-bean-jsonschema</a>
 * </ul>
 */
package org.apache.juneau.bean.jsonschema;
