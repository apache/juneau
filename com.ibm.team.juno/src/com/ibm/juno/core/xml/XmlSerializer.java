/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static com.ibm.juno.core.xml.XmlSerializerProperties.*;
import static com.ibm.juno.core.xml.annotation.XmlFormat.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.xml.annotation.*;

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
 * 	<li>{@link XmlSerializerProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * 	<li>{@link XmlJson} - Default serializer with JSON attribute tags.
 * 	<li>{@link XmlJsonSq} - Default serializer with JSON attribute tags, single quotes.
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
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

	/** XML serializer properties currently set on this serializer. */
	protected transient XmlSerializerProperties xsp = new XmlSerializerProperties();


	/**
	 * Recursively searches for the XML namespaces on the specified POJO and adds them to the serializer context object.
	 *
	 * @param o The POJO to check.
	 * @param ctx The context that exists for the duration of a single serialization.
	 * @throws SerializeException
	 */
	protected void findNsfMappings(Object o, XmlSerializerContext ctx) throws SerializeException {
		BeanContext bc = ctx.getBeanContext();
		ClassMeta<?> aType = null;						// The actual type
		aType = ctx.push(null, o, null);

		if (aType != null) {
			Namespace ns = aType.getXmlMeta().getNamespace();
			if (ns != null) {
				if (ns.uri != null)
					ctx.addNamespace(ns);
				else
					ns = null;
			}
		}

		// Handle recursion
		if (aType != null && ! aType.isPrimitive()) {

			BeanMap bm = null;
			if (aType.isBeanMap()) {
				bm = (BeanMap)o;
			} else if (aType.isBean()) {
				bm = bc.forBean(o);
			} else if (aType.isDelegate()) {
				ClassMeta innerType = ((Delegate)o).getClassMeta();
				Namespace ns = innerType.getXmlMeta().getNamespace();
				if (ns != null) {
					if (ns.uri != null)
						ctx.addNamespace(ns);
					else
						ns = null;
				}

				if (innerType.isBean()) {
					for (BeanPropertyMeta bpm : (Collection<BeanPropertyMeta>)innerType.getBeanMeta().getPropertyMetas()) {
						ns = bpm.getXmlMeta().getNamespace();
						if (ns != null && ns.uri != null)
							ctx.addNamespace(ns);
					}

				} else if (innerType.isMap()) {
					for (Object o2 : ((Map)o).values())
						findNsfMappings(o2, ctx);
				} else if (innerType.isCollection()) {
					for (Object o2 : ((Collection)o))
						findNsfMappings(o2, ctx);
				}

			} else if (aType.isMap()) {
				for (Object o2 : ((Map)o).values())
					findNsfMappings(o2, ctx);
			} else if (aType.isCollection()) {
				for (Object o2 : ((Collection)o))
					findNsfMappings(o2, ctx);
			} else if (aType.isArray() && ! aType.getElementType().isPrimitive()) {
				for (Object o2 : ((Object[])o))
					findNsfMappings(o2, ctx);
			}
			if (bm != null) {
				for (BeanMapEntry p : (Set<BeanMapEntry>)bm.entrySet()) {

					Namespace ns = p.getMeta().getXmlMeta().getNamespace();
					if (ns != null && ns.uri != null)
						ctx.addNamespace(ns);

					try {
						findNsfMappings(p.getValue(), ctx);
					} catch (Throwable x) {
						// Ignore
					}
				}
			}
		}

		ctx.pop();
	}

	/**
	 * Workhorse method.
	 *
	 * @param out The writer to send the output to.
	 * @param o The object to serialize.
	 * @param eType The expected type if this is a bean property value being serialized.
	 * @param ctx The serializer context.
	 * @param elementName The root element name.
	 * @param elementNamespace The namespace of the element.
	 * @param addNamespaceUris Flag indicating that namespace URIs need to be added.
	 * @param format The format to serialize the output to.
	 * @param pMeta The bean property metadata if this is a bean property being serialized.
	 * @return The same writer passed in so that calls to the writer can be chained.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	protected XmlSerializerWriter serializeAnything(XmlSerializerWriter out, Object o, ClassMeta eType,
			XmlSerializerContext ctx, String elementName, Namespace elementNamespace, boolean addNamespaceUris,
			XmlFormat format, BeanPropertyMeta<?> pMeta) throws IOException, SerializeException {

		BeanContext bc = ctx.getBeanContext();
		String ts = null;              // The type string (e.g. <type> or <x x='type'>
		int indent = ctx.indent;       // Current indentation
		ClassMeta<?> aType = null;     // The actual type
		ClassMeta<?> wType = null;     // The wrapped type
		ClassMeta<?> gType = object(); // The generic type

		aType = ctx.push(elementName, o, eType);

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

			gType = aType.getFilteredClassMeta();

			// Filter if necessary
			PojoFilter filter = aType.getPojoFilter();
			if (filter != null) {
				o = filter.filter(o);

				// If the filter's getFilteredClass() method returns Object, we need to figure out
				// the actual type now.
				if (gType.isObject())
					gType = bc.getClassMetaForObject(o);
			}
		} else {
			gType = eType.getFilteredClassMeta();
		}

		String classAttr = null;
		if (ctx.isAddClassAttrs()) {
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
			isCollapsed = gType.getXmlMeta().getFormat() == XmlFormat.COLLAPSED;
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
			elementName = gType.getXmlMeta().getElementName();
		if (elementName == null)
			elementName = aType.getXmlMeta().getElementName();

		// If the value is null then it's either going to be <null/> or <XmlSerializer nil='true'/>
		// depending on whether the element has a name.
		boolean isNullTag = (elementName == null && o == null);

		if (isNullTag)
			ts = "null";

		if (ctx.isEnableNamespaces()) {
			if (elementNamespace == null)
				elementNamespace = gType.getXmlMeta().getNamespace();
			if (elementNamespace == null)
				elementNamespace = aType.getXmlMeta().getNamespace();
			if (elementNamespace != null && elementNamespace.uri == null)
				elementNamespace = null;
			if (elementNamespace == null)
				elementNamespace = ctx.getDefaultNamespace();
		} else {
			elementNamespace = null;
		}

		// Do we need a carriage return after the start tag?
		boolean cr = o != null && (gType.isMap() || gType.isCollection() || gType.isArray() || gType.isBean() || gType.hasToObjectMapMethod());

		String en = (elementName == null ? ts : elementName);
		boolean encodeEn = elementName != null;
		String ns = (elementNamespace == null ? null : elementNamespace.name);
		String xsi = null, dns = null, elementNs = null;
		if (ctx.isEnableNamespaces()) {
			xsi = ctx.getXsiNamespace().name;
			dns = elementName == null && ctx.getDefaultNamespace() != null ? ctx.getDefaultNamespace().name : null;
			elementNs = elementName == null ? dns : ns;
			if (elementName == null)
				elementNamespace = null;
		}

		// Render the start tag.
		if (! isCollapsed) {
			out.oTag(indent, elementNs, en, encodeEn);
			if (addNamespaceUris) {
				out.attr((String)null, "xmlns", ctx.getDefaultNamespace().getUri());

				for (Namespace n : ctx.getNamespaces())
					out.attr("xmlns", n.getName(), n.getUri());

				Namespace xsiNs = ctx.getXsiNamespace();
				if (xsiNs != null)
					out.attr("xmlns", xsiNs.name, xsiNs.uri);
			}
			if (elementName != null && ctx.isAddJsonTypeAttrs() && (ctx.isAddJsonStringTypeAttrs() || ! ts.equals("string")))
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
				out.encodeText(ctx.trim(o));
			else if (gType.isNumber() || gType.isBoolean())
				out.append(o);
			else if (gType.isMap() || (wType != null && wType.isMap())) {
				if (o instanceof BeanMap)
					hasChildren = serializeBeanMap(out, (BeanMap)o, elementNamespace, ctx, isCollapsed);
				else
					hasChildren = serializeMap(out, (Map)o, gType, ctx);
			}
			else if (gType.hasToObjectMapMethod())
				hasChildren = serializeMap(out, gType.toObjectMap(o), gType, ctx);
			else if (gType.isBean())
				hasChildren = serializeBeanMap(out, bc.forBean(o), elementNamespace, ctx, isCollapsed);
			else if (gType.isCollection() || (wType != null && wType.isCollection())) {
				if (isCollapsed)
					ctx.indent--;
				serializeCollection(out, (Collection)o, gType, ctx, pMeta);
				if (isCollapsed)
					ctx.indent++;
			}
			else if (gType.isArray()) {
				if (isCollapsed)
					ctx.indent--;
				serializeCollection(out, toList(gType.getInnerClass(), o), gType, ctx, pMeta);
				if (isCollapsed)
					ctx.indent++;
			}
			else
				out.encodeText(o);
		}

		ctx.pop();

		// Render the end tag.
		if (! isCollapsed) {
			if (o == null || ! hasChildren)
				out.append('/').append('>').nl();
			else
				out.i(cr ? indent : 0).eTag(elementNs, en, encodeEn).nl();
		}

		return out;
	}

	private boolean serializeMap(XmlSerializerWriter out, Map m, ClassMeta<?> type, XmlSerializerContext ctx) throws IOException, SerializeException {

		m = ctx.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		boolean hasChildren = false;
		for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry)i.next();

			Object k = e.getKey();
			if (k == null) {
				k = "\u0000";
			} else {
				k = ctx.generalize(k, keyType);
				if (ctx.isTrimStrings() && k instanceof String)
					k = k.toString().trim();
			}

			Object value = e.getValue();

			if (! hasChildren) {
				hasChildren = true;
				out.append('>').nl();
			}
			serializeAnything(out, value, valueType, ctx, k.toString(), null, false, NORMAL, null);
		}
		return hasChildren;
	}

	private boolean serializeBeanMap(XmlSerializerWriter out, BeanMap m, Namespace elementNs, XmlSerializerContext ctx, boolean isCollapsed) throws IOException, SerializeException {
		boolean hasChildren = false;
		BeanMeta bm = m.getMeta();

		Map<String,BeanPropertyMeta> xmlAttrs = bm.getXmlMeta().getXmlAttrProperties();
		Object content = null;
		for (BeanPropertyMeta p : xmlAttrs.values()) {

			String key = p.getName();
			Object value = null;
			try {
				value = p.get(m);
			} catch (StackOverflowError e) {
				throw e;
			} catch (Throwable x) {
				ctx.addBeanGetterWarning(p, x);
			}

			if (ctx.canIgnoreValue(p.getClassMeta(), key, value))
				continue;

			Namespace ns = (ctx.isEnableNamespaces() && p.getXmlMeta().getNamespace() != elementNs ? p.getXmlMeta().getNamespace() : null);

			if (p.isBeanUri() || p.isUri())
				out.attrUri(ns, key, value);
			else
				out.attr(ns, key, value);
		}

		boolean hasContent = false;

		for (BeanMapEntry p : (Set<BeanMapEntry>)m.entrySet()) {
			BeanPropertyMeta pMeta = p.getMeta();
			XmlFormat xf = pMeta.getXmlMeta().getXmlFormat();

			if (xf == CONTENT) {
				content = p.getValue();
				hasContent = true;
			} else if (xf == ATTR) {
				// Do nothing
			} else {
				String key = p.getKey();
				Object value = null;
				try {
					value = p.getValue();
				} catch (StackOverflowError e) {
					throw e;
				} catch (Throwable x) {
					ctx.addWarning("Could not call getValue() on property ''{0}'', {1}", key, x.getLocalizedMessage());
				}

				if (ctx.canIgnoreValue(pMeta.getClassMeta(), key, value))
					continue;

				if (! hasChildren) {
					hasChildren = true;
					out.appendIf(! isCollapsed, '>').nl();
				}
				serializeAnything(out, value, pMeta.getClassMeta(), ctx, key, pMeta.getXmlMeta().getNamespace(), false, pMeta.getXmlMeta().getXmlFormat(), pMeta);
			}
		}
		if ((! hasContent) || ctx.canIgnoreValue(string(), null, content))
			return hasChildren;
		out.append('>').cr(ctx.indent);

		// Serialize XML content.
		XmlContentHandler h = bm.getXmlMeta().getXmlContentHandler();
		if (h != null)
			try {
				h.serialize(out, m.getBean());
			} catch (Exception e) {
				throw new SerializeException(e);
			}
		else
			out.encodeText(content);
		out.nl();
		return true;
	}

	private XmlSerializerWriter serializeCollection(XmlSerializerWriter out, Collection c, ClassMeta<?> type, XmlSerializerContext ctx, BeanPropertyMeta<?> ppMeta) throws IOException, SerializeException {

		c = ctx.sort(c);

		ClassMeta<?> elementType = type.getElementType();

		String eName = null;
		Namespace eNs = null;

		if (ppMeta != null) {
			eName = ppMeta.getXmlMeta().getChildName();
			eNs = ppMeta.getXmlMeta().getNamespace();
		}

		if (eName == null) {
			eName = type.getXmlMeta().getChildName();
			eNs = type.getXmlMeta().getNamespace();
		}

		if (eName == null && ! elementType.isObject()) {
			eName = elementType.getXmlMeta().getElementName();
			eNs = elementType.getXmlMeta().getNamespace();
		}

		for (Iterator i = c.iterator(); i.hasNext();) {
			Object value = i.next();
			serializeAnything(out, value, elementType, ctx, eName, eNs, false, NORMAL, null);
		}
		return out;
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 * @return The schema serializer.
	 */
	public XmlSerializer getSchemaSerializer() {
		XmlSchemaSerializer s = new XmlSchemaSerializer();
		s.beanContextFactory = this.beanContextFactory;
		s.sp = this.sp;
		s.xsp = this.xsp;
		return s;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		XmlSerializerContext xctx = (XmlSerializerContext)ctx;
		if (xctx.isEnableNamespaces() && xctx.isAutoDetectNamespaces())
			findNsfMappings(o, xctx);
		serializeAnything(xctx.getWriter(out), o, null, xctx, null, null, xctx.isEnableNamespaces() && xctx.isAddNamespaceUrlsToRoot(), NORMAL, null);
	}

	@Override /* Serializer */
	public XmlSerializerContext createContext(ObjectMap properties, Method javaMethod) {
		return new XmlSerializerContext(getBeanContext(), sp, xsp, properties, javaMethod);
	}

	@Override /* CoreApi */
	public XmlSerializer setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! xsp.setProperty(property, value))
			super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public XmlSerializer addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
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
			c.xsp = xsp.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
