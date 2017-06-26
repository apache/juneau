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

import static org.apache.juneau.html.HtmlSerializer.ContentResult.*;
import static org.apache.juneau.serializer.SerializerContext.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Serializes POJO models to HTML.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>text/html</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * The conversion is as follows...
 * 	<ul class='spaced-list'>
 * 		<li>{@link Map Maps} (e.g. {@link HashMap}, {@link TreeMap}) and beans are converted to HTML tables with
 * 			'key' and 'value' columns.
 * 		<li>{@link Collection Collections} (e.g. {@link HashSet}, {@link LinkedList}) and Java arrays are converted
 * 			to HTML ordered lists.
 * 		<li>{@code Collections} of {@code Maps} and beans are converted to HTML tables with keys as headers.
 * 		<li>Everything else is converted to text.
 * 	</ul>
 * <p>
 * This serializer provides several serialization options.  Typically, one of the predefined <jsf>DEFAULT</jsf>
 * serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 * <p>
 * The {@link HtmlLink} annotation can be used on beans to add hyperlinks to the output.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul class='spaced-list'>
 * 	<li>{@link HtmlSerializerContext}
 * </ul>
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
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
@Produces("text/html")
@SuppressWarnings("hiding")
public class HtmlSerializer extends XmlSerializer {

	/** Default serializer, all default settings. */
	public static final HtmlSerializer DEFAULT = new HtmlSerializer(PropertyStore.create());

	/** Default serializer, single quotes. */
	public static final HtmlSerializer DEFAULT_SQ = new HtmlSerializer.Sq(PropertyStore.create());

	/** Default serializer, single quotes, whitespace added. */
	public static final HtmlSerializer DEFAULT_SQ_READABLE = new HtmlSerializer.SqReadable(PropertyStore.create());


	/** Default serializer, single quotes. */
	public static class Sq extends HtmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Sq(PropertyStore propertyStore) {
			super(propertyStore);
		}


		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(SERIALIZER_quoteChar, '\'');
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
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(SERIALIZER_quoteChar, '\'').append(SERIALIZER_useWhitespace, true);
		}
	}


	final HtmlSerializerContext ctx;
	private volatile HtmlSchemaDocSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public HtmlSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(HtmlSerializerContext.class);
	}

	@Override /* CoreObject */
	public HtmlSerializerBuilder builder() {
		return new HtmlSerializerBuilder(propertyStore);
	}

	/**
	 * Main serialization routine.
	 *
	 * @param session The serialization context object.
	 * @param o The object being serialized.
	 * @param w The writer to serialize to.
	 * @return The same writer passed in.
	 * @throws IOException If a problem occurred trying to send output to the writer.
	 */
	private HtmlWriter doSerialize(HtmlSerializerSession session, Object o, HtmlWriter w) throws Exception {
		serializeAnything(session, w, o, session.getExpectedRootType(o), null, session.getInitialDepth()-1, null, true);
		return w;
	}

	/**
	 * Serialize the specified object to the specified writer.
	 *
	 * @param session The context object that lives for the duration of this serialization.
	 * @param out The writer.
	 * @param o The object to serialize.
	 * @param eType The expected type of the object if this is a bean property.
	 * @param name The attribute name of this object if this object was a field in a JSON object (i.e. key of a
	 * {@link java.util.Map.Entry} or property name of a bean).
	 * @param indent The current indentation value.
	 * @param pMeta The bean property being serialized, or <jk>null</jk> if we're not serializing a bean property.
	 * @param isRoot <jk>true</jk> if this is the root element of the document.
	 * @return The type of content encountered.  Either simple (no whitespace) or normal (elements with whitespace).
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected ContentResult serializeAnything(HtmlSerializerSession session, HtmlWriter out, Object o,
			ClassMeta<?> eType, String name, int indent, BeanPropertyMeta pMeta, boolean isRoot) throws Exception {

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;     // The wrapped type (delegate)
		ClassMeta<?> sType = object();   // The serialized type

		if (eType == null)
			eType = object();

		aType = session.push(name, o, eType);

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		session.indent += indent;

		ContentResult cr = CR_NORMAL;

		// Determine the type.
		if (o == null || (aType.isChar() && ((Character)o).charValue() == 0)) {
			out.tag("null");
			cr = ContentResult.CR_SIMPLE;

		} else {

			if (aType.isDelegate()) {
				wType = aType;
				aType = ((Delegate)o).getClassMeta();
			}

			sType = aType.getSerializedClassMeta();
			String typeName = null;
			if (session.isAddBeanTypeProperties() && ! eType.equals(aType))
				typeName = aType.getDictionaryName();

			// Swap if necessary
			PojoSwap swap = aType.getPojoSwap();
			if (swap != null) {
				o = swap.swap(session, o);

				// If the getSwapClass() method returns Object, we need to figure out
				// the actual type now.
				if (sType.isObject())
					sType = session.getClassMetaForObject(o);
			}

			HtmlClassMeta html = sType.getExtendedMeta(HtmlClassMeta.class);
			HtmlRender render = (pMeta == null ? null : pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).getRender());
			if (render == null)
				render = html.getRender();

			if (render != null) {
				Object o2 = render.getContent(session, o);
				if (o2 != o)
					return serializeAnything(session, out, o2, null, typeName, 2, null, false);
			}

			if (html.isAsXml() || (pMeta != null && pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).isAsXml())) {
				super.serializeAnything(session, out, o, null, null, null, false, XmlFormat.MIXED, false, false, null);

			} else if (html.isAsPlainText() || (pMeta != null && pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).isAsPlainText())) {
				out.write(o == null ? "null" : o.toString());
				cr = CR_SIMPLE;

			} else if (o == null || (sType.isChar() && ((Character)o).charValue() == 0)) {
				out.tag("null");
				cr = CR_SIMPLE;

			} else if (sType.isNumber()) {
				if (eType.isNumber() && ! isRoot)
					out.append(o);
				else
					out.sTag("number").append(o).eTag("number");
				cr = CR_SIMPLE;

			} else if (sType.isBoolean()) {
				if (eType.isBoolean() && ! isRoot)
					out.append(o);
				else
					out.sTag("boolean").append(o).eTag("boolean");
				cr = CR_SIMPLE;

			} else if (sType.isMap() || (wType != null && wType.isMap())) {
				out.nlIf(! isRoot, indent+1);
				if (o instanceof BeanMap)
					serializeBeanMap(session, out, (BeanMap)o, eType, pMeta);
				else
					serializeMap(session, out, (Map)o, sType, eType.getKeyType(), eType.getValueType(), typeName, pMeta);

			} else if (sType.isBean()) {
				BeanMap m = session.toBeanMap(o);
				Class<?> c = o.getClass();
				if (c.isAnnotationPresent(HtmlLink.class)) {
					HtmlLink h = o.getClass().getAnnotation(HtmlLink.class);
					Object urlProp = m.get(h.hrefProperty());
					Object nameProp = m.get(h.nameProperty());
					out.oTag("a").attrUri("href", urlProp).append('>').text(nameProp).eTag("a");
					cr = CR_SIMPLE;
				} else {
					out.nlIf(! isRoot, indent+2);
					serializeBeanMap(session, out, m, eType, pMeta);
				}

			} else if (sType.isCollection() || sType.isArray() || (wType != null && wType.isCollection())) {
				out.nlIf(! isRoot, indent+1);
				serializeCollection(session, out, o, sType, eType, name, pMeta);

			} else if (session.isUri(sType, pMeta, o)) {
				String label = session.getAnchorText(pMeta, o);
				out.oTag("a").attrUri("href", o).append('>');
				out.text(label);
				out.eTag("a");
				cr = CR_SIMPLE;

			} else {
				if (isRoot)
					out.sTag("string").text(session.toString(o)).eTag("string");
				else
					out.text(session.toString(o));
				cr = CR_SIMPLE;
			}
		}
		session.pop();
		session.indent -= indent;
		return cr;
	}

	/**
	 * Identifies what the contents were of a serialized bean.
	 */
	static enum ContentResult {
		CR_SIMPLE,    // Simple content.  Shouldn't use whitespace.
		CR_NORMAL     // Normal content.  Use whitespace.
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeMap(HtmlSerializerSession session, HtmlWriter out, Map m, ClassMeta<?> sType,
			ClassMeta<?> eKeyType, ClassMeta<?> eValueType, String typeName, BeanPropertyMeta ppMeta) throws Exception {

		ClassMeta<?> keyType = eKeyType == null ? session.string() : eKeyType;
		ClassMeta<?> valueType = eValueType == null ? session.object() : eValueType;
		ClassMeta<?> aType = session.getClassMetaForObject(m);       // The actual type

		int i = session.getIndent();

		out.oTag(i, "table");

		if (typeName != null && ppMeta != null && ppMeta.getClassMeta() != aType)
			out.attr(session.getBeanTypePropertyName(sType), typeName);

		out.append(">").nl(i+1);
		if (session.isAddKeyValueTableHeaders() && ! (aType.getExtendedMeta(HtmlClassMeta.class).isNoTableHeaders()
				|| (ppMeta != null && ppMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).isNoTableHeaders()))) {
			out.sTag(i+1, "tr").nl(i+2);
			out.sTag(i+2, "th").append("key").eTag("th").nl(i+3);
			out.sTag(i+2, "th").append("value").eTag("th").nl(i+3);
			out.ie(i+1).eTag("tr").nl(i+2);
		}
		for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {

			Object key = session.generalize(e.getKey(), keyType);
			Object value = null;
			try {
				value = e.getValue();
			} catch (StackOverflowError t) {
				throw t;
			} catch (Throwable t) {
				session.onError(t, "Could not call getValue() on property ''{0}'', {1}", e.getKey(), t.getLocalizedMessage());
			}

			String link = getLink(ppMeta);
			String style = getStyle(session, ppMeta, value);

			out.sTag(i+1, "tr").nl(i+2);
			out.oTag(i+2, "td");
			if (style != null)
				out.attr("style", style);
			out.cTag();
			if (link != null)
				out.oTag(i+3, "a").attrUri("href", link.replace("{#}", StringUtils.toString(value))).cTag();
			ContentResult cr = serializeAnything(session, out, key, keyType, null, 2, null, false);
			if (link != null)
				out.eTag("a");
			if (cr == CR_NORMAL)
				out.i(i+2);
			out.eTag("td").nl(i+2);
			out.sTag(i+2, "td");
			cr = serializeAnything(session, out, value, valueType, (key == null ? "_x0000_" : session.toString(key)), 2, null, false);
			if (cr == CR_NORMAL)
				out.ie(i+2);
			out.eTag("td").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}
		out.ie(i).eTag("table").nl(i);
	}

	private void serializeBeanMap(HtmlSerializerSession session, HtmlWriter out, BeanMap<?> m, ClassMeta<?> eType,
			BeanPropertyMeta ppMeta) throws Exception {
		int i = session.getIndent();

		out.oTag(i, "table");

		String typeName = m.getMeta().getDictionaryName();
		if (typeName != null && eType != m.getClassMeta())
			out.attr(session.getBeanTypePropertyName(m.getClassMeta()), typeName);

		out.append('>').nl(i);
		if (session.isAddKeyValueTableHeaders() && ! (m.getClassMeta().getExtendedMeta(HtmlClassMeta.class).isNoTableHeaders()
				|| (ppMeta != null && ppMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).isNoTableHeaders()))) {
			out.sTag(i+1, "tr").nl(i+1);
			out.sTag(i+2, "th").append("key").eTag("th").nl(i+2);
			out.sTag(i+2, "th").append("value").eTag("th").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}

		for (BeanPropertyValue p : m.getValues(session.isTrimNulls())) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();

			String key = p.getName();
			Object value = p.getValue();
			Throwable t = p.getThrown();
			if (t != null)
				session.onBeanGetterException(pMeta, t);

			if (session.canIgnoreValue(cMeta, key, value))
				continue;

			String link = cMeta.isCollectionOrArray() ? null : getLink(pMeta);

			out.sTag(i+1, "tr").nl(i+1);
			out.sTag(i+2, "td").text(key).eTag("td").nl(i+2);
			out.oTag(i+2, "td");
			String style = getStyle(session, pMeta, value);
			if (style != null)
				out.attr("style", style);
			out.cTag();

			try {
				if (link != null)
					out.oTag(i+3, "a").attrUri("href", m.resolveVars(link)).cTag();
				ContentResult cr = serializeAnything(session, out, value, cMeta, key, 2, pMeta, false);
				if (cr == CR_NORMAL)
					out.i(i+2);
				if (link != null)
					out.eTag("a");
			} catch (SerializeException e) {
				throw e;
			} catch (Error e) {
				throw e;
			} catch (Throwable e) {
				e.printStackTrace();
				session.onBeanGetterException(pMeta, e);
			}
			out.eTag("td").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}
		out.ie(i).eTag("table").nl(i);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeCollection(HtmlSerializerSession session, HtmlWriter out, Object in, ClassMeta<?> sType,
			ClassMeta<?> eType, String name, BeanPropertyMeta ppMeta) throws Exception {

		ClassMeta<?> seType = sType.getElementType();
		if (seType == null)
			seType = session.object();

		Collection c = (sType.isCollection() ? (Collection)in : toList(sType.getInnerClass(), in));

		int i = session.getIndent();
		if (c.isEmpty()) {
			out.appendln(i, "<ul></ul>");
			return;
		}

		String type2 = null;
		if (sType != eType)
			type2 = sType.getDictionaryName();
		if (type2 == null)
			type2 = "array";

		c = session.sort(c);

		HtmlBeanPropertyMeta hbpMeta = (ppMeta == null ? null : ppMeta.getExtendedMeta(HtmlBeanPropertyMeta.class));
		String btpn = session.getBeanTypePropertyName(eType);

		// Look at the objects to see how we're going to handle them.  Check the first object to see how we're going to
		// handle this.
		// If it's a map or bean, then we'll create a table.
		// Otherwise, we'll create a list.
		Object[] th = getTableHeaders(session, c, hbpMeta);

		if (th != null) {

			out.oTag(i, "table").attr(btpn, type2).append('>').nl(i+1);
			out.sTag(i+1, "tr").nl(i+2);
			for (Object key : th) {
				out.sTag(i+2, "th");
				out.text(session.convertToType(key, String.class));
				out.eTag("th").nl(i+2);
			}
			out.ie(i+1).eTag("tr").nl(i+1);

			for (Object o : c) {
				ClassMeta<?> cm = session.getClassMetaForObject(o);

				if (cm != null && cm.getPojoSwap() != null) {
					PojoSwap f = cm.getPojoSwap();
					o = f.swap(session, o);
					cm = cm.getSerializedClassMeta();
				}

				out.oTag(i+1, "tr");
				String typeName = (cm == null ? null : cm.getDictionaryName());
				String typeProperty = session.getBeanTypePropertyName(cm);

				if (typeName != null && eType.getElementType() != cm)
					out.attr(typeProperty, typeName);
				out.cTag().nl(i+2);

				if (cm == null) {
					serializeAnything(session, out, o, null, null, 1, null, false);

				} else if (cm.isMap() && ! (cm.isBeanMap())) {
					Map m2 = session.sort((Map)o);

					for (Object k : th) {
						out.sTag(i+2, "td");
						ContentResult cr = serializeAnything(session, out, m2.get(k), eType.getElementType(), session.toString(k), 2, null, false);
						if (cr == CR_NORMAL)
							out.i(i+2);
						out.eTag("td").nl(i+2);
					}
				} else {
					BeanMap m2 = null;
					if (o instanceof BeanMap)
						m2 = (BeanMap)o;
					else
						m2 = session.toBeanMap(o);

					for (Object k : th) {
						BeanMapEntry p = m2.getProperty(session.toString(k));
						BeanPropertyMeta pMeta = p.getMeta();
						String link = pMeta.getClassMeta().isCollectionOrArray() ? null : getLink(pMeta);
						Object value = p.getValue();
						String style = getStyle(session, pMeta, value);
						out.oTag(i+2, "td");
						if (style != null)
							out.attr("style", style);
						out.cTag();
						if (link != null)
							out.oTag(i+3, "a").attrUri("href", m2.resolveVars(link)).cTag();
						ContentResult cr = serializeAnything(session, out, value, pMeta.getClassMeta(), p.getKey().toString(), 2, pMeta, false);
						if (cr == CR_NORMAL)
							out.i(i+2);
						if (link != null)
							out.eTag("a");
						out.eTag("td").nl(i+2);
					}
				}
				out.ie(i+1).eTag("tr").nl(i+1);
			}
			out.ie(i).eTag("table").nl(i);

		} else {
			out.oTag(i, "ul");
			if (! type2.equals("array"))
				out.attr(btpn, type2);
			out.append('>').nl(i+1);
			for (Object o : c) {
				out.oTag(i+1, "li");
				String style = getStyle(session, ppMeta, o);
				String link = getLink(ppMeta);
				if (style != null)
					out.attr("style", style);
				out.cTag();
				if (link != null)
					out.oTag(i+2, "a").attrUri("href", link.replace("{#}", StringUtils.toString(o))).cTag();
				ContentResult cr = serializeAnything(session, out, o, eType.getElementType(), name, 1, null, false);
				if (link != null)
					out.eTag("a");
				if (cr == CR_NORMAL)
					out.ie(i+1);
				out.eTag("li").nl(i+1);
			}
			out.ie(i).eTag("ul").nl(i);
		}
	}

	private static HtmlRender<?> getRender(HtmlSerializerSession session, BeanPropertyMeta pMeta, Object value) {
		if (pMeta == null)
			return null;
		HtmlBeanPropertyMeta hpMeta = pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class);
		HtmlRender<?> render = hpMeta.getRender();
		if (render != null)
			return render;
		ClassMeta<?> cMeta = session.getClassMetaForObject(value);
		render = cMeta == null ? null : cMeta.getExtendedMeta(HtmlClassMeta.class).getRender();
		return render;
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	private static String getStyle(HtmlSerializerSession session, BeanPropertyMeta pMeta, Object value) {
		HtmlRender render = getRender(session, pMeta, value);
		return render == null ? null : render.getStyle(session, value);
	}

	private static String getLink(BeanPropertyMeta pMeta) {
		return pMeta == null ? null : pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).getLink();
	}

	/*
	 * Returns the table column headers for the specified collection of objects.
	 * Returns null if collection should not be serialized as a 2-dimensional table.
	 * 2-dimensional tables are used for collections of objects that all have the same set of property names.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object[] getTableHeaders(SerializerSession session, Collection c, HtmlBeanPropertyMeta hbpMeta) throws Exception {
		if (c.size() == 0)
			return null;
		c = session.sort(c);
		Object[] th;
		Set<ClassMeta> prevC = new HashSet<ClassMeta>();
		Object o1 = null;
		for (Object o : c)
			if (o != null) {
				o1 = o;
				break;
			}
		if (o1 == null)
			return null;
		ClassMeta<?> cm = session.getClassMetaForObject(o1);
		if (cm.getPojoSwap() != null) {
			PojoSwap f = cm.getPojoSwap();
			o1 = f.swap(session, o1);
			cm = cm.getSerializedClassMeta();
		}
		if (cm == null || ! cm.isMapOrBean())
			return null;
		if (cm.getInnerClass().isAnnotationPresent(HtmlLink.class))
			return null;
		HtmlClassMeta h = cm.getExtendedMeta(HtmlClassMeta.class);
		if (h.isNoTables() || (hbpMeta != null && hbpMeta.isNoTables()))
			return null;
		if (h.isNoTableHeaders() || (hbpMeta != null && hbpMeta.isNoTableHeaders()))
			return new Object[0];
		if (session.canIgnoreValue(cm, null, o1))
			return null;
		if (cm.isMap() && ! cm.isBeanMap()) {
			Set<Object> set = new LinkedHashSet<Object>();
			for (Object o : c) {
				if (! session.canIgnoreValue(cm, null, o)) {
					if (! cm.isInstance(o))
						return null;
					Map m = session.sort((Map)o);
					for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {
						if (e.getValue() != null)
							set.add(e.getKey() == null ? null : e.getKey());
					}
				}
			}
			th = set.toArray(new Object[set.size()]);
		} else {
			Map<String,Boolean> m = new LinkedHashMap<String,Boolean>();
			for (Object o : c) {
				if (! session.canIgnoreValue(cm, null, o)) {
					if (! cm.isInstance(o))
						return null;
					BeanMap<?> bm = (o instanceof BeanMap ? (BeanMap)o : session.toBeanMap(o));
					for (Map.Entry<String,Object> e : bm.entrySet()) {
						String key = e.getKey();
						if (e.getValue() != null)
							m.put(key, true);
						else if (! m.containsKey(key))
							m.put(key, false);
					}
				}
			}
			for (Iterator<Boolean> i = m.values().iterator(); i.hasNext();)
				if (! i.next())
					i.remove();
			th = m.keySet().toArray(new Object[m.size()]);
		}
		prevC.add(cm);
		boolean isSortable = true;
		for (Object o : th)
			isSortable &= (o instanceof Comparable);
		Set<Object> s = (isSortable ? new TreeSet<Object>() : new LinkedHashSet<Object>());
		s.addAll(Arrays.asList(th));

		for (Object o : c) {
			if (o == null)
				continue;
			cm = session.getClassMetaForObject(o);
			if (cm != null && cm.getPojoSwap() != null) {
				PojoSwap f = cm.getPojoSwap();
				o = f.swap(session, o);
				cm = cm.getSerializedClassMeta();
			}
			if (prevC.contains(cm))
				continue;
			if (cm == null || ! (cm.isMap() || cm.isBean()))
				return null;
			if (cm.getInnerClass().isAnnotationPresent(HtmlLink.class))
				return null;
			if (session.canIgnoreValue(cm, null, o))
				return null;
			if (cm.isMap() && ! cm.isBeanMap()) {
				Map m = (Map)o;
				if (th.length != m.keySet().size())
					return null;
				for (Object k : m.keySet())
					if (! s.contains(k.toString()))
						return null;
			} else {
				BeanMap<?> bm = (o instanceof BeanMap ? (BeanMap)o : session.toBeanMap(o));
				int l = 0;
				for (String k : bm.keySet()) {
					if (! s.contains(k))
						return null;
					l++;
				}
				if (s.size() != l)
					return null;
			}
		}
		return th;
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 * @return The schema serializer.
	 */
	@Override /* XmlSerializer */
	public HtmlSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = new HtmlSchemaDocSerializer(propertyStore, getOverrideProperties());
		return schemaSerializer;
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public HtmlSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale,
			TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		return new HtmlSerializerSession(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		HtmlSerializerSession s = (HtmlSerializerSession)session;
		doSerialize(s, o, s.getWriter());
	}
}
