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

import static org.apache.juneau.collections.JsonMap.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

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
 * 		JsonMap.<jsm>ofJson</jsm>(<js>"{firstName:'Bob',lastName:'Costas'}"</js>),
 * 		JsonMap.<jsm>ofJson</jsm>(<js>"{firstName:'Billy',lastName:'TheKid'}"</js>),
 * 		JsonMap.<jsm>ofJson</jsm>(<js>"{firstName:'Barney',lastName:'Miller'}"</js>)
 * 	);
 * 	String <jv>html</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>list</jv>);
 *
 * 	<jc>// Produces: </jc>
 * 	<jc>//    &lt;table&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;foo&lt;/td&gt;&lt;td&gt;bar&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;baz&lt;/td&gt;&lt;td&gt;123&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//    &lt;/table&gt; </jc>
 * 	Map <jv>map</jv> = JsonMap.<jsm>ofJson</jsm>(<js>"{foo:'bar',baz:123}"</js>);
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
 * 	Map <jv>map</jv> = JsonMap.<jsm>ofJson</jsm>(<js>"{foo:'bar',baz:123}"</js>);
 * 	<jv>map</jv>.put(<js>"someNumbers"</js>, JsonList.<jsm>of</jsm>(1, 2, 3));
 * 	<jv>map</jv>.put(<js>"someSubMap"</js>, JsonMap.<jsm>ofJson</jsm>(<js>"{a:'b'}"</js>));
 * 	String <jv>html</jv> = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>map</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class HtmlSerializer extends XmlSerializer implements HtmlMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings. */
	public static final HtmlSerializer DEFAULT = new HtmlSerializer(create());

	/** Default serializer, single quotes. */
	public static final HtmlSerializer DEFAULT_SQ = new HtmlSerializer.Sq(create());

	/** Default serializer, single quotes, whitespace added. */
	public static final HtmlSerializer DEFAULT_SQ_READABLE = new HtmlSerializer.SqReadable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, single quotes. */
	public static class Sq extends HtmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Sq(Builder builder) {
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
		public SqReadable(Builder builder) {
			super(builder.quoteChar('\'').useWhitespace());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends XmlSerializer.Builder {

		private static final Cache<HashKey,HtmlSerializer> CACHE = Cache.of(HashKey.class, HtmlSerializer.class).build();

		boolean addBeanTypesHtml, addKeyValueTableHeaders, disableDetectLabelParameters, disableDetectLinksInStrings;
		String labelParameter;
		AnchorText uriAnchorText;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			produces("text/html");
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
		protected Builder(HtmlSerializer copyFrom) {
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
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			addBeanTypesHtml = copyFrom.addBeanTypesHtml;
			addKeyValueTableHeaders = copyFrom.addKeyValueTableHeaders;
			disableDetectLabelParameters = copyFrom.disableDetectLabelParameters;
			disableDetectLinksInStrings = copyFrom.disableDetectLinksInStrings;
			labelParameter = copyFrom.labelParameter;
			uriAnchorText = copyFrom.uriAnchorText;
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public HtmlSerializer build() {
			return cache(CACHE).build(HtmlSerializer.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				addBeanTypesHtml,
				addKeyValueTableHeaders,
				disableDetectLabelParameters,
				disableDetectLinksInStrings,
				labelParameter,
				uriAnchorText
			);
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
		 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder addBeanTypesHtml() {
			return addBeanTypesHtml(true);
		}

		/**
		 * Same as {@link #addBeanTypesHtml()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addBeanTypesHtml(boolean value) {
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
		@FluentSetter
		public Builder addKeyValueTableHeaders() {
			return addKeyValueTableHeaders(true);
		}

		/**
		 * Same as {@link #addKeyValueTableHeaders()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder addKeyValueTableHeaders(boolean value) {
			addKeyValueTableHeaders = value;
			return this;
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
		@FluentSetter
		public Builder disableDetectLinksInStrings() {
			return disableDetectLinksInStrings(true);
		}

		/**
		 * Same as {@link #disableDetectLinksInStrings()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableDetectLinksInStrings(boolean value) {
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder labelParameter(String value) {
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
		@FluentSetter
		public Builder disableDetectLabelParameters() {
			return disableDetectLabelParameters(true);
		}

		/**
		 * Same as {@link #disableDetectLabelParameters()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder disableDetectLabelParameters(boolean value) {
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
		 * 	<br>The default is {@link AnchorText#TO_STRING}.
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriAnchorText(AnchorText value) {
			uriAnchorText = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder detectRecursions(boolean value) {
			super.detectRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder ignoreRecursions(boolean value) {
			super.ignoreRecursions(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanTraverseContext.Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addBeanTypes(boolean value) {
			super.addBeanTypes(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder addRootType(boolean value) {
			super.addRootType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder keepNullProperties(boolean value) {
			super.keepNullProperties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
			super.listener(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortCollections(boolean value) {
			super.sortCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder sortMaps(boolean value) {
			super.sortMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyCollections(boolean value) {
			super.trimEmptyCollections(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimEmptyMaps(boolean value) {
			super.trimEmptyMaps(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.Serializer.Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder quoteCharOverride(char value) {
			super.quoteCharOverride(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializer.Builder */
		public Builder ws() {
			super.ws();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder addBeanTypesXml() {
			super.addBeanTypesXml();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder addBeanTypesXml(boolean value) {
			super.addBeanTypesXml(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder addNamespaceUrisToRoot() {
			super.addNamespaceUrisToRoot();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder addNamespaceUrisToRoot(boolean value) {
			super.addNamespaceUrisToRoot(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder defaultNamespace(Namespace value) {
			super.defaultNamespace(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder disableAutoDetectNamespaces() {
			super.disableAutoDetectNamespaces();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder disableAutoDetectNamespaces(boolean value) {
			super.disableAutoDetectNamespaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder enableNamespaces() {
			super.enableNamespaces();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder enableNamespaces(boolean value) {
			super.enableNamespaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder namespaces(Namespace...values) {
			super.namespaces(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.xml.XmlSerializer.Builder */
		public Builder ns() {
			super.ns();
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final AnchorText uriAnchorText;
	final boolean
		detectLabelParameters,
		detectLinksInStrings,
		addKeyValueTableHeaders,
		addBeanTypesHtml;
	final String labelParameter;

	private final Map<ClassMeta<?>,HtmlClassMeta> htmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,HtmlBeanPropertyMeta> htmlBeanPropertyMetas = new ConcurrentHashMap<>();

	private volatile HtmlSchemaSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public HtmlSerializer(Builder builder) {
		super(builder);
		detectLabelParameters = ! builder.disableDetectLabelParameters;
		detectLinksInStrings = ! builder.disableDetectLinksInStrings;
		addKeyValueTableHeaders = builder.addKeyValueTableHeaders;
		labelParameter = builder.labelParameter;
		uriAnchorText = builder.uriAnchorText;
		addBeanTypesHtml = builder.addBeanTypesHtml;
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public HtmlSerializerSession.Builder createSession() {
		return HtmlSerializerSession.create(this);
	}

	@Override /* Context */
	public HtmlSerializerSession getSession() {
		return createSession().build();
	}

	/**
	 * Returns the schema serializer.
	 *
	 * @return The schema serializer.
	 */
	public HtmlSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = HtmlSchemaSerializer.create().beanContext(getBeanContext()).build();
		return schemaSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HtmlMetaProvider */
	public HtmlClassMeta getHtmlClassMeta(ClassMeta<?> cm) {
		HtmlClassMeta m = htmlClassMetas.get(cm);
		if (m == null) {
			m = new HtmlClassMeta(cm, this);
			htmlClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* HtmlMetaProvider */
	public HtmlBeanPropertyMeta getHtmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return HtmlBeanPropertyMeta.DEFAULT;
		HtmlBeanPropertyMeta m = htmlBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new HtmlBeanPropertyMeta(bpm.getDelegateFor(), this);
			htmlBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Builder#addBeanTypesHtml()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypesHtml || super.isAddBeanTypes();
	}

	/**
	 * Add key/value headers on bean/map tables.
	 *
	 * @see Builder#addKeyValueTableHeaders()
	 * @return
	 * 	<jk>true</jk> if <bc>key</bc> and <bc>value</bc> column headers are added to tables.
	 */
	protected final boolean isAddKeyValueTableHeaders() {
		return addKeyValueTableHeaders;
	}

	/**
	 * Look for link labels in URIs.
	 *
	 * @see Builder#disableDetectLabelParameters()
	 * @return
	 * 	<jk>true</jk> if we should look for URL label parameters (e.g. <js>"?label=foobar"</js>).
	 */
	protected final boolean isDetectLabelParameters() {
		return detectLabelParameters;
	}

	/**
	 * Look for URLs in {@link String Strings}.
	 *
	 * @see Builder#disableDetectLinksInStrings()
	 * @return
	 * 	<jk>true</jk> if we should automatically convert strings to URLs if they look like a URL.
	 */
	protected final boolean isDetectLinksInStrings() {
		return detectLinksInStrings;
	}

	/**
	 * Link label parameter name.
	 *
	 * @see Builder#labelParameter(String)
	 * @return
	 * 	The parameter name to look for when resolving link labels.
	 */
	protected final String getLabelParameter() {
		return labelParameter;
	}

	/**
	 * Anchor text source.
	 *
	 * @see Builder#uriAnchorText(AnchorText)
	 * @return
	 * 	When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * 	<xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 */
	protected final AnchorText getUriAnchorText() {
		return uriAnchorText;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap()
			.append("uriAnchorText", uriAnchorText)
			.append("detectLabelParameters", detectLabelParameters)
			.append("detectLinksInStrings", detectLinksInStrings)
			.append("labelParameter", labelParameter)
			.append("addKeyValueTableHeaders", addKeyValueTableHeaders)
			.append("addBeanTypesHtml", addBeanTypesHtml);
	}
}
