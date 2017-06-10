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

import static org.apache.juneau.serializer.SerializerContext.*;
import static org.apache.juneau.xml.XmlSerializer.ContentResult.*;
import static org.apache.juneau.xml.XmlSerializer.JsonType.*;
import static org.apache.juneau.xml.XmlSerializerContext.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Serializes POJO models to XML.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Accept</code> types: <code>text/xml</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/xml</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * See the {@link JsonSerializer} class for details on how Java models map to JSON.
 * <p>
 * For example, the following JSON...
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
 * An additional "add-json-properties" mode is also provided to prevent loss of JSON data types...
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
 * This serializer provides several serialization options.  Typically, one of the predefined <jsf>DEFAULT</jsf> serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 * <p>
 * If an attribute name contains any non-valid XML element characters, they will be escaped using standard {@code _x####_} notation.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link XmlSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Produces("text/xml")
public class XmlSerializer extends WriterSerializer {

	/** Default serializer without namespaces. */
	public static final XmlSerializer DEFAULT = new XmlSerializer(PropertyStore.create());

	/** Default serializer without namespaces, with single quotes. */
	public static final XmlSerializer DEFAULT_SQ = new Sq(PropertyStore.create());

	/** Default serializer without namespaces, with single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_SQ_READABLE = new SqReadable(PropertyStore.create());

	/** Default serializer, all default settings. */
	public static final XmlSerializer DEFAULT_NS = new Ns(PropertyStore.create());

	/** Default serializer, single quotes. */
	public static final XmlSerializer DEFAULT_NS_SQ = new NsSq(PropertyStore.create());

	/** Default serializer, single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_NS_SQ_READABLE = new NsSqReadable(PropertyStore.create());


	/** Default serializer, single quotes. */
	public static class Sq extends XmlSerializer {

		/**
		 * Constructor.
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
	public static class SqReadable extends XmlSerializer {

		/**
		 * Constructor.
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

	/** Default serializer without namespaces. */
	@Produces(value="text/xml+simple",contentType="text/xml")
	public static class Ns extends XmlSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Ns(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(XML_enableNamespaces, true);
		}
	}

	/** Default serializer without namespaces, single quotes. */
	public static class NsSq extends XmlSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public NsSq(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(XML_enableNamespaces, true).append(SERIALIZER_quoteChar, '\'');
		}
	}

	/** Default serializer without namespaces, single quotes, with whitespace. */
	public static class NsSqReadable extends XmlSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public NsSqReadable(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(XML_enableNamespaces, true).append(SERIALIZER_quoteChar, '\'').append(SERIALIZER_useWhitespace, true);
		}
	}


	private final XmlSerializerContext ctx;
	private volatile XmlSchemaSerializer schemaSerializer;

	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public XmlSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(XmlSerializerContext.class);
	}

	@Override /* CoreObject */
	public XmlSerializerBuilder builder() {
		return new XmlSerializerBuilder(propertyStore);
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
					out.attr(dns, session.getBeanTypePropertyName(eType), resolvedDictionaryName);
				else if (type != null && type != STRING)
					out.attr(dns, session.getBeanTypePropertyName(eType), type);
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
				out.textUri(o);
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
			if (rc == CR_EMPTY) {
				if (session.isHtmlMode())
					out.append('>').eTag(elementNs, en, encodeEn);
				else
					out.append('/').append('>');
			} else if (rc == CR_VOID || o == null) {
				out.append('/').append('>');
			}
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
			if (attrs.contains(n) || attrs.contains("*") || n.equals(attrsProperty)) {
				BeanPropertyMeta pMeta = p.getMeta();
				ClassMeta<?> cMeta = p.getClassMeta();

				String key = p.getName();
				Object value = p.getValue();
				Throwable t = p.getThrown();
				if (t != null)
					session.onBeanGetterException(pMeta, t);

				if (session.canIgnoreValue(cMeta, key, value))
					continue;

				Namespace ns = (session.isEnableNamespaces() && pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace() != elementNs ? pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace() : null);

				if (pMeta.isUri()  ) {
					out.attrUri(ns, key, value);
				} else if (n.equals(attrsProperty)) {
					if (value instanceof BeanMap) {
						BeanMap<?> bm2 = (BeanMap)value;
						for (BeanPropertyValue p2 : bm2.getValues(true)) {
							String key2 = p2.getName();
							Object value2 = p2.getValue();
							Throwable t2 = p2.getThrown();
							if (t2 != null)
								session.onBeanGetterException(pMeta, t);
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

		boolean
			hasContent = false,
			preserveWhitespace = false,
			isVoidElement = xbm.getContentFormat() == VOID;

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
			} else if (elements.contains(n) || collapsedElements.contains(n) || elements.contains("*") || collapsedElements.contains("*") ) {
				String key = p.getName();
				Object value = p.getValue();
				Throwable t = p.getThrown();
				if (t != null)
					session.onBeanGetterException(pMeta, t);

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
			return (hasChildren ? CR_ELEMENTS : isVoidElement ? CR_VOID : CR_EMPTY);
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
		if (schemaSerializer == null)
			schemaSerializer = new XmlSchemaSerializer(propertyStore, getOverrideProperties());
		return schemaSerializer;
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
		CR_VOID,      // No content...append "/>" to the start tag.
		CR_EMPTY,     // No content...append "/>" to the start tag if XML, "/></end>" if HTML.
		CR_MIXED,     // Mixed content...don't add whitespace.
		CR_ELEMENTS   // Elements...use normal whitespace rules.
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		XmlSerializerSession s = (XmlSerializerSession)session;
		if (s.isEnableNamespaces() && s.isAutoDetectNamespaces())
			findNsfMappings(s, o);
		serializeAnything(s, s.getWriter(), o, s.getExpectedRootType(o), null, null, s.isEnableNamespaces() && s.isAddNamespaceUrlsToRoot(), XmlFormat.DEFAULT, false, false, null);
	}

	@Override /* Serializer */
	public XmlSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		return new XmlSerializerSession(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
	}
}
