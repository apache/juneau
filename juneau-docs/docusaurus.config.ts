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

import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import type {GiscusProps} from '@giscus/react';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

type GiscusThemeConfig = Partial<GiscusProps>;

type ThemeConfigWithGiscus = Preset.ThemeConfig & {
  giscus?: GiscusThemeConfig;
};

const giscusConfig: GiscusThemeConfig = {
  repo: 'apache/juneau',
  repoId: process.env.GISCUS_REPO_ID ?? 'MDEwOlJlcG9zaXRvcnk2NDczMjY2NQ==',
  category: process.env.GISCUS_CATEGORY ?? 'Announcements',
  categoryId: process.env.GISCUS_CATEGORY_ID ?? 'DIC_kwDOA9u9-c4CvzB6',
  mapping: 'pathname',
  reactionsEnabled: '1',
  emitMetadata: '0',
  strict: '0',
  inputPosition: 'top',
  theme: 'preferred_color_scheme',
  lang: 'en',
  loading: 'lazy',
};

const baseThemeConfig: Preset.ThemeConfig = {
  // Replace with your project's social card
  image: 'img/docusaurus-social-card.jpg',
  algolia: {
    // The application ID provided by Algolia
    appId: 'DVIGEYTUK7',

    // Public API key: it is safe to commit it
    apiKey: 'db9fd10aa92e0ad212e08340e37293c0',

    indexName: 'juneau_docsJuneau Website',

    // Optional: see doc section below
    contextualSearch: true,

    // Optional: Specify domains where the navigation should occur through window.location instead on history.push. Useful when our Algolia config crawls multiple documentation sites and we want to navigate with window.location.href to them.
    // externalUrlRegex: 'external\\.com|domain\\.com',

    // Optional: Replace parts of the item URLs from Algolia. Useful when using the same search index for multiple deployments using a different baseUrl. You can use regexp or string in the `from` param. For example: localhost:3000 vs myCompany.com/docs
    // replaceSearchResultPathname: {
    //   from: '/docs/', // or as RegExp: /\/docs\//
    //   to: '/',
    // },

    // Optional: Algolia search parameters
    searchParameters: {},

    // Optional: path for search page that enabled by default (`false` to disable it)
    searchPagePath: 'search',

    // Optional: whether the insights feature is enabled or not on Docsearch (`false` by default)
    insights: false,
  },
  navbar: {
    title: 'Apache Juneau',
    logo: {
      alt: 'Apache Juneau',
      src: 'img/oakleaf.svg',
    },
    items: [
      {to: '/about', label: 'About', position: 'left'},
      {
        type: 'docSidebar',
        sidebarId: 'mainSidebar',
        position: 'left',
        label: 'Documentation',
      },
      {to: '/downloads', label: 'Downloads', position: 'left'},
      {to: '/security', label: 'Security', position: 'left'},
      {to: '/apache', label: 'Apache', position: 'left'},
      {
        href: 'https://github.com/apache/juneau',
        label: 'GitHub',
        position: 'right',
      },
      {
        href: 'https://github.com/apache/juneau/wiki',
        label: 'Wiki',
        position: 'right',
      },
      {
        href: '/javadocs/',
        label: 'Javadocs',
        position: 'right',
      },
    ],
  },
  footer: {
    style: 'dark',
    links: [
      {
        title: 'Documentation',
        items: [
          {
            label: 'About',
            to: '/about',
          },
          {
            label: 'Downloads',
            to: '/downloads',
          },
          {
            label: 'Javadocs',
            href: '/site/apidocs/index.html',
          },
        ],
      },
      {
        title: 'Community',
        items: [
          {
            label: 'GitHub',
            href: 'https://github.com/apache/juneau',
          },
          {
            label: 'Wiki',
            href: 'https://github.com/apache/juneau/wiki',
          },
          {
            label: 'Mailing List',
            href: 'mailto:dev@juneau.apache.org?Subject=Apache%20Juneau%20question',
          },
        ],
      },
      {
        title: 'Apache',
        items: [
          {
            label: 'Apache Foundation',
            href: 'http://www.apache.org/foundation',
          },
          {
            label: 'License',
            href: 'http://www.apache.org/licenses/',
          },
          {
            label: 'Security',
            href: 'http://www.apache.org/security',
          },
        ],
      },
    ],
    copyright: `Copyright © ${new Date().getFullYear()} The Apache Software Foundation. Licensed under the Apache License, Version 2.0. Apache, Apache Juneau, and the Apache feather logo are trademarks of The Apache Software Foundation.`,
  },
  prism: {
    theme: {plain: {}, styles: []}, // Use custom Eclipse colors instead of default theme
    darkTheme: {plain: {}, styles: []}, // Use custom Eclipse colors for dark mode too
    additionalLanguages: ['java', 'json', 'yaml', 'bash', 'properties', 'ini'],
    defaultLanguage: 'java',
  },
} satisfies Preset.ThemeConfig;

const themeConfig: ThemeConfigWithGiscus = {
  ...baseThemeConfig,
  giscus: giscusConfig,
};

const config: Config = {
  title: 'Apache Juneau',
  tagline: 'Universal toolkit for marshalling POJOs to a wide variety of content types using a common framework',
  favicon: 'img/favicon.ico',

  // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
  future: {
    v4: true, // Improve compatibility with the upcoming Docusaurus v4
  },

  // Set the production url of your site here
  url: 'https://juneau.apache.org',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'apache', // Usually your GitHub org/user name.
  projectName: 'juneau', // Usually your repo name.

  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          path: './docs',
          sidebarPath: './sidebars.ts',
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/apache/juneau/tree/master/juneau-docs/',
        },
        blog: {
          showReadingTime: true,
          feedOptions: {
            type: ['rss', 'atom'],
            xslt: true,
          },
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
          // Useful options to enforce blogging best practices
          onInlineTags: 'warn',
          onInlineAuthors: 'warn',
          onUntruncatedBlogPosts: 'warn',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig,
};

export default config;
