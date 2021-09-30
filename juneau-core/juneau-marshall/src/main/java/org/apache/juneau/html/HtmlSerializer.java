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
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJO models to HTML.
 * {@review}
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/html</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/html</bc>
 *
 * <h5 class='topic'>Description</h5>
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
 * <h5 class='topic'>Behavior-specific subclasses</h5>
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
 * <p class='bcode w800'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 	<jc>// Create a custom serializer that doesn't use whitespace and newlines</jc>
 * 	HtmlSerializer serializer = <jk>new</jk> HtmlSerializerBuider().ws().build();
 *
 * 	<jc>// Same as above, except uses cloning</jc>
 * 	HtmlSerializer serializer = HtmlSerializer.<jsf>DEFAULT</jsf>.copy().ws().build();
 *
 * 	<jc>// Serialize POJOs to HTML</jc>
 *
 * 	<jc>// Produces: </jc>
 * 	<jc>// &lt;ul&gt;&lt;li&gt;1&lt;li&gt;2&lt;li&gt;3&lt;/ul&gt;</jc>
 * 	List l = OList.<jsm>of</jsm>(1, 2, 3);
 * 	String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(l);
 *
 * 	<jc>// Produces: </jc>
 * 	<jc>//    &lt;table&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;th&gt;firstName&lt;/th&gt;&lt;th&gt;lastName&lt;/th&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;Bob&lt;/td&gt;&lt;td&gt;Costas&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;Billy&lt;/td&gt;&lt;td&gt;TheKid&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;Barney&lt;/td&gt;&lt;td&gt;Miller&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//    &lt;/table&gt; </jc>
 * 	l = OList.<jsm>of</jsm>(
 * 		OMap.<jsm>ofJson</jsm>(<js>"{firstName:'Bob',lastName:'Costas'}"</js>),
 * 		OMap.<jsm>ofJson</jsm>(<js>"{firstName:'Billy',lastName:'TheKid'}"</js>),
 * 		OMap.<jsm>ofJson</jsm>(<js>"{firstName:'Barney',lastName:'Miller'}"</js>)
 * 	);
 * 	String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(l);
 *
 * 	<jc>// Produces: </jc>
 * 	<jc>//    &lt;table&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;th&gt;key&lt;/th&gt;&lt;th&gt;value&lt;/th&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;foo&lt;/td&gt;&lt;td&gt;bar&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//       &lt;tr&gt;&lt;td&gt;baz&lt;/td&gt;&lt;td&gt;123&lt;/td&gt;&lt;/tr&gt; </jc>
 * 	<jc>//    &lt;/table&gt; </jc>
 * 	Map m = OMap.<jsm>ofJson</jsm>(<js>"{foo:'bar',baz:123}"</js>);
 * 	String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(m);
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
 * 	Map m = OMap.<jsm>ofJson</jsm>(<js>"{foo:'bar',baz:123}"</js>);
 * 	m.put(<js>"someNumbers"</js>, OList.<jsm>of</jsm>(1, 2, 3));
 * 	m.put(<js>"someSubMap"</js>, OMap.<jsm>ofJson</jsm>(<js>"{a:'b'}"</js>));
 * 	String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(m);
 * </p>
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
		protected Sq(HtmlSerializerBuilder builder) {
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
		protected SqReadable(HtmlSerializerBuilder builder) {
			super(builder.quoteChar('\'').useWhitespace());
		}
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
	protected HtmlSerializer(HtmlSerializerBuilder builder) {
		super(builder);
		detectLabelParameters = ! builder.disableDetectLabelParameters;
		detectLinksInStrings = ! builder.disableDetectLinksInStrings;
		addKeyValueTableHeaders = builder.addKeyValueTableHeaders;
		labelParameter = builder.labelParameter;
		uriAnchorText = builder.uriAnchorText;
		addBeanTypesHtml = builder.addBeanTypesHtml;
	}

	@Override /* Context */
	public HtmlSerializerBuilder copy() {
		return new HtmlSerializerBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link HtmlSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> HtmlSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link HtmlSerializerBuilder} object.
	 */
	public static HtmlSerializerBuilder create() {
		return new HtmlSerializerBuilder();
	}

	@Override /* Serializer */
	public HtmlSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public HtmlSerializerSession createSession(SerializerSessionArgs args) {
		return new HtmlSerializerSession(this, args);
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
	 * @see HtmlSerializerBuilder#addBeanTypesHtml()
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
	 * @see HtmlSerializerBuilder#addKeyValueTableHeaders()
	 * @return
	 * 	<jk>true</jk> if <bc>key</bc> and <bc>value</bc> column headers are added to tables.
	 */
	protected final boolean isAddKeyValueTableHeaders() {
		return addKeyValueTableHeaders;
	}

	/**
	 * Look for link labels in URIs.
	 *
	 * @see HtmlSerializerBuilder#disableDetectLabelParameters()
	 * @return
	 * 	<jk>true</jk> if we should look for URL label parameters (e.g. <js>"?label=foobar"</js>).
	 */
	protected final boolean isDetectLabelParameters() {
		return detectLabelParameters;
	}

	/**
	 * Look for URLs in {@link String Strings}.
	 *
	 * @see HtmlSerializerBuilder#disableDetectLinksInStrings()
	 * @return
	 * 	<jk>true</jk> if we should automatically convert strings to URLs if they look like a URL.
	 */
	protected final boolean isDetectLinksInStrings() {
		return detectLinksInStrings;
	}

	/**
	 * Link label parameter name.
	 *
	 * @see HtmlSerializerBuilder#labelParameter(String)
	 * @return
	 * 	The parameter name to look for when resolving link labels.
	 */
	protected final String getLabelParameter() {
		return labelParameter;
	}

	/**
	 * Anchor text source.
	 *
	 * @see HtmlSerializerBuilder#uriAnchorText(AnchorText)
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
	public OMap toMap() {
		return super.toMap()
			.a(
				"HtmlSerializer",
				OMap
					.create()
					.filtered()
					.a("uriAnchorText", uriAnchorText)
					.a("detectLabelParameters", detectLabelParameters)
					.a("detectLinksInStrings", detectLinksInStrings)
					.a("labelParameter", labelParameter)
					.a("addKeyValueTableHeaders", addKeyValueTableHeaders)
					.a("addBeanTypesHtml", addBeanTypesHtml)
			);
	}
}
