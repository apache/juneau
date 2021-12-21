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
package org.apache.juneau.xml;

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.xml.XmlSerializerSession.ContentResult.*;
import static org.apache.juneau.xml.XmlSerializerSession.JsonType.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlSerializer}.
 *
 * <ul class='spaced-list'>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc XmlDetails}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class XmlSerializerSession extends WriterSerializerSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(XmlSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends WriterSerializerSession.Builder {

		XmlSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(XmlSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public XmlSerializerSession build() {
			return new XmlSerializerSession(this);
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

	private final XmlSerializer ctx;
	private Namespace
		defaultNamespace;
	private Namespace[] namespaces = new Namespace[0];

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected XmlSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		namespaces = ctx.getNamespaces();
		defaultNamespace = findDefaultNamespace(ctx.getDefaultNamespace());
	}

	private Namespace findDefaultNamespace(Namespace n) {
		if (n == null)
			return null;
		if (n.name != null && n.uri != null)
			return n;
		if (n.uri == null) {
			for (Namespace n2 : getNamespaces())
				if (n2.name.equals(n.name))
					return n2;
		}
		if (n.name == null) {
			for (Namespace n2 : getNamespaces())
				if (n2.uri.equals(n.uri))
					return n2;
		}
		return n;
	}

	/*
	 * Add a namespace to this session.
	 *
	 * @param ns The namespace being added.
	 */
	private void addNamespace(Namespace ns) {
		if (ns == defaultNamespace)
			return;

		for (Namespace n : namespaces)
			if (n == ns)
				return;

		if (defaultNamespace != null && (ns.uri.equals(defaultNamespace.uri) || ns.name.equals(defaultNamespace.name)))
			defaultNamespace = ns;
		else
			namespaces = append(namespaces, ns);
	}

	/**
	 * Returns <jk>true</jk> if we're serializing HTML.
	 *
	 * <p>
	 * The difference in behavior is how empty non-void elements are handled.
	 * The XML serializer will produce a collapsed tag, whereas the HTML serializer will produce a start and end tag.
	 *
	 * @return <jk>true</jk> if we're generating HTML.
	 */
	protected boolean isHtmlMode() {
		return false;
	}

	/**
	 * Converts the specified output target object to an {@link XmlWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link XmlWriter}.
	 * @throws IOException Thrown by underlying stream.
	 */
	public final XmlWriter getXmlWriter(SerializerPipe out) throws IOException {
		Object output = out.getRawOutput();
		if (output instanceof XmlWriter)
			return (XmlWriter)output;
		XmlWriter w = new XmlWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(), getQuoteChar(), getUriResolver(), isEnableNamespaces(), defaultNamespace);
		out.setWriter(w);
		return w;
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		if (isEnableNamespaces() && isAutoDetectNamespaces())
			findNsfMappings(o);
		serializeAnything(getXmlWriter(out), o, getExpectedRootType(o), null, null, null, isEnableNamespaces() && isAddNamespaceUrisToRoot(), XmlFormat.DEFAULT, false, false, null);
	}

	/**
	 * Recursively searches for the XML namespaces on the specified POJO and adds them to the serializer context object.
	 *
	 * @param o The POJO to check.
	 * @throws SerializeException Thrown if bean recursion occurred.
	 */
	protected final void findNsfMappings(Object o) throws SerializeException {
		ClassMeta<?> aType = null;						// The actual type

		try {
			aType = push(null, o, null);
		} catch (BeanRecursionException e) {
			throw new SerializeException(e);
		}

		if (aType != null) {
			Namespace ns = getXmlClassMeta(aType).getNamespace();
			if (ns != null) {
				if (ns.uri != null)
					addNamespace(ns);
				else
					ns = null;
			}
		}

		// Handle recursion
		if (aType != null && ! aType.isPrimitive()) {

			BeanMap<?> bm = null;
			if (aType.isBeanMap()) {
				bm = (BeanMap<?>)o;
			} else if (aType.isBean()) {
				bm = toBeanMap(o);
			} else if (aType.isDelegate()) {
				ClassMeta<?> innerType = ((Delegate<?>)o).getClassMeta();
				Namespace ns = getXmlClassMeta(innerType).getNamespace();
				if (ns != null) {
					if (ns.uri != null)
						addNamespace(ns);
					else
						ns = null;
				}

				if (innerType.isBean()) {
					for (BeanPropertyMeta bpm : innerType.getBeanMeta().getPropertyMetas()) {
						if (bpm.canRead()) {
							ns = getXmlBeanPropertyMeta(bpm).getNamespace();
							if (ns != null && ns.uri != null)
								addNamespace(ns);
						}
					}

				} else if (innerType.isMap()) {
					for (Object o2 : ((Map<?,?>)o).values())
						findNsfMappings(o2);
				} else if (innerType.isCollection()) {
					for (Object o2 : ((Collection<?>)o))
						findNsfMappings(o2);
				}

			} else if (aType.isMap()) {
				for (Object o2 : ((Map<?,?>)o).values())
					findNsfMappings(o2);
			} else if (aType.isCollection()) {
				for (Object o2 : ((Collection<?>)o))
					findNsfMappings(o2);
			} else if (aType.isArray() && ! aType.getElementType().isPrimitive()) {
				for (Object o2 : ((Object[])o))
					findNsfMappings(o2);
			}
			if (bm != null) {
				for (BeanPropertyValue p : bm.getValues(isKeepNullProperties())) {

					Namespace ns = getXmlBeanPropertyMeta(p.getMeta()).getNamespace();
					if (ns != null && ns.uri != null)
						addNamespace(ns);

					try {
						findNsfMappings(p.getValue());
					} catch (Throwable x) {
						// Ignore
					}
				}
			}
		}

		pop();
	}

	/**
	 * Workhorse method.
	 *
	 * @param out The writer to send the output to.
	 * @param o The object to serialize.
	 * @param eType The expected type if this is a bean property value being serialized.
	 * @param keyName The property name or map key name.
	 * @param elementName The root element name.
	 * @param elementNamespace The namespace of the element.
	 * @param addNamespaceUris Flag indicating that namespace URIs need to be added.
	 * @param format The format to serialize the output to.
	 * @param isMixedOrText We're serializing mixed content, so don't use whitespace.
	 * @param preserveWhitespace
	 * 	<jk>true</jk> if we're serializing {@link XmlFormat#MIXED_PWS} or {@link XmlFormat#TEXT_PWS}.
	 * @param pMeta The bean property metadata if this is a bean property being serialized.
	 * @return The same writer passed in so that calls to the writer can be chained.
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException General serialization error occurred.
	 */
	protected ContentResult serializeAnything(
			XmlWriter out,
			Object o,
			ClassMeta<?> eType,
			String keyName,
			String elementName,
			Namespace elementNamespace,
			boolean addNamespaceUris,
			XmlFormat format,
			boolean isMixedOrText,
			boolean preserveWhitespace,
			BeanPropertyMeta pMeta) throws IOException, SerializeException {

		JsonType type = null;              // The type string (e.g. <type> or <x x='type'>
		int i = isMixedOrText ? 0 : indent;       // Current indentation
		ClassMeta<?> aType = null;     // The actual type
		ClassMeta<?> wType = null;     // The wrapped type (delegate)
		ClassMeta<?> sType = object(); // The serialized type

		aType = push2(keyName, o, eType);

		if (eType == null)
			eType = object();

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

		if (o != null) {

			if (aType.isDelegate()) {
				wType = aType;
				eType = aType = ((Delegate<?>)o).getClassMeta();
			}

			sType = aType;

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
		} else {
			sType = eType.getSerializedClassMeta(this);
		}

		// Does the actual type match the expected type?
		boolean isExpectedType = true;
		if (o == null || ! eType.same(aType)) {
			if (eType.isNumber())
				isExpectedType = aType.isNumber();
			else if (eType.isMap())
				isExpectedType = aType.isMap();
			else if (eType.isCollectionOrArray())
				isExpectedType = aType.isCollectionOrArray();
			else
				isExpectedType = false;
		}

		String resolvedDictionaryName = isExpectedType ? null : aType.getDictionaryName();

		// Note that the dictionary name may be specified on the actual type or the serialized type.
		// HTML templates will have them defined on the serialized type.
		String dictionaryName = aType.getDictionaryName();
		if (dictionaryName == null)
			dictionaryName = sType.getDictionaryName();

		// char '\0' is interpreted as null.
		if (o != null && sType.isChar() && ((Character)o).charValue() == 0)
			o = null;

		boolean isCollapsed = false;		// If 'true', this is a collection and we're not rendering the outer element.
		boolean isRaw = (sType.isReader() || sType.isInputStream()) && o != null;

		// Get the JSON type string.
		if (o == null) {
			type = NULL;
		} else if (sType.isCharSequence() || sType.isChar()) {
			type = STRING;
		} else if (sType.isNumber()) {
			type = NUMBER;
		} else if (sType.isBoolean()) {
			type = BOOLEAN;
		} else if (sType.isMapOrBean()) {
			isCollapsed = getXmlClassMeta(sType).getFormat() == COLLAPSED;
			type = OBJECT;
		} else if (sType.isCollectionOrArray()) {
			isCollapsed = (format == COLLAPSED && ! addNamespaceUris);
			type = ARRAY;
		} else {
			type = STRING;
		}

		if (format.isOneOf(MIXED,MIXED_PWS,TEXT,TEXT_PWS,XMLTEXT) && type.isOneOf(NULL,STRING,NUMBER,BOOLEAN))
			isCollapsed = true;

		// Is there a name associated with this bean?

		String name = keyName;
		if (elementName == null && dictionaryName != null) {
			elementName = dictionaryName;
			isExpectedType = o != null;  // preserve type='null' when it's null.
		}

		if (elementName == null) {
			elementName = name;
			name = null;
		}

		if (eq(name, elementName))
			name = null;

		if (isEnableNamespaces()) {
			if (elementNamespace == null)
				elementNamespace = getXmlClassMeta(sType).getNamespace();
			if (elementNamespace == null)
				elementNamespace = getXmlClassMeta(aType).getNamespace();
			if (elementNamespace != null && elementNamespace.uri == null)
				elementNamespace = null;
			if (elementNamespace == null)
				elementNamespace = defaultNamespace;
		} else {
			elementNamespace = null;
		}

		// Do we need a carriage return after the start tag?
		boolean cr = o != null && (sType.isMapOrBean() || sType.isCollectionOrArray()) && ! isMixedOrText;

		String en = elementName;
		if (en == null && ! isRaw) {
			en = type.toString();
			type = null;
		}

		boolean encodeEn = elementName != null;
		String ns = (elementNamespace == null ? null : elementNamespace.name);
		String dns = null, elementNs = null;
		if (isEnableNamespaces()) {
			dns = elementName == null && defaultNamespace != null ? defaultNamespace.name : null;
			elementNs = elementName == null ? dns : ns;
			if (elementName == null)
				elementNamespace = null;
		}

		// Render the start tag.
		if (! isCollapsed) {
			if (en != null) {
				out.oTag(i, elementNs, en, encodeEn);
				if (addNamespaceUris) {
					out.attr((String)null, "xmlns", defaultNamespace.getUri());

					for (Namespace n : namespaces)
						out.attr("xmlns", n.getName(), n.getUri());
				}
				if (! isExpectedType) {
					if (resolvedDictionaryName != null)
						out.attr(dns, getBeanTypePropertyName(eType), resolvedDictionaryName);
					else if (type != null && type != STRING)
						out.attr(dns, getBeanTypePropertyName(eType), type);
				}
				if (name != null)
					out.attr(getNamePropertyName(), name);
			} else {
				out.i(i);
			}
			if (o == null) {
				if ((sType.isBoolean() || sType.isNumber()) && ! sType.isNullable())
					o = sType.getPrimitiveDefault();
			}

			if (o != null && ! (sType.isMapOrBean() || en == null))
				out.append('>');

			if (cr && ! (sType.isMapOrBean()))
				out.nl(i+1);
		}

		ContentResult rc = CR_ELEMENTS;

		// Render the tag contents.
		if (o != null) {
			if (sType.isUri() || (pMeta != null && pMeta.isUri())) {
				out.textUri(o);
			} else if (sType.isCharSequence() || sType.isChar()) {
				if (isXmlText(format, sType))
					out.append(o);
				else
					out.text(o, preserveWhitespace);
			} else if (sType.isNumber() || sType.isBoolean()) {
				out.append(o);
			} else if (sType.isMap() || (wType != null && wType.isMap())) {
				if (o instanceof BeanMap)
					rc = serializeBeanMap(out, (BeanMap)o, elementNamespace, isCollapsed, isMixedOrText);
				else
					rc = serializeMap(out, (Map)o, sType, eType.getKeyType(), eType.getValueType(), isMixedOrText);
			} else if (sType.isBean()) {
				rc = serializeBeanMap(out, toBeanMap(o), elementNamespace, isCollapsed, isMixedOrText);
			} else if (sType.isCollection() || (wType != null && wType.isCollection())) {
				if (isCollapsed)
					this.indent--;
				serializeCollection(out, o, sType, eType, pMeta, isMixedOrText);
				if (isCollapsed)
					this.indent++;
			} else if (sType.isArray()) {
				if (isCollapsed)
					this.indent--;
				serializeCollection(out, o, sType, eType, pMeta, isMixedOrText);
				if (isCollapsed)
					this.indent++;
			} else if (sType.isReader()) {
				pipe((Reader)o, out);
			} else if (sType.isInputStream()) {
				pipe((InputStream)o, out);
			} else {
				if (isXmlText(format, sType))
					out.append(toString(o));
				else
					out.text(toString(o));
			}
		}

		pop();

		// Render the end tag.
		if (! isCollapsed) {
			if (en != null) {
				if (rc == CR_EMPTY) {
					if (isHtmlMode())
						out.append('>').eTag(elementNs, en, encodeEn);
					else
						out.append('/').append('>');
				} else if (rc == CR_VOID || o == null) {
					out.append('/').append('>');
				}
				else
					out.ie(cr && rc != CR_MIXED ? i : 0).eTag(elementNs, en, encodeEn);
			}
			if (! isMixedOrText)
				out.nl(i);
		}

		return rc;
	}

	private boolean isXmlText(XmlFormat format, ClassMeta<?> sType) {
		if (format == XMLTEXT)
			return true;
		XmlClassMeta xcm = getXmlClassMeta(sType);
		if (xcm == null)
			return false;
		return xcm.getFormat() == XMLTEXT;
	}

	private ContentResult serializeMap(XmlWriter out, Map m, ClassMeta<?> sType,
			ClassMeta<?> eKeyType, ClassMeta<?> eValueType, boolean isMixed) throws IOException, SerializeException {

		m = sort(m);

		ClassMeta<?> keyType = eKeyType == null ? sType.getKeyType() : eKeyType;
		ClassMeta<?> valueType = eValueType == null ? sType.getValueType() : eValueType;

		boolean hasChildren = false;
		for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry)i.next();

			Object k = e.getKey();
			if (k == null) {
				k = "\u0000";
			} else {
				k = generalize(k, keyType);
				if (isTrimStrings() && k instanceof String)
					k = k.toString().trim();
			}

			Object value = e.getValue();

			if (! hasChildren) {
				hasChildren = true;
				out.append('>').nlIf(! isMixed, indent);
			}
			serializeAnything(out, value, valueType, toString(k), null, null, false, XmlFormat.DEFAULT, isMixed, false, null);
		}
		return hasChildren ? CR_ELEMENTS : CR_EMPTY;
	}

	private ContentResult serializeBeanMap(XmlWriter out, BeanMap<?> m,
			Namespace elementNs, boolean isCollapsed, boolean isMixedOrText) throws IOException, SerializeException {
		boolean hasChildren = false;
		BeanMeta<?> bm = m.getMeta();

		List<BeanPropertyValue> lp = m.getValues(isKeepNullProperties());

		XmlBeanMeta xbm = getXmlBeanMeta(bm);

		Set<String>
			attrs = xbm.getAttrPropertyNames(),
			elements = xbm.getElementPropertyNames(),
			collapsedElements = xbm.getCollapsedPropertyNames();
		String
			attrsProperty = xbm.getAttrsPropertyName(),
			contentProperty = xbm.getContentPropertyName();

		XmlFormat cf = null;

		Object content = null;
		ClassMeta<?> contentType = null;
		for (BeanPropertyValue p : lp) {
			String n = p.getName();
			if (attrs.contains(n) || attrs.contains("*") || n.equals(attrsProperty)) {
				BeanPropertyMeta pMeta = p.getMeta();
				if (pMeta.canRead()) {
					ClassMeta<?> cMeta = p.getClassMeta();

					String key = p.getName();
					Object value = p.getValue();
					Throwable t = p.getThrown();
					if (t != null)
						onBeanGetterException(pMeta, t);

					if (canIgnoreValue(cMeta, key, value))
						continue;

					XmlBeanPropertyMeta bpXml = getXmlBeanPropertyMeta(pMeta);
					Namespace ns = (isEnableNamespaces() && bpXml.getNamespace() != elementNs ? bpXml.getNamespace() : null);

					if (pMeta.isUri()  ) {
						out.attrUri(ns, key, value);
					} else if (n.equals(attrsProperty)) {
						if (value instanceof BeanMap) {
							BeanMap<?> bm2 = (BeanMap)value;
							for (BeanPropertyValue p2 : bm2.getValues(false)) {
								String key2 = p2.getName();
								Object value2 = p2.getValue();
								Throwable t2 = p2.getThrown();
								if (t2 != null)
									onBeanGetterException(pMeta, t);
								out.attr(ns, key2, value2);
							}
						} else /* Map */ {
							Map m2 = (Map)value;
							if (m2 != null)
								for (Map.Entry e : (Set<Map.Entry>)(m2.entrySet()))
									out.attr(ns, toString(e.getKey()), e.getValue());
						}
					} else {
						out.attr(ns, key, value);
					}
				}
			}
		}

		boolean
			hasContent = false,
			preserveWhitespace = false,
			isVoidElement = xbm.getContentFormat() == VOID;

		for (BeanPropertyValue p : lp) {
			BeanPropertyMeta pMeta = p.getMeta();
			if (pMeta.canRead()) {
				ClassMeta<?> cMeta = p.getClassMeta();

				String n = p.getName();
				if (n.equals(contentProperty)) {
					content = p.getValue();
					contentType = p.getClassMeta();
					hasContent = true;
					cf = xbm.getContentFormat();
					if (cf.isOneOf(MIXED,MIXED_PWS,TEXT,TEXT_PWS,XMLTEXT))
						isMixedOrText = true;
					if (cf.isOneOf(MIXED_PWS, TEXT_PWS))
						preserveWhitespace = true;
					if (contentType.isCollection() && ((Collection)content).isEmpty())
						hasContent = false;
					else if (contentType.isArray() && Array.getLength(content) == 0)
						hasContent = false;
				} else if (elements.contains(n) || collapsedElements.contains(n) || elements.contains("*") || collapsedElements.contains("*") ) {
					String key = p.getName();
					Object value = p.getValue();
					Throwable t = p.getThrown();
					if (t != null)
						onBeanGetterException(pMeta, t);

					if (canIgnoreValue(cMeta, key, value))
						continue;

					if (! hasChildren) {
						hasChildren = true;
						out.appendIf(! isCollapsed, '>').nlIf(! isMixedOrText, indent);
					}

					XmlBeanPropertyMeta bpXml = getXmlBeanPropertyMeta(pMeta);
					serializeAnything(out, value, cMeta, key, null, bpXml.getNamespace(), false, bpXml.getXmlFormat(), isMixedOrText, false, pMeta);
				}
			}
		}
		if (contentProperty == null && ! hasContent)
			return (hasChildren ? CR_ELEMENTS : isVoidElement ? CR_VOID : CR_EMPTY);

		// Serialize XML content.
		if (content != null) {
			out.append('>').nlIf(! isMixedOrText, indent);
			if (contentType == null) {
			} else if (contentType.isCollection()) {
				Collection c = (Collection)content;
				for (Iterator i = c.iterator(); i.hasNext();) {
					Object value = i.next();
					serializeAnything(out, value, contentType.getElementType(), null, null, null, false, cf, isMixedOrText, preserveWhitespace, null);
				}
			} else if (contentType.isArray()) {
				Collection c = toList(Object[].class, content);
				for (Iterator i = c.iterator(); i.hasNext();) {
					Object value = i.next();
					serializeAnything(out, value, contentType.getElementType(), null, null, null, false, cf, isMixedOrText, preserveWhitespace, null);
				}
			} else {
				serializeAnything(out, content, contentType, null, null, null, false, cf, isMixedOrText, preserveWhitespace, null);
			}
		} else {
			out.attr("nil", "true").append('>').nlIf(! isMixedOrText, indent);
		}
		return isMixedOrText ? CR_MIXED : CR_ELEMENTS;
	}

	private XmlWriter serializeCollection(XmlWriter out, Object in, ClassMeta<?> sType,
			ClassMeta<?> eType, BeanPropertyMeta ppMeta, boolean isMixed) throws IOException, SerializeException {

		ClassMeta<?> eeType = eType.getElementType();

		Collection c = (sType.isCollection() ? (Collection)in : toList(sType.getInnerClass(), in));

		c = sort(c);

		String type2 = null;

		String eName = type2;
		Namespace eNs = null;

		if (ppMeta != null) {
			XmlBeanPropertyMeta bpXml = getXmlBeanPropertyMeta(ppMeta);
			eName = bpXml.getChildName();
			eNs = bpXml.getNamespace();
		}

		for (Iterator i = c.iterator(); i.hasNext();) {
			Object value = i.next();
			serializeAnything(out, value, eeType, null, eName, eNs, false, XmlFormat.DEFAULT, isMixed, false, null);
		}
		return out;
	}

	static enum JsonType {
		STRING("string"),BOOLEAN("boolean"),NUMBER("number"),ARRAY("array"),OBJECT("object"),NULL("null");

		private final String value;
		private JsonType(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

		boolean isOneOf(JsonType...types) {
			for (JsonType type : types)
				if (type == this)
					return true;
			return false;
		}
	}

	/**
	 * Identifies what the contents were of a serialized bean.
	 */
	@SuppressWarnings("javadoc")
	public static enum ContentResult {
		CR_VOID,      // No content...append "/>" to the start tag.
		CR_EMPTY,     // No content...append "/>" to the start tag if XML, "/></end>" if HTML.
		CR_MIXED,     // Mixed content...don't add whitespace.
		CR_ELEMENTS   // Elements...use normal whitespace rules.
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * @see XmlSerializer.Builder#addBeanTypesXml()
	 * @return
	 * 	<jk>true</jk> if<js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Configuration property:  Add namespace URLs to the root element.
	 *
	 * @see XmlSerializer.Builder#addNamespaceUrisToRoot()
	 * @return
	 * 	<jk>true</jk> if {@code xmlns:x} attributes are added to the root element for the default and all mapped namespaces.
	 */
	protected final boolean isAddNamespaceUrisToRoot() {
		return ctx.isAddNamespaceUrlsToRoot();
	}

	/**
	 * Configuration property:  Auto-detect namespace usage.
	 *
	 * @see XmlSerializer.Builder#disableAutoDetectNamespaces()
	 * @return
	 * 	<jk>true</jk> if namespace usage is detected before serialization.
	 */
	protected final boolean isAutoDetectNamespaces() {
		return ctx.isAutoDetectNamespaces();
	}

	/**
	 * Configuration property:  Default namespace.
	 *
	 * @see XmlSerializer.Builder#defaultNamespace(Namespace)
	 * @return
	 * 	The default namespace URI for this document.
	 */
	protected final Namespace getDefaultNamespace() {
		return defaultNamespace;
	}

	/**
	 * Configuration property:  Enable support for XML namespaces.
	 *
	 * @see XmlSerializer.Builder#enableNamespaces()
	 * @return
	 * 	<jk>false</jk> if XML output will not contain any namespaces regardless of any other settings.
	 */
	protected final boolean isEnableNamespaces() {
		return ctx.isEnableNamespaces();
	}

	/**
	 * Configuration property:  Default namespaces.
	 *
	 * @see XmlSerializer.Builder#namespaces(Namespace...)
	 * @return
	 * 	The default list of namespaces associated with this serializer.
	 */
	protected final Namespace[] getNamespaces() {
		return namespaces;
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
	public XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		return ctx.getXmlClassMeta(cm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean.
	 *
	 * @param bm The bean to return the metadata on.
	 * @return The metadata.
	 */
	public XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		return ctx.getXmlBeanMeta(bm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	public XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return bpm == null ? XmlBeanPropertyMeta.DEFAULT : ctx.getXmlBeanPropertyMeta(bpm);
	}
}
