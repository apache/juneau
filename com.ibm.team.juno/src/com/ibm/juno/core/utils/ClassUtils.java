/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;

/**
 * Class-related utility methods.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class ClassUtils {

	/**
	 * Given the specified list of objects, return readable names for the class types of the objects.
	 *
	 * @param o The objects.
	 * @return An array of readable class type strings.
	 */
	public static ObjectList getReadableClassNames(Object[] o) {
		ObjectList l = new ObjectList();
		for (int i = 0; i < o.length; i++)
			l.add(o[i] == null ? "null" : getReadableClassName(o[i].getClass()));
		return l;
	}

	/**
	 * Shortcut for calling <code><jsm>getReadableClassName</jsm>(c.getName())</code>
	 *
	 * @param c The class.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassName(Class<?> c) {
		if (c == null)
			return null;
		return getReadableClassName(c.getName());
	}

	/**
	 * Converts the specified class name to a readable form when class name is a special construct like <js>"[[Z"</js>.
	 * <p>
	 * Examples:
	 * <p class='bcode'>
	 * 	<jsm>getReadableClassName</jsm>(<js>"java.lang.Object"</js>);  <jc>// Returns "java.lang.Object"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"boolean"</js>);  <jc>// Returns "boolean"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[Z"</js>);  <jc>// Returns "boolean[]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[[Z"</js>);  <jc>// Returns "boolean[][]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<js>"[Ljava.lang.Object;"</js>);  <jc>// Returns "java.lang.Object[]"</jc>
	 * 	<jsm>getReadableClassName</jsm>(<jk>null</jk>);  <jc>// Returns null</jc>
	 * </p>
	 *
	 * @param className The class name.
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public static String getReadableClassName(String className) {
		if (className == null)
			return null;
		if (! StringUtils.startsWith(className, '['))
			return className;
		int depth = 0;
		for (int i = 0; i < className.length(); i++) {
			if (className.charAt(i) == '[')
				depth++;
			else
				break;
		}
		char type = className.charAt(depth);
		String c;
		switch (type) {
			case 'Z': c = "boolean"; break;
			case 'B': c = "byte"; break;
			case 'C': c = "char"; break;
			case 'D': c = "double"; break;
			case 'F': c = "float"; break;
			case 'I': c = "int"; break;
			case 'J': c = "long"; break;
			case 'S': c = "short"; break;
			default: c = className.substring(depth+1, className.length()-1);
		}
		StringBuilder sb = new StringBuilder(c.length() + 2*depth).append(c);
		for (int i = 0; i < depth; i++)
			sb.append("[]");
		return sb.toString();
	}

	/**
	 * Returns <jk>true</jk> if <code>parent</code> is a parent class of <code>child</code>.
	 *
	 * @param parent The parent class.
	 * @param child The child class.
	 * @param strict If <jk>true</jk> returns <jk>false</jk> if the classes are the same.
	 * @return <jk>true</jk> if <code>parent</code> is a parent class of <code>child</code>.
	 */
	public static boolean isParentClass(Class<?> parent, Class<?> child, boolean strict) {
		return parent.isAssignableFrom(child) && ((!strict) || ! parent.equals(child));
	}

	/**
	 * Returns <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 *
	 * @param parent The parent class.
	 * @param child The child class.
	 * @return <jk>true</jk> if <code>parent</code> is a parent class or the same as <code>child</code>.
	 */
	public static boolean isParentClass(Class<?> parent, Class<?> child) {
		return isParentClass(parent, child, false);
	}

	/**
	 * Comparator for use with {@link TreeMap TreeMaps} with {@link Class} keys.
	 *
	 * @author James Bognar (jbognar@us.ibm.com)
	 */
	public final static class ClassComparator implements Comparator<Class<?>>, Serializable {

		private static final long serialVersionUID = 1L;

		@Override /* Comparator */
		public int compare(Class<?> object1, Class<?> object2) {
			return object1.getName().compareTo(object2.getName());
		}
	}

	/**
	 * Returns the signature of the specified method.
	 * For no-arg methods, the signature will be a simple string such as <js>"toString"</js>.
	 * For methods with one or more args, the arguments will be fully-qualified class names (e.g. <js>"append(java.util.StringBuilder,boolean)"</js>)
	 *
	 * @param m The methods to get the signature on.
	 * @return The methods signature.
	 */
	public static String getMethodSignature(Method m) {
		StringBuilder sb = new StringBuilder(m.getName());
		Class<?>[] pt = m.getParameterTypes();
		if (pt.length > 0) {
			sb.append('(');
			for (int i = 0; i < pt.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(getReadableClassName(pt[i]));
			}
			sb.append(')');
		}
		return sb.toString();
	}

	private final static Map<Class<?>, Class<?>> pmap1 = new HashMap<Class<?>, Class<?>>(), pmap2 = new HashMap<Class<?>, Class<?>>();
	static {
		pmap1.put(boolean.class, Boolean.class);
		pmap1.put(byte.class, Byte.class);
		pmap1.put(short.class, Short.class);
		pmap1.put(char.class, Character.class);
		pmap1.put(int.class, Integer.class);
		pmap1.put(long.class, Long.class);
		pmap1.put(float.class, Float.class);
		pmap1.put(double.class, Double.class);
		pmap2.put(Boolean.class, boolean.class);
		pmap2.put(Byte.class, byte.class);
		pmap2.put(Short.class, short.class);
		pmap2.put(Character.class, char.class);
		pmap2.put(Integer.class, int.class);
		pmap2.put(Long.class, long.class);
		pmap2.put(Float.class, float.class);
		pmap2.put(Double.class, double.class);
	}

	/**
	 * If the specified class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>)
	 * 	returns it's wrapper class (e.g. <code>Integer.<jk>class</jk></code>).
	 *
	 * @param c The class.
	 * @return The wrapper class, or <jk>null</jk> if class is not a primitive.
	 */
	public static Class<?> getPrimitiveWrapper(Class<?> c) {
		return pmap1.get(c);
	}

	/**
	 * If the specified class is a primitive wrapper (e.g. <code><jk>Integer</jk>.<jk>class</jk></code>)
	 * 	returns it's primitive class (e.g. <code>int.<jk>class</jk></code>).
	 *
	 * @param c The class.
	 * @return The primitive class, or <jk>null</jk> if class is not a primitive wrapper.
	 */
	public static Class<?> getPrimitiveForWrapper(Class<?> c) {
		return pmap2.get(c);
	}

	/**
	 * If the specified class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>)
	 * 	returns it's wrapper class (e.g. <code>Integer.<jk>class</jk></code>).
	 *
	 * @param c The class.
	 * @return The wrapper class if it's primitive, or the same class if class is not a primitive.
	 */
	public static Class<?> getWrapperIfPrimitive(Class<?> c) {
		if (! c.isPrimitive())
			return c;
		return pmap1.get(c);
	}
}
