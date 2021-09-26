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
package org.apache.juneau.html;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for building instances of HTML serializers.
 * {@review}
 */
@FluentSetters
public class HtmlSerializerBuilder extends XmlSerializerBuilder {

	boolean addBeanTypesHtml, addKeyValueTableHeaders, disableDetectLabelParameters, disableDetectLinksInStrings;
	String labelParameter;
	AnchorText uriAnchorText;

	/**
	 * Constructor, default settings.
	 */
	protected HtmlSerializerBuilder() {
		super();
		produces("text/html");
		type(HtmlSerializer.class);
		addBeanTypesHtml = env("HtmlSerializer.addBeanTypesHtml", false);
		addKeyValueTableHeaders = env("HtmlSerializer.addKeyValueTableHeaders", false);
		disableDetectLabelParameters = env("HtmlSerializer.disableDetectLabelParameters", false);
		disableDetectLinksInStrings = env("HtmlSerializer.disableDetectLinksInStrings", false);
		uriAnchorText = env("HtmlSerializer.uriAnchorText", AnchorText.TO_STRING);
		labelParameter =  env("HtmlSerializer.labelParameter", "label");
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected HtmlSerializerBuilder(HtmlSerializer copyFrom) {
		super(copyFrom);
		addBeanTypesHtml = copyFrom.addBeanTypesHtml;
		addKeyValueTableHeaders = copyFrom.addKeyValueTableHeaders;
		disableDetectLabelParameters = ! copyFrom.detectLabelParameters;
		disableDetectLinksInStrings = ! copyFrom.detectLinksInStrings;
		labelParameter = copyFrom.labelParameter;
		uriAnchorText = copyFrom.uriAnchorText;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected HtmlSerializerBuilder(HtmlSerializerBuilder copyFrom) {
		super(copyFrom);
		addBeanTypesHtml = copyFrom.addBeanTypesHtml;
		addKeyValueTableHeaders = copyFrom.addKeyValueTableHeaders;
		disableDetectLabelParameters = copyFrom.disableDetectLabelParameters;
		disableDetectLinksInStrings = copyFrom.disableDetectLinksInStrings;
		labelParameter = copyFrom.labelParameter;
		uriAnchorText = copyFrom.uriAnchorText;
	}

	@Override /* ContextBuilder */
	public HtmlSerializerBuilder copy() {
		return new HtmlSerializerBuilder(this);
	}

	@Override /* ContextBuilder */
	public HtmlSerializer build() {
		return (HtmlSerializer)super.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link SerializerBuilder#addBeanTypes()} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 *
	 * @return This object.
	 */
	@FluentSetter
	public HtmlSerializerBuilder addBeanTypesHtml() {
		return addBeanTypesHtml(true);
	}

	/**
	 * Same as {@link #addBeanTypesHtml()} but allows you to explicitly specify the value.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlSerializerBuilder addBeanTypesHtml(boolean value) {
		addBeanTypesHtml = value;
		return this;
	}

	/**
	 * <i><l>HtmlSerializer</l> configuration property:&emsp;</i>  Add key/value headers on bean/map tables.
	 *
	 * <p>
	 * When enabled, <bc>key</bc> and <bc>value</bc> column headers are added to tables.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our bean class.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>f1</jf> = <js>"foo"</js>;
	 * 		<jk>public</jk> String <jf>f2</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 *  <jc>// Serializer without headers.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>;
	 *
	 *  <jc>// Serializer with headers.</jc>
	 * 	WriterSerializer <jv>serializer2</jv> = HtmlSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addKeyValueTableHeaders()
	 * 		.build();
	 *
	 * 	String <jv>withoutHeaders</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 	String <jv>withHeaders</jv> = <jv>serializer2</jv>.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><c>withoutHeaders</c></th>
	 * 		<th><c>withHeaders</c></th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><td>f1</td><td>foo</td></tr>
	 * 				<tr><td>f2</td><td>bar</td></tr>
	 * 			</table>
	 * 		</td>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td>foo</td></tr>
	 * 				<tr><td>f2</td><td>bar</td></tr>
	 * 			</table>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSerializerBuilder addKeyValueTableHeaders() {
		return addKeyValueTableHeaders(true);
	}

	/**
	 * Same as {@link #addKeyValueTableHeaders()} but allows you to explicitly specify the value.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlSerializerBuilder addKeyValueTableHeaders(boolean value) {
		addKeyValueTableHeaders = value;
		return this;
	}

	/**
	 * <i><l>HtmlSerializer</l> configuration property:&emsp;</i>  Don't look for URLs in {@link String Strings}.
	 *
	 * <p>
	 * Disables the feature where if a string looks like a URL (i.e. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * and make it into a hyperlink based on the rules specified by {@link HtmlSerializerBuilder#uriAnchorText(AnchorText)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our bean class with a property containing what looks like a URL.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> String <jf>f1</jf> = <js>"http://www.apache.org"</js>;
	 * 	}
	 *
	 *  <jc>// Serializer with link detection.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addKeyValueTableHeaders()
	 * 		.build();
	 *
	 *  <jc>// Serializer without link detection.</jc>
	 * 	WriterSerializer <jv>serializer2</jv> = HtmlSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addKeyValueTableHeaders()
	 * 		.disableDetectLinksInStrings()
	 * 		.build();
	 *
	 * 	String <jv>withLinks</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 	String <jv>withoutLinks</jv> = <jv>serializer2</jv>.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><c>withLinks</c></th>
	 * 		<th><c>withoutLinks</c></th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td><a href='http://www.apache.org'>http://www.apache.org</a></td></tr>
	 * 			</table>
	 * 		</td>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td>http://www.apache.org</td></tr>
	 * 			</table>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSerializerBuilder disableDetectLinksInStrings() {
		return disableDetectLinksInStrings(true);
	}

	/**
	 * Same as {@link #disableDetectLinksInStrings()} but allows you to explicitly specify the value.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlSerializerBuilder disableDetectLinksInStrings(boolean value) {
		disableDetectLinksInStrings = value;
		return this;
	}

	/**
	 * <i><l>HtmlSerializer</l> configuration property:&emsp;</i>  Link label parameter name.
	 *
	 * <p>
	 * The parameter name to look for when resolving link labels}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"label"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSerializerBuilder labelParameter(String value) {
		labelParameter = value;
		return this;
	}

	/**
	 * <i><l>HtmlSerializer</l> configuration property:&emsp;</i>  Dont look for link labels in URIs.
	 *
	 * <p>
	 * Disables the feature where if the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 *
	 * <p>
	 * The parameter name can be changed via the {@link #labelParameter(String)} property.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our bean class with a property containing what looks like a URL.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"http://www.apache.org?label=Apache%20Foundation"</js>);
	 * 	}
	 *
	 *  <jc>// Serializer with label detection.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addKeyValueTableHeaders()
	 * 		.build();
	 *
	 *  <jc>// Serializer without label detection.</jc>
	 * 	WriterSerializer <jv>serializer2</jv> = HtmlSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.addKeyValueTableHeaders()
	 * 		.disableDetectLabelParameters()
	 * 		.build();
	 *
	 * 	String <jv>withLabels</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 	String <jv>withoutLabels</jv> = <jv>serializer2</jv>.serialize(<jk>new</jk> MyBean());
	 * </p>
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs.
	 * <br>Note that they're both hyperlinks, but the anchor text differs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><c>withLabels</c></th>
	 * 		<th><c>withoutLabels</c></th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td><a href='http://www.apache.org?label=Apache%20Foundation'>Apache Foundation</a></td></tr>
	 * 			</table>
	 * 		</td>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td><a href='http://www.apache.org?label=Apache%20Foundation'>http://www.apache.org?label=Apache%20Foundation</a></td></tr>
	 * 			</table>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSerializerBuilder disableDetectLabelParameters() {
		return disableDetectLabelParameters(true);
	}

	/**
	 * Same as {@link #disableDetectLabelParameters()} but allows you to explicitly specify the value.
	 *
	 * @param value The value for this setting.
	 * @return This object.
	 */
	@FluentSetter
	public HtmlSerializerBuilder disableDetectLabelParameters(boolean value) {
		disableDetectLabelParameters = value;
		return this;
	}

	/**
	 * <i><l>HtmlSerializer</l> configuration property:&emsp;</i>  Anchor text source.
	 *
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * <xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 *
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li class='jc'>{@link AnchorText}
	 * 	<ul>
	 * 		<li class='jf'>{@link AnchorText#TO_STRING TO_STRING} (default) - Set to whatever is returned by {@link #toString()} on the object.
	 * 			<br>
	 * 			<h5 class='section'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Our bean class with a URI property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"http://www.apache.org?foo=bar#myAnchor"</js>);
	 * 	}
	 *
	 * 	<jc>// Serializer with TO_STRING anchor text.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer.<jsm>create</jsm>().anchorText(<jsf>TO_STRING</jsf>).build();
	 *
	 * 	<jc>// Produces: &lt;a href='http://www.apache.org?foo=bar#myAnchor'&gt;http://www.apache.org?foo=bar#myAnchor&lt;/a&gt;</jc>
	 * 	String <jv>html</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 			</p>
	 * 		<li class='jf'>{@link AnchorText#PROPERTY_NAME PROPERTY_NAME} - Set to the bean property name.
	 * 			<br>
	 * 			<h5 class='section'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Our bean class with a URI property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"http://www.apache.org?foo=bar#myAnchor"</js>);
	 * 	}
	 *
	 * 	<jc>// Serializer with PROPERTY_NAME anchor text.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer.<jsm>create</jsm>().anchorText(<jsf>PROPERTY_NAME</jsf>).build();
	 *
	 * 	<jc>// Produces: &lt;a href='http://www.apache.org?foo=bar#myAnchor'&gt;f1&lt;/a&gt;</jc>
	 * 	String <jv>html</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 			</p>
	 * 		<li class='jf'>{@link AnchorText#URI URI} - Set to the URI value.
	 * 			<br>
	 * 			<h5 class='section'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Our bean class with a URI property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"http://www.apache.org?foo=bar#myAnchor"</js>);
	 * 	}
	 *
	 * 	<jc>// Serializer with URI anchor text.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer.<jsm>create</jsm>().anchorText(<jsf>URI</jsf>).build();
	 *
	 * 	<jc>// Produces: &lt;a href='http://www.apache.org?foo=bar#myAnchor'&gt;http://www.apache.org?foo=bar&lt;/a&gt;</jc>
	 * 	String <jv>html</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 			</p>
	 * 		<li class='jf'>{@link AnchorText#LAST_TOKEN LAST_TOKEN} - Set to the last token of the URI value.
	 * 			<br>
	 * 			<h5 class='section'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Our bean class with a URI property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"http://www.apache.org/foo/bar?baz=qux#myAnchor"</js>);
	 * 	}
	 *
	 * 	<jc>// Serializer with LAST_TOKEN anchor text.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer.<jsm>create</jsm>().anchorText(<jsf>LAST_TOKEN</jsf>).build();
	 *
	 * 	<jc>// Produces: &lt;a href='http://www.apache.org/foo/bar?baz=qux#myAnchor'&gt;bar&lt;/a&gt;</jc>
	 * 	String <jv>html</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 			</p>
	 * 		<li class='jf'>{@link AnchorText#URI_ANCHOR URI_ANCHOR} - Set to the anchor of the URL.
	 * 			<br>
	 * 			<h5 class='section'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Our bean class with a URI property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"http://www.apache.org/foo/bar?baz=qux#myAnchor"</js>);
	 * 	}
	 *
	 * 	<jc>// Serializer with URI_ANCHOR anchor text.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer.<jsm>create</jsm>().anchorText(<jsf>URI_ANCHOR</jsf>).build();
	 *
	 * 	<jc>// Produces: &lt;a href='http://www.apache.org/foo/bar?baz=qux#myAnchor'&gt;myAnchor&lt;/a&gt;</jc>
	 * 	String <jv>html</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 			</p>
	 * 		<li class='jf'>{@link AnchorText#CONTEXT_RELATIVE CONTEXT_RELATIVE} - Same as {@link AnchorText#TO_STRING TO_STRING} but assumes it's a context-relative path.
	 * 			<br>
	 * 			<h5 class='section'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Our bean class with a URI property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"bar/baz"</js>);
	 * 	}
	 *
	 * 	<jc>// Serializer with CONTEXT_RELATIVE anchor text.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.anchorText(<jsf>CONTEXT_RELATIVE</jsf>)
	 * 		.uriResolution(<jsf>ROOT_RELATIVE</jsf>)
	 * 		.uriRelativity(<jsf>RESOURCE</jsf>)
	 * 		.uriContext(<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces: &lt;a href&#61;'/myContext/myServlet/bar/baz'&gt;myServlet/bar/baz&lt;/a&gt;</jc>
	 * 	String <jv>html</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 			</p>
	 * 		<li class='jf'>{@link AnchorText#SERVLET_RELATIVE SERVLET_RELATIVE} - Same as {@link AnchorText#TO_STRING TO_STRING} but assumes it's a servlet-relative path.
	 * 			<br>
	 * 			<h5 class='section'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Our bean class with a URI property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"bar/baz"</js>);
	 * 	}
	 *
	 * 	<jc>// Serializer with SERVLET_RELATIVE anchor text.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.anchorText(<jsf>SERVLET_RELATIVE</jsf>)
	 * 		.uriResolution(<jsf>ROOT_RELATIVE</jsf>)
	 * 		.uriRelativity(<jsf>RESOURCE</jsf>)
	 * 		.uriContext(<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces: &lt;a href&#61;'/myContext/myServlet/bar/baz'&gt;bar/baz&lt;/a&gt;</jc>
	 * 	String <jv>html</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 			</p>
	 * 		<li class='jf'>{@link AnchorText#PATH_RELATIVE PATH_RELATIVE} - Same as {@link AnchorText#TO_STRING TO_STRING} but assumes it's a path-relative path.
	 * 			<br>
	 * 			<h5 class='section'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	<jc>// Our bean class with a URI property.</jc>
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public</jk> URI <jf>f1</jf> = URI.<jsm>create</jsm>(<js>"bar/baz"</js>);
	 * 	}
	 *
	 * 	<jc>// Serializer with PATH_RELATIVE anchor text.</jc>
	 * 	WriterSerializer <jv>serializer1</jv> = HtmlSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.anchorText(<jsf>PATH_RELATIVE</jsf>)
	 * 		.uriResolution(<jsf>ROOT_RELATIVE</jsf>)
	 * 		.uriRelativity(<jsf>PATH_INFO</jsf>)
	 * 		.uriContext(<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Produces: &lt;a href&#61;'/myContext/myServlet/foo/bar/baz'&gt;bar/baz&lt;/a&gt;</jc>
	 * 	String <jv>html</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> MyBean());
	 * 			</p>
	 * 	</ul>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link AnchorText#TO_STRING}.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HtmlSerializerBuilder uriAnchorText(AnchorText value) {
		uriAnchorText = value;
		return this;
	}

	// <FluentSetters>

	@Override
	public HtmlSerializerBuilder produces(String value) {
		super.produces(value);
		return this;
	}

	@Override
	public HtmlSerializerBuilder accept(String value) {
		super.accept(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public HtmlSerializerBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> HtmlSerializerBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> HtmlSerializerBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder swaps(Class<?>...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public HtmlSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public HtmlSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder keepNullProperties() {
		super.keepNullProperties();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public HtmlSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSerializerBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSerializerBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* GENERATED - WriterSerializerBuilder */
	public HtmlSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSerializerBuilder addNamespaceUrisToRoot() {
		super.addNamespaceUrisToRoot();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSerializerBuilder defaultNamespace(String value) {
		super.defaultNamespace(value);
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSerializerBuilder disableAutoDetectNamespaces() {
		super.disableAutoDetectNamespaces();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSerializerBuilder enableNamespaces() {
		super.enableNamespaces();
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSerializerBuilder namespaces(String...values) {
		super.namespaces(values);
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSerializerBuilder namespaces(Namespace...values) {
		super.namespaces(values);
		return this;
	}

	@Override /* GENERATED - XmlSerializerBuilder */
	public HtmlSerializerBuilder ns() {
		super.ns();
		return this;
	}

	// </FluentSetters>
}