/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import static com.ibm.juno.core.html.HtmlSerializerProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * Serializes POJO models to HTML.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/html</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	The conversion is as follows...
 * 	<ul>
 * 		<li>{@link Map Maps} (e.g. {@link HashMap}, {@link TreeMap}) and beans are converted to HTML tables with 'key' and 'value' columns.
 * 		<li>{@link Collection Collections} (e.g. {@link HashSet}, {@link LinkedList}) and Java arrays are converted to HTML ordered lists.
 * 		<li>{@code Collections} of {@code Maps} and beans are converted to HTML tables with keys as headers.
 * 		<li>Everything else is converted to text.
 * 	</ul>
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined <jsf>DEFAULT</jsf> serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 * <p>
 * 	The {@link HtmlLink} annotation can be used on beans to add hyperlinks to the output.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link HtmlSerializerProperties}
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
 * </ul>
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 		String html = HtmlSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 		<jc>// Create a custom serializer that doesn't use whitespace and newlines</jc>
 * 		HtmlSerializer serializer = <jk>new</jk> HtmlSerializer()
 * 			.setProperty(SerializerProperties.<jsf>SERIALIZER_useIndentation</jsf>, <jk>false</jk>);
 *
 * 		<jc>// Same as above, except uses cloning</jc>
 * 		HtmlSerializer serializer = HtmlSerializer.<jsf>DEFAULT</jsf>.clone()
 * 			.setProperty(SerializerProperties.<jsf>SERIALIZER_useIndentation</jsf>, <jk>false</jk>);
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
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces("text/html")
@SuppressWarnings("hiding")
public class HtmlSerializer extends XmlSerializer {

	/** Default serializer, all default settings. */
	public static final HtmlSerializer DEFAULT = new HtmlSerializer().lock();

	/** Default serializer, single quotes. */
	public static final HtmlSerializer DEFAULT_SQ = new HtmlSerializer.Sq().lock();

	/** Default serializer, single quotes, whitespace added. */
	public static final HtmlSerializer DEFAULT_SQ_READABLE = new HtmlSerializer.SqReadable().lock();

	/** Default serializer, single quotes. */
	public static class Sq extends HtmlSerializer {
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


	/** HTML serializer properties currently set on this serializer. */
	protected transient HtmlSerializerProperties hsp = new HtmlSerializerProperties();

	/**
	 * Main serialization routine.
	 *
	 * @param o The object being serialized.
	 * @param w The writer to serialize to.
	 * @param ctx The serialization context object.
	 *
	 * @return The same writer passed in.
	 * @throws IOException If a problem occurred trying to send output to the writer.
	 */
	private HtmlSerializerWriter doSerialize(Object o, HtmlSerializerWriter w, HtmlSerializerContext ctx) throws SerializeException, IOException {
		serializeAnything(w, o, null, ctx, null, ctx.getInitialDepth()-1, null);
		return w;
	}

	/**
	 * Serialize the specified object to the specified writer.
	 *
	 * @param out The writer.
	 * @param o The object to serialize.
	 * @param eType The expected type of the object if this is a bean property.
	 * @param ctx The context object that lives for the duration of this serialization.
	 * @param name The attribute name of this object if this object was a field in a JSON object (i.e. key of a {@link java.util.Map.Entry} or property name of a bean).
	 * @param indent The current indentation value.
	 * @param pMeta The bean property being serialized, or <jk>null</jk> if we're not serializing a bean property.
	 * @throws IOException
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void serializeAnything(HtmlSerializerWriter out, Object o, ClassMeta<?> eType, HtmlSerializerContext ctx, String name, int indent, BeanPropertyMeta pMeta) throws IOException, SerializeException {

		BeanContext bc = ctx.getBeanContext();
		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> gType = object();   // The generic type

		if (eType == null)
			eType = object();

		aType = ctx.push(name, o, eType);

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		ctx.indent += indent;
		int i = ctx.indent;

		// Determine the type.
		if (o == null || (aType.isChar() && ((Character)o).charValue() == 0))
			out.tag(i, "null").nl();
		else {

			gType = aType.getFilteredClassMeta();
			String classAttr = null;
			if (ctx.isAddClassAttrs() && ! eType.equals(aType))
				classAttr = aType.toString();

			// Filter if necessary
			PojoFilter filter = aType.getPojoFilter();
			if (filter != null) {
				o = filter.filter(o);

				// If the filter's getFilteredClass() method returns Object, we need to figure out
				// the actual type now.
				if (gType.isObject())
					gType = bc.getClassMetaForObject(o);
			}

			HtmlClassMeta html = gType.getHtmlMeta();

			if (html.isAsXml() || (pMeta != null && pMeta.getHtmlMeta().isAsXml()))
				super.serializeAnything(out, o, null, ctx, null, null, false, XmlFormat.NORMAL, null);
			else if (html.isAsPlainText() || (pMeta != null && pMeta.getHtmlMeta().isAsPlainText()))
				out.write(o == null ? "null" : o.toString());
			else if (o == null || (gType.isChar() && ((Character)o).charValue() == 0))
				out.tag(i, "null").nl();
			else if (gType.hasToObjectMapMethod())
				serializeMap(out, gType.toObjectMap(o), eType, ctx, classAttr, pMeta);
			else if (gType.isBean())
				serializeBeanMap(out, bc.forBean(o), ctx, classAttr, pMeta);
			else if (gType.isNumber())
				out.sTag(i, "number").append(o).eTag("number").nl();
			else if (gType.isBoolean())
				out.sTag(i, "boolean").append(o).eTag("boolean").nl();
			else if (gType.isMap()) {
				if (o instanceof BeanMap)
					serializeBeanMap(out, (BeanMap)o, ctx, classAttr, pMeta);
				else
					serializeMap(out, (Map)o, eType, ctx, classAttr, pMeta);
			}
			else if (gType.isCollection()) {
				if (classAttr != null)
					serializeCollection(out, (Collection)o, gType, ctx, name, classAttr, pMeta);
				else
					serializeCollection(out, (Collection)o, eType, ctx, name, null, pMeta);
			}
			else if (gType.isArray()) {
				if (classAttr != null)
					serializeCollection(out, toList(gType.getInnerClass(), o), gType, ctx, name, classAttr, pMeta);
				else
					serializeCollection(out, toList(gType.getInnerClass(), o), eType, ctx, name, null, pMeta);
			}
			else if (gType.isUri() || (pMeta != null && (pMeta.isUri() || pMeta.isBeanUri()))) {
				String label = null;
				String at = ctx.getUriAnchorText();
				if (at != null) {
					if (at.equals(LAST_TOKEN)) {
						label = o.toString();
						if (label.indexOf('/') != -1)
							label = label.substring(label.lastIndexOf('/')+1);
					} else if (at.equals(PROPERTY_NAME)) {
						label = (pMeta != null ? pMeta.getName() : null);
					} else {
						label = o.toString();
					}
				}
				if (label == null)
					label = o.toString();
				out.oTag(i, "a").attrUri("href", o).append('>');
				if (at != null && at.equals(URI))
					out.appendUri(label);
				else
					out.append(label);
				out.eTag("a").nl();
			}
			else
				out.sTag(i, "string").encodeText(o).eTag("string").nl();
		}
		ctx.pop();
		ctx.indent -= indent;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeMap(HtmlSerializerWriter out, Map m, ClassMeta<?> type, HtmlSerializerContext ctx, String classAttr, BeanPropertyMeta<?> ppMeta) throws IOException, SerializeException {
		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		int i = ctx.getIndent();
		out.oTag(i, "table").attr("type", "object");
		if (classAttr != null)
			out.attr("class", classAttr);
		out.appendln(">");
		if (! (ppMeta != null && ppMeta.getHtmlMeta().isNoTableHeaders())) {
		out.sTag(i+1, "tr").nl();
		out.sTag(i+2, "th").nl().appendln(i+3, "<string>key</string>").eTag(i+2, "th").nl();
		out.sTag(i+2, "th").nl().appendln(i+3, "<string>value</string>").eTag(i+2, "th").nl();
		out.eTag(i+1, "tr").nl();
		}
		for (Map.Entry e : (Set<Map.Entry>)m.entrySet()) {

			Object key = generalize(ctx, e.getKey(), keyType);
			Object value = null;
			try {
				value = e.getValue();
			} catch (StackOverflowError t) {
				throw t;
			} catch (Throwable t) {
				ctx.addWarning("Could not call getValue() on property ''{0}'', {1}", e.getKey(), t.getLocalizedMessage());
			}

			out.sTag(i+1, "tr").nl();
			out.sTag(i+2, "td").nl();
			serializeAnything(out, key, keyType, ctx, null, 2, null);
			out.eTag(i+2, "td").nl();
			out.sTag(i+2, "td").nl();
			serializeAnything(out, value, valueType, ctx, (key == null ? "_x0000_" : key.toString()), 2, null);
			out.eTag(i+2, "td").nl();
			out.eTag(i+1, "tr").nl();
		}
		out.eTag(i, "table").nl();
	}

	@SuppressWarnings({ "rawtypes" })
	private void serializeBeanMap(HtmlSerializerWriter out, BeanMap m, HtmlSerializerContext ctx, String classAttr, BeanPropertyMeta<?> ppMeta) throws IOException, SerializeException {
		int i = ctx.getIndent();

		Object o = m.getBean();

		Class<?> c = o.getClass();
		if (c.isAnnotationPresent(HtmlLink.class)) {
			HtmlLink h = o.getClass().getAnnotation(HtmlLink.class);
			Object urlProp = m.get(h.hrefProperty());
			Object nameProp = m.get(h.nameProperty());
			out.oTag(i, "a").attrUri("href", urlProp).append('>').encodeText(nameProp).eTag("a").nl();
			return;
		}

		out.oTag(i, "table").attr("type", "object");
		if (classAttr != null)
			out.attr("_class", classAttr);
		out.append('>').nl();
		if (! (m.getClassMeta().getHtmlMeta().isNoTableHeaders() || (ppMeta != null && ppMeta.getHtmlMeta().isNoTableHeaders()))) {
		out.sTag(i+1, "tr").nl();
		out.sTag(i+2, "th").nl().appendln(i+3, "<string>key</string>").eTag(i+2, "th").nl();
		out.sTag(i+2, "th").nl().appendln(i+3, "<string>value</string>").eTag(i+2, "th").nl();
		out.eTag(i+1, "tr").nl();
		}

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			BeanMapEntry p = (BeanMapEntry)mapEntries.next();
			BeanPropertyMeta pMeta = p.getMeta();

			String key = p.getKey();
			Object value = null;
			try {
				value = p.getValue();
			} catch (StackOverflowError e) {
				throw e;
			} catch (Throwable t) {
				ctx.addBeanGetterWarning(pMeta, t);
			}

			if (canIgnoreValue(ctx, pMeta.getClassMeta(), key, value))
				continue;

			out.sTag(i+1, "tr").nl();
			out.sTag(i+2, "td").nl();
			out.sTag(i+3, "string").encodeText(key).eTag("string").nl();
			out.eTag(i+2, "td").nl();
			out.sTag(i+2, "td").nl();
			try {
				serializeAnything(out, value, p.getMeta().getClassMeta(), ctx, key, 2, pMeta);
			} catch (SerializeException t) {
				throw t;
			} catch (StackOverflowError t) {
				throw t;
			} catch (Throwable t) {
				ctx.addBeanGetterWarning(pMeta, t);
			}
			out.eTag(i+2, "td").nl();
			out.eTag(i+1, "tr").nl();
		}
		out.eTag(i, "table").nl();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeCollection(HtmlSerializerWriter out, Collection c, ClassMeta<?> type, HtmlSerializerContext ctx, String name, String classAttr, BeanPropertyMeta<?> ppMeta) throws IOException, SerializeException {

		BeanContext bc = ctx.getBeanContext();
		ClassMeta<?> elementType = type.getElementType();

		int i = ctx.getIndent();
		if (c.isEmpty()) {
			out.appendln(i, "<ul></ul>");
			return;
		}

		c = sort(ctx, c);

		// Look at the objects to see how we're going to handle them.  Check the first object to see how we're going to handle this.
		// If it's a map or bean, then we'll create a table.
		// Otherwise, we'll create a list.
		String[] th = getTableHeaders(ctx, c, ppMeta);

		if (th != null) {

			out.oTag(i, "table").attr("type", "array");
			if (classAttr != null)
				out.attr("_class", classAttr);
			out.append('>').nl();
			out.sTag(i+1, "tr").nl();
			for (String key : th)
				out.sTag(i+2, "th").append(key).eTag("th").nl();
			out.eTag(i+1, "tr").nl();

			for (Object o : c) {
				ClassMeta<?> cm = bc.getClassMetaForObject(o);

				if (cm != null && cm.getPojoFilter() != null) {
					PojoFilter f = cm.getPojoFilter();
					o = f.filter(o);
					cm = cm.getFilteredClassMeta();
				}

				if (cm != null && ctx.isAddClassAttrs() && elementType.getInnerClass() != o.getClass())
					out.oTag(i+1, "tr").attr("_class", o.getClass().getName()).append('>').nl();
				else
					out.sTag(i+1, "tr").nl();

				if (cm == null) {
					serializeAnything(out, o, null, ctx, null, 1, null);

				} else if (cm.isMap() && ! (cm.isBeanMap())) {
					Map m2 = sort(ctx, (Map)o);

					Iterator mapEntries = m2.entrySet().iterator();
					while (mapEntries.hasNext()) {
						Map.Entry e = (Map.Entry)mapEntries.next();
						out.sTag(i+2, "td").nl();
						serializeAnything(out, e.getValue(), elementType, ctx, e.getKey().toString(), 2, null);
						out.eTag(i+2, "td").nl();
					}
				} else {
					BeanMap m2 = null;
					if (o instanceof BeanMap)
						m2 = (BeanMap)o;
					else
						m2 = bc.forBean(o);

					Iterator mapEntries = m2.entrySet().iterator();
					while (mapEntries.hasNext()) {
						BeanMapEntry p = (BeanMapEntry)mapEntries.next();
						BeanPropertyMeta pMeta = p.getMeta();
						out.sTag(i+2, "td").nl();
						serializeAnything(out, p.getValue(), pMeta.getClassMeta(), ctx, p.getKey().toString(), 2, pMeta);
						out.eTag(i+2, "td").nl();
					}
				}
				out.eTag(i+1, "tr").nl();
			}
			out.eTag(i, "table").nl();

		} else {
			out.sTag(i, "ul").nl();
			for (Object o : c) {
				out.sTag(i+1, "li").nl();
				serializeAnything(out, o, elementType, ctx, name, 1, null);
				out.eTag(i+1, "li").nl();
			}
			out.eTag(i, "ul").nl();
		}
	}

	/*
	 * Returns the table column headers for the specified collection of objects.
	 * Returns null if collection should not be serialized as a 2-dimensional table.
	 * 2-dimensional tables are used for collections of objects that all have the same set of property names.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String[] getTableHeaders(SerializerContext ctx, Collection c, BeanPropertyMeta<?> pMeta) throws SerializeException {
		BeanContext bc = ctx.getBeanContext();
		if (c.size() == 0)
			return null;
		c = sort(ctx, c);
		String[] th;
		Set<String> s = new TreeSet<String>();
		Set<ClassMeta> prevC = new HashSet<ClassMeta>();
		Object o1 = null;
		for (Object o : c)
			if (o != null) {
				o1 = o;
				break;
			}
		if (o1 == null)
			return null;
		ClassMeta cm = bc.getClassMetaForObject(o1);
		if (cm.getPojoFilter() != null) {
			PojoFilter f = cm.getPojoFilter();
			o1 = f.filter(o1);
			cm = cm.getFilteredClassMeta();
		}
		if (cm == null || ! (cm.isMap() || cm.isBean()))
			return null;
		if (cm.getInnerClass().isAnnotationPresent(HtmlLink.class))
			return null;
		HtmlClassMeta h = cm.getHtmlMeta();
		if (h.isNoTables() || (pMeta != null && pMeta.getHtmlMeta().isNoTables()))
				return null;
		if (h.isNoTableHeaders() || (pMeta != null && pMeta.getHtmlMeta().isNoTableHeaders()))
				return new String[0];
		if (canIgnoreValue(ctx, cm, null, o1))
			return null;
		if (cm.isMap() && ! cm.isBeanMap()) {
			Map m = (Map)o1;
			th = new String[m.size()];
			int i = 0;
			for (Object k : m.keySet())
				th[i++] = (k == null ? null : k.toString());
		} else {
			BeanMap<?> bm = (o1 instanceof BeanMap ? (BeanMap)o1 : bc.forBean(o1));
			List<String> l = new LinkedList<String>();
			for (String k : bm.keySet())
				l.add(k);
			th = l.toArray(new String[l.size()]);
		}
		prevC.add(cm);
		s.addAll(Arrays.asList(th));

		for (Object o : c) {
			if (o == null)
				continue;
			cm = bc.getClassMetaForObject(o);
			if (cm != null && cm.getPojoFilter() != null) {
				PojoFilter f = cm.getPojoFilter();
				o = f.filter(o);
				cm = cm.getFilteredClassMeta();
			}
			if (prevC.contains(cm))
				continue;
			if (cm == null || ! (cm.isMap() || cm.isBean()))
				return null;
			if (cm.getInnerClass().isAnnotationPresent(HtmlLink.class))
				return null;
			if (canIgnoreValue(ctx, cm, null, o))
				return null;
			if (cm.isMap() && ! cm.isBeanMap()) {
				Map m = (Map)o;
				if (th.length != m.keySet().size())
					return null;
				for (Object k : m.keySet())
					if (! s.contains(k.toString()))
						return null;
			} else {
				BeanMap<?> bm = (o instanceof BeanMap ? (BeanMap)o : bc.forBean(o));
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
		HtmlSchemaDocSerializer s = new HtmlSchemaDocSerializer();
		s.beanContextFactory = this.beanContextFactory;
		s.sp = this.sp;
		s.xsp = this.xsp;
		s.hsp = this.hsp;
		return s;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public HtmlSerializerContext createContext(ObjectMap properties, Method javaMethod) {
		return new HtmlSerializerContext(getBeanContext(), sp, xsp, hsp, properties, javaMethod);
	}

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		HtmlSerializerContext hctx = (HtmlSerializerContext)ctx;
		doSerialize(o, hctx.getWriter(out), hctx);
	}

	@Override /* CoreApi */
	public HtmlSerializer setProperty(String property, Object value) throws LockedException {
		if (! hsp.setProperty(property, value))
			super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public HtmlSerializer setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public HtmlSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public HtmlSerializer addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> HtmlSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public HtmlSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public HtmlSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public HtmlSerializer clone() {
		HtmlSerializer c = (HtmlSerializer)super.clone();
		c.hsp = hsp.clone();
		return c;
	}
}
