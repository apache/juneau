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
package org.apache.juneau.rest.annotation;

/**
 * Dummy class that indicates that serializers, parsers, or transforms should be inherited from parent-class-to-class or class-to-method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestOp</ja>(
 *
 * 		<jc>// Override the serializers for this method, but also inherit the serializers from the class.
 * 		// Class-level serializers will be inserted in the location in the array.</jc>
 * 		serializers={JsonSerializer.<jk>class</jk>, Inherit.<jk>class</jk>},
 *
 * 		<jc>// Override the parsers for this method, but also inherit the parsers from the class.
 * 		// Class-level parsers will be inserted in the location in the array.</jc>
 * 		parsers={JsonParser.<jk>class</jk>, Inherit.<jk>class</jk>},
 *
 * 		<jc>// Override the bean filters for this method, but also inherit the bean filters from the class.
 * 		// Overridden bean filters only apply to NEW serializers and parsers defined on the method
 * 		// (not those inherited from the class).</jc>
 * 		beanFilters={MyFilter.<jk>class</jk>, Inherit.<jk>class</jk>},
 *
 * 		<jc>// Override the POJO swaps for this method, but also inherit the POJO swaps from the class.
 * 		// Overridden POJO swaps only apply to NEW serializers and parsers defined on the method
 * 		// (not those inherited from the class).</jc>
 * 		swaps={MySwap.<jk>class</jk>, Inherit.<jk>class</jk>}
 * 	)
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class Inherit {}
