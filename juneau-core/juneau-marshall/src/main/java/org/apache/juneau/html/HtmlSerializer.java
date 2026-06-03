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
package org.apache.juneau.html;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * Serializes POJO models to HTML.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/html</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/html</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link Map Maps} (e.g. {@link HashMap}, {@link TreeMap}) and beans are converted to HTML tables with
 * 		'key' and 'value' columns.
 * 	<li>
 * 		{@link Collection Collections} (e.g. {@link HashSet}, {@link LinkedList}) and Java arrays are converted
 * 		to HTML ordered lists.
 * 	<li>
 * 		{@code Collections} of {@code Maps} and beans are converted to HTML tables with keys as headers.
 * 	<li>
 * 		Everything else is converted to text.
 * </ul>
 *
 * <p>
 * This serializer provides several serialization options.  Typically, one of the predefined <jsf>DEFAULT</jsf>
 * serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <p>
 * The {@link HtmlLink} annotation can be used on beans to add hyperlinks to the output.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link Sq} - Default serializer, single quotes.
 * 	<li>
 * 		{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>html</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer that doesn't use whitespace and newlines</jc>
 * 	HtmlSerializer <jv>serializer</jv> = HtmlSerializer.<jsm>create</jsm>().ws().build();
 *
 * 	<jc>// Same as above, except uses cloning</jc>
 * 	HtmlSerializer <jv>serializer</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>.copy().ws().build();
 *
 * 	<jc>// Serialize POJOs to HTML</jc>
 *
 * 	<jc>// Produces: </jc>
 * 	<jc>// &lt;ul&gt;&lt;li&gt;1&lt;li&gt;2&lt;li&gt;3&lt;/ul&gt;</jc>
 * 	List <jv>list</jv> = JsonList.<jsm>of</jsm>(1, 2, 3);
 * 	String <jv>html</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>list</jv>);
 *
 * 	<jc>// Produces: </jc>
 * 	<jc>//    &lt;table&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;th&gt;firstName&lt;/th&gt;&lt;th&gt;lastName&lt;/th&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;Bob&lt;/td&gt;&lt;td&gt;Costas&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;Billy&lt;/td&gt;&lt;td&gt;TheKid&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;Barney&lt;/td&gt;&lt;td&gt;Miller&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//    &lt;/table&gt; </jc>
 * 	<jv>html</jv> = JsonList.<jsm>of</jsm>(
 * 		JsonMap.<jsm>ofText</jsm>(<js>"{firstName:'Bob',lastName:'Costas'}"</js>),
 * 		JsonMap.<jsm>ofText</jsm>(<js>"{firstName:'Billy',lastName:'TheKid'}"</js>),
 * 		JsonMap.<jsm>ofText</jsm>(<js>"{firstName:'Barney',lastName:'Miller'}"</js>)
 * 	);
 * 	String <jv>html</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>list</jv>);
 *
 * 	<jc>// Produces: </jc>
 * 	<jc>//    &lt;table&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;foo&lt;/td&gt;&lt;td&gt;bar&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;baz&lt;/td&gt;&lt;td&gt;123&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//    &lt;/table&gt; </jc>
 * 	Map <jv>map</jv> = JsonMap.<jsm>ofText</jsm>(<js>"{foo:'bar',baz:123}"</js>);
 * 	String <jv>html</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>map</jv>);
 *
 * 	<jc>// HTML elements can be nested arbitrarily deep</jc>
 * 	<jc>// Produces: </jc>
 * 	<jc>//	&lt;table&gt; </jc>
 * 	<jc>//		&lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 	<jc>//		&lt;tr&gt;&lt;td&gt;foo&lt;/td&gt;&lt;td&gt;bar&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//		&lt;tr&gt;&lt;td&gt;baz&lt;/td&gt;&lt;td&gt;123&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//		&lt;tr&gt;&lt;td&gt;someNumbers&lt;/td&gt;&lt;td&gt;&lt;ul&gt;&lt;li&gt;1&lt;li&gt;2&lt;li&gt;3&lt;/ul&gt;&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//		&lt;tr&gt;&lt;td&gt;someSubMap&lt;/td&gt;&lt;td&gt; </jc>
 * 	<jc>//			&lt;table&gt; </jc>
 * 	<jc>//				&lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 	<jc>//				&lt;tr&gt;&lt;td&gt;a&lt;/td&gt;&lt;td&gt;b&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//			&lt;/table&gt; </jc>
 * 	<jc>//		&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//	&lt;/table&gt; </jc>
 * 	Map <jv>map</jv> = JsonMap.<jsm>ofText</jsm>(<js>"{foo:'bar',baz:123}"</js>);
 * 	<jv>map</jv>.put(<js>"someNumbers"</js>, JsonList.<jsm>of</jsm>(1, 2, 3));
 * 	<jv>map</jv>.put(<js>"someSubMap"</js>, JsonMap.<jsm>ofText</jsm>(<js>"{a:'b'}"</js>));
 * 	String <jv>html</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>map</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class HtmlSerializer extends XmlSerializer implements HtmlMetaProvider {

	// Property name constants
	private static final String PROP_addBeanTypesHtml = "addBeanTypesHtml";
	private static final String PROP_addKeyValueTableHeaders = "addKeyValueTableHeaders";
	private static final String PROP_detectLabelParameters = "detectLabelParameters";
	private static final String PROP_detectLinksInStrings = "detectLinksInStrings";
	private static final String PROP_labelParameter = "labelParameter";
	private static final String PROP_uriAnchorText = "uriAnchorText";

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends XmlSerializer.Builder<SELF> {

		private static final Cache<HashKey,HtmlSerializer> CACHE = Cache.of(HashKey.class, HtmlSerializer.class).build();

		private boolean addBeanTypesHtml;
		private boolean addKeyValueTableHeaders;
		private boolean disableDetectLabelParameters;
		private boolean disableDetectLinksInStrings;
		private String labelParameter;
		private AnchorText uriAnchorText;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/html");
			addBeanTypesHtml = env("HtmlSerializer.addBeanTypesHtml", false);
			addKeyValueTableHeaders = env("HtmlSerializer.addKeyValueTableHeaders", false);
			disableDetectLabelParameters = env("HtmlSerializer.disableDetectLabelParameters", false);
			disableDetectLinksInStrings = env("HtmlSerializer.disableDetectLinksInStrings", false);
			uriAnchorText = env("HtmlSerializer.uriAnchorText", AnchorText.TO_STRING);
			labelParameter = env("HtmlSerializer.labelParameter", "label");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesHtml = copyFrom.addBeanTypesHtml;
			addKeyValueTableHeaders = copyFrom.addKeyValueTableHeaders;
			disableDetectLabelParameters = copyFrom.disableDetectLabelParameters;
			disableDetectLinksInStrings = copyFrom.disableDetectLinksInStrings;
			labelParameter = copyFrom.labelParameter;
			uriAnchorText = copyFrom.uriAnchorText;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(HtmlSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesHtml = copyFrom.addBeanTypesHtml;
			addKeyValueTableHeaders = copyFrom.addKeyValueTableHeaders;
			disableDetectLabelParameters = ! copyFrom.detectLabelParameters;
			disableDetectLinksInStrings = ! copyFrom.detectLinksInStrings;
			labelParameter = copyFrom.labelParameter;
			uriAnchorText = copyFrom.uriAnchorText;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		public SELF addBeanTypesHtml() {
			return addBeanTypesHtml(true);
		}

		/**
		 * Same as {@link #addBeanTypesHtml()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF addBeanTypesHtml(boolean value) {
			addBeanTypesHtml = value;
			return self();
		}

		/**
		 * <i><l>HtmlSerializer</l> configuration property:&emsp;</i>  Add key/value headers on bean/map tables.
		 *
		 * <p>
		 * When enabled, <bc>key</bc> and <bc>value</bc> column headers are added to tables.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjson'>
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
		 * @return This object.
		 */
		public SELF addKeyValueTableHeaders() {
			return addKeyValueTableHeaders(true);
		}

		/**
		 * Same as {@link #addKeyValueTableHeaders()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF addKeyValueTableHeaders(boolean value) {
			addKeyValueTableHeaders = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HtmlSerializer build() {
			return cache(CACHE).build(HtmlSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

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
		 * <p class='bjson'>
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
		 * @return This object.
		 */
		public SELF disableDetectLabelParameters() {
			return disableDetectLabelParameters(true);
		}

		/**
		 * Same as {@link #disableDetectLabelParameters()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF disableDetectLabelParameters(boolean value) {
			disableDetectLabelParameters = value;
			return self();
		}

		/**
		 * <i><l>HtmlSerializer</l> configuration property:&emsp;</i>  Don't look for URLs in {@link String Strings}.
		 *
		 * <p>
		 * Disables the feature where if a string looks like a URL (i.e. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
		 * and make it into a hyperlink based on the rules specified by {@link Builder#uriAnchorText(AnchorText)}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjson'>
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
		 * @return This object.
		 */
		public SELF disableDetectLinksInStrings() {
			return disableDetectLinksInStrings(true);
		}

		/**
		 * Same as {@link #disableDetectLinksInStrings()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF disableDetectLinksInStrings(boolean value) {
			disableDetectLinksInStrings = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				addBeanTypesHtml,
				addKeyValueTableHeaders,
				disableDetectLabelParameters,
				disableDetectLinksInStrings,
				labelParameter,
				uriAnchorText
			);
			// @formatter:on
		}

		/**
		 * <i><l>HtmlSerializer</l> configuration property:&emsp;</i>  Link label parameter name.
		 *
		 * <p>
		 * The parameter name to look for when resolving link labels}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Cannot be <jk>null</jk>.
		 * 	<br>The default is <js>"label"</js>.
		 * @return This object.
		 */
		public SELF labelParameter(String value) {
			labelParameter = assertArgNotNull(ARG_value, value);
			return self();
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
		 * 			<p class='bjson'>
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
		 * 			<p class='bjson'>
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
		 * 			<p class='bjson'>
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
		 * 			<p class='bjson'>
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
		 * 			<p class='bjson'>
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
		 * 			<p class='bjson'>
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
		 * 			<p class='bjson'>
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
		 * 			<p class='bjson'>
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
		 * 	<br>Cannot be <jk>null</jk>.
		 * 	<br>The default is {@link AnchorText#TO_STRING}.
		 * @return This object.
		 */
		public SELF uriAnchorText(AnchorText value) {
			uriAnchorText = assertArgNotNull(ARG_value, value);
			return self();
		}


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link HtmlSerializer#create()} / {@link HtmlSerializer#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(HtmlSerializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/** Default serializer, single quotes. */
	public static class Sq extends HtmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Sq(Builder<?> builder) {
			super(builder.quoteChar('\''));
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends HtmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public SqReadable(Builder<?> builder) {
			super(builder.quoteChar('\'').useWhitespace());
		}
	}

	/** Default serializer, all default settings. */
	public static final HtmlSerializer DEFAULT = new HtmlSerializer(create());

	/** Default serializer, single quotes. */
	public static final HtmlSerializer DEFAULT_SQ = new HtmlSerializer.Sq(create());
	/** Default serializer, single quotes, whitespace added. */
	public static final HtmlSerializer DEFAULT_SQ_READABLE = new HtmlSerializer.SqReadable(create());

	/** Default serializer, single quotes, simplified (no JSON type tags on strings). */
	public static final HtmlSerializer DEFAULT_SIMPLE_SQ = new HtmlSerializer(create().sq().disableJsonTags());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	protected final boolean addBeanTypesHtml;
	protected final boolean addKeyValueTableHeaders;
	protected final boolean detectLabelParameters;
	protected final boolean detectLinksInStrings;
	protected final AnchorText uriAnchorText;
	protected final String labelParameter;

	private final Map<ClassMeta<?>,HtmlClassMeta> htmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,HtmlBeanPropertyMeta> htmlBeanPropertyMetas = new ConcurrentHashMap<>();

	private final AtomicReference<HtmlSchemaSerializer> schemaSerializer = new AtomicReference<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public HtmlSerializer(Builder<?> builder) {
		super(builder);
		addBeanTypesHtml = builder.addBeanTypesHtml;
		addKeyValueTableHeaders = builder.addKeyValueTableHeaders;
		detectLabelParameters = ! builder.disableDetectLabelParameters;
		detectLinksInStrings = ! builder.disableDetectLinksInStrings;
		labelParameter = builder.labelParameter;
		uriAnchorText = builder.uriAnchorText;
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public HtmlSerializerSession.Builder<?> createSession() {
		return HtmlSerializerSession.create(this);
	}

	@Override /* Overridden from HtmlMetaProvider */
	public HtmlBeanPropertyMeta getHtmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return HtmlBeanPropertyMeta.DEFAULT;
		return htmlBeanPropertyMetas.computeIfAbsent(bpm, k -> new HtmlBeanPropertyMeta(k.getDelegateFor(), this.getAnnotationProvider(), this));
	}

	@Override /* Overridden from HtmlMetaProvider */
	public HtmlClassMeta getHtmlClassMeta(ClassMeta<?> cm) {
		return htmlClassMetas.computeIfAbsent(cm, k -> new HtmlClassMeta(k, this));
	}

	/**
	 * Returns the schema serializer.
	 *
	 * @return The schema serializer.
	 */
	public HtmlSerializer getSchemaSerializer() {
		HtmlSchemaSerializer result = schemaSerializer.get();
		if (result == null) {
			result = HtmlSchemaSerializer.create().marshallingContext(getMarshallingContext()).build();
			if (! schemaSerializer.compareAndSet(null, result)) {
				result = schemaSerializer.get();
			}
		}
		return result;
	}

	@Override /* Overridden from Context */
	public HtmlSerializerSession getSession() { return createSession().build(); }

	/**
	 * Link label parameter name.
	 *
	 * @see Builder#labelParameter(String)
	 * @return
	 * 	The parameter name to look for when resolving link labels.
	 */
	protected final String getLabelParameter() { return labelParameter; }

	/**
	 * Anchor text source.
	 *
	 * @see Builder#uriAnchorText(AnchorText)
	 * @return
	 * 	When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * 	<xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 */
	protected final AnchorText getUriAnchorText() { return uriAnchorText; }

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Builder#addBeanTypesHtml()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() { return addBeanTypesHtml || super.isAddBeanTypes(); }

	/**
	 * Add key/value headers on bean/map tables.
	 *
	 * @see Builder#addKeyValueTableHeaders()
	 * @return
	 * 	<jk>true</jk> if <bc>key</bc> and <bc>value</bc> column headers are added to tables.
	 */
	protected final boolean isAddKeyValueTableHeaders() { return addKeyValueTableHeaders; }

	/**
	 * Look for link labels in URIs.
	 *
	 * @see Builder#disableDetectLabelParameters()
	 * @return
	 * 	<jk>true</jk> if we should look for URL label parameters (e.g. <js>"?label=foobar"</js>).
	 */
	protected final boolean isDetectLabelParameters() { return detectLabelParameters; }

	/**
	 * Look for URLs in {@link String Strings}.
	 *
	 * @see Builder#disableDetectLinksInStrings()
	 * @return
	 * 	<jk>true</jk> if we should automatically convert strings to URLs if they look like a URL.
	 */
	protected final boolean isDetectLinksInStrings() { return detectLinksInStrings; }

	@Override /* Overridden from XmlSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypesHtml, addBeanTypesHtml)
			.a(PROP_addKeyValueTableHeaders, addKeyValueTableHeaders)
			.a(PROP_detectLabelParameters, detectLabelParameters)
			.a(PROP_detectLinksInStrings, detectLinksInStrings)
			.a(PROP_labelParameter, labelParameter)
			.a(PROP_uriAnchorText, uriAnchorText);
	}
}