/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

import TOCItemTree from '@theme-original/TOCItems/Tree';
import type {ComponentProps} from 'react';

type TocItems = ComponentProps<typeof TOCItemTree>['toc'];

// Each TOC entry is itself a link, and its label is the heading's HTML. A link inside a heading would nest an <a>
// inside that <a>, which browsers split apart while parsing and so never matches what React rendered (hydration
// error #418). Keep the heading text and drop the nested anchor tags.
const stripLinks = (toc: TocItems): TocItems =>
  toc.map((item) => ({
    ...item,
    value: item.value.replace(/<\/?a\b[^>]*>/gi, ''),
    children: stripLinks(item.children),
  }));

export default function TOCItemTreeWrapper(props: ComponentProps<typeof TOCItemTree>) {
  return <TOCItemTree {...props} toc={stripLinks(props.toc)} />;
}
