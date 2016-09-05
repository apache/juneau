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
import static org.apache.juneau.xml.XmlSerializerContext.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

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
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/xml</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/xml</code>
 *
 *
 * <h6 class='topic'>Description</h6>
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
 * 	...maps to the following XML...
 * <p class='bcode'>
 * 	<xt>&lt;object&gt;</xt>
 * 		<xt>&lt;name</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>John Smith<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;address</xt> <xa>type</xa>=<xs>'object'</xs><xt>&gt;</xt>
 * 			<xt>&lt;streetAddress</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>21 2nd Street<xt>&lt;/streetAddress&gt;</xt>
 * 			<xt>&lt;city</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>New York<xt>&lt;/city&gt;</xt>
 * 			<xt>&lt;state</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>NY<xt>&lt;/state&gt;</xt>
 * 			<xt>&lt;postalCode</xt> <xa>type</xa>=<xs>'number'</xs><xt>&gt;</xt>10021<xt>&lt;/postalCode&gt;</xt>
 * 		<xt>&lt;/address&gt;</xt>
 * 		<xt>&lt;phoneNumbers</xt> <xa>type</xa>=<xs>'array'</xs><xt>&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-1111<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-2222<xt>&lt;/string&gt;</xt>
 * 		<xt>&lt;/phoneNumbers&gt;</xt>
 * 		<xt>&lt;additionalInfo</xt> <xa>type</xa>=<xs>'null'</xs><xt>&gt;&lt;/additionalInfo&gt;</xt>
 * 		<xt>&lt;remote</xt> <xa>type</xa>=<xs>'boolean'</xs><xt>&gt;</xt>false<xt>&lt;/remote&gt;</xt>
 * 		<xt>&lt;height</xt> <xa>type</xa>=<xs>'number'</xs><xt>&gt;</xt>62.4<xt>&lt;/height&gt;</xt>
 * 		<xt>&lt;fico_x0020_score</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt> &amp;gt; 640<xt>&lt;/fico_x0020_score&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined <jsf>DEFAULT</jsf> serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 * <p>
 * 	If an attribute name contains any non-valid XML element characters, they will be escaped using standard {@code _x####_} notation.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link XmlSerializerContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * 	<li>{@link XmlJson} - Default serializer with JSON attribute tags.
 * 	<li>{@link XmlJsonSq} - Default serializer with JSON attribute tags, single quotes.
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Produces("text/xml")
public class XmlSerializer extends WriterSerializer {

	/** Default serializer, all default settings. */
	public static final XmlSerializer DEFAULT = new XmlSerializer().lock();

	/** Default serializer, single quotes. */
	public static final XmlSerializer DEFAULT_SQ = new XmlSerializer.Sq().lock();

	/** Default serializer, single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_SQ_READABLE = new XmlSerializer.SqReadable().lock();

	/** Default serializer with JSON attribute tags. */
	public static final XmlSerializer DEFAULT_XMLJSON = new XmlSerializer.XmlJson().lock();

	/** Default serializer with JSON attribute tags, single quotes. */
	public static final XmlSerializer DEFAULT_XMLJSON_SQ = new XmlSerializer.XmlJsonSq().lock();

	/** Default serializer without namespaces. */
	public static final XmlSerializer DEFAULT_SIMPLE = new XmlSerializer.Simple().lock();

	/** Default serializer without namespaces, with single quotes. */
	public static final XmlSerializer DEFAULT_SIMPLE_SQ = new XmlSerializer.SimpleSq().lock();

	/** Default serializer without namespaces, with JSON attribute tags and single quotes. */
	public static final XmlSerializer DEFAULT_SIMPLE_XMLJSON_SQ = new XmlSerializer.SimpleXmlJsonSq().lock();


	/** Default serializer, single quotes. */
	public static class Sq extends XmlSerializer {
		/** Constructor */
		public Sq() {
			setProperty(SERIALIZER_quoteChar, '\'');
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends Sq {
		/** Constructor */
		public SqReadable() {
			setProperty(SERIALIZER_useIndentation, true);
		}
	}

	/** Default serializer with JSON attribute tags. */
	@Produces(value="text/xml+json",contentType="text/xml")
	public static class XmlJson extends XmlSerializer {
		/** Constructor */
		public XmlJson() {
			setProperty(XML_addJsonTypeAttrs, true);
		}
	}

	/** Default serializer with JSON attribute tags, single quotes. */
	public static class XmlJsonSq extends XmlJson {
		/** Constructor */
		public XmlJsonSq() {
			setProperty(SERIALIZER_quoteChar, '\'');
		}
	}

	/** Default serializer without namespaces. */
	@Produces(value="text/xml+simple",contentType="text/xml")
	public static class Simple extends XmlSerializer {
		/** Constructor */
		public Simple() {
			setProperty(XML_enableNamespaces, false);
		}
	}

	/** Default serializer without namespaces, single quotes. */
	public static class SimpleSq extends Simple {
		/** Constructor */
		public SimpleSq() {
			setProperty(SERIALIZER_quoteChar, '\'');
		}
	}

	/** Default serializer with JSON attribute tags, single quotes. */
	public static class SimpleXmlJsonSq extends SimpleSq {
		/** Constructor */
		public SimpleXmlJsonSq() {
			setProperty(XML_addJsonTypeAttrs, true);
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
		BeanContext bc = session.getBeanContext();
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
				bm = bc.forBean(o);
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
	 * @param pMeta The bean property metadata if this is a bean property being serialized.
	 * @return The same writer passed in so that calls to the writer can be chained.
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	protected XmlWriter serializeAnything(XmlSerializerSession session, XmlWriter out, Object o,
			ClassMeta eType, String elementName, Namespace elementNamespace, boolean addNamespaceUris,
			XmlFormat format, BeanPropertyMeta pMeta) throws Exception {

		BeanContext bc = session.getBeanContext();
		String ts = null;              // The type string (e.g. <type> or <x x='type'>
		int indent = session.indent;       // Current indentation
		ClassMeta<?> aType = null;     // The actual type
		ClassMeta<?> wType = null;     // The wrapped type
		ClassMeta<?> gType = object(); // The generic type

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
				aType = ((Delegate)o).getClassMeta();
			}

			gType = aType.getSerializedClassMeta();

			// Swap if necessary
			PojoSwap swap = aType.getPojoSwap();
			if (swap != null) {
				o = swap.swap(o, bc);

				// If the getSwapClass() method returns Object, we need to figure out
				// the actual type now.
				if (gType.isObject())
					gType = bc.getClassMetaForObject(o);
			}
		} else {
			gType = eType.getSerializedClassMeta();
		}

		String classAttr = null;
		if (session.isAddClassAttrs()) {
			if (o != null && ! eType.equals(aType))
				classAttr = aType.toString();
			else if (o == null)
				classAttr = eType.toString();
		}

		// char '\0' is interpreted as null.
		if (o != null && gType.isChar() && ((Character)o).charValue() == 0)
			o = null;

		boolean isCollapsed = false;		// If 'true', this is a collection and we're not rendering the outer element.

		// Get the JSON type string.
		if (gType.isCharSequence() || gType.isChar())
			ts = "string";
		else if (gType.isNumber())
			ts = "number";
		else if (gType.isBoolean())
			ts = "boolean";
		else if (gType.isMap() || gType.isBean() || gType.hasToObjectMapMethod()) {
			isCollapsed = gType.getExtendedMeta(XmlClassMeta.class).getFormat() == XmlFormat.COLLAPSED;
			ts = "object";
		}
		else if (gType.isCollection() || gType.isArray()) {
			isCollapsed = (format == COLLAPSED && ! addNamespaceUris);
			ts = "array";
		}
		else
			ts = "string";


		// Is there a name associated with this bean?
		if (elementName == null)
			elementName = gType.getLexiconName();
		if (elementName == null)
			elementName = aType.getLexiconName();

		// If the value is null then it's either going to be <null/> or <XmlSerializer nil='true'/>
		// depending on whether the element has a name.
		boolean isNullTag = (elementName == null && o == null);

		if (isNullTag)
			ts = "null";

		if (session.isEnableNamespaces()) {
			if (elementNamespace == null)
				elementNamespace = gType.getExtendedMeta(XmlClassMeta.class).getNamespace();
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
		boolean cr = o != null && (gType.isMap() || gType.isCollection() || gType.isArray() || gType.isBean() || gType.hasToObjectMapMethod());

		String en = (elementName == null ? ts : elementName);
		boolean encodeEn = elementName != null;
		String ns = (elementNamespace == null ? null : elementNamespace.name);
		String xsi = null, dns = null, elementNs = null;
		if (session.isEnableNamespaces()) {
			xsi = session.getXsiNamespace().name;
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

				Namespace xsiNs = session.getXsiNamespace();
				if (xsiNs != null)
					out.attr("xmlns", xsiNs.name, xsiNs.uri);
			}
			if (elementName != null && session.isAddJsonTypeAttrs() && (session.isAddJsonStringTypeAttrs() || ! ts.equals("string")))
				out.attr(dns, "type", ts);
			if (classAttr != null)
				out.attr(dns, "_class", classAttr);
			if (o == null) {
				if (! isNullTag)
					out.attr(xsi, "nil", "true");
				if ((gType.isBoolean() || gType.isNumber()) && ! gType.isNullable())
					o = gType.getPrimitiveDefault();
			}

			if (o != null && !(gType.isMap() || gType.isBean() || gType.hasToObjectMapMethod()))
				out.append('>');

			if (cr && !(gType.isMap() || gType.isBean() || gType.hasToObjectMapMethod()))
				out.nl();
		}

		boolean hasChildren = true;

		// Render the tag contents.
		if (o != null) {
			if (gType.isUri() || (pMeta != null && pMeta.isUri()))
				out.appendUri(o);
			else if (gType.isCharSequence() || gType.isChar())
				out.encodeText(session.trim(o));
			else if (gType.isNumber() || gType.isBoolean())
				out.append(o);
			else if (gType.isMap() || (wType != null && wType.isMap())) {
				if (o instanceof BeanMap)
					hasChildren = serializeBeanMap(session, out, (BeanMap)o, elementNamespace, isCollapsed);
				else
					hasChildren = serializeMap(session, out, (Map)o, gType);
			}
			else if (gType.hasToObjectMapMethod())
				hasChildren = serializeMap(session, out, gType.toObjectMap(o), gType);
			else if (gType.isBean())
				hasChildren = serializeBeanMap(session, out, bc.forBean(o), elementNamespace, isCollapsed);
			else if (gType.isCollection() || (wType != null && wType.isCollection())) {
				if (isCollapsed)
					session.indent--;
				serializeCollection(session, out, (Collection)o, gType, pMeta);
				if (isCollapsed)
					session.indent++;
			}
			else if (gType.isArray()) {
				if (isCollapsed)
					session.indent--;
				serializeCollection(session, out, toList(gType.getInnerClass(), o), gType, pMeta);
				if (isCollapsed)
					session.indent++;
			}
			else
				out.encodeText(session.toString(o));
		}

		session.pop();

		// Render the end tag.
		if (! isCollapsed) {
			if (o == null || ! hasChildren)
				out.append('/').append('>').nl();
			else
				out.i(cr ? indent : 0).eTag(elementNs, en, encodeEn).nl();
		}

		return out;
	}

	private boolean serializeMap(XmlSerializerSession session, XmlWriter out, Map m, ClassMeta<?> type) throws Exception {

		m = session.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

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
				out.append('>').nl();
			}
			serializeAnything(session, out, value, valueType, session.toString(k), null, false, NORMAL, null);
		}
		return hasChildren;
	}

	private boolean serializeBeanMap(XmlSerializerSession session, XmlWriter out, BeanMap<?> m, Namespace elementNs, boolean isCollapsed) throws Exception {
		boolean hasChildren = false;
		BeanMeta<?> bm = m.getMeta();

		List<BeanPropertyValue> lp = m.getValues(session.isTrimNulls());

		Map<String,BeanPropertyMeta> xmlAttrs = bm.getExtendedMeta(XmlBeanMeta.class).getXmlAttrProperties();
		Object content = null;
		for (BeanPropertyValue p : lp) {
			if (xmlAttrs.containsKey(p.getName())) {
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

				if (pMeta.isUri())
					out.attrUri(ns, key, value);
				else
					out.attr(ns, key, value);
			}
		}

		boolean hasContent = false;

		for (BeanPropertyValue p : lp) {
			BeanPropertyMeta pMeta = p.getMeta();
			ClassMeta<?> cMeta = p.getClassMeta();

			XmlFormat xf = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getXmlFormat();

			if (xf == CONTENT) {
				content = p.getValue();
				hasContent = true;
			} else if (xf == ATTR) {
				// Do nothing
			} else {
				String key = p.getName();
				Object value = p.getValue();
				Throwable t = p.getThrown();
				if (t != null)
					session.addBeanGetterWarning(pMeta, t);

				if (session.canIgnoreValue(cMeta, key, value))
					continue;

				if (! hasChildren) {
					hasChildren = true;
					out.appendIf(! isCollapsed, '>').nl();
				}
				serializeAnything(session, out, value, cMeta, key, pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace(), false, pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getXmlFormat(), pMeta);
			}
		}
		if ((! hasContent) || session.canIgnoreValue(string(), null, content))
			return hasChildren;
		out.append('>').cr(session.indent);

		// Serialize XML content.
		XmlContentHandler h = bm.getExtendedMeta(XmlBeanMeta.class).getXmlContentHandler();
		if (h != null)
			h.serialize(out, m.getBean());
		else
			out.encodeText(content);
		out.nl();
		return true;
	}

	private XmlWriter serializeCollection(XmlSerializerSession session, XmlWriter out, Collection c, ClassMeta<?> type, BeanPropertyMeta ppMeta) throws Exception {

		c = session.sort(c);

		ClassMeta<?> elementType = type.getElementType();

		String eName = null;
		Namespace eNs = null;

		if (ppMeta != null) {
			eName = ppMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getChildName();
			eNs = ppMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
		}

		if (eName == null) {
			eName = type.getExtendedMeta(XmlClassMeta.class).getChildName();
			eNs = type.getExtendedMeta(XmlClassMeta.class).getNamespace();
		}

		if (eName == null && ! elementType.isObject()) {
			eName = elementType.getLexiconName();
			eNs = elementType.getExtendedMeta(XmlClassMeta.class).getNamespace();
		}

		for (Iterator i = c.iterator(); i.hasNext();) {
			Object value = i.next();
			serializeAnything(session, out, value, elementType, eName, eNs, false, NORMAL, null);
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


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		XmlSerializerSession s = (XmlSerializerSession)session;
		if (s.isEnableNamespaces() && s.isAutoDetectNamespaces())
			findNsfMappings(s, o);
		serializeAnything(s, s.getWriter(), o, null, null, null, s.isEnableNamespaces() && s.isAddNamespaceUrlsToRoot(), NORMAL, null);
	}

	@Override /* Serializer */
	public XmlSerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new XmlSerializerSession(getContext(XmlSerializerContext.class), getBeanContext(), output, properties, javaMethod);
	}

	@Override /* CoreApi */
	public XmlSerializer setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> XmlSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

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
