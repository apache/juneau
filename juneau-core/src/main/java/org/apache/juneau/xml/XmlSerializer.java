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

import static org.apache.juneau.xml.XmlSerializerContext.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.apache.juneau.xml.XmlSerializer.ContentResult.*;
import static org.apache.juneau.xml.XmlSerializer.JsonType.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Serializes POJO models to XML.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/xml</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/xml</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * 	See the {@link JsonSerializer} class for details on how Java models map to JSON.
 * <p>
 * 	For example, the following JSON...
 * <p class='bcode'>
 * 	{
 * 		name:<js>'John Smith'</js>,
 * 		address: {
 * 			streetAddress: <js>'21 2nd Street'</js>,
 * 			city: <js>'New York'</js>,
 * 			state: <js>'NY'</js>,
 * 			postalCode: <js>10021</js>
 * 		},
 * 		phoneNumbers: [
 * 			<js>'212 555-1111'</js>,
 * 			<js>'212 555-2222'</js>
 * 		],
 * 		additionalInfo: <jk>null</jk>,
 * 		remote: <jk>false</jk>,
 * 		height: <js>62.4</js>,
 * 		<js>'fico score'</js>:  <js>' &gt; 640'</js>
 * 	}
 * <p>
 * 	...maps to the following XML using the default serializer...
 * <p class='bcode'>
 * 	<xt>&lt;object&gt;</xt>
 * 		<xt>&lt;name&gt;</xt>John Smith<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;address&gt;</xt>
 * 			<xt>&lt;streetAddress&gt;</xt>21 2nd Street<xt>&lt;/streetAddress&gt;</xt>
 * 			<xt>&lt;city&gt;</xt>New York<xt>&lt;/city&gt;</xt>
 * 			<xt>&lt;state&gt;</xt>NY<xt>&lt;/state&gt;</xt>
 * 			<xt>&lt;postalCode&gt;</xt>10021<xt>&lt;/postalCode&gt;</xt>
 * 		<xt>&lt;/address&gt;</xt>
 * 		<xt>&lt;phoneNumbers&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-1111<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-2222<xt>&lt;/string&gt;</xt>
 * 		<xt>&lt;/phoneNumbers&gt;</xt>
 * 		<xt>&lt;additionalInfo</xt> <xa>_type</xa>=<xs>'null'</xs><xt>&gt;&lt;/additionalInfo&gt;</xt>
 * 		<xt>&lt;remote&gt;</xt>false<xt>&lt;/remote&gt;</xt>
 * 		<xt>&lt;height&gt;</xt>62.4<xt>&lt;/height&gt;</xt>
 * 		<xt>&lt;fico_x0020_score&gt;</xt> &amp;gt; 640<xt>&lt;/fico_x0020_score&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 * <p>
 * 	An additional "add-json-properties" mode is also provided to prevent loss of JSON data types...
 * <p class='bcode'>
 * 		<xt>&lt;name</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>John Smith<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;address</xt> <xa>_type</xa>=<xs>'object'</xs><xt>&gt;</xt>
 * 			<xt>&lt;streetAddress</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>21 2nd Street<xt>&lt;/streetAddress&gt;</xt>
 * 			<xt>&lt;city</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>New York<xt>&lt;/city&gt;</xt>
 * 			<xt>&lt;state</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>NY<xt>&lt;/state&gt;</xt>
 * 			<xt>&lt;postalCode</xt> <xa>_type</xa>=<xs>'number'</xs><xt>&gt;</xt>10021<xt>&lt;/postalCode&gt;</xt>
 * 		<xt>&lt;/address&gt;</xt>
 * 		<xt>&lt;phoneNumbers</xt> <xa>_type</xa>=<xs>'array'</xs><xt>&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-1111<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-2222<xt>&lt;/string&gt;</xt>
 * 		<xt>&lt;/phoneNumbers&gt;</xt>
 * 		<xt>&lt;additionalInfo</xt> <xa>_type</xa>=<xs>'null'</xs><xt>&gt;&lt;/additionalInfo&gt;</xt>
 * 		<xt>&lt;remote</xt> <xa>_type</xa>=<xs>'boolean'</xs><xt>&gt;</xt>false<xt>&lt;/remote&gt;</xt>
 * 		<xt>&lt;height</xt> <xa>_type</xa>=<xs>'number'</xs><xt>&gt;</xt>62.4<xt>&lt;/height&gt;</xt>
 * 		<xt>&lt;fico_x0020_score</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt> &amp;gt; 640<xt>&lt;/fico_x0020_score&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined <jsf>DEFAULT</jsf> serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 * <p>
 * 	If an attribute name contains any non-valid XML element characters, they will be escaped using standard {@code _x####_} notation.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link XmlSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Produces("text/xml")
public class XmlSerializer extends WriterSerializer {

	/** Default serializer without namespaces. */
	public static final XmlSerializer DEFAULT = new XmlSerializer().lock();

	/** Default serializer without namespaces, with single quotes. */
	public static final XmlSerializer DEFAULT_SQ = new XmlSerializer.Sq().lock();

	/** Default serializer without namespaces, with single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_SQ_READABLE = new XmlSerializer.SqReadable().lock();

	/** Default serializer, all default settings. */
	public static final XmlSerializer DEFAULT_NS = new XmlSerializer.Ns().lock();

	/** Default serializer, single quotes. */
	public static final XmlSerializer DEFAULT_NS_SQ = new XmlSerializer.NsSq().lock();

	/** Default serializer, single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_NS_SQ_READABLE = new XmlSerializer.NsSqReadable().lock();

	/** Default serializer, single quotes. */
	public static class Sq extends XmlSerializer {
		/** Constructor */
		public Sq() {
			setQuoteChar('\'');
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends Sq {
		/** Constructor */
		public SqReadable() {
			setUseIndentation(true);
		}
	}

	/** Default serializer without namespaces. */
	@Produces(value="text/xml+simple",contentType="text/xml")
	public static class Ns extends XmlSerializer {
		/** Constructor */
		public Ns() {
			setEnableNamespaces(true);
		}
	}

	/** Default serializer without namespaces, single quotes. */
	public static class NsSq extends Ns {
		/** Constructor */
		public NsSq() {
			setQuoteChar('\'');
		}
	}

	/** Default serializer without namespaces, single quotes, with whitespace. */
	public static class NsSqReadable extends NsSq {
		/** Constructor */
		public NsSqReadable() {
			setUseIndentation(true);
		}
	}

	/**
	 * Recursively searches for the XML namespaces on the specified POJO and adds them to the serializer context object.
	 *
	 * @param session The context that exists for the duration of a single serialization.
	 * @param o The POJO to check.
	 * @throws SerializeException
	 */
	protected void findNsfMappings(XmlSerializerSession session, Object o) throws SerializeException {
		ClassMeta<?> aType = null;						// The actual type
		aType = session.push(null, o, null);

		if (aType != null) {
			Namespace ns = aType.getExtendedMeta(XmlClassMeta.class).getNamespace();
			if (ns != null) {
				if (ns.uri != null)
					session.addNamespace(ns);
				else
					ns = null;
			}
		}

		// Handle recursion
		if (aType != null && ! aType.isPrimitive()) {

			BeanMap<?> bm = null;
			if (aType.isBeanMap()) {
				bm = (BeanMap)o;
			} else if (aType.isBean()) {
				bm = session.toBeanMap(o);
			} else if (aType.isDelegate()) {
				ClassMeta<?> innerType = ((Delegate)o).getClassMeta();
				Namespace ns = innerType.getExtendedMeta(XmlClassMeta.class).getNamespace();
				if (ns != null) {
					if (ns.uri != null)
						session.addNamespace(ns);
					else
						ns = null;
				}

				if (innerType.isBean()) {
					for (BeanPropertyMeta bpm : innerType.getBeanMeta().getPropertyMetas()) {
						ns = bpm.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
						if (ns != null && ns.uri != null)
							session.addNamespace(ns);
					}

				} else if (innerType.isMap()) {
					for (Object o2 : ((Map)o).values())
						findNsfMappings(session, o2);
				} else if (innerType.isCollection()) {
					for (Object o2 : ((Collection)o))
						findNsfMappings(session, o2);
				}

			} else if (aType.isMap()) {
				for (Object o2 : ((Map)o).values())
					findNsfMappings(session, o2);
			} else if (aType.isCollection()) {
				for (Object o2 : ((Collection)o))
					findNsfMappings(session, o2);
			} else if (aType.isArray() && ! aType.getElementType().isPrimitive()) {
				for (Object o2 : ((Object[])o))
					findNsfMappings(session, o2);
			}
			if (bm != null) {
				for (BeanPropertyValue p : bm.getValues(session.isTrimNulls())) {

					Namespace ns = p.getMeta().getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
					if (ns != null && ns.uri != null)
						session.addNamespace(ns);

					try {
						findNsfMappings(session, p.getValue());
					} catch (Throwable x) {
						// Ignore
					}
				}
			}
		}

		session.pop();
	}

	/**
	 * Workhorse method.
	 *
	 * @param session The serializer context.
	 * @param out The writer to send the output to.
	 * @param o The object to serialize.
	 * @param eType The expected type if this is a bean property value being serialized.
	 * @param elementName The root element name.
	 * @param elementNamespace The namespace of the element.
	 * @param addNamespaceUris Flag indicating that namespace URIs need to be added.
	 * @param format The format to serialize the output to.
	 * @param isMixed We're serializing mixed content, so don't use whitespace.
	 * @param preserveWhitespace <jk>true</jk> if we're serializing {@link XmlFormat#MIXED_PWS} or {@link XmlFormat#TEXT_PWS}.
	 * @param pMeta The bean property metadata if this is a bean property being serialized.
	 * @return The same writer passed in so that calls to the writer can be chained.
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	protected XmlWriter serializeAnything(
			XmlSerializerSession session,
			XmlWriter out,
			Object o,
			ClassMeta eType,
			String elementName,
			Namespace elementNamespace,
			boolean addNamespaceUris,
			XmlFormat format,
			boolean isMixed,
			boolean preserveWhitespace,
			BeanPropertyMeta pMeta) throws Exception {

		JsonType type = null;              // The type string (e.g. <type> or <x x='type'>
		int indent = isMixed ? 0 : session.indent;       // Current indentation
		ClassMeta<?> aType = null;     // The actual type
		ClassMeta<?> wType = null;     // The wrapped type (delegate)
		ClassMeta<?> sType = object(); // The serialized type

		aType = session.push(elementName, o, eType);

		if (eType == null)
			eType = object();

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		if (o != null) {

			if (aType.isDelegate()) {
				wType = aType;
				eType = aType = ((Delegate)o).getClassMeta();
			}

			sType = aType.getSerializedClassMeta();

			// Swap if necessary
			PojoSwap swap = aType.getPojoSwap();
			if (swap != null) {
				o = swap.swap(session, o);

				// If the getSwapClass() method returns Object, we need to figure out
				// the actual type now.
				if (sType.isObject())
					sType = session.getClassMetaForObject(o);
			}
		} else {
			sType = eType.getSerializedClassMeta();
		}

		// Does the actual type match the expected type?
		boolean isExpectedType = true;
		if (o == null || ! eType.same(aType)) {
			if (eType.isNumber())
				isExpectedType = aType.isNumber();
			else if (eType.isMap())
				isExpectedType = aType.isMap();
			else if (eType.isCollection())
				isExpectedType = aType.isCollection();
			else
				isExpectedType = false;
		}

		String resolvedDictionaryName = isExpectedType ? null : aType.getResolvedDictionaryName();

		// Note that the dictionary name may be specified on the actual type or the serialized type.
		// HTML templates will have them defined on the serialized type.
		String dictionaryName = aType.getDictionaryName();
		if (dictionaryName == null)
			dictionaryName = sType.getDictionaryName();

		// char '\0' is interpreted as null.
		if (o != null && sType.isChar() && ((Character)o).charValue() == 0)
			o = null;

		boolean isCollapsed = false;		// If 'true', this is a collection and we're not rendering the outer element.

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
			isCollapsed = sType.getExtendedMeta(XmlClassMeta.class).getFormat() == COLLAPSED;
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
		if (elementName == null && dictionaryName != null) {
			elementName = dictionaryName;
			isExpectedType = true;
		}

		if (session.isEnableNamespaces()) {
			if (elementNamespace == null)
				elementNamespace = sType.getExtendedMeta(XmlClassMeta.class).getNamespace();
			if (elementNamespace == null)
				elementNamespace = aType.getExtendedMeta(XmlClassMeta.class).getNamespace();
			if (elementNamespace != null && elementNamespace.uri == null)
				elementNamespace = null;
			if (elementNamespace == null)
				elementNamespace = session.getDefaultNamespace();
		} else {
			elementNamespace = null;
		}

		// Do we need a carriage return after the start tag?
		boolean cr = o != null && (sType.isMapOrBean() || sType.isCollectionOrArray()) && ! isMixed;

		String en = elementName;
		if (en == null) {
			en = type.toString();
			type = null;
		}
		boolean encodeEn = elementName != null;
		String ns = (elementNamespace == null ? null : elementNamespace.name);
		String dns = null, elementNs = null;
		if (session.isEnableNamespaces()) {
			dns = elementName == null && session.getDefaultNamespace() != null ? session.getDefaultNamespace().name : null;
			elementNs = elementName == null ? dns : ns;
			if (elementName == null)
				elementNamespace = null;
		}

		// Render the start tag.
		if (! isCollapsed) {
			out.oTag(indent, elementNs, en, encodeEn);
			if (addNamespaceUris) {
				out.attr((String)null, "xmlns", session.getDefaultNamespace().getUri());

				for (Namespace n : session.getNamespaces())
					out.attr("xmlns", n.getName(), n.getUri());
			}
			if (! isExpectedType) {
				if (resolvedDictionaryName != null)
					out.attr(dns, session.getBeanTypePropertyName(), resolvedDictionaryName);
				else if (type != null && type != STRING)
					out.attr(dns, session.getBeanTypePropertyName(), type);
			}
			if (o == null) {
				if ((sType.isBoolean() || sType.isNumber()) && ! sType.isNullable())
					o = sType.getPrimitiveDefault();
			}

			if (o != null && ! (sType.isMapOrBean()))
				out.append('>');

			if (cr && ! (sType.isMapOrBean()))
				out.nl();
		}

		ContentResult rc = CR_ELEMENTS;

		// Render the tag contents.
		if (o != null) {
			if (sType.isUri() || (pMeta != null && pMeta.isUri())) {
				out.appendUri(o);
			} else if (sType.isCharSequence() || sType.isChar()) {
				if (format == XMLTEXT)
					out.append(o);
				else
					out.text(o, preserveWhitespace);
			} else if (sType.isNumber() || sType.isBoolean()) {
				out.append(o);
			} else if (sType.isMap() || (wType != null && wType.isMap())) {
				if (o instanceof BeanMap)
					rc = serializeBeanMap(session, out, (BeanMap)o, elementNamespace, isCollapsed, isMixed);
				else
					rc = serializeMap(session, out, (Map)o, sType, eType.getKeyType(), eType.getValueType(), isMixed);
			} else if (sType.isBean()) {
				rc = serializeBeanMap(session, out, session.toBeanMap(o), elementNamespace, isCollapsed, isMixed);
			} else if (sType.isCollection() || (wType != null && wType.isCollection())) {
				if (isCollapsed)
					session.indent--;
				serializeCollection(session, out, o, sType, eType, pMeta, isMixed);
				if (isCollapsed)
					session.indent++;
			} else if (sType.isArray()) {
				if (isCollapsed)
					session.indent--;
				if (resolvedDictionaryName != null)
					eType = aType;
				serializeCollection(session, out, o, sType, eType, pMeta, isMixed);
				if (isCollapsed)
					session.indent++;
			} else {
				if (format == XMLTEXT)
					out.append(session.toString(o));
				else
					out.text(session.toString(o));
			}
		}

		session.pop();

		// Render the end tag.
		if (! isCollapsed) {
			if (o == null || rc == CR_EMPTY)
				out.append('/').append('>');
			else
				out.i(cr && rc != CR_MIXED ? indent : 0).eTag(elementNs, en, encodeEn);
			if (! isMixed)
				out.nl();
		}

		return out;
	}

	private ContentResult serializeMap(XmlSerializerSession session, XmlWriter out, Map m, ClassMeta<?> sType, ClassMeta<?> eKeyType, ClassMeta<?> eValueType, boolean isMixed) throws Exception {

		m = session.sort(m);

		ClassMeta<?> keyType = eKeyType == null ? sType.getKeyType() : eKeyType;
		ClassMeta<?> valueType = eValueType == null ? sType.getValueType() : eValueType;

		boolean hasChildren = false;
		for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry)i.next();

			Object k = e.getKey();
			if (k == null) {
				k = "\u0000";
			} else {
				k = session.generalize(k, keyType);
				if (session.isTrimStrings() && k instanceof String)
					k = k.toString().trim();
			}

			Object value = e.getValue();

			if (! hasChildren) {
				hasChildren = true;
				out.append('>').nlIf(! isMixed);
			}
			serializeAnything(session, out, value, valueType, session.toString(k), null, false, XmlFormat.DEFAULT, isMixed, false, null);
		}
		return hasChildren ? CR_ELEMENTS : CR_EMPTY;
	}

	private ContentResult serializeBeanMap(XmlSerializerSession session, XmlWriter out, BeanMap<?> m, Namespace elementNs, boolean isCollapsed, boolean isMixed) throws Exception {
		boolean hasChildren = false;
		BeanMeta<?> bm = m.getMeta();

		List<BeanPropertyValue> lp = m.getValues(session.isTrimNulls());

		XmlBeanMeta xbm = bm.getExtendedMeta(XmlBeanMeta.class);

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
			if (attrs.contains(n) || n.equals(attrsProperty)) {
				BeanPropertyMeta pMeta = p.getMeta();
				ClassMeta<?> cMeta = p.getClassMeta();

				String key = p.getName();
				Object value = p.getValue();
				Throwable t = p.getThrown();
				if (t != null)
					session.addBeanGetterWarning(pMeta, t);

				if (session.canIgnoreValue(cMeta, key, value))
					continue;

				Namespace ns = (session.isEnableNamespaces() && pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace() != elementNs ? pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace() : null);

				if (pMeta.isUri()) {
					out.attrUri(ns, key, value);
				} else if (n.equals(attrsProperty)) {
					if (value instanceof BeanMap) {
						BeanMap<?> bm2 = (BeanMap)value;
						for (BeanPropertyValue p2 : bm2.getValues(true)) {
							String key2 = p2.getName();
							Object value2 = p2.getValue();
							Throwable t2 = p2.getThrown();
							if (t2 != null)
								session.addBeanGetterWarning(pMeta, t);
							out.attr(ns, key2, value2);
						}
					} else /* Map */ {
						Map m2 = (Map)value;
						for (Map.Entry e : (Set<Map.Entry>)(m2.entrySet())) {
							out.attr(ns, session.toString(e.getKey()), e.getValue());
						}
					}
				} else {
					out.attr(ns, key, value);
				}
			}
		}

		boolean hasContent = false, preserveWhitespace = false;

		for (BeanPropertyValue p : lp) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();

			String n = p.getName();
			if (n.equals(contentProperty)) {
				content = p.getValue();
				contentType = p.getClassMeta();
				hasContent = true;
				cf = xbm.getContentFormat();
				if (cf.isOneOf(MIXED,MIXED_PWS,TEXT,TEXT_PWS,XMLTEXT))
					isMixed = true;
				if (cf.isOneOf(MIXED_PWS, TEXT_PWS))
					preserveWhitespace = true;
				if (contentType.isCollection() && ((Collection)content).isEmpty())
					hasContent = false;
				else if (contentType.isArray() && Array.getLength(content) == 0)
					hasContent = false;
			} else if (elements.contains(n) || collapsedElements.contains(n)) {
				String key = p.getName();
				Object value = p.getValue();
				Throwable t = p.getThrown();
				if (t != null)
					session.addBeanGetterWarning(pMeta, t);

				if (session.canIgnoreValue(cMeta, key, value))
					continue;

				if (! hasChildren) {
					hasChildren = true;
					out.appendIf(! isCollapsed, '>').nlIf(! isMixed);
				}

				XmlBeanPropertyMeta xbpm = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class);
				serializeAnything(session, out, value, cMeta, key, xbpm.getNamespace(), false, xbpm.getXmlFormat(), isMixed, false, pMeta);
			}
		}
		if (! hasContent)
			return (hasChildren ? CR_ELEMENTS : CR_EMPTY);
		out.append('>').nlIf(! isMixed);

		// Serialize XML content.
		if (content != null) {
			if (contentType == null) {
			} else if (contentType.isCollection()) {
				Collection c = (Collection)content;
				for (Iterator i = c.iterator(); i.hasNext();) {
					Object value = i.next();
					serializeAnything(session, out, value, contentType.getElementType(), null, null, false, cf, isMixed, preserveWhitespace, null);
				}
			} else if (contentType.isArray()) {
				Collection c = toList(Object[].class, content);
				for (Iterator i = c.iterator(); i.hasNext();) {
					Object value = i.next();
					serializeAnything(session, out, value, contentType.getElementType(), null, null, false, cf, isMixed, preserveWhitespace, null);
				}
			} else {
				serializeAnything(session, out, content, contentType, null, null, false, cf, isMixed, preserveWhitespace, null);
			}
		} else {
			if (! session.isTrimNulls()) {
				if (! isMixed)
					out.i(session.indent);
				out.text(content);
				if (! isMixed)
					out.nl();
			}
		}
		return isMixed ? CR_MIXED : CR_ELEMENTS;
	}

	private XmlWriter serializeCollection(XmlSerializerSession session, XmlWriter out, Object in, ClassMeta<?> sType, ClassMeta<?> eType, BeanPropertyMeta ppMeta, boolean isMixed) throws Exception {

		ClassMeta<?> seType = sType.getElementType();
		if (seType == null)
			seType = session.object();
		ClassMeta<?> eeType = eType.getElementType();

		Collection c = (sType.isCollection() ? (Collection)in : toList(sType.getInnerClass(), in));

		c = session.sort(c);

		String type2 = null;
		if (sType != eType)
			type2 = sType.getDictionaryName();

		String eName = type2;
		Namespace eNs = null;

		if (ppMeta != null) {
			XmlBeanPropertyMeta xbpm = ppMeta.getExtendedMeta(XmlBeanPropertyMeta.class);
			eName = xbpm.getChildName();
			eNs = xbpm.getNamespace();
		}

		for (Iterator i = c.iterator(); i.hasNext();) {
			Object value = i.next();
			serializeAnything(session, out, value, eeType, eName, eNs, false, XmlFormat.DEFAULT, isMixed, false, null);
		}
		return out;
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 * @return The schema serializer.
	 */
	public XmlSerializer getSchemaSerializer() {
		XmlSchemaSerializer s = new XmlSchemaSerializer(getContextFactory());
		return s;
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
	static enum ContentResult {
		CR_EMPTY,    // No content...append "/>" to the start tag.
		CR_MIXED,    // Mixed content...don't add whitespace.
		CR_ELEMENTS  // Elements...use normal whitespace rules.
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		XmlSerializerSession s = (XmlSerializerSession)session;
		if (s.isEnableNamespaces() && s.isAutoDetectNamespaces())
			findNsfMappings(s, o);
		serializeAnything(s, s.getWriter(), o, null, null, null, s.isEnableNamespaces() && s.isAddNamespaceUrlsToRoot(), XmlFormat.DEFAULT, false, false, null);
	}

	@Override /* Serializer */
	public XmlSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new XmlSerializerSession(getContext(XmlSerializerContext.class), op, output, javaMethod, locale, timeZone, mediaType);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Enable support for XML namespaces.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.enableNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If not enabled, XML output will not contain any namespaces regardless of any other settings.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>XML_enableNamespaces</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see XmlSerializerContext#XML_enableNamespaces
	 */
	public XmlSerializer setEnableNamespaces(boolean value) throws LockedException {
		return setProperty(XML_enableNamespaces, value);
	}

	/**
	 * <b>Configuration property:</b>  Auto-detect namespace usage.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.autoDetectNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Detect namespace usage before serialization.
	 * <p>
	 * Used in conjunction with {@link XmlSerializerContext#XML_addNamespaceUrisToRoot} to reduce
	 * the list of namespace URLs appended to the root element to only those
	 * that will be used in the resulting document.
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for
	 * namespaces that will be encountered before the root element is
	 * serialized.
	 * <p>
	 * This setting is ignored if {@link XmlSerializerContext#XML_enableNamespaces} is not enabled.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Auto-detection of namespaces can be costly performance-wise.
	 * 		In high-performance environments, it's recommended that namespace detection be
	 * 		disabled, and that namespaces be manually defined through the {@link XmlSerializerContext#XML_namespaces} property.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>XML_autoDetectNamespaces</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see XmlSerializerContext#XML_autoDetectNamespaces
	 */
	public XmlSerializer setAutoDetectNamespaces(boolean value) throws LockedException {
		return setProperty(XML_autoDetectNamespaces, value);
	}

	/**
	 * <b>Configuration property:</b>  Add namespace URLs to the root element.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.addNamespaceUrisToRoot"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Use this setting to add {@code xmlns:x} attributes to the root
	 * element for the default and all mapped namespaces.
	 * <p>
	 * This setting is ignored if {@link XmlSerializerContext#XML_enableNamespaces} is not enabled.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>XML_addNamespaceUrisToRoot</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see XmlSerializerContext#XML_addNamespaceUrisToRoot
	 */
	public XmlSerializer setAddNamespaceUrisToRoot(boolean value) throws LockedException {
		return setProperty(XML_addNamespaceUrisToRoot, value);
	}

	/**
	 * <b>Configuration property:</b>  Default namespace.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.defaultNamespace"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"{juneau:'http://www.apache.org/2013/Juneau'}"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specifies the default namespace URI for this document.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>XML_defaultNamespace</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see XmlSerializerContext#XML_defaultNamespace
	 */
	public XmlSerializer setDefaultNamespace(String value) throws LockedException {
		return setProperty(XML_defaultNamespace, value);
	}

	/**
	 * <b>Configuration property:</b>  XMLSchema namespace.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.xsNamespace"</js>
	 * 	<li><b>Data type:</b> {@link Namespace}
	 * 	<li><b>Default:</b> <code>{name:<js>'xs'</js>,uri:<js>'http://www.w3.org/2001/XMLSchema'</js>}</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specifies the namespace for the <code>XMLSchema</code> namespace, used by the schema generated
	 * by the {@link XmlSchemaSerializer} class.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>XML_xsNamespace</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see XmlSerializerContext#XML_xsNamespace
	 */
	public XmlSerializer setXsNamespace(Namespace value) throws LockedException {
		return setProperty(XML_xsNamespace, value);
	}

	/**
	 * <b>Configuration property:</b>  Default namespaces.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.namespaces"</js>
	 * 	<li><b>Data type:</b> <code>Set&lt;{@link Namespace}&gt;</code>
	 * 	<li><b>Default:</b> empty set
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>XML_namespaces</jsf>, values)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see XmlSerializerContext#XML_namespaces
	 */
	public XmlSerializer setNamespaces(Namespace...values) throws LockedException {
		return setProperty(XML_namespaces, values);
	}

	@Override /* Serializer */
	public XmlSerializer setMaxDepth(int value) throws LockedException {
		super.setMaxDepth(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setInitialDepth(int value) throws LockedException {
		super.setInitialDepth(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setDetectRecursions(boolean value) throws LockedException {
		super.setDetectRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setIgnoreRecursions(boolean value) throws LockedException {
		super.setIgnoreRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setUseIndentation(boolean value) throws LockedException {
		super.setUseIndentation(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setAddBeanTypeProperties(boolean value) throws LockedException {
		super.setAddBeanTypeProperties(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setQuoteChar(char value) throws LockedException {
		super.setQuoteChar(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setTrimNullProperties(boolean value) throws LockedException {
		super.setTrimNullProperties(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setTrimEmptyCollections(boolean value) throws LockedException {
		super.setTrimEmptyCollections(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setTrimEmptyMaps(boolean value) throws LockedException {
		super.setTrimEmptyMaps(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setTrimStrings(boolean value) throws LockedException {
		super.setTrimStrings(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setRelativeUriBase(String value) throws LockedException {
		super.setRelativeUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setAbsolutePathUriBase(String value) throws LockedException {
		super.setAbsolutePathUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setSortCollections(boolean value) throws LockedException {
		super.setSortCollections(value);
		return this;
	}

	@Override /* Serializer */
	public XmlSerializer setSortMaps(boolean value) throws LockedException {
		super.setSortMaps(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public XmlSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public XmlSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public XmlSerializer clone() {
		try {
			XmlSerializer c = (XmlSerializer)super.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
