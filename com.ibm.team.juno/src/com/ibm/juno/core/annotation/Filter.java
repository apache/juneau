/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.serializer.*;

/**
 * Annotation that can be applied to a class to associate a filter with it.
 * <p>
 * 	Typically used to associate {@link PojoFilter PojoFilters} with classes using annotations
 * 		instead of programatically through a method such as {@link Serializer#addFilters(Class...)}.
 *
 * <h6 class='topic'>Example</h6>
 * <p>
 * 	In this case, a filter is being applied to a bean that will force it to be serialized as a <code>String</code>
 * <p class='bcode'>
 * 	<jc>// Our bean class</jc>
 * 	<ja>@Filter</ja>(BFilter.<jk>class</jk>)
 * 	<jk>public class</jk> B {
 * 		<jk>public</jk> String <jf>f1</jf>;
 * 	}
 *
 * 	<jc>// Our filter to force the bean to be serialized as a String</jc>
 * 	<jk>public class</jk> BFilter <jk>extends</jk> PojoFilter&lt;B,String&gt; {
 * 		<jk>public</jk> String filter(B o) <jk>throws</jk> SerializeException {
 * 			<jk>return</jk> o.f1;
 * 		}
 * 		<jk>public</jk> B unfilter(String f, ClassMeta&lt;?&gt; hint) <jk>throws</jk> ParseException {
 * 			B b1 = <jk>new</jk> B();
 * 			b1.<jf>f1</jf> = f;
 * 			<jk>return</jk> b1;
 * 		}
 * 	}
 *
 * 	<jk>public void</jk> testFilter() <jk>throws</jk> Exception {
 * 		WriterSerializer s = JsonSerializer.<jsf>DEFAULT</jsf>;
 * 		B b = <jk>new</jk> B();
 * 		b.<jf>f1</jf> = <js>"bar"</js>;
 * 		String json = s.serialize(b);
 * 		<jsm>assertEquals</jsm>(<js>"'bar'"</js>, json);
 *
 * 		ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
 * 		b = p.parse(json, B.<jk>class</jk>);
 * 		<jsm>assertEquals</jsm>(<js>"bar"</js>, t.<jf>f1</jf>);
 * 	}
 * </p>
 * <p>
 * 	Note that using this annotation is functionally equivalent to adding filters to the serializers and parsers:
 * <p class='bcode'>
 * 	WriterSerializer s = <jk>new</jk> JsonSerializer.addFilters(BFilter.<jk>class</jk>);
 * 	ReaderParser p = <jk>new</jk> JsonParser.addFilters(BFilter.<jk>class</jk>);
 * </p>
 * <p>
 * 	It is technically possible to associate a {@link BeanFilter} with a bean class using this annotation.
 * 	However in practice, it's almost always less code to simply use the {@link Bean @Bean} annotation.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Filter {

	/**
	 * The filter class.
	 */
	Class<? extends com.ibm.juno.core.filter.Filter> value() default com.ibm.juno.core.filter.Filter.NULL.class;
}