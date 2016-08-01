/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import static com.ibm.juno.core.jena.Constants.*;
import static com.ibm.juno.core.jena.RdfProperties.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.hp.hpl.jena.rdf.model.*;
import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.*;

/**
 * Serializes POJOs to RDF.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	Refer to <a class='doclink' href='package-summary.html#SerializerConfigurableProperties'>Configurable Properties</a>
 * 		for the entire list of configurable properties.
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for language-specific serializers:
 * <ul>
 * 	<li>{@link RdfSerializer.Xml} - RDF/XML.
 * 	<li>{@link RdfSerializer.XmlAbbrev} - RDF/XML-ABBREV.
 * 	<li>{@link RdfSerializer.NTriple} - N-TRIPLE.
 * 	<li>{@link RdfSerializer.Turtle} - TURTLE.
 * 	<li>{@link RdfSerializer.N3} - N3.
 * </ul>
 *
 *
 * <h6 class='topic'>Additional Information</h6>
 * <p>
 * 	See <a class='doclink' href='package-summary.html#TOC'>RDF Overview</a> for an overview of RDF support in Juno.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
public class RdfSerializer extends WriterSerializer {

	/** Default RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XML = new RdfSerializer.Xml().lock();

	/** Default Abbreviated RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XMLABBREV = new RdfSerializer.XmlAbbrev().lock();

	/** Default Turtle serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_TURTLE = new RdfSerializer.Turtle().lock();

	/** Default N-Triple serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_NTRIPLE = new RdfSerializer.NTriple().lock();

	/** Default N3 serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_N3 = new RdfSerializer.N3().lock();


	/** Produces RDF/XML output */
	@Produces("text/xml+rdf")
	public static class Xml extends RdfSerializer {
		/** Constructor */
		public Xml() {
			setProperty(RDF_language, LANG_RDF_XML);
		}
	}

	/** Produces Abbreviated RDF/XML output */
	@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
	public static class XmlAbbrev extends RdfSerializer {
		/** Constructor */
		public XmlAbbrev() {
			setProperty(RDF_language, LANG_RDF_XML_ABBREV);
		}
	}

	/** Produces N-Triple output */
	@Produces("text/n-triple")
	public static class NTriple extends RdfSerializer {
		/** Constructor */
		public NTriple() {
			setProperty(RDF_language, LANG_NTRIPLE);
		}
	}

	/** Produces Turtle output */
	@Produces("text/turtle")
	public static class Turtle extends RdfSerializer {
		/** Constructor */
		public Turtle() {
			setProperty(RDF_language, LANG_TURTLE);
		}
	}

	/** Produces N3 output */
	@Produces("text/n3")
	public static class N3 extends RdfSerializer {
		/** Constructor */
		public N3() {
			setProperty(RDF_language, LANG_N3);
		}
	}


	/** Jena serializer properties currently set on this serializer. */
	protected transient RdfSerializerProperties rsp = new RdfSerializerProperties();

	/** Xml serializer properties currently set on this serializer. */
	protected transient XmlSerializerProperties xsp = new XmlSerializerProperties();


	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {

		RdfSerializerContext rctx = (RdfSerializerContext)ctx;

		Model model = rctx.getModel();
		Resource r = null;

		ClassMeta<?> cm = rctx.getBeanContext().getClassMetaForObject(o);
		if (rctx.isLooseCollection() && cm != null && (cm.isCollection() || cm.isArray())) {
			Collection c = ctx.sort(cm.isCollection() ? (Collection)o : toList(cm.getInnerClass(), o));
			for (Object o2 : c)
				serializeAnything(o2, false, object(), rctx, "root", null, null);
		} else {
			RDFNode n = serializeAnything(o, false, object(), rctx, "root", null, null);
			if (n.isLiteral()) {
				r = model.createResource();
				r.addProperty(rctx.getValueProperty(), n);
			} else {
				r = n.asResource();
			}

			if (rctx.isAddRootProperty())
				r.addProperty(rctx.getRootProperty(), "true");
		}

		rctx.getWriter().write(model, out, "http://unknown/");
	}

	private RDFNode serializeAnything(Object o, boolean isURI, ClassMeta<?> eType, RdfSerializerContext ctx, String attrName, BeanPropertyMeta bpm, Resource parentResource) throws SerializeException {
		Model m = ctx.getModel();
		BeanContext bc = ctx.getBeanContext();

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;       // The wrapped type
		ClassMeta<?> gType = object();   // The generic type

		aType = ctx.push(attrName, o, eType);

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

		RDFNode n = null;

		if (o == null || gType.isChar() && ((Character)o).charValue() == 0) {
			if (bpm != null) {
				if (! ctx.isTrimNulls()) {
					n = m.createResource(RDF_NIL);
				}
			} else {
				n = m.createResource(RDF_NIL);
			}

		} else if (gType.isUri() || isURI) {
			n = m.createResource(getUri(o, null, ctx));

		} else if (gType.isCharSequence() || gType.isChar()) {
			n = m.createLiteral(ctx.encodeTextInvalidChars(o));

		} else if (gType.isNumber() || gType.isBoolean()) {
			if (! ctx.isAddLiteralTypes())
				n = m.createLiteral(o.toString());
			else
				n = m.createTypedLiteral(o);

		} else if (gType.isMap() || (wType != null && wType.isMap())) {
			if (o instanceof BeanMap) {
				BeanMap bm = (BeanMap)o;
				String uri = getUri(bm.getBeanUri(), null, ctx);
				n = m.createResource(uri);
				serializeBeanMap(bm, (Resource)n, ctx);
			} else {
				Map m2 = (Map)o;
				n = m.createResource();
				serializeMap(m2, (Resource)n, gType, ctx);
			}

		} else if (gType.hasToObjectMapMethod()) {
			Map m2 = gType.toObjectMap(o);
			n = m.createResource();
			serializeMap(m2, (Resource)n, gType, ctx);

		} else if (gType.isBean()) {
			BeanMap bm = bc.forBean(o);
			String uri = getUri(bm.getBeanUri(), null, ctx);
			n = m.createResource(uri);
			serializeBeanMap(bm, (Resource)n, ctx);

		} else if (gType.isCollection() || gType.isArray() || (wType != null && wType.isCollection())) {
			Collection c = ctx.sort(gType.isCollection() ? (Collection)o : toList(gType.getInnerClass(), o));
			RdfCollectionFormat f = ctx.getCollectionFormat();
			if (gType.getRdfMeta().getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = gType.getRdfMeta().getCollectionFormat();
			if (bpm != null && bpm.getRdfMeta().getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = bpm.getRdfMeta().getCollectionFormat();
			switch (f) {
				case BAG: n = serializeToContainer(c, gType, m.createBag(), ctx); break;
				case LIST: n = serializeToList(c, gType, ctx); break;
				case MULTI_VALUED: serializeToMultiProperties(c, gType, bpm, ctx, attrName, parentResource); break;
				default: n = serializeToContainer(c, gType, m.createSeq(), ctx);
			}
		} else {
			n = m.createLiteral(ctx.encodeTextInvalidChars(o));
		}

		if (ctx.isAddClassAttrs() && n != null && n.isResource()) {
			if (o != null && ! eType.equals(aType))
				n.asResource().addProperty(ctx.getClassProperty(), aType.toString());
			else if (o == null)
				n.asResource().addProperty(ctx.getClassProperty(), eType.toString());
		}

		ctx.pop();

		return n;
	}

	private String getUri(Object uri, Object uri2, RdfSerializerContext ctx) {
		String s = null;
		if (uri != null)
			s = uri.toString();
		if ((s == null || s.isEmpty()) && uri2 != null)
			s = uri2.toString();
		if (s == null)
			return null;
		if (s.indexOf("://") == -1) {
			String aUri = ctx.getAbsolutePathUriBase();
			String rUri = ctx.getRelativeUriBase();
			if (StringUtils.startsWith(s, '/')) {
				if (aUri != null)
					return aUri + s;
			} else {
				if (rUri != null) {
					if (rUri.equals("/"))
						return '/' + s;
					return rUri + '/' + s;
				}
			}
		}
		return s;
	}

	private void serializeMap(Map m, Resource r, ClassMeta<?> type, RdfSerializerContext ctx) throws SerializeException {

		m = ctx.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		ArrayList<Map.Entry<Object,Object>> l = new ArrayList<Map.Entry<Object,Object>>(m.entrySet());
		Collections.reverse(l);
		for (Map.Entry<Object,Object> me : l) {
			Object value = me.getValue();

			Object key = ctx.generalize(me.getKey(), keyType);

			Namespace ns = ctx.getJunoBpNs();
			Model model = ctx.getModel();
			Property p = model.createProperty(ns.getUri(), ctx.encodeElementName(key));
			RDFNode n = serializeAnything(value, false, valueType, ctx, key == null ? null : key.toString(), null, r);
			if (n != null)
				r.addProperty(p, n);
		}
	}

	private void serializeBeanMap(BeanMap m, Resource r, RdfSerializerContext ctx) throws SerializeException {
		ArrayList<BeanMapEntry> l = new ArrayList<BeanMapEntry>(m.entrySet());
		Collections.reverse(l);
		for (BeanMapEntry bme : l) {
			BeanPropertyMeta pMeta = bme.getMeta();
			ClassMeta<?> cm = pMeta.getClassMeta();

			if (pMeta.isBeanUri())
				continue;

			String key = bme.getKey();
			Object value = null;
			try {
				value = bme.getValue();
			} catch (StackOverflowError e) {
				throw e;
			} catch (Throwable t) {
				ctx.addBeanGetterWarning(pMeta, t);
			}

			if (ctx.canIgnoreValue(cm, key, value))
				continue;

			BeanPropertyMeta bpm = bme.getMeta();
			Namespace ns = bpm.getRdfMeta().getNamespace();
			if (ns == null && ctx.isUseXmlNamespaces())
				ns = bpm.getXmlMeta().getNamespace();
			if (ns == null)
				ns = ctx.getJunoBpNs();
			else if (ctx.isAutoDetectNamespaces())
				ctx.addModelPrefix(ns);

			Property p = ctx.getModel().createProperty(ns.getUri(), ctx.encodeElementName(key));
			RDFNode n = serializeAnything(value, pMeta.isUri(), cm, ctx, key, pMeta, r);
			if (n != null)
				r.addProperty(p, n);
		}
	}


	private Container serializeToContainer(Collection c, ClassMeta<?> type, Container list, RdfSerializerContext ctx) throws SerializeException {

		ClassMeta<?> elementType = type.getElementType();
		for (Object e : c) {
			RDFNode n = serializeAnything(e, false, elementType, ctx, null, null, null);
			list = list.add(n);
		}
		return list;
	}

	private RDFList serializeToList(Collection c, ClassMeta<?> type, RdfSerializerContext ctx) throws SerializeException {
		ClassMeta<?> elementType = type.getElementType();
		List<RDFNode> l = new ArrayList<RDFNode>(c.size());
		for (Object e : c) {
			l.add(serializeAnything(e, false, elementType, ctx, null, null, null));
		}
		return ctx.getModel().createList(l.iterator());
	}

	private void serializeToMultiProperties(Collection c, ClassMeta<?> gType, BeanPropertyMeta bpm, RdfSerializerContext ctx, String attrName, Resource parentResource) throws SerializeException {
		ClassMeta<?> elementType = gType.getElementType();
		for (Object e : c) {
			Namespace ns = null;
			if (bpm != null) {
				ns = bpm.getRdfMeta().getNamespace();
				if (ns == null && ctx.isUseXmlNamespaces())
					ns = bpm.getXmlMeta().getNamespace();
			}
			if (ns == null)
				ns = ctx.getJunoBpNs();
			else if (ctx.isAutoDetectNamespaces())
				ctx.addModelPrefix(ns);
			RDFNode n2 = serializeAnything(e, false, elementType, ctx, null, null, null);
			Property p = ctx.getModel().createProperty(ns.getUri(), ctx.encodeElementName(attrName));
			parentResource.addProperty(p, n2);
		}

	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public RdfSerializerContext createContext(ObjectMap properties, Method javaMethod) {
		return new RdfSerializerContext(getBeanContext(), sp, xsp, rsp, properties, javaMethod);
	}

	@Override /* CoreApi */
	public RdfSerializer setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! rsp.setProperty(property, value))
			if (! xsp.setProperty(property, value))
				super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> RdfSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public RdfSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public RdfSerializer clone() {
		try {
			RdfSerializer c = (RdfSerializer)super.clone();
			c.rsp = rsp.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
