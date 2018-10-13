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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.xml.XmlSerializerSession.ContentResult.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
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

	private final HtmlSerializer ctx;
	private final Pattern urlPattern = Pattern.compile("http[s]?\\:\\/\\/.*");
	private final Pattern labelPattern;


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
	protected HtmlSerializerSession(HtmlSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
		labelPattern = Pattern.compile("[\\?\\&]" + Pattern.quote(ctx.getLabelParameter()) + "=([^\\&]*)");
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("HtmlSerializerSession", new ObjectMap()
			);
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
		if (isDetectLinksInStrings() && o instanceof CharSequence && urlPattern.matcher(o.toString()).matches())
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
		if (isLookForLabelParameters()) {
			Matcher m = labelPattern.matcher(s);
			if (m.find())
				return urlDecode(m.group(1));
		}
		switch (getUriAnchorText()) {
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
	private XmlWriter doSerialize(Object o, XmlWriter w) throws Exception {
		serializeAnything(w, o, getExpectedRootType(o), null, null, getInitialDepth()-1, true);
		return w;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override /* XmlSerializerSession */
	protected ContentResult serializeAnything(
			XmlWriter out,
			Object o,
			ClassMeta<?> eType,
			String elementName,
			Namespace elementNamespace,
			boolean addNamespaceUris,
			XmlFormat format,
			boolean isMixed,
			boolean preserveWhitespace,
			BeanPropertyMeta pMeta) throws Exception {

		// If this is a bean, then we want to serialize it as HTML unless it's @Html(format=XML).
		ClassMeta<?> type = push(elementName, o, eType);
		pop();

		if (type == null)
			type = object();
		else if (type.isDelegate())
			type = ((Delegate)o).getClassMeta();
		PojoSwap swap = type.getPojoSwap(this);
		if (swap != null) {
			o = swap.swap(this, o);
			type = swap.getSwapClassMeta(this);
			if (type.isObject())
				type = getClassMetaForObject(o);
		}

		HtmlClassMeta cHtml = cHtml(type);

		if (type.isMapOrBean() && ! cHtml.isXml())
			return serializeAnything(out, o, eType, elementName, pMeta, 0, false);

		return super.serializeAnything(out, o, eType, elementName, elementNamespace, addNamespaceUris, format, isMixed, preserveWhitespace, pMeta);
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
	 * @param pMeta The bean property being serialized, or <jk>null</jk> if we're not serializing a bean property.
	 * @param xIndent The current indentation value.
	 * @param isRoot <jk>true</jk> if this is the root element of the document.
	 * @return The type of content encountered.  Either simple (no whitespace) or normal (elements with whitespace).
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected ContentResult serializeAnything(XmlWriter out, Object o,
			ClassMeta<?> eType, String name, BeanPropertyMeta pMeta, int xIndent, boolean isRoot) throws Exception {

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

		ContentResult cr = CR_ELEMENTS;

		// Determine the type.
		if (o == null || (aType.isChar() && ((Character)o).charValue() == 0)) {
			out.tag("null");
			cr = ContentResult.CR_MIXED;

		} else {

			if (aType.isDelegate()) {
				wType = aType;
				aType = ((Delegate)o).getClassMeta();
			}

			sType = aType;

			String typeName = null;
			if (isAddBeanTypes() && ! eType.equals(aType))
				typeName = aType.getDictionaryName();

			// Swap if necessary
			PojoSwap swap = aType.getPojoSwap(this);
			if (swap != null) {
				o = swap.swap(this, o);
				sType = swap.getSwapClassMeta(this);

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
				return ContentResult.CR_MIXED;
			}

			HtmlClassMeta cHtml = cHtml(sType);
			HtmlBeanPropertyMeta bpHtml = bpHtml(pMeta);

			HtmlRender render = firstNonNull(bpHtml.getRender(), cHtml.getRender());

			if (render != null) {
				Object o2 = render.getContent(this, o);
				if (o2 != o) {
					indent -= xIndent;
					pop();
					out.nl(indent);
					return serializeAnything(out, o2, null, typeName, null, xIndent, false);
				}
			}

			if (cHtml.isXml() || bpHtml.isXml()) {
				pop();
				indent++;
				super.serializeAnything(out, o, null, null, null, false, XmlFormat.MIXED, false, false, null);
				indent -= xIndent+1;
				return cr;

			} else if (cHtml.isPlainText() || bpHtml.isPlainText()) {
				out.write(o == null ? "null" : o.toString());
				cr = CR_MIXED;

			} else if (o == null || (sType.isChar() && ((Character)o).charValue() == 0)) {
				out.tag("null");
				cr = CR_MIXED;

			} else if (sType.isNumber()) {
				if (eType.isNumber() && ! isRoot)
					out.append(o);
				else
					out.sTag("number").append(o).eTag("number");
				cr = CR_MIXED;

			} else if (sType.isBoolean()) {
				if (eType.isBoolean() && ! isRoot)
					out.append(o);
				else
					out.sTag("boolean").append(o).eTag("boolean");
				cr = CR_MIXED;

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
					Object urlProp = m.get(h.uriProperty());
					Object nameProp = m.get(h.nameProperty());
					out.oTag("a").attrUri("href", urlProp).append('>').text(nameProp).eTag("a");
					cr = CR_MIXED;
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
				cr = CR_MIXED;

			} else {
				if (isRoot)
					out.sTag("string").text(toString(o)).eTag("string");
				else
					out.text(toString(o));
				cr = CR_MIXED;
			}
		}
		pop();
		indent -= xIndent;
		return cr;
	}

	@SuppressWarnings({ "rawtypes" })
	private void serializeMap(XmlWriter out, Map m, ClassMeta<?> sType,
			ClassMeta<?> eKeyType, ClassMeta<?> eValueType, String typeName, BeanPropertyMeta ppMeta) throws Exception {

		ClassMeta<?> keyType = eKeyType == null ? string() : eKeyType;
		ClassMeta<?> valueType = eValueType == null ? object() : eValueType;
		ClassMeta<?> aType = getClassMetaForObject(m);       // The actual type
		HtmlClassMeta cHtml = cHtml(aType);
		HtmlBeanPropertyMeta bpHtml = bpHtml(ppMeta);

		int i = indent;

		out.oTag(i, "table");

		if (typeName != null && ppMeta != null && ppMeta.getClassMeta() != aType)
			out.attr(getBeanTypePropertyName(sType), typeName);

		out.append(">").nl(i+1);
		if (isAddKeyValueTableHeaders() && ! (cHtml.isNoTableHeaders() || bpHtml.isNoTableHeaders())) {
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
				out.oTag(i+3, "a").attrUri("href", link.replace("{#}", asString(value))).cTag();
			ContentResult cr = serializeAnything(out, key, keyType, null, null, 2, false);
			if (link != null)
				out.eTag("a");
			if (cr == CR_ELEMENTS)
				out.i(i+2);
			out.eTag("td").nl(i+2);
			out.sTag(i+2, "td");
			cr = serializeAnything(out, value, valueType, (key == null ? "_x0000_" : toString(key)), null, 2, false);
			if (cr == CR_ELEMENTS)
				out.ie(i+2);
			out.eTag("td").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}
		out.ie(i).eTag("table").nl(i);
	}

	private void serializeBeanMap(XmlWriter out, BeanMap<?> m, ClassMeta<?> eType, BeanPropertyMeta ppMeta) throws Exception {

		HtmlClassMeta cHtml = cHtml(m.getClassMeta());
		HtmlBeanPropertyMeta bpHtml = bpHtml(ppMeta);

		int i = indent;

		out.oTag(i, "table");

		String typeName = m.getMeta().getDictionaryName();
		if (typeName != null && eType != m.getClassMeta())
			out.attr(getBeanTypePropertyName(m.getClassMeta()), typeName);

		out.append('>').nl(i);
		if (isAddKeyValueTableHeaders() && ! (cHtml.isNoTableHeaders() || bpHtml.isNoTableHeaders())) {
			out.sTag(i+1, "tr").nl(i+1);
			out.sTag(i+2, "th").append("key").eTag("th").nl(i+2);
			out.sTag(i+2, "th").append("value").eTag("th").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}

		for (BeanPropertyValue p : m.getValues(isTrimNullProperties())) {
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
				ContentResult cr = serializeAnything(out, value, cMeta, key, pMeta, 2, false);
				if (cr == CR_ELEMENTS)
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeCollection(XmlWriter out, Object in, ClassMeta<?> sType, ClassMeta<?> eType, String name, BeanPropertyMeta ppMeta) throws Exception {

		HtmlClassMeta cHtml = cHtml(sType);
		HtmlBeanPropertyMeta bpHtml = bpHtml(ppMeta);

		Collection c = (sType.isCollection() ? (Collection)in : toList(sType.getInnerClass(), in));

		boolean isCdc = cHtml.isHtmlCdc() || bpHtml.isHtmlCdc();
		boolean isSdc = cHtml.isHtmlSdc() || bpHtml.isHtmlSdc();
		boolean isDc = isCdc || isSdc;

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

		String btpn = getBeanTypePropertyName(eType);

		// Look at the objects to see how we're going to handle them.  Check the first object to see how we're going to
		// handle this.
		// If it's a map or bean, then we'll create a table.
		// Otherwise, we'll create a list.
		Object[] th = getTableHeaders(c, bpHtml);

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

				if (cm != null && cm.getPojoSwap(this) != null) {
					PojoSwap swap = cm.getPojoSwap(this);
					o = swap.swap(this, o);
					cm = swap.getSwapClassMeta(this);
				}

				out.oTag(i+1, "tr");
				String typeName = (cm == null ? null : cm.getDictionaryName());
				String typeProperty = getBeanTypePropertyName(cm);

				if (typeName != null && eType.getElementType() != cm)
					out.attr(typeProperty, typeName);
				out.cTag().nl(i+2);

				if (cm == null) {
					out.i(i+2);
					serializeAnything(out, o, null, null, null, 1, false);
					out.nl(0);

				} else if (cm.isMap() && ! (cm.isBeanMap())) {
					Map m2 = sort((Map)o);

					for (Object k : th) {
						out.sTag(i+2, "td");
						ContentResult cr = serializeAnything(out, m2.get(k), eType.getElementType(), toString(k), null, 2, false);
						if (cr == CR_ELEMENTS)
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
						if (pMeta.canRead()) {
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
							ContentResult cr = serializeAnything(out, value, pMeta.getClassMeta(), p.getKey().toString(), pMeta, 2, false);
							if (cr == CR_ELEMENTS)
								out.i(i+2);
							if (link != null)
								out.eTag("a");
							out.eTag("td").nl(i+2);
						}
					}
				}
				out.ie(i+1).eTag("tr").nl(i+1);
			}
			out.ie(i).eTag("table").nl(i);

		} else {
			out.oTag(i, isDc ? "p" : "ul");
			if (! type2.equals("array"))
				out.attr(btpn, type2);
			out.append('>').nl(i+1);
			boolean isFirst = true;
			for (Object o : c) {
				if (isDc && ! isFirst)
					out.append(isCdc ? ", " : " ");
				if (! isDc)
					out.oTag(i+1, "li");
				String style = getStyle(this, ppMeta, o);
				String link = getLink(ppMeta);
				if (style != null && ! isDc)
					out.attr("style", style);
				if (! isDc)
					out.cTag();
				if (link != null)
					out.oTag(i+2, "a").attrUri("href", link.replace("{#}", asString(o))).cTag();
				ContentResult cr = serializeAnything(out, o, eType.getElementType(), name, null, 1, false);
				if (link != null)
					out.eTag("a");
				if (cr == CR_ELEMENTS)
					out.ie(i+1);
				if (! isDc)
					out.eTag("li").nl(i+1);
				isFirst = false;
			}
			out.ie(i).eTag(isDc ? "p" : "ul").nl(i);
		}
	}

	private static HtmlRender<?> getRender(HtmlSerializerSession session, BeanPropertyMeta pMeta, Object value) {
		if (pMeta == null)
			return null;
		HtmlRender<?> render = bpHtml(pMeta).getRender();
		if (render != null)
			return render;
		ClassMeta<?> cMeta = session.getClassMetaForObject(value);
		render = cMeta == null ? null : cHtml(cMeta).getRender();
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

	private static HtmlClassMeta cHtml(ClassMeta<?> cm) {
		return cm.getExtendedMeta(HtmlClassMeta.class);
	}

	private static HtmlBeanPropertyMeta bpHtml(BeanPropertyMeta pMeta) {
		return pMeta == null ? HtmlBeanPropertyMeta.DEFAULT : pMeta.getExtendedMeta(HtmlBeanPropertyMeta.class);
	}

	/*
	 * Returns the table column headers for the specified collection of objects.
	 * Returns null if collection should not be serialized as a 2-dimensional table.
	 * 2-dimensional tables are used for collections of objects that all have the same set of property names.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object[] getTableHeaders(Collection c, HtmlBeanPropertyMeta bpHtml) throws Exception {
		if (c.size() == 0)
			return null;
		c = sort(c);
		Object[] th;
		Set<ClassMeta> prevC = new HashSet<>();
		Object o1 = null;
		for (Object o : c)
			if (o != null) {
				o1 = o;
				break;
			}
		if (o1 == null)
			return null;
		ClassMeta<?> cm = getClassMetaForObject(o1);

		PojoSwap swap = cm.getPojoSwap(this);
		if (swap != null) {
			o1 = swap.swap(this, o1);
			cm = swap.getSwapClassMeta(this);
		}

		if (cm == null || ! cm.isMapOrBean())
			return null;
		if (cm.getInnerClass().isAnnotationPresent(HtmlLink.class))
			return null;

		HtmlClassMeta cHtml = cHtml(cm);

		if (cHtml.isNoTables() || bpHtml.isNoTables())
			return null;
		if (cHtml.isNoTableHeaders() || bpHtml.isNoTableHeaders())
			return new Object[0];
		if (canIgnoreValue(cm, null, o1))
			return null;
		if (cm.isMap() && ! cm.isBeanMap()) {
			Set<Object> set = new LinkedHashSet<>();
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
			Map<String,Boolean> m = new LinkedHashMap<>();
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
		Set<Object> s = (isSortable ? new TreeSet<>() : new LinkedHashSet<>());
		s.addAll(Arrays.asList(th));

		for (Object o : c) {
			if (o == null)
				continue;
			cm = getClassMetaForObject(o);

			PojoSwap ps = cm == null ? null : cm.getPojoSwap(this);
			if (ps != null) {
				o = ps.swap(this, o);
				cm = ps.getSwapClassMeta(this);
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
	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Look for link labels in URIs.
	 *
	 * @see HtmlSerializer#HTML_detectLabelParameters
	 * @return
	 * 	<jk>true</jk> if we should look for URL label parameters (e.g. <js>"?label=foobar"</js>).
	 */
	protected final boolean isLookForLabelParameters() {
		return ctx.isLookForLabelParameters();
	}

	/**
	 * Configuration property:  Look for URLs in {@link String Strings}.
	 *
	 * @see HtmlSerializer#HTML_detectLinksInStrings
	 * @return
	 * 	<jk>true</jk> if we should automatically convert strings to URLs if they look like a URL.
	 */
	protected final boolean isDetectLinksInStrings() {
		return ctx.isDetectLinksInStrings();
	}

	/**
	 * Configuration property:  Add key/value headers on bean/map tables.
	 *
	 * @see HtmlSerializer#HTML_addKeyValueTableHeaders
	 * @return
	 * 	<jk>true</jk> if <code><b>key</b></code> and <code><b>value</b></code> column headers are added to tables.
	 */
	// TODO - Make protected in 8.0.
	public final boolean isAddKeyValueTableHeaders() {
		return ctx.isAddKeyValueTableHeaders();
	}

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * @see HtmlSerializer#HTML_addBeanTypes
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Configuration property:  Link label parameter name.
	 *
	 * @see HtmlSerializer#HTML_labelParameter
	 * @return
	 * 	The parameter name to look for when resolving link labels via {@link HtmlSerializer#HTML_detectLabelParameters}.
	 */
	protected final String getLabelParameter() {
		return ctx.getLabelParameter();
	}

	/**
	 * Configuration property:  Anchor text source.
	 *
	 * @see HtmlSerializer#HTML_uriAnchorText
	 * @return
	 * 	When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * 	<xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 */
	protected final AnchorText getUriAnchorText() {
		return ctx.getUriAnchorText();
	}

	/**
	 * @deprecated Unused.
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public final boolean isAddBeanTypeProperties() {
		return false;
	}
}
