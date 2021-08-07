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
import org.apache.juneau.annotation.*;
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
@ConfigurableContext
public class HtmlSerializer extends XmlSerializer implements HtmlMetaProvider, HtmlCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "HtmlSerializer";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypes} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlSerializer#HTML_addBeanTypes HTML_addBeanTypes}
	 * 	<li><b>Name:</b>  <js>"HtmlSerializer.addBeanTypes.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>HtmlSerializer.addBeanTypes</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLSERIALIZER_ADDBEANTYPES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlConfig#addBeanTypes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlSerializerBuilder#addBeanTypes()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTML_addBeanTypes = PREFIX + ".addBeanTypes.b";

	/**
	 * Configuration property:  Add key/value headers on bean/map tables.
	 *
	 * <p>
	 * When enabled, <bc>key</bc> and <bc>value</bc> column headers are added to tables.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlSerializer#HTML_addKeyValueTableHeaders HTML_addKeyValueTableHeaders}
	 * 	<li><b>Name:</b>  <js>"HtmlSerializer.addKeyValueTableHeaders.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>HtmlSerializer.addKeyValueTableHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLSERIALIZER_ADDKEYVALUETABLEHEADERS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.Html#noTableHeaders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlSerializerBuilder#addKeyValueTableHeaders()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTML_addKeyValueTableHeaders = PREFIX + ".addKeyValueTableHeaders.b";

	/**
	 * Configuration property:  Link label parameter name.
	 *
	 * <p>
	 * The parameter name to look for when resolving link labels.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlSerializer#HTML_labelParameter HTML_labelParameter}
	 * 	<li><b>Name:</b>  <js>"HtmlSerializer.labelParameter.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>HtmlSerializer.labelParameter</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLSERIALIZER_LABELPARAMETER</c>
	 * 	<li><b>Default:</b>  <js>"label"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlConfig#labelParameter()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlSerializerBuilder#labelParameter(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTML_labelParameter = PREFIX + ".labelParameter.s";

	/**
	 * Configuration property:  Don't look for link labels in URIs.
	 *
	 * <p>
	 * Disables the feature where if the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlSerializer#HTML_disableDetectLabelParameters HTML_disableDetectLabelParameters}
	 * 	<li><b>Name:</b>  <js>"HtmlSerializer.disableDetectLabelParameters.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>HtmlSerializer.disableDetectLabelParameters</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLSERIALIZER_DONTDETECTLABELPARAMETERS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlConfig#disableDetectLabelParameters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlSerializerBuilder#disableDetectLabelParameters()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTML_disableDetectLabelParameters = PREFIX + ".disableDetectLabelParameters.b";

	/**
	 * Configuration property:  Don't look for URLs in {@link java.lang.String Strings}.
	 *
	 * <p>
	 * Disables the feature where if a string looks like a URL (i.e. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * and make it into a hyperlink based on the rules specified by {@link #HTML_uriAnchorText}.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlSerializer#HTML_disableDetectLinksInStrings HTML_disableDetectLinksInStrings}
	 * 	<li><b>Name:</b>  <js>"HtmlSerializer.disableDetectLinksInStrings.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>HtmlSerializer.disableDetectLinksInStrings</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLSERIALIZER_DISABLEDETECTLINKSINSTRINGS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlConfig#disableDetectLinksInStrings()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlSerializerBuilder#disableDetectLinksInStrings()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTML_disableDetectLinksInStrings = PREFIX + ".disableDetectLinksInStrings.b";

	/**
	 * Configuration property:  Anchor text source.
	 *
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * <xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.html.HtmlSerializer#HTML_uriAnchorText HTML_uriAnchorText}
	 * 	<li><b>Name:</b>  <js>"HtmlSerializer.uriAnchorText.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.html.AnchorText}
	 * 	<li><b>System property:</b>  <c>HtmlSerializer.uriAnchorText</c>
	 * 	<li><b>Environment variable:</b>  <c>HTMLSERIALIZER_URIANCHORTEXT</c>
	 * 	<li><b>Default:</b>  <js>"TO_STRING"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.Html#anchorText()}
	 * 			<li class='ja'>{@link org.apache.juneau.html.annotation.HtmlConfig#uriAnchorText()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.html.HtmlSerializerBuilder#uriAnchorText(AnchorText)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String HTML_uriAnchorText = PREFIX + ".uriAnchorText.s";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, all default settings. */
	public static final HtmlSerializer DEFAULT = new HtmlSerializer(ContextProperties.DEFAULT);

	/** Default serializer, single quotes. */
	public static final HtmlSerializer DEFAULT_SQ = new HtmlSerializer.Sq(ContextProperties.DEFAULT);

	/** Default serializer, single quotes, whitespace added. */
	public static final HtmlSerializer DEFAULT_SQ_READABLE = new HtmlSerializer.SqReadable(ContextProperties.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, single quotes. */
	public static class Sq extends HtmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param cp The property store containing all the settings for this object.
		 */
		public Sq(ContextProperties cp) {
			super(
				cp.copy()
					.setDefault(WSERIALIZER_quoteChar, '\'')
					.build()
			);
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends HtmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param cp The property store containing all the settings for this object.
		 */
		public SqReadable(ContextProperties cp) {
			super(
				cp.copy()
					.setDefault(WSERIALIZER_quoteChar, '\'')
					.setDefault(WSERIALIZER_useWhitespace, true)
					.build()
			);
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final AnchorText uriAnchorText;
	private final boolean
		detectLabelParameters,
		detectLinksInStrings,
		addKeyValueTableHeaders,
		addBeanTypes;
	private final String labelParameter;
	private final Map<ClassMeta<?>,HtmlClassMeta> htmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,HtmlBeanPropertyMeta> htmlBeanPropertyMetas = new ConcurrentHashMap<>();

	private volatile HtmlSchemaSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param cp
	 * 	The property store containing all the settings for this object.
	 */
	public HtmlSerializer(ContextProperties cp) {
		this(cp, "text/html", (String)null);
	}

	/**
	 * Constructor.
	 *
	 * @param cp
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of
	 * 	{@doc ExtRFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <c>produces</c>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	public HtmlSerializer(ContextProperties cp, String produces, String accept) {
		super(cp, produces, accept);
		uriAnchorText = cp.get(HTML_uriAnchorText, AnchorText.class).orElse(AnchorText.TO_STRING);
		detectLabelParameters = ! cp.getBoolean(HTML_disableDetectLabelParameters).orElse(false);
		detectLinksInStrings = ! cp.getBoolean(HTML_disableDetectLinksInStrings).orElse(false);
		labelParameter = cp.getString(HTML_labelParameter).orElse("label");
		addKeyValueTableHeaders = cp.getBoolean(HTML_addKeyValueTableHeaders).orElse(false);
		addBeanTypes = cp.getFirstBoolean(HTML_addBeanTypes, SERIALIZER_addBeanTypes).orElse(false);
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
			schemaSerializer = copy().build(HtmlSchemaSerializer.class);
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
	 * @see #HTML_addBeanTypes
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Add key/value headers on bean/map tables.
	 *
	 * @see #HTML_addKeyValueTableHeaders
	 * @return
	 * 	<jk>true</jk> if <bc>key</bc> and <bc>value</bc> column headers are added to tables.
	 */
	protected final boolean isAddKeyValueTableHeaders() {
		return addKeyValueTableHeaders;
	}

	/**
	 * Look for link labels in URIs.
	 *
	 * @see #HTML_disableDetectLabelParameters
	 * @return
	 * 	<jk>true</jk> if we should look for URL label parameters (e.g. <js>"?label=foobar"</js>).
	 */
	protected final boolean isDetectLabelParameters() {
		return detectLabelParameters;
	}

	/**
	 * Look for URLs in {@link String Strings}.
	 *
	 * @see #HTML_disableDetectLinksInStrings
	 * @return
	 * 	<jk>true</jk> if we should automatically convert strings to URLs if they look like a URL.
	 */
	protected final boolean isDetectLinksInStrings() {
		return detectLinksInStrings;
	}

	/**
	 * Link label parameter name.
	 *
	 * @see #HTML_labelParameter
	 * @return
	 * 	The parameter name to look for when resolving link labels.
	 */
	protected final String getLabelParameter() {
		return labelParameter;
	}

	/**
	 * Anchor text source.
	 *
	 * @see #HTML_uriAnchorText
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
					.a("addBeanTypes", addBeanTypes)
			);
	}
}
