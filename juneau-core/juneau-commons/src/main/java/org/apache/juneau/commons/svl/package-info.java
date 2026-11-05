/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Simple Variable Language (SVL).
 *
 * <p>
 * SVL has two marker forms: <c>${Type:key:default}</c> (a var lookup; a body whose prefix is not a registered
 * var name is a property lookup) and <c>#{expr}</c> (a typed expression with operators, paths, collections and
 * functions).  Top-level <c>:</c> separates parts; a backslash before <c>:</c>, <c>'</c>, <c>"</c>, <c>,</c> or <c>}</c> is removed before the var sees the text (<c>\\</c> is one backslash).  The 9.x dollar-brace variable form and
 * the comma separator were removed in 10.0.
 *
 * <p>
 * See {@link org.apache.juneau.commons.svl.VarResolver} for the builder options (<c>strict</c>,
 * <c>allowFunctions</c>, <c>valueFormatter</c>, <c>maxExprDepth</c>, <c>maxCollectionSize</c>).
 */
package org.apache.juneau.commons.svl;
