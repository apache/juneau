/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filter;

import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Used to convert non-serializable objects to a serializable form.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	<code>PojoFilters</code> are used to extend the functionality of the serializers and parsers to be able to handle POJOs
 * 	that aren't automatically handled by the serializers or parsers.  For example, JSON does not have a standard
 * 	representation for rendering dates.  By defining a special {@code Date} filter and associating it with a serializer and
 * 	parser, you can convert a {@code Date} object to a {@code String} during serialization, and convert that {@code String} object back into
 * 	a {@code Date} object during parsing.
 * <p>
 * 	Object filters MUST declare a public no-arg constructor so that the bean context can instantiate them.
 * <p>
 * 	<code>PojoFilters</code> are associated with instances of {@link BeanContext BeanContexts} by passing the filter class to
 * 	the {@link BeanContextFactory#addFilters(Class...)} method.<br>
 * 	When associated with a bean context, fields of the specified type will automatically be converted when the
 * 	{@link BeanMap#get(Object)} or {@link BeanMap#put(String, Object)} methods are called.<br>
 * <p>
 * 	<code>PojoFilters</code> have two parameters:
 * 	<ol>
 * 		<li>{@code <F>} - The filtered representation of an object.
 * 		<li>{@code <T>} - The normal representation of an object.
 * 	</ol>
 * 	<br>
 * 	{@link Serializer Serializers} use object filters to convert objects of type T into objects of type F, and on calls to {@link BeanMap#get(Object)}.<br>
 * 	{@link Parser Parsers} use object filters to convert objects of type F into objects of type T, and on calls to {@link BeanMap#put(String,Object)}.
 *
 *
 * <h6 class='topic'>Filtered Class Type {@code <F>}</h6>
 * <p>
 * 	The filtered object representation of an object must be an object type that the serializers can
 * 	natively convert to JSON (or language-specific equivalent).  The list of valid filtered types are as follows...
 * 	<ul>
 * 		<li>{@link String}
 * 		<li>{@link Number}
 * 		<li>{@link Boolean}
 * 		<li>{@link Collection} containing anything on this list.
 * 		<li>{@link Map} containing anything on this list.
 * 		<li>A java bean with properties of anything on this list.
 * 		<li>An array of anything on this list.
 * 	</ul>
 *
 *
 * <h6 class='topic'>Normal Class Type {@code <T>}</h6>
 * <p>
 * 	The normal object representation of an object.<br>
 *
 *
 * <h6 class='topic'>One-way vs. Two-way Serialization</h6>
 * <p>
 * 	Note that while there is a unified interface for handling filtering during both serialization and parsing,
 * 	in many cases only one of the {@link #filter(Object)} or {@link #unfilter(Object, ClassMeta)} methods will be defined
 * 	because the filter is one-way.  For example, a filter may be defined to convert an {@code Iterator} to a {@code ObjectList}, but
 * 	it's not possible to unfilter an {@code Iterator}.  In that case, the {@code generalize(Object}} method would
 * 	be implemented, but the {@code narrow(ObjectMap)} object would not, and the filter would be associated on
 * 	the serializer, but not the parser.  Also, you may choose to serialize objects like {@code Dates} to readable {@code Strings},
 * 	in which case it's not possible to reparse it back into a {@code Date}, since there is no way for the {@code Parser} to
 * 	know it's a {@code Date} from just the JSON or XML text.
 *
 *
 * <h6 class='topic'>Additional information</h6>
 * 	See {@link com.ibm.juno.core.filter} for more information.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The normal form of the class.
 * @param <F> The filtered form of the class.
 */
public abstract class PojoFilter<T,F> extends Filter {

	/** Represents no filter. */
	public static class NULL extends PojoFilter<Object,Object> {}

	Class<T> normalClass;
	Class<F> filteredClass;
	ClassMeta<F> filteredClassMeta;

	/**
	 * Constructor.
	 */
	@SuppressWarnings("unchecked")
	protected PojoFilter() {
		super();

		Class<?> c = this.getClass().getSuperclass();
		Type t = this.getClass().getGenericSuperclass();
		while (c != PojoFilter.class) {
			t = c.getGenericSuperclass();
			c = c.getSuperclass();
		}

		// Attempt to determine the T and G classes using reflection.
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			Type[] pta = pt.getActualTypeArguments();
			if (pta.length == 2) {
				Type nType = pta[0];
				if (nType instanceof Class) {
					this.normalClass = (Class<T>)nType;

				// <byte[],x> ends up containing a GenericArrayType, so it has to
				// be handled as a special case.
				} else if (nType instanceof GenericArrayType) {
					Class<?> cmpntType = (Class<?>)((GenericArrayType)nType).getGenericComponentType();
					this.normalClass = (Class<T>)Array.newInstance(cmpntType, 0).getClass();

				// <Class<?>,x> ends up containing a ParameterizedType, so just use the raw type.
				} else if (nType instanceof ParameterizedType) {
					this.normalClass = (Class<T>)((ParameterizedType)nType).getRawType();

				} else
					throw new RuntimeException("Unsupported parameter type: " + nType);
				if (pta[1] instanceof Class)
					this.filteredClass = (Class<F>)pta[1];
				else if (pta[1] instanceof ParameterizedType)
					this.filteredClass = (Class<F>)((ParameterizedType)pta[1]).getRawType();
				else
					throw new RuntimeException("Unexpected filtered class type: " + pta[1].getClass().getName());
			}
		}
	}

	/**
	 * Constructor for when the normal and filtered classes are already known.
	 *
	 * @param normalClass The normal class (cannot be serialized).
	 * @param filteredClass The filtered class (serializable).
	 */
	protected PojoFilter(Class<T> normalClass, Class<F> filteredClass) {
		this.normalClass = normalClass;
		this.filteredClass = filteredClass;
	}

	/**
	 * If this filter is to be used to serialize non-serializable POJOs, it must implement this method.
	 * <p>
	 * 	The object must be converted into one of the following serializable types:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>{@link Number}
	 * 		<li>{@link Boolean}
	 * 		<li>{@link Collection} containing anything on this list.
	 * 		<li>{@link Map} containing anything on this list.
	 * 		<li>A java bean with properties of anything on this list.
	 * 		<li>An array of anything on this list.
	 * 	</ul>
	 *
	 * @param o The object to be filtered.
	 * @return The filtered object.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public F filter(T o) throws SerializeException {
		throw new SerializeException("Generalize method not implemented on filter ''{0}''", this.getClass().getName());
	}

	/**
	 * If this filter is to be used to reconstitute POJOs that aren't true Java beans, it must implement this method.
	 *
	 * @param f The filtered object.
	 * @param hint If possible, the parser will try to tell you the object type being created.  For example,
	 * 	on a serialized date, this may tell you that the object being created must be of type {@code GregorianCalendar}.<br>
	 * 	This may be <jk>null</jk> if the parser cannot make this determination.
	 * @return The narrowed object.
	 * @throws ParseException If this method is not implemented.
	 */
	public T unfilter(F f, ClassMeta<?> hint) throws ParseException {
		throw new ParseException("Narrow method not implemented on filter ''{0}''", this.getClass().getName());
	}

	/**
	 * Returns the T class, the normalized form of the class.
	 *
	 * @return The normal form of this class.
	 */
	public Class<T> getNormalClass() {
		return normalClass;
	}

	/**
	 * Returns the G class, the generialized form of the class.
	 * <p>
	 * 	Subclasses must override this method if the generialized class is {@code Object},
	 * 	meaning it can produce multiple generialized forms.
	 *
	 * @return The filtered form of this class.
	 */
	public Class<F> getFilteredClass() {
		return filteredClass;
	}

	/**
	 * Returns the {@link ClassMeta} of the filtered class type.
	 * This value is cached for quick lookup.
	 *
	 * @return The {@link ClassMeta} of the filtered class type.
	 */
	public ClassMeta<F> getFilteredClassMeta() {
		if (filteredClassMeta == null)
			filteredClassMeta = beanContext.getClassMeta(filteredClass);
		return filteredClassMeta;
	}

	/**
	 * Checks if the specified object is an instance of the normal class defined on this filter.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is a subclass of the normal class defined on this filter.
	 * <jk>null</jk> always return <jk>false</jk>.
	 */
	public boolean isNormalObject(Object o) {
		if (o == null)
			return false;
		return ClassUtils.isParentClass(normalClass, o.getClass());
	}

	/**
	 * Checks if the specified object is an instance of the filtered class defined on this filter.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is a subclass of the filtered class defined on this filter.
	 * <jk>null</jk> always return <jk>false</jk>.
	 */
	public boolean isFilteredObject(Object o) {
		if (o == null)
			return false;
		return ClassUtils.isParentClass(filteredClass, o.getClass());
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Filter */
	public Class<?> forClass() {
		return normalClass;
	}

	@Override /* Object */
	public String toString() {
		return getClass().getSimpleName() + '<' + getNormalClass().getSimpleName() + "," + getFilteredClass().getSimpleName() + '>';
	}
}
