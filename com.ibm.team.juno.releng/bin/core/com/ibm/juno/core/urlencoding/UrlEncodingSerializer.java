/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import static com.ibm.juno.core.urlencoding.UonSerializerProperties.*;
import static com.ibm.juno.core.urlencoding.UrlEncodingProperties.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Serializes POJO models to URL-encoded notation with UON-encoded values (a notation for URL-encoded query paramter values).
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>application/x-www-form-urlencoded</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>application/x-www-form-urlencoded</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link UonSerializerProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 * <p>
 * 	The following shows a sample object defined in Javascript:
 * </p>
 * <p class='bcode'>
 * 	{
 * 		id: 1,
 * 		name: <js>'John Smith'</js>,
 * 		uri: <js>'http://sample/addressBook/person/1'</js>,
 * 		addressBookUri: <js>'http://sample/addressBook'</js>,
 * 		birthDate: <js>'1946-08-12T00:00:00Z'</js>,
 * 		otherIds: <jk>null</jk>,
 * 		addresses: [
 * 			{
 * 				uri: <js>'http://sample/addressBook/address/1'</js>,
 * 				personUri: <js>'http://sample/addressBook/person/1'</js>,
 * 				id: 1,
 * 				street: <js>'100 Main Street'</js>,
 * 				city: <js>'Anywhereville'</js>,
 * 				state: <js>'NY'</js>,
 * 				zip: 12345,
 * 				isCurrent: <jk>true</jk>,
 * 			}
 * 		]
 * 	}
 * </p>
 * <p>
 * 	Using the "strict" syntax defined in this document, the equivalent
 * 		URL-encoded notation would be as follows:
 * </p>
 * <p class='bcode'>
 * 	<xa>id</xa>=$n(<xs>1</xs>)
 * 	&amp;<xa>name</xa>=<xs>John+Smith</xs>,
 * 	&amp;<xa>uri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 	&amp;<xa>addressBookUri</xa>=<xs>http://sample/addressBook</xs>,
 * 	&amp;<xa>birthDate</xa>=<xs>1946-08-12T00:00:00Z</xs>,
 * 	&amp;<xa>otherIds</xa>=<xs>%00</xs>,
 * 	&amp;<xa>addresses</xa>=$a(
 * 		$o(
 * 			<xa>uri</xa>=<xs>http://sample/addressBook/address/1</xs>,
 * 			<xa>personUri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 			<xa>id</xa>=$n(<xs>1</xs>),
 * 			<xa>street</xa>=<xs>100+Main+Street</xs>,
 * 			<xa>city</xa>=<xs>Anywhereville</xs>,
 * 			<xa>state</xa>=<xs>NY</xs>,
 * 			<xa>zip</xa>=$n(<xs>12345</xs>),
 * 			<xa>isCurrent</xa>=$b(<xs>true</xs>)
 * 		)
 * 	)
 * </p>
 * <p>
 * 	A secondary "lax" syntax is available when the data type of the
 * 		values are already known on the receiving end of the transmission:
 * </p>
 * <p class='bcode'>
 * 	<xa>id</xa>=<xs>1</xs>,
 * 	&amp;<xa>name</xa>=<xs>John+Smith</xs>,
 * 	&amp;<xa>uri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 	&amp;<xa>addressBookUri</xa>=<xs>http://sample/addressBook</xs>,
 * 	&amp;<xa>birthDate</xa>=<xs>1946-08-12T00:00:00Z</xs>,
 * 	&amp;<xa>otherIds</xa>=<xs>%00</xs>,
 * 	&amp;<xa>addresses</xa>=(
 * 		(
 * 			<xa>uri</xa>=<xs>http://sample/addressBook/address/1</xs>,
 * 			<xa>personUri</xa>=<xs>http://sample/addressBook/person/1</xs>,
 * 			<xa>id</xa>=<xs>1</xs>,
 * 			<xa>street</xa>=<xs>100+Main+Street</xs>,
 * 			<xa>city</xa>=<xs>Anywhereville</xs>,
 * 			<xa>state</xa>=<xs>NY</xs>,
 * 			<xa>zip</xa>=<xs>12345</xs>,
 * 			<xa>isCurrent</xa>=<xs>true</xs>
 * 		)
 * 	)
 * </p>
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Serialize a Map</jc>
 * 	Map m = <jk>new</jk> ObjectMap(<js>"{a:'b',c:1,d:false,e:['f',1,false],g:{h:'i'}}"</js>);
 *
 * 	<jc>// Serialize to value equivalent to JSON.</jc>
 * 	<jc>// Produces "a=b&amp;c=$n(1)&amp;d=$b(false)&amp;e=$a(f,$n(1),$b(false))&amp;g=$o(h=i)"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 *
 * 	<jc>// Serialize to simplified value (for when data type is already known by receiver).</jc>
 * 	<jc>// Produces "a=b&amp;c=1&amp;d=false&amp;e=(f,1,false)&amp;g=(h=i))"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT_SIMPLE</jsf>.serialize(s);
 *
 * 	<jc>// Serialize a bean</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> Person(String s);
 * 		<jk>public</jk> String getName();
 * 		<jk>public int</jk> getAge();
 * 		<jk>public</jk> Address getAddress();
 * 		<jk>public boolean</jk> deceased;
 * 	}
 *
 * 	<jk>public class</jk> Address {
 * 		<jk>public</jk> String getStreet();
 * 		<jk>public</jk> String getCity();
 * 		<jk>public</jk> String getState();
 * 		<jk>public int</jk> getZip();
 * 	}
 *
 * 	Person p = <jk>new</jk> Person(<js>"John Doe"</js>, 23, <js>"123 Main St"</js>, <js>"Anywhere"</js>, <js>"NY"</js>, 12345, <jk>false</jk>);
 *
 * 	<jc>// Produces "name=John+Doe&amp;age=23&amp;address=$o(street=123+Main+St,city=Anywhere,state=NY,zip=$n(12345))&amp;deceased=$b(false)"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 *
 * 	<jc>// Produces "name=John+Doe&amp;age=23&amp;address=(street=123+Main+St,city=Anywhere,state=NY,zip=12345)&amp;deceased=false)"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT_SIMPLE</jsf>.serialize(s);
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces("application/x-www-form-urlencoded")
@SuppressWarnings("hiding")
public class UrlEncodingSerializer extends UonSerializer {

	/** Reusable instance of {@link UrlEncodingSerializer}, all default settings. */
	public static final UrlEncodingSerializer DEFAULT = new UrlEncodingSerializer().lock();

	/** Reusable instance of {@link UrlEncodingSerializer.Simple}. */
	public static final UrlEncodingSerializer DEFAULT_SIMPLE = new Simple().lock();

	/** Reusable instance of {@link UrlEncodingSerializer.SimpleExpanded}. */
	public static final UrlEncodingSerializer DEFAULT_SIMPLE_EXPANDED = new SimpleExpanded().lock();

	/** Reusable instance of {@link UrlEncodingSerializer.Readable}. */
	public static final UrlEncodingSerializer DEFAULT_READABLE = new Readable().lock();

	/**
	 * Constructor.
	 */
	public UrlEncodingSerializer() {
		setProperty(UON_encodeChars, true);
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializer().setProperty(UonSerializerProperties.<jsf>UON_simpleMode</jsf>,<jk>true</jk>);</code>.
	 */
	@Produces(value={"application/x-www-form-urlencoded-simple"},contentType="application/x-www-form-urlencoded")
	public static class Simple extends UrlEncodingSerializer {
		/** Constructor */
		public Simple() {
			setProperty(UON_simpleMode, true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializer().setProperty(UonSerializerProperties.<jsf>UON_simpleMode</jsf>,<jk>true</jk>).setProperty(UonSerializerProperties.<jsf>URLENC_expandedParams</jsf>,<jk>true</jk>);</code>.
	 */
	@Produces(value={"application/x-www-form-urlencoded-simple"},contentType="application/x-www-form-urlencoded")
	public static class SimpleExpanded extends Simple {
		/** Constructor */
		public SimpleExpanded() {
			setProperty(URLENC_expandedParams, true);
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializer().setProperty(UonSerializerProperties.<jsf>UON_useWhitespace</jsf>,<jk>true</jk>);</code>.
	 */
	public static class Readable extends UrlEncodingSerializer {
		/** Constructor */
		public Readable() {
			setProperty(UON_useWhitespace, true);
		}
	}

	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeAnything(UonSerializerWriter out, Object o, UonSerializerContext ctx) throws SerializeException {
		try {

			BeanContext bc = ctx.getBeanContext();

			boolean addClassAttr;		// Add "_class" attribute to element?
			ClassMeta<?> aType;			// The actual type
			ClassMeta<?> gType;			// The generic type

			aType = ctx.push("root", o, object());
			ctx.indent--;
			if (aType == null)
				aType = object();

			gType = aType.getFilteredClassMeta();
			addClassAttr = (ctx.isAddClassAttrs());

			// Filter if necessary
			PojoFilter filter = aType.getPojoFilter();				// The filter
			if (filter != null) {
				o = filter.filter(o);

				// If the filter's getFilteredClass() method returns Object, we need to figure out
				// the actual type now.
				if (gType.isObject())
					gType = bc.getClassMetaForObject(o);
			}

			if (gType.isMap()) {
				if (o instanceof BeanMap)
					serializeBeanMap(out, (BeanMap)o, addClassAttr, ctx);
				else
					serializeMap(out, (Map)o, gType, ctx);
			} else if (gType.hasToObjectMapMethod()) {
				serializeMap(out, gType.toObjectMap(o), gType, ctx);
			} else if (gType.isBean()) {
				serializeBeanMap(out, bc.forBean(o), addClassAttr, ctx);
			} else if (gType.isCollection()) {
				serializeMap(out, getCollectionMap((Collection)o), bc.getMapClassMeta(Map.class, Integer.class, gType.getElementType()), ctx);
			} else {
				// All other types can't be serialized as key/value pairs, so we create a
				// mock key/value pair with a "_value" key.
				out.append("_value=");
				super.serializeAnything(out, o, null, ctx, null, null, false, true);
			}

			ctx.pop();
			return out;
		} catch (SerializeException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw e;
		} catch (Throwable e) {
			throw new SerializeException("Exception occurred trying to process object of type ''{0}''", (o == null ? null : o.getClass().getName())).initCause(e);
		}
	}

	private Map<Integer,Object> getCollectionMap(Collection<?> c) {
		Map<Integer,Object> m = new TreeMap<Integer,Object>();
		int i = 0;
		for (Object o : c)
			m.put(i++, o);
		return m;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(UonSerializerWriter out, Map m, ClassMeta<?> type, UonSerializerContext ctx) throws IOException, SerializeException {

		m = sort(ctx, m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		int depth = ctx.getIndent();
		boolean addAmp = false;

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();
			Object key = generalize(ctx, e.getKey(), keyType);


			if (shouldUseExpandedParams(value, ctx)) {
				Iterator i = value instanceof Collection ? ((Collection)value).iterator() : ArrayUtils.iterator(value);
				while (i.hasNext()) {
					if (addAmp)
						out.cr(depth).append('&');
					out.appendObject(key, false, true, true).append('=');
					super.serializeAnything(out, i.next(), null, ctx, (key == null ? null : key.toString()), null, false, true);
					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(depth).append('&');
			out.appendObject(key, false, true, true).append('=');
			super.serializeAnything(out, value, valueType, ctx, (key == null ? null : key.toString()), null, false, true);
				addAmp = true;
			}
		}

		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeBeanMap(UonSerializerWriter out, BeanMap m, boolean addClassAttr, UonSerializerContext ctx) throws IOException, SerializeException {
		int depth = ctx.getIndent();

		Iterator mapEntries = m.entrySet().iterator();

		// Print out "_class" attribute on this bean if required.
		if (addClassAttr) {
			String attr = "_class";
			out.appendObject(attr, false, false, true).append('=').append(m.getClassMeta().getInnerClass().getName());
			if (mapEntries.hasNext())
				out.cr(depth).append('&');
		}

		boolean addAmp = false;

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

			if (value != null && shouldUseExpandedParams(pMeta, ctx)) {
				ClassMeta cm = pMeta.getClassMeta();
				// Filtered object array bean properties may be filtered resulting in ArrayLists,
				// so we need to check type if we think it's an array.
				Iterator i = (cm.isCollection() || value instanceof Collection) ? ((Collection)value).iterator() : ArrayUtils.iterator(value);
				while (i.hasNext()) {
					if (addAmp)
						out.cr(depth).append('&');

					out.appendObject(key, false, true, true).append('=');

					super.serializeAnything(out, i.next(), pMeta.getClassMeta().getElementType(), ctx, key, pMeta, false, true);

					addAmp = true;
				}
			} else {
				if (addAmp)
					out.cr(depth).append('&');

				out.appendObject(key, false, true, true).append('=');

				super.serializeAnything(out, value, pMeta.getClassMeta(), ctx, key, pMeta, false, true);

				addAmp = true;
			}

		}
		return out;
	}

	/**
	 * Returns true if the specified bean property should be expanded as multiple key-value pairs.
	 */
	private final boolean shouldUseExpandedParams(BeanPropertyMeta<?> pMeta, UonSerializerContext ctx) {
		ClassMeta<?> cm = pMeta.getClassMeta();
		if (cm.isArray() || cm.isCollection()) {
			if (ctx.isExpandedParams())
				return true;
			if (pMeta.getBeanMeta().getClassMeta().getUrlEncodingMeta().isExpandedParams())
				return true;
		}
		return false;
	}

	private final boolean shouldUseExpandedParams(Object value, UonSerializerContext ctx) {
		if (value == null)
			return false;
		ClassMeta<?> cm = ctx.getBeanContext().getClassMetaForObject(value).getFilteredClassMeta();
		if (cm.isArray() || cm.isCollection()) {
			if (ctx.isExpandedParams())
				return true;
		}
		return false;
	}

	//--------------------------------------------------------------------------------
	// Methods for constructing individual parameter values.
	//--------------------------------------------------------------------------------

	/**
	 * Converts the specified object to a string using this serializers {@link BeanContext#convertToType(Object, Class)} method
	 * 	and runs {@link URLEncoder#encode(String,String)} against the results.
	 * Useful for constructing URL parts.
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 */
	public String serializeUrlPart(Object o) {
		try {
			// Shortcut for simple types.
			ClassMeta<?> cm = getBeanContext().getClassMetaForObject(o);
			if (cm != null)
				if (cm.isCharSequence() || cm.isNumber() || cm.isBoolean())
					return o.toString();

			UonSerializerContext uctx = createContext(null, null);
			StringWriter w = new StringWriter();
			super.doSerialize(o, w, uctx);
			return w.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public UonSerializerContext createContext(ObjectMap properties, Method javaMethod) {
		return new UonSerializerContext(getBeanContext(), sp, usp, uep, properties, javaMethod);
	}

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		UonSerializerContext uctx = (UonSerializerContext)ctx;
		serializeAnything(uctx.getWriter(out), o, uctx);
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! usp.setProperty(property, value))
			if (! uep.setProperty(property, value))
				super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> UrlEncodingSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public UrlEncodingSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public UrlEncodingSerializer clone() {
		UrlEncodingSerializer c = (UrlEncodingSerializer)super.clone();
		c.usp = usp.clone();
		c.uep = uep.clone();
		return c;
	}
}
