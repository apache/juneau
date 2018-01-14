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
package org.apache.juneau.transform;

import org.apache.juneau.*;

/**
 * Identifies a class as being a surrogate class.
 * 
 * <p>
 * Surrogate classes are used in place of other classes during serialization.
 * For example, you may want to use a surrogate class to change the names or order of bean properties on a bean.
 * 
 * <p>
 * This interface has no methods to implement.
 * It's simply used by the framework to identify the class as a surrogate class when specified as a swap.
 * 
 * <p>
 * The following is an example of a surrogate class change changes a property name:
 * <p class='bcode'>
 * 	<jk>public class</jk> MySurrogate <jk>implements</jk> Surrogate {
 * 		<jk>public</jk> String surrogateField;  <jc>// New bean property</jc>
 * 
 * 		<jk>public</jk> MySurrogate(NormalClass normalClass) {
 * 			<jk>this</jk>.surrogateField = normalClass.normalField;
 * 		}
 * 	}
 * </p>
 * 
 * <p>
 * Optionally, a public static method can be used to un-transform a class during parsing:
 * <p class='bcode'>
 * 	<jk>public class</jk> MySurrogate <jk>implements</jk> Surrogate {
 * 		...
 * 		<jk>public static</jk> NormalClass <jsm>toNormalClass</jsm>(SurrogateClass surrogateClass) {
 * 			<jk>return new</jk> NormalClass(surrogateClass.transformedField);
 * 		}
 * 	}
 * </p>
 * 
 * <p>
 * Surrogate classes must conform to the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		It must have a one or more public constructors that take in a single parameter whose type is the normal types.
 * 		(It is possible to define a class as a surrogate for multiple class types by using multiple constructors with
 * 		different parameter types).
 * 	<li>
 * 		It optionally can have a public static method that takes in a single parameter whose type is the transformed
 * 		type and returns an instance of the normal type.
 * 		This is called the un-transform method.
 * 		The method can be called anything.
 * 	<li>
 * 		If an un-transform method is present, the class must also contain a no-arg constructor (so that the
 * 		transformed class can be instantiated by the parser before being converted into the normal class by the
 * 		un-transform method).
 * </ul>
 * 
 * <p>
 * Surrogate classes are associated with serializers and parsers using the {@link BeanContextBuilder#pojoSwaps(Class...)}
 * method.
 * <p class='bcode'>
 * 	<ja>@Test</ja>
 * 	<jk>public void</jk> test() <jk>throws</jk> Exception {
 * 		JsonSerializer s = JsonSerializer.<jsm>create</jsm>().simple().pojoSwaps(MySurrogate.<jk>class</jk>).build();
 * 		JsonParser p = JsonParser.<jsm>create</jsm>().pojoSwaps(MySurrogate.<jk>class</jk>).build();
 * 		String r;
 * 		Normal n = Normal.<jsm>create</jsm>();
 * 
 * 		r = s.serialize(n);
 * 		assertEquals(<js>"{f2:'f1'}"</js>, r);
 * 
 * 		n = p.parse(r, Normal.<jk>class</jk>);
 * 		assertEquals(<js>"f1"</js>, n.f1);
 * 	}
 * 
 * 	<jc>// The normal class</jc>
 * 	<jk>public class</jk> Normal {
 * 		<jk>public</jk> String f1;
 * 
 * 		<jk>public static</jk> Normal <jsm>create</jsm>() {
 * 			Normal n = <jk>new</jk> Normal();
 * 			n.f1 = <js>"f1"</js>;
 * 			<jk>return</jk> n;
 * 		}
 * 	}
 * 
 * 	<jc>// The surrogate class</jc>
 * 	<jk>public class</jk> MySurrogate <jk>implements</jk> Surrogate {
 * 		<jk>public</jk> String f2;
 * 
 * 		<jc>// Surrogate constructor</jc>
 * 		<jk>public</jk> MySurrogate(Normal n) {
 * 			f2 = n.f1;
 * 		}
 * 
 * 		<jc>// Constructor used during parsing (only needed if un-transform method specified)</jc>
 * 		<jk>public</jk> MySurrogate() {}
 * 
 * 		<jc>// Un-transform method (optional)</jc>
 * 		<jk>public static</jk> Normal <jsm>toNormal</jsm>(Surrogate f) {
 * 			Normal n = <jk>new</jk> Normal();
 * 			n.f1 = f.f2;
 * 			<jk>return</jk> n;
 * 		}
 * 	}
 * </p>
 * 
 * <p>
 * It should be noted that a surrogate class is functionally equivalent to the following {@link PojoSwap}
 * implementation:
 * <p class='bcode'>
 * 	<jk>public static class</jk> MySurrogate <jk>extends</jk> PojoSwap&lt;Normal,MySurrogate&gt; {
 * 		<jk>public</jk> MySurrogate swap(Normal n) <jk>throws</jk> SerializeException {
 * 			<jk>return new</jk> MySurrogate(n);
 * 		}
 * 		<jk>public</jk> Normal unswap(MySurrogate s, ClassMeta&lt;?&gt; hint) <jk>throws</jk> ParseException {
 * 			<jk>return</jk> MySurrogate.<jsm>toNormal</jsm>(s);
 * 		}
 * 	}
 * </p>
 * 
 */
public interface Surrogate {}
