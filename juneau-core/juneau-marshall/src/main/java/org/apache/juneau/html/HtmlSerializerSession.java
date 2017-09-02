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

import static org.apache.juneau.html.HtmlSerializerSession.ContentResult.*;
import static org.apache.juneau.html.HtmlSerializerContext.*;
import static org.apache.juneau.msgpack.MsgPackSerializerContext.*;
import static org.apache.juneau.xml.XmlUtils.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Session object that lives for the duration of a single use of {@link HtmlSerializer}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class HtmlSerializerSession extends XmlSerializerSession {

	private final AnchorText anchorText;
	private final boolean
		detectLinksInStrings,
		lookForLabelParameters,
		addKeyValueTableHeaders,
		addBeanTypeProperties;
	private final Pattern urlPattern = Pattern.compile("http[s]?\\:\\/\\/.*");
	private final Pattern labelPattern;


	@SuppressWarnings("hiding")
	enum AnchorText {
		PROPERTY_NAME, TO_STRING, URI, LAST_TOKEN, URI_ANCHOR, CONTEXT_RELATIVE, SERVLET_RELATIVE, PATH_RELATIVE
	}

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected HtmlSerializerSession(HtmlSerializerContext ctx, SerializerSessionArgs args) {
		super(ctx, args);
		String labelParameter;
		ObjectMap p = getProperties();
		if (p.isEmpty()) {
			anchorText = Enum.valueOf(AnchorText.class, ctx.uriAnchorText);
			detectLinksInStrings = ctx.detectLinksInStrings;
			lookForLabelParameters = ctx.lookForLabelParameters;
			labelParameter = ctx.labelParameter;
			addKeyValueTableHeaders = ctx.addKeyValueTableHeaders;
			addBeanTypeProperties = ctx.addBeanTypeProperties;
		} else {
			anchorText = Enum.valueOf(AnchorText.class, p.getString(HTML_uriAnchorText, ctx.uriAnchorText));
			detectLinksInStrings = p.getBoolean(HTML_detectLinksInStrings, ctx.detectLinksInStrings);
			lookForLabelParameters = p.getBoolean(HTML_lookForLabelParameters, ctx.lookForLabelParameters);
			labelParameter = p.getString(HTML_labelParameter, ctx.labelParameter);
			addKeyValueTableHeaders = p.getBoolean(HTML_addKeyValueTableHeaders, ctx.addKeyValueTableHeaders);
			addBeanTypeProperties = p.getBoolean(MSGPACK_addBeanTypeProperties, ctx.addBeanTypeProperties);
		}
		labelPattern = Pattern.compile("[\\?\\&]" + Pattern.quote(labelParameter) + "=([^\\&]*)");
	}

	/**
	 * Converts the specified output target object to an {@link HtmlWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link HtmlWriter}.
	 * @throws Exception
	 */
	protected final HtmlWriter getHtmlWriter(SerializerPipe out) throws Exception {
		Object output = out.getRawOutput();
		if (output instanceof HtmlWriter)
			return (HtmlWriter)output;
		HtmlWriter w = new HtmlWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(), getQuoteChar(),
			getUriResolver());
		out.setWriter(w);
		return w;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is a URL.
	 *
	 * @param cm The ClassMeta of the object being serialized.
	 * @param pMeta
	 * 	The property metadata of the bean property of the object.
	 * 	Can be <jk>null</jk> if the object isn't from a bean property.
	 * @param o The object.
	 * @return <jk>true</jk> if the specified object is a URL.
	 */
	public boolean isUri(ClassMeta<?> cm, BeanPropertyMeta pMeta, Object o) {
		if (cm.isUri())
			return true;
		if (pMeta != null && pMeta.isUri())
			return true;
		if (detectLinksInStrings && o instanceof CharSequence && urlPattern.matcher(o.toString()).matches())
			return true;
		return false;
	}

	/**
	 * Returns the anchor text to use for the specified URL object.
	 *
	 * @param pMeta
	 * 	The property metadata of the bean property of the object.
	 * 	Can be <jk>null</jk> if the object isn't from a bean property.
	 * @param o The URL object.
	 * @return The anchor text to use for the specified URL object.
	 */
	public String getAnchorText(BeanPropertyMeta pMeta, Object o) {
		String s = o.toString();
		if (lookForLabelParameters) {
			Matcher m = labelPattern.matcher(s);
			if (m.find())
				return urlDecode(m.group(1));
		}
		switch (anchorText) {
			case LAST_TOKEN:
				s = resolveUri(s);
				if (s.indexOf('/') != -1)
					s = s.substring(s.lastIndexOf('/')+1);
				if (s.indexOf('?') != -1)
					s = s.substring(0, s.indexOf('?'));
				if (s.indexOf('#') != -1)
					s = s.substring(0, s.indexOf('#'));
				if (s.isEmpty())
					s = "/";
				return urlDecode(s);
			case URI_ANCHOR:
				if (s.indexOf('#') != -1)
					s = s.substring(s.lastIndexOf('#')+1);
				return urlDecode(s);
			case PROPERTY_NAME:
				return pMeta == null ? s : pMeta.getName();
			case URI:
				return resolveUri(s);
			case CONTEXT_RELATIVE:
				return relativizeUri("context:/", s);
			case SERVLET_RELATIVE:
				return relativizeUri("servlet:/", s);
			case PATH_RELATIVE:
				return relativizeUri("request:/", s);
			default /* TO_STRING */:
				return s;
		}
	}

	/**
	 * Returns the {@link HtmlSerializerContext#HTML_addKeyValueTableHeaders} setting value for this session.
	 *
	 * @return The {@link HtmlSerializerContext#HTML_addKeyValueTableHeaders} setting value for this session.
	 */
	public final boolean isAddKeyValueTableHeaders() {
		return addKeyValueTableHeaders;
	}

	/**
	 * Returns the {@link HtmlSerializerContext#HTML_addBeanTypeProperties} setting value for this session.
	 *
	 * @return The {@link HtmlSerializerContext#HTML_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	public final boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	@Override /* XmlSerializer */
	public boolean isHtmlMode() {
		return true;
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		doSerialize(o, getHtmlWriter(out));
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
	private HtmlWriter doSerialize(Object o, HtmlWriter w) throws Exception {
		serializeAnything(w, o, getExpectedRootType(o), null, getInitialDepth()-1, null, true);
		return w;
	}

	/**
	 * Serialize the specified object to the specified writer.
	 *
	 * @param out The writer.
	 * @param o The object to serialize.
	 * @param eType The expected type of the object if this is a bean property.
	 * @param name
	 * 	The attribute name of this object if this object was a field in a JSON object (i.e. key of a
	 * 	{@link java.util.Map.Entry} or property name of a bean).
	 * @param xIndent The current indentation value.
	 * @param pMeta The bean property being serialized, or <jk>null</jk> if we're not serializing a bean property.
	 * @param isRoot <jk>true</jk> if this is the root element of the document.
	 * @return The type of content encountered.  Either simple (no whitespace) or normal (elements with whitespace).
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected ContentResult serializeAnything(HtmlWriter out, Object o,
			ClassMeta<?> eType, String name, int xIndent, BeanPropertyMeta pMeta, boolean isRoot) throws Exception {

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;     // The wrapped type (delegate)
		ClassMeta<?> sType = object();   // The serialized type

		if (eType == null)
			eType = object();

		aType = push(name, o, eType);

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		indent += xIndent;

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
			if (isAddBeanTypeProperties() && ! eType.equals(aType))
				typeName = aType.getDictionaryName();

			// Swap if necessary
			PojoSwap swap = aType.getPojoSwap();
			if (swap != null) {
				o = swap.swap(this, o);

				// If the getSwapClass() method returns Object, we need to figure out
				// the actual type now.
				if (sType.isObject())
					sType = getClassMetaForObject(o);
			}

			// Handle the case where we're serializing a raw stream.
			if (sType.isReader() || sType.isInputStream()) {
				pop();
				indent -= xIndent;
				IOUtils.pipe(o, out);
				return ContentResult.CR_SIMPLE;
			}

			HtmlClassMeta html = sType.getExtendedMeta(HtmlClassMeta.class);
			HtmlRender render = (pMeta == null ? null : pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).getRender());
			if (render == null)
				render = html.getRender();

			if (render != null) {
				Object o2 = render.getContent(this, o);
				if (o2 != o) {
					indent -= xIndent;
					pop();
					out.nl(indent);
					return serializeAnything(out, o2, null, typeName, xIndent, null, false);
				}
			}

			if (html.isAsXml() || (pMeta != null && pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).isAsXml())) {
				pop();
				indent++;
				super.serializeAnything(out, o, null, null, null, false, XmlFormat.MIXED, false, false, null);
				indent -= xIndent+1;
				return cr;

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
				out.nlIf(! isRoot, xIndent+1);
				if (o instanceof BeanMap)
					serializeBeanMap(out, (BeanMap)o, eType, pMeta);
				else
					serializeMap(out, (Map)o, sType, eType.getKeyType(), eType.getValueType(), typeName, pMeta);

			} else if (sType.isBean()) {
				BeanMap m = toBeanMap(o);
				Class<?> c = o.getClass();
				if (c.isAnnotationPresent(HtmlLink.class)) {
					HtmlLink h = o.getClass().getAnnotation(HtmlLink.class);
					Object urlProp = m.get(h.hrefProperty());
					Object nameProp = m.get(h.nameProperty());
					out.oTag("a").attrUri("href", urlProp).append('>').text(nameProp).eTag("a");
					cr = CR_SIMPLE;
				} else {
					out.nlIf(! isRoot, xIndent+2);
					serializeBeanMap(out, m, eType, pMeta);
				}

			} else if (sType.isCollection() || sType.isArray() || (wType != null && wType.isCollection())) {
				out.nlIf(! isRoot, xIndent+1);
				serializeCollection(out, o, sType, eType, name, pMeta);

			} else if (isUri(sType, pMeta, o)) {
				String label = getAnchorText(pMeta, o);
				out.oTag("a").attrUri("href", o).append('>');
				out.text(label);
				out.eTag("a");
				cr = CR_SIMPLE;

			} else {
				if (isRoot)
					out.sTag("string").text(toString(o)).eTag("string");
				else
					out.text(toString(o));
				cr = CR_SIMPLE;
			}
		}
		pop();
		indent -= xIndent;
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
	private void serializeMap(HtmlWriter out, Map m, ClassMeta<?> sType,
			ClassMeta<?> eKeyType, ClassMeta<?> eValueType, String typeName, BeanPropertyMeta ppMeta) throws Exception {

		ClassMeta<?> keyType = eKeyType == null ? string() : eKeyType;
		ClassMeta<?> valueType = eValueType == null ? object() : eValueType;
		ClassMeta<?> aType = getClassMetaForObject(m);       // The actual type

		int i = indent;

		out.oTag(i, "table");

		if (typeName != null && ppMeta != null && ppMeta.getClassMeta() != aType)
			out.attr(getBeanTypePropertyName(sType), typeName);

		out.append(">").nl(i+1);
		if (isAddKeyValueTableHeaders() && ! (aType.getExtendedMeta(HtmlClassMeta.class).isNoTableHeaders()
				|| (ppMeta != null && ppMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).isNoTableHeaders()))) {
			out.sTag(i+1, "tr").nl(i+2);
			out.sTag(i+2, "th").append("key").eTag("th").nl(i+3);
			out.sTag(i+2, "th").append("value").eTag("th").nl(i+3);
			out.ie(i+1).eTag("tr").nl(i+2);
		}
		for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {

			Object key = generalize(e.getKey(), keyType);
			Object value = null;
			try {
				value = e.getValue();
			} catch (StackOverflowError t) {
				throw t;
			} catch (Throwable t) {
				onError(t, "Could not call getValue() on property ''{0}'', {1}", e.getKey(), t.getLocalizedMessage());
			}

			String link = getLink(ppMeta);
			String style = getStyle(this, ppMeta, value);

			out.sTag(i+1, "tr").nl(i+2);
			out.oTag(i+2, "td");
			if (style != null)
				out.attr("style", style);
			out.cTag();
			if (link != null)
				out.oTag(i+3, "a").attrUri("href", link.replace("{#}", StringUtils.toString(value))).cTag();
			ContentResult cr = serializeAnything(out, key, keyType, null, 2, null, false);
			if (link != null)
				out.eTag("a");
			if (cr == CR_NORMAL)
				out.i(i+2);
			out.eTag("td").nl(i+2);
			out.sTag(i+2, "td");
			cr = serializeAnything(out, value, valueType, (key == null ? "_x0000_" : toString(key)), 2, null, false);
			if (cr == CR_NORMAL)
				out.ie(i+2);
			out.eTag("td").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}
		out.ie(i).eTag("table").nl(i);
	}

	@SuppressWarnings("hiding")
	private void serializeBeanMap(HtmlWriter out, BeanMap<?> m, ClassMeta<?> eType,
			BeanPropertyMeta ppMeta) throws Exception {
		int i = indent;

		out.oTag(i, "table");

		String typeName = m.getMeta().getDictionaryName();
		if (typeName != null && eType != m.getClassMeta())
			out.attr(getBeanTypePropertyName(m.getClassMeta()), typeName);

		out.append('>').nl(i);
		if (isAddKeyValueTableHeaders() && ! (m.getClassMeta().getExtendedMeta(HtmlClassMeta.class).isNoTableHeaders()
				|| (ppMeta != null && ppMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).isNoTableHeaders()))) {
			out.sTag(i+1, "tr").nl(i+1);
			out.sTag(i+2, "th").append("key").eTag("th").nl(i+2);
			out.sTag(i+2, "th").append("value").eTag("th").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}

		for (BeanPropertyValue p : m.getValues(isTrimNulls())) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();

			String key = p.getName();
			Object value = p.getValue();
			Throwable t = p.getThrown();
			if (t != null)
				onBeanGetterException(pMeta, t);

			if (canIgnoreValue(cMeta, key, value))
				continue;

			String link = null, anchorText = null;
			if (! cMeta.isCollectionOrArray()) {
				link = m.resolveVars(getLink(pMeta));
				anchorText = m.resolveVars(getAnchorText(pMeta));
			}

			if (anchorText != null)
				value = anchorText;

			out.sTag(i+1, "tr").nl(i+1);
			out.sTag(i+2, "td").text(key).eTag("td").nl(i+2);
			out.oTag(i+2, "td");
			String style = getStyle(this, pMeta, value);
			if (style != null)
				out.attr("style", style);
			out.cTag();

			try {
				if (link != null)
					out.oTag(i+3, "a").attrUri("href", link).cTag();
				ContentResult cr = serializeAnything(out, value, cMeta, key, 2, pMeta, false);
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
				onBeanGetterException(pMeta, e);
			}
			out.eTag("td").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}
		out.ie(i).eTag("table").nl(i);
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "hiding" })
	private void serializeCollection(HtmlWriter out, Object in, ClassMeta<?> sType,
			ClassMeta<?> eType, String name, BeanPropertyMeta ppMeta) throws Exception {

		ClassMeta<?> seType = sType.getElementType();
		if (seType == null)
			seType = object();

		Collection c = (sType.isCollection() ? (Collection)in : toList(sType.getInnerClass(), in));

		int i = indent;
		if (c.isEmpty()) {
			out.appendln(i, "<ul></ul>");
			return;
		}

		String type2 = null;
		if (sType != eType)
			type2 = sType.getDictionaryName();
		if (type2 == null)
			type2 = "array";

		c = sort(c);

		HtmlBeanPropertyMeta hbpMeta = (ppMeta == null ? null : ppMeta.getExtendedMeta(HtmlBeanPropertyMeta.class));
		String btpn = getBeanTypePropertyName(eType);

		// Look at the objects to see how we're going to handle them.  Check the first object to see how we're going to
		// handle this.
		// If it's a map or bean, then we'll create a table.
		// Otherwise, we'll create a list.
		Object[] th = getTableHeaders(c, hbpMeta);

		if (th != null) {

			out.oTag(i, "table").attr(btpn, type2).append('>').nl(i+1);
			out.sTag(i+1, "tr").nl(i+2);
			for (Object key : th) {
				out.sTag(i+2, "th");
				out.text(convertToType(key, String.class));
				out.eTag("th").nl(i+2);
			}
			out.ie(i+1).eTag("tr").nl(i+1);

			for (Object o : c) {
				ClassMeta<?> cm = getClassMetaForObject(o);

				if (cm != null && cm.getPojoSwap() != null) {
					PojoSwap f = cm.getPojoSwap();
					o = f.swap(this, o);
					cm = cm.getSerializedClassMeta();
				}

				out.oTag(i+1, "tr");
				String typeName = (cm == null ? null : cm.getDictionaryName());
				String typeProperty = getBeanTypePropertyName(cm);

				if (typeName != null && eType.getElementType() != cm)
					out.attr(typeProperty, typeName);
				out.cTag().nl(i+2);

				if (cm == null) {
					out.i(i+2);
					serializeAnything(out, o, null, null, 1, null, false);
					out.nl(0);

				} else if (cm.isMap() && ! (cm.isBeanMap())) {
					Map m2 = sort((Map)o);

					for (Object k : th) {
						out.sTag(i+2, "td");
						ContentResult cr = serializeAnything(out, m2.get(k), eType.getElementType(), toString(k), 2, null, false);
						if (cr == CR_NORMAL)
							out.i(i+2);
						out.eTag("td").nl(i+2);
					}
				} else {
					BeanMap m2 = null;
					if (o instanceof BeanMap)
						m2 = (BeanMap)o;
					else
						m2 = toBeanMap(o);

					for (Object k : th) {
						BeanMapEntry p = m2.getProperty(toString(k));
						BeanPropertyMeta pMeta = p.getMeta();
						Object value = p.getValue();

						String link = null, anchorText = null;
						if (! pMeta.getClassMeta().isCollectionOrArray()) {
							link = m2.resolveVars(getLink(pMeta));
							anchorText = m2.resolveVars(getAnchorText(pMeta));
						}

						if (anchorText != null)
							value = anchorText;

						String style = getStyle(this, pMeta, value);
						out.oTag(i+2, "td");
						if (style != null)
							out.attr("style", style);
						out.cTag();
						if (link != null)
							out.oTag("a").attrUri("href", link).cTag();
						ContentResult cr = serializeAnything(out, value, pMeta.getClassMeta(), p.getKey().toString(), 2, pMeta, false);
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
				String style = getStyle(this, ppMeta, o);
				String link = getLink(ppMeta);
				if (style != null)
					out.attr("style", style);
				out.cTag();
				if (link != null)
					out.oTag(i+2, "a").attrUri("href", link.replace("{#}", StringUtils.toString(o))).cTag();
				ContentResult cr = serializeAnything(out, o, eType.getElementType(), name, 1, null, false);
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

	private static String getAnchorText(BeanPropertyMeta pMeta) {
		return pMeta == null ? null : pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class).getAnchorText();
	}

	/*
	 * Returns the table column headers for the specified collection of objects.
	 * Returns null if collection should not be serialized as a 2-dimensional table.
	 * 2-dimensional tables are used for collections of objects that all have the same set of property names.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object[] getTableHeaders(Collection c, HtmlBeanPropertyMeta hbpMeta) throws Exception {
		if (c.size() == 0)
			return null;
		c = sort(c);
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
		ClassMeta<?> cm = getClassMetaForObject(o1);
		if (cm.getPojoSwap() != null) {
			PojoSwap f = cm.getPojoSwap();
			o1 = f.swap(this, o1);
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
		if (canIgnoreValue(cm, null, o1))
			return null;
		if (cm.isMap() && ! cm.isBeanMap()) {
			Set<Object> set = new LinkedHashSet<Object>();
			for (Object o : c) {
				if (! canIgnoreValue(cm, null, o)) {
					if (! cm.isInstance(o))
						return null;
					Map m = sort((Map)o);
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
				if (! canIgnoreValue(cm, null, o)) {
					if (! cm.isInstance(o))
						return null;
					BeanMap<?> bm = (o instanceof BeanMap ? (BeanMap)o : toBeanMap(o));
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
			cm = getClassMetaForObject(o);
			if (cm != null && cm.getPojoSwap() != null) {
				PojoSwap f = cm.getPojoSwap();
				o = f.swap(this, o);
				cm = cm.getSerializedClassMeta();
			}
			if (prevC.contains(cm))
				continue;
			if (cm == null || ! (cm.isMap() || cm.isBean()))
				return null;
			if (cm.getInnerClass().isAnnotationPresent(HtmlLink.class))
				return null;
			if (canIgnoreValue(cm, null, o))
				return null;
			if (cm.isMap() && ! cm.isBeanMap()) {
				Map m = (Map)o;
				if (th.length != m.keySet().size())
					return null;
				for (Object k : m.keySet())
					if (! s.contains(k.toString()))
						return null;
			} else {
				BeanMap<?> bm = (o instanceof BeanMap ? (BeanMap)o : toBeanMap(o));
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
}
