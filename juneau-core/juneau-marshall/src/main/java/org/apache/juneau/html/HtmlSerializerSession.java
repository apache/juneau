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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.xml.XmlSerializerSession.ContentResult.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Session object that lives for the duration of a single use of {@link HtmlSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
public class HtmlSerializerSession extends XmlSerializerSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(HtmlSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends XmlSerializerSession.Builder {

		HtmlSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(HtmlSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public HtmlSerializerSession build() {
			return new HtmlSerializerSession(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder useWhitespace(Boolean value) {
			super.useWhitespace(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final HtmlSerializer ctx;
	private final Pattern urlPattern = Pattern.compile("http[s]?\\:\\/\\/.*");
	private final Pattern labelPattern;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected HtmlSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		labelPattern = Pattern.compile("[\\?\\&]" + Pattern.quote(ctx.getLabelParameter()) + "=([^\\&]*)");
	}

	/**
	 * Converts the specified output target object to an {@link HtmlWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link HtmlWriter}.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected final HtmlWriter getHtmlWriter(SerializerPipe out) throws IOException {
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
		if (cm.isUri() || (pMeta != null && pMeta.isUri()))
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
		if (isDetectLabelParameters()) {
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
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
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
	private XmlWriter doSerialize(Object o, XmlWriter w) throws IOException, SerializeException {
		serializeAnything(w, o, getExpectedRootType(o), null, null, getInitialDepth()-1, true, false);
		return w;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override /* XmlSerializerSession */
	protected ContentResult serializeAnything(
			XmlWriter out,
			Object o,
			ClassMeta<?> eType,
			String keyName,
			String elementName,
			Namespace elementNamespace,
			boolean addNamespaceUris,
			XmlFormat format,
			boolean isMixed,
			boolean preserveWhitespace,
			BeanPropertyMeta pMeta) throws SerializeException {

		// If this is a bean, then we want to serialize it as HTML unless it's @Html(format=XML).
		ClassMeta<?> type = push2(elementName, o, eType);
		pop();

		if (type == null)
			type = object();
		else if (type.isDelegate())
			type = ((Delegate)o).getClassMeta();
		ObjectSwap swap = type.getSwap(this);
		if (swap != null) {
			o = swap(swap, o);
			type = swap.getSwapClassMeta(this);
			if (type.isObject())
				type = getClassMetaForObject(o);
		}

		HtmlClassMeta cHtml = getHtmlClassMeta(type);

		if (type.isMapOrBean() && ! cHtml.isXml())
			return serializeAnything(out, o, eType, elementName, pMeta, 0, false, false);

		return super.serializeAnything(out, o, eType, keyName, elementName, elementNamespace, addNamespaceUris, format, isMixed, preserveWhitespace, pMeta);
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
	 * @param nlIfElement <jk>true</jk> if we should add a newline to the output before serializing only if the object is an element and not text.
	 * @return The type of content encountered.  Either simple (no whitespace) or normal (elements with whitespace).
	 * @throws SerializeException Generic serialization error occurred.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected ContentResult serializeAnything(XmlWriter out, Object o,
			ClassMeta<?> eType, String name, BeanPropertyMeta pMeta, int xIndent, boolean isRoot, boolean nlIfElement) throws SerializeException {

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;     // The wrapped type (delegate)
		ClassMeta<?> sType = object();   // The serialized type

		if (eType == null)
			eType = object();

		aType = push2(name, o, eType);

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		// Handle Optional<X>
		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
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
			ObjectSwap swap = aType.getSwap(this);
			if (swap != null) {
				o = swap(swap, o);
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
				if (sType.isReader())
					pipe((Reader)o, out, SerializerSession::handleThrown);
				else
					pipe((InputStream)o, out, SerializerSession::handleThrown);
				return ContentResult.CR_MIXED;
			}

			HtmlClassMeta cHtml = getHtmlClassMeta(sType);
			HtmlBeanPropertyMeta bpHtml = getHtmlBeanPropertyMeta(pMeta);

			HtmlRender render = firstNonNull(bpHtml.getRender(), cHtml.getRender());

			if (render != null) {
				Object o2 = render.getContent(this, o);
				if (o2 != o) {
					indent -= xIndent;
					pop();
					out.nl(indent);
					return serializeAnything(out, o2, null, typeName, null, xIndent, false, false);
				}
			}

			if (cHtml.isXml() || bpHtml.isXml()) {
				pop();
				indent++;
				if (nlIfElement)
					out.nl(0);
				super.serializeAnything(out, o, null, null, null, null, false, XmlFormat.MIXED, false, false, null);
				indent -= xIndent+1;
				return cr;

			} else if (cHtml.isPlainText() || bpHtml.isPlainText()) {
				out.w(o == null ? "null" : o.toString());
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
				if (aType.hasAnnotation(HtmlLink.class)) {
					Value<String> uriProperty = Value.empty(), nameProperty = Value.empty();
					aType.forEachAnnotation(HtmlLink.class, x -> isNotEmpty(x.uriProperty()), x -> uriProperty.set(x.uriProperty()));
					aType.forEachAnnotation(HtmlLink.class, x -> isNotEmpty(x.nameProperty()), x -> nameProperty.set(x.nameProperty()));
					Object urlProp = m.get(uriProperty.orElse(""));
					Object nameProp = m.get(nameProperty.orElse(""));

					out.oTag("a").attrUri("href", urlProp).w('>').text(nameProp).eTag("a");
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
				out.oTag("a").attrUri("href", o).w('>');
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeMap(XmlWriter out, Map m, ClassMeta<?> sType,
			ClassMeta<?> eKeyType, ClassMeta<?> eValueType, String typeName, BeanPropertyMeta ppMeta) throws SerializeException {

		ClassMeta<?> keyType = eKeyType == null ? string() : eKeyType;
		ClassMeta<?> valueType = eValueType == null ? object() : eValueType;
		ClassMeta<?> aType = getClassMetaForObject(m);       // The actual type
		HtmlClassMeta cHtml = getHtmlClassMeta(aType);
		HtmlBeanPropertyMeta bpHtml = getHtmlBeanPropertyMeta(ppMeta);

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

		forEachEntry(m, x -> serializeMapEntry(out, x, keyType, valueType, i, ppMeta));

		out.ie(i).eTag("table").nl(i);
	}

	@SuppressWarnings("rawtypes")
	private void serializeMapEntry(XmlWriter out, Map.Entry e, ClassMeta<?> keyType, ClassMeta<?> valueType, int i, BeanPropertyMeta ppMeta) throws SerializeException {
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
			out.oTag(i+3, "a").attrUri("href", link.replace("{#}", stringify(value))).cTag();
		ContentResult cr = serializeAnything(out, key, keyType, null, null, 2, false, false);
		if (link != null)
			out.eTag("a");
		if (cr == CR_ELEMENTS)
			out.i(i+2);
		out.eTag("td").nl(i+2);
		out.sTag(i+2, "td");
		cr = serializeAnything(out, value, valueType, (key == null ? "_x0000_" : toString(key)), null, 2, false, true);
		if (cr == CR_ELEMENTS)
			out.ie(i+2);
		out.eTag("td").nl(i+2);
		out.ie(i+1).eTag("tr").nl(i+1);

	}

	private void serializeBeanMap(XmlWriter out, BeanMap<?> m, ClassMeta<?> eType, BeanPropertyMeta ppMeta) throws SerializeException {

		HtmlClassMeta cHtml = getHtmlClassMeta(m.getClassMeta());
		HtmlBeanPropertyMeta bpHtml = getHtmlBeanPropertyMeta(ppMeta);

		int i = indent;

		out.oTag(i, "table");

		String typeName = m.getMeta().getDictionaryName();
		if (typeName != null && eType != m.getClassMeta())
			out.attr(getBeanTypePropertyName(m.getClassMeta()), typeName);

		out.w('>').nl(i);
		if (isAddKeyValueTableHeaders() && ! (cHtml.isNoTableHeaders() || bpHtml.isNoTableHeaders())) {
			out.sTag(i+1, "tr").nl(i+1);
			out.sTag(i+2, "th").append("key").eTag("th").nl(i+2);
			out.sTag(i+2, "th").append("value").eTag("th").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		}

		Predicate<Object> checkNull = x -> isKeepNullProperties() || x != null;

		m.forEachValue(checkNull, (pMeta,key,value,thrown) -> {
			ClassMeta<?> cMeta = pMeta.getClassMeta();

			if (thrown != null)
				onBeanGetterException(pMeta, thrown);

			if (canIgnoreValue(cMeta, key, value))
				return;

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
				ContentResult cr = serializeAnything(out, value, cMeta, key, pMeta, 2, false, true);
				if (cr == CR_ELEMENTS)
					out.i(i+2);
				if (link != null)
					out.eTag("a");
			} catch (SerializeException e) {
				throw e;
			} catch (Error e) {
				throw e;
			} catch (Throwable e) {
				onBeanGetterException(pMeta, e);
			}
			out.eTag("td").nl(i+2);
			out.ie(i+1).eTag("tr").nl(i+1);
		});

		out.ie(i).eTag("table").nl(i);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeCollection(XmlWriter out, Object in, ClassMeta<?> sType, ClassMeta<?> eType, String name, BeanPropertyMeta ppMeta) throws SerializeException {

		HtmlClassMeta cHtml = getHtmlClassMeta(sType);
		HtmlBeanPropertyMeta bpHtml = getHtmlBeanPropertyMeta(ppMeta);

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

			out.oTag(i, "table").attr(btpn, type2).w('>').nl(i+1);
			if (th.length > 0) {
				out.sTag(i+1, "tr").nl(i+2);
				for (Object key : th) {
					out.sTag(i+2, "th");
					out.text(convertToType(key, String.class));
					out.eTag("th").nl(i+2);
				}
				out.ie(i+1).eTag("tr").nl(i+1);
			} else {
				th = null;
			}

			for (Object o : c) {
				ClassMeta<?> cm = getClassMetaForObject(o);

				if (cm != null && cm.getSwap(this) != null) {
					ObjectSwap swap = cm.getSwap(this);
					o = swap(swap, o);
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
					serializeAnything(out, o, null, null, null, 1, false, false);
					out.nl(0);

				} else if (cm.isMap() && ! (cm.isBeanMap())) {
					Map m2 = sort((Map)o);

					if (th == null)
						th = m2.keySet().toArray(new Object[m2.size()]);

					for (Object k : th) {
						out.sTag(i+2, "td");
						ContentResult cr = serializeAnything(out, m2.get(k), eType.getElementType(), toString(k), null, 2, false, true);
						if (cr == CR_ELEMENTS)
							out.i(i+2);
						out.eTag("td").nl(i+2);
					}
				} else {
					BeanMap m2 = toBeanMap(o);

					if (th == null)
						th = m2.keySet().toArray(new Object[m2.size()]);

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
							ContentResult cr = serializeAnything(out, value, pMeta.getClassMeta(), p.getKey().toString(), pMeta, 2, false, true);
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
			out.w('>').nl(i+1);
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
					out.oTag(i+2, "a").attrUri("href", link.replace("{#}", stringify(o))).cTag();
				ContentResult cr = serializeAnything(out, o, eType.getElementType(), name, null, 1, false, true);
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

	private HtmlRender<?> getRender(HtmlSerializerSession session, BeanPropertyMeta pMeta, Object value) {
		if (pMeta == null)
			return null;
		HtmlRender<?> render = getHtmlBeanPropertyMeta(pMeta).getRender();
		if (render != null)
			return render;
		ClassMeta<?> cMeta = session.getClassMetaForObject(value);
		render = cMeta == null ? null : getHtmlClassMeta(cMeta).getRender();
		return render;
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	private String getStyle(HtmlSerializerSession session, BeanPropertyMeta pMeta, Object value) {
		HtmlRender render = getRender(session, pMeta, value);
		return render == null ? null : render.getStyle(session, value);
	}

	private String getLink(BeanPropertyMeta pMeta) {
		return pMeta == null ? null : getHtmlBeanPropertyMeta(pMeta).getLink();
	}

	private String getAnchorText(BeanPropertyMeta pMeta) {
		return pMeta == null ? null : getHtmlBeanPropertyMeta(pMeta).getAnchorText();
	}

	/*
	 * Returns the table column headers for the specified collection of objects.
	 * Returns null if collection should not be serialized as a 2-dimensional table.
	 * Returns an empty array if it should be treated as a table but without headers.
	 * 2-dimensional tables are used for collections of objects that all have the same set of property names.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object[] getTableHeaders(Collection c, HtmlBeanPropertyMeta bpHtml) throws SerializeException  {

		if (c.size() == 0)
			return null;

		c = sort(c);

		Object o1 = null;
		for (Object o : c)
			if (o != null) {
				o1 = o;
				break;
			}
		if (o1 == null)
			return null;

		ClassMeta<?> cm1 = getClassMetaForObject(o1);

		ObjectSwap swap = cm1.getSwap(this);
		o1 = swap(swap, o1);
		if (swap != null)
			cm1 = swap.getSwapClassMeta(this);

		if (cm1 == null || ! cm1.isMapOrBean() || cm1.hasAnnotation(HtmlLink.class))
			return null;

		HtmlClassMeta cHtml = getHtmlClassMeta(cm1);

		if (cHtml.isNoTables() || bpHtml.isNoTables() || cHtml.isXml() || bpHtml.isXml() || canIgnoreValue(cm1, null, o1))
			return null;

		if (cHtml.isNoTableHeaders() || bpHtml.isNoTableHeaders())
			return new Object[0];

		// If it's a non-bean map, only use table if all entries are also maps.
		if (cm1.isMap() && ! cm1.isBeanMap()) {

			Set<Object> set = CollectionUtils.set();
			for (Object o : c) {
				o = swap(swap, o);
				if (! canIgnoreValue(cm1, null, o)) {
					if (! cm1.isInstance(o))
						return null;
					forEachEntry((Map)o, x -> set.add(x.getKey()));
				}
			}
			return set.toArray(new Object[set.size()]);
		}

		// Must be a bean or BeanMap.
		for (Object o : c) {
			o = swap(swap, o);
			if (! canIgnoreValue(cm1, null, o)) {
				if (! cm1.isInstance(o))
					return null;
			}
		}

		BeanMap<?> bm = toBeanMap(o1);
		return bm.keySet().toArray(new String[bm.size()]);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see HtmlSerializer.Builder#addBeanTypesHtml()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Add key/value headers on bean/map tables.
	 *
	 * @see HtmlSerializer.Builder#addKeyValueTableHeaders()
	 * @return
	 * 	<jk>true</jk> if <bc>key</bc> and <bc>value</bc> column headers are added to tables.
	 */
	protected final boolean isAddKeyValueTableHeaders() {
		return ctx.isAddKeyValueTableHeaders();
	}

	/**
	 * Look for link labels in URIs.
	 *
	 * @see HtmlSerializer.Builder#disableDetectLabelParameters()
	 * @return
	 * 	<jk>true</jk> if we should ook for URL label parameters (e.g. <js>"?label=foobar"</js>).
	 */
	protected final boolean isDetectLabelParameters() {
		return ctx.isDetectLabelParameters();
	}

	/**
	 * Look for URLs in {@link String Strings}.
	 *
	 * @see HtmlSerializer.Builder#disableDetectLinksInStrings()
	 * @return
	 * 	<jk>true</jk> if we should automatically convert strings to URLs if they look like a URL.
	 */
	protected final boolean isDetectLinksInStrings() {
		return ctx.isDetectLinksInStrings();
	}

	/**
	 * Link label parameter name.
	 *
	 * @see HtmlSerializer.Builder#labelParameter(String)
	 * @return
	 * 	The parameter name to look for when resolving link labels.
	 */
	protected final String getLabelParameter() {
		return ctx.getLabelParameter();
	}

	/**
	 * Anchor text source.
	 *
	 * @see HtmlSerializer.Builder#uriAnchorText(AnchorText)
	 * @return
	 * 	When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * 	<xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 */
	protected final AnchorText getUriAnchorText() {
		return ctx.getUriAnchorText();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected HtmlClassMeta getHtmlClassMeta(ClassMeta<?> cm) {
		return ctx.getHtmlClassMeta(cm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	protected HtmlBeanPropertyMeta getHtmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return ctx.getHtmlBeanPropertyMeta(bpm);
	}
}
