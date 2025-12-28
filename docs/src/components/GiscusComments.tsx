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

import Giscus from '@giscus/react';
import {useColorMode, useThemeConfig} from '@docusaurus/theme-common';
import type {GiscusProps} from '@giscus/react';
import type {ThemeConfig} from '@docusaurus/types';

type ThemeConfigWithGiscus = ThemeConfig & {
  giscus?: Partial<GiscusProps>;
};

export default function GiscusComments() {
  const themeConfig = useThemeConfig() as ThemeConfigWithGiscus;
  const giscusConfig = themeConfig.giscus;
  const {colorMode} = useColorMode();
  if (!giscusConfig?.repo || !giscusConfig.repoId || !giscusConfig.mapping) {
    return null;
  }

  const props: GiscusProps = {
    repo: giscusConfig.repo,
    repoId: giscusConfig.repoId,
    mapping: giscusConfig.mapping,
    category: giscusConfig.category,
    categoryId: giscusConfig.categoryId,
    term: giscusConfig.term,
    theme:
      giscusConfig.theme && giscusConfig.theme !== 'preferred_color_scheme'
        ? giscusConfig.theme
        : colorMode === 'dark'
          ? 'dark'
          : 'light',
    strict: giscusConfig.strict ?? '0',
    reactionsEnabled: giscusConfig.reactionsEnabled ?? '1',
    emitMetadata: giscusConfig.emitMetadata ?? '0',
    inputPosition: giscusConfig.inputPosition ?? 'bottom',
    lang: giscusConfig.lang ?? 'en',
    loading: giscusConfig.loading ?? 'lazy',
    host: giscusConfig.host,
    id: giscusConfig.id,
  };

  return (
    <section className="giscus-comments" aria-live="polite">
      <div className="giscus-comments__header">
        <span className="giscus-comments__label">Discussion</span>
        <hr className="giscus-comments__divider" aria-hidden="true" />
      </div>
      <p className="giscus-comments__description">
        Share feedback or follow-up questions for this page directly through GitHub.
      </p>
      <Giscus {...props} />
    </section>
  );
}

