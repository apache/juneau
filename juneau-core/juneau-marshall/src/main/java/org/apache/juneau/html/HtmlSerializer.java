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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJO models to HTML.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>text/html</code>
 *
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 * <h5 class='section'>Description:</h5>
 *
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
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 *
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link Sq} - Default serializer, single quotes.
 * 	<li>
 * 		{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 		<jc>// Create a custom serializer that doesn't use whitespace and newlines</jc>
 * 		HtmlSerializer serializer = <jk>new</jk> HtmlSerializerBuider().ws().build();
 *
 * 		<jc>// Same as above, except uses cloning</jc>
 * 		HtmlSerializer serializer = HtmlSerializer.<jsf>DEFAULT</jsf>.builder().ws().build();
 *
 * 		<jc>// Serialize POJOs to HTML</jc>
 *
 * 		<jc>// Produces: </jc>
 * 		<jc>// &lt;ul&gt;&lt;li&gt;1&lt;li&gt;2&lt;li&gt;3&lt;/ul&gt;</jc>
 * 		List l = new ObjectList(1, 2, 3);
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(l);
 *
 * 		<jc>// Produces: </jc>
 * 		<jc>//    &lt;table&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;th&gt;firstName&lt;/th&gt;&lt;th&gt;lastName&lt;/th&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;Bob&lt;/td&gt;&lt;td&gt;Costas&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;Billy&lt;/td&gt;&lt;td&gt;TheKid&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;Barney&lt;/td&gt;&lt;td&gt;Miller&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//    &lt;/table&gt; </jc>
 * 		l = <jk>new</jk> ObjectList();
 * 		l.add(<jk>new</jk> ObjectMap(<js>"{firstName:'Bob',lastName:'Costas'}"</js>));
 * 		l.add(<jk>new</jk> ObjectMap(<js>"{firstName:'Billy',lastName:'TheKid'}"</js>));
 * 		l.add(<jk>new</jk> ObjectMap(<js>"{firstName:'Barney',lastName:'Miller'}"</js>));
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(l);
 *
 * 		<jc>// Produces: </jc>
 * 		<jc>//    &lt;table&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;foo&lt;/td&gt;&lt;td&gt;bar&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//       &lt;tr&gt;&lt;td&gt;baz&lt;/td&gt;&lt;td&gt;123&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//    &lt;/table&gt; </jc>
 * 		Map m = <jk>new</jk> ObjectMap(<js>"{foo:'bar',baz:123}"</js>);
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(m);
 *
 * 		<jc>// HTML elements can be nested arbitrarily deep</jc>
 * 		<jc>// Produces: </jc>
 * 		<jc>//	&lt;table&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;td&gt;foo&lt;/td&gt;&lt;td&gt;bar&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;td&gt;baz&lt;/td&gt;&lt;td&gt;123&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;td&gt;someNumbers&lt;/td&gt;&lt;td&gt;&lt;ul&gt;&lt;li&gt;1&lt;li&gt;2&lt;li&gt;3&lt;/ul&gt;&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//		&lt;tr&gt;&lt;td&gt;someSubMap&lt;/td&gt;&lt;td&gt; </jc>
 * 		<jc>//			&lt;table&gt; </jc>
 * 		<jc>//				&lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 		<jc>//				&lt;tr&gt;&lt;td&gt;a&lt;/td&gt;&lt;td&gt;b&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//			&lt;/table&gt; </jc>
 * 		<jc>//		&lt;/td&gt;&lt;/tr&gt; </jc>
 * 		<jc>//	&lt;/table&gt; </jc>
 * 		Map m = <jk>new</jk> ObjectMap(<js>"{foo:'bar',baz:123}"</js>);
 * 		m.put("someNumbers", new ObjectList(1, 2, 3));
 * 		m.put(<js>"someSubMap"</js>, new ObjectMap(<js>"{a:'b'}"</js>));
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(m);
 * </p>
 */
public class HtmlSerializer extends XmlSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "HtmlSerializer.";

	/**
	 * <b>Configuration property:</b>  Anchor text source.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.uriAnchorText"</js>
	 * 	<li><b>Data type:</b> <code>AnchorText</code>
	 * 	<li><b>Default:</b> <jsf>TO_STRING</jsf>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * <xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 *
	 * <p>
	 * See the {@link AnchorText} enum for possible values.
	 */
	public static final String HTML_uriAnchorText = PREFIX + "uriAnchorText";

	/**
	 * <b>Configuration property:</b>  Look for URLs in {@link String Strings}.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.detectLinksInStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If a string looks like a URL (e.g. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * and make it into a hyperlink based on the rules specified by {@link #HTML_uriAnchorText}.
	 */
	public static final String HTML_detectLinksInStrings = PREFIX + "detectLinksInStrings";

	/**
	 * <b>Configuration property:</b>  Look for link labels in the <js>"label"</js> parameter of the URL.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.lookForLabelParameters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 *
	 * <p>
	 * The parameter name can be changed via the {@link #HTML_labelParameter} property.
	 */
	public static final String HTML_lookForLabelParameters = PREFIX + "lookForLabelParameters";

	/**
	 * <b>Configuration property:</b>  The parameter name to use when using {@link #HTML_lookForLabelParameters}.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.labelParameter"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"label"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 */
	public static final String HTML_labelParameter = PREFIX + "labelParameter";

	/**
	 * <b>Configuration property:</b>  Add key/value headers on bean/map tables.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.addKeyValueTableHeaders"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 */
	public static final String HTML_addKeyValueTableHeaders = PREFIX + "addKeyValueTableHeaders";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.addBeanTypeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined
	 * from the value type.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypeProperties} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String HTML_addBeanTypeProperties = PREFIX + "addBeanTypeProperties";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings. */
	public static final HtmlSerializer DEFAULT = new HtmlSerializer(PropertyStore.create());

	/** Default serializer, single quotes. */
	public static final HtmlSerializer DEFAULT_SQ = new HtmlSerializer.Sq(PropertyStore.create());

	/** Default serializer, single quotes, whitespace added. */
	public static final HtmlSerializer DEFAULT_SQ_READABLE = new HtmlSerializer.SqReadable(PropertyStore.create());


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, single quotes. */
	public static class Sq extends HtmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Sq(PropertyStore propertyStore) {
			super(propertyStore.copy().append(SERIALIZER_quoteChar, '\''));
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends HtmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public SqReadable(PropertyStore propertyStore) {
			super(propertyStore.copy().append(SERIALIZER_quoteChar, '\'').append(SERIALIZER_useWhitespace, true));
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final HtmlSerializerContext ctx;
	private volatile HtmlSchemaDocSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param propertyStore
	 * 	The property store containing all the settings for this object.
	 */
	public HtmlSerializer(PropertyStore propertyStore) {
		this(propertyStore, "text/html");
	}

	/**
	 * Constructor.
	 *
	 * @param propertyStore
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC2616/14.1</a>
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"application/json"</js>, <js>"text/json"</js>);</code>
	 * 	<br>...or...
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);</code>
	 */
	public HtmlSerializer(PropertyStore propertyStore, String produces, String...accept) {
		super(propertyStore, produces, accept);
		this.ctx = createContext(HtmlSerializerContext.class);
	}

	@Override /* CoreObject */
	public HtmlSerializerBuilder builder() {
		return new HtmlSerializerBuilder(propertyStore);
	}

	@Override /* XmlSerializer */
	public HtmlSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = new HtmlSchemaDocSerializer(propertyStore);
		return schemaSerializer;
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlSerializerSession(ctx, args);
	}
}
