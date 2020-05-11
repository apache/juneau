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
package org.apache.juneau.oapi;

/**
 * Configurable properties common to both the {@link OpenApiSerializer} and {@link OpenApiParser} classes.
 */
public interface OpenApiCommon {

	/**
	 * Property prefix.
	 */
	static final String PREFIX = "OpenApi";

	/**
	 * Configuration property:  Default format for HTTP parts.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.oapi.OpenApiCommon#OAPI_format OAPI_format}
	 * 	<li><b>Name:</b>  <js>"OpenApi.format.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.httppart.HttpPartFormat}
	 * 	<li><b>System property:</b>  <c>OpenApi.format</c>
	 * 	<li><b>Environment variable:</b>  <c>OPENAPI_FORMAT</c>
	 * 	<li><b>Default:</b>  <js>"NO_FORMAT"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.oapi.annotation.OpenApiConfig#format()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.oapi.OpenApiSerializerBuilder#format(org.apache.juneau.httppart.HttpPartFormat)}
	 * 			<li class='jm'>{@link org.apache.juneau.oapi.OpenApiParserBuilder#format(org.apache.juneau.httppart.HttpPartFormat)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.jsonschema.annotation.Schema#format()}.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='javatree'>
	 * 	<li class='jc'>{@link org.apache.juneau.httppart.HttpPartFormat}
	 * 	<ul>
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#UON UON} - UON notation (e.g. <js>"'foo bar'"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#INT32 INT32} - Signed 32 bits.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#INT64 INT64} - Signed 64 bits.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#FLOAT FLOAT} - 32-bit floating point number.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DOUBLE DOUBLE} - 64-bit floating point number.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BYTE BYTE} - BASE-64 encoded characters.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BINARY BINARY} - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#BINARY_SPACED BINARY_SPACED} - Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DATE DATE} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#DATE_TIME DATE_TIME} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#PASSWORD PASSWORD} - Used to hint UIs the input needs to be obscured.
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartFormat#NO_FORMAT NO_FORMAT} - (default) Not specified.
	 * 	</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a plain-text serializer.</jc>
	 * 	OpenApiSerializer s1 = OpenApiSerializer.
	 * 		.<jsm>create</jsm>()
	 * 		.build();
	 *
	 * 	<jc>// Create a UON serializer.</jc>
	 * 	OpenApiSerializer s2 = OpenApiSerializer.
	 * 		.<jsm>create</jsm>()
	 * 		.format(<jsf>UON</jsf>)
	 * 		.build();
	 *
	 * 	OMap m = OMap.<jsm>of</jsm>(
	 * 		<js>"foo"</js>, <js>"bar"</js>,
	 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
	 *  );
	 *
	 * 	<jc>// Produces: "foo=bar,baz=qux\,true\,123"</jc>
	 * 	String v1 = s1.serialize(m);
	 *
	 * <jc>// Produces: "(foo=bar,baz=@(qux,'true','123'))"</jc>
	 * 	String v2 = s2.serialize(m);
	 * </p>
	 */
	public static final String OAPI_format = PREFIX + ".format.s";

	/**
	 * Configuration property:  Default collection format for HTTP parts.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.oapi.OpenApiCommon#OAPI_collectionFormat OAPI_collectionFormat}
	 * 	<li><b>Name:</b>  <js>"OpenApi.format.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.httppart.HttpPartCollectionFormat}
	 * 	<li><b>System property:</b>  <c>OpenApi.collectionFormat</c>
	 * 	<li><b>Environment variable:</b>  <c>OPENAPI_COLLECTIONFORMAT</c>
	 * 	<li><b>Default:</b>  <js>"NO_COLLECTION_FORMAT"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.oapi.annotation.OpenApiConfig#collectionFormat()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.oapi.OpenApiSerializerBuilder#collectionFormat(org.apache.juneau.httppart.HttpPartCollectionFormat)}
	 * 			<li class='jm'>{@link org.apache.juneau.oapi.OpenApiParserBuilder#collectionFormat(org.apache.juneau.httppart.HttpPartCollectionFormat)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the collection format to use for HTTP parts when not otherwise specified via {@link org.apache.juneau.jsonschema.annotation.Schema#collectionFormat()}.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='javatree'>
	 * 	<li class='jc'>{@link org.apache.juneau.httppart.HttpPartFormat}
	 * 	<ul>
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#CSV CSV} - (default) Comma-separated values (e.g. <js>"foo,bar"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#SSV SSV} - Space-separated values (e.g. <js>"foo bar"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#TSV TSV} - Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#PIPES PIPES} - Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#MULTI MULTI} - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 * 		<li class='jf'>{@link org.apache.juneau.httppart.HttpPartCollectionFormat#UONC UONC} - UON collection notation (e.g. <js>"@(foo,bar)"</js>).
	 * 	</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer using CSV for collections.</jc>
	 * 	OpenApiSerializer s2 = OpenApiSerializer.
	 * 		.<jsm>create</jsm>()
	 * 		.collectionFormat(<jsf>CSV</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Create a serializer using UON for collections.</jc>
	 * 	OpenApiSerializer s2 = OpenApiSerializer.
	 * 		.<jsm>create</jsm>()
	 * 		.collectionFormat(<jsf>UON</jsf>)
	 * 		.build();
	 *
	 * 	OList l = OList.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>);
	 *
	 * 	<jc>// Produces: "foo=bar,baz=qux,true,123"</jc>
	 * 	String v1 = s1.serialize(l)
	 *
	 * <jc>// Produces: "(foo=bar,baz=@(qux,'true','123'))"</jc>
	 * 	String v2 = s2.serialize(l)
	 * </p>
	 */
	public static final String OAPI_collectionFormat = PREFIX + ".collectionformat.s";
}
