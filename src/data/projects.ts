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

/**
 * The Juneau projects: one source of truth for names, order, modules and Read-more topics.
 *
 * Read by the home page, ProjectRail and ProjectLayout, and (through scripts/project_index.py)
 * by scripts/check-project-pages.py, scripts/check-doc-inventory.py and scripts/generate-ai-artifacts.py.
 * Format rules, enforced by project_index.py (it raises with a line number on any violation):
 *   - one object per entry; each field on its own line as `key: <literal>,`;
 *   - strings are single-quoted with no backslash or inner quote (use the typographic apostrophe, U+2019);
 *   - `modules` and `docs` are single-line arrays of single-quoted strings;
 *   - `gettingStarted` is optional; when present it is a single-quoted topic slug on its own line;
 *   - MODULE_DOCS has one `'module': 'Slug',` pair per line, no duplicate keys;
 *   - no comments inside the GROUPS, PROJECTS or MODULE_DOCS literals;
 *   - PROJECTS is in display order, and each group's entries are contiguous, in GROUPS order.
 */

export type Group = {
  id: string;
  label: string;
};

export type Project = {
  slug: string;
  name: string;
  group: string;
  tagline: string;
  modules: string[];
  docs: string[];
  gettingStarted?: string;
};

export const GROUPS: Group[] = [
  {
    id: 'data',
    label: 'Data',
  },
  {
    id: 'web',
    label: 'Web',
  },
  {
    id: 'foundations',
    label: 'Foundations',
  },
];

export const PROJECTS: Project[] = [
  {
    slug: 'marshall',
    name: 'Juneau Marshall',
    group: 'data',
    tagline: 'One bean. Every format.',
    modules: ['juneau-marshall', 'juneau-marshall-rdf'],
    docs: ['JuneauMarshall'],
    gettingStarted: 'GettingStartedSerializer',
  },
  {
    slug: 'beans',
    name: 'Juneau Beans',
    group: 'data',
    tagline: 'Ready-made beans for the specs you already use.',
    modules: ['juneau-bean-openapi-v3', 'juneau-bean-swagger-v2', 'juneau-bean-jsonschema', 'juneau-bean-html5', 'juneau-bean-atom', 'juneau-bean-hal', 'juneau-bean-jsonapi', 'juneau-bean-jsonrpc', 'juneau-bean-jsonpatch', 'juneau-bean-rfc7807', 'juneau-bean-mcp-v20250618', 'juneau-bean-mcp-v20260728', 'juneau-bean-common'],
    docs: ['JuneauBean'],
  },
  {
    slug: 'bean-query',
    name: 'Juneau Bean Query',
    group: 'data',
    tagline: 'Search, sort, project and page beans with one query, in memory or in a database.',
    modules: ['juneau-commons', 'juneau-beanquery-sql', 'juneau-beanquery-postgres'],
    docs: ['JuneauCommonsBeanQuery'],
  },
  {
    slug: 'rest-server',
    name: 'Juneau REST Server',
    group: 'web',
    tagline: 'Annotate a POJO and serve it as REST in every format Juneau speaks.',
    modules: ['juneau-rest-server', 'juneau-rest-common', 'juneau-microservice', 'juneau-microservice-jetty', 'juneau-microservice-tomcat', 'juneau-rest-server-springboot', 'juneau-rest-mock', 'juneau-rest-server-auth-jwt', 'juneau-rest-server-auth-oauth', 'juneau-rest-server-auth-oidc-rp', 'juneau-rest-server-auth-saml', 'juneau-rest-server-metrics-micrometer', 'juneau-rest-server-tracing-otel'],
    docs: ['JuneauRestServer'],
    gettingStarted: 'GettingStartedRestApp',
  },
  {
    slug: 'rest-client',
    name: 'Juneau REST Client',
    group: 'web',
    tagline: 'Call REST APIs through typed Java interfaces.',
    modules: ['juneau-rest-client', 'juneau-rest-client-apache-httpclient-45', 'juneau-rest-client-apache-httpclient-50', 'juneau-rest-client-jetty', 'juneau-rest-client-okhttp', 'juneau-rest-client-classic'],
    docs: ['NextGenRestClient', 'JuneauRestClient'],
    gettingStarted: 'GettingStartedClient',
  },
  {
    slug: 'console',
    name: 'Juneau Console',
    group: 'web',
    tagline: 'Browser admin consoles built from your REST resources.',
    modules: ['juneau-console', 'juneau-console-freemarker', 'juneau-console-views', 'juneau-console-views-markdown', 'juneau-console-widgets', 'juneau-console-datatables', 'juneau-console-bus', 'juneau-console-bus-websocket', 'juneau-rest-server-view-jsp', 'juneau-rest-server-view-thymeleaf', 'juneau-rest-server-view-mustache', 'juneau-rest-server-view-freemarker'],
    docs: ['AdminConsoleChrome'],
  },
  {
    slug: 'ai',
    name: 'Juneau AI',
    group: 'web',
    tagline: 'Expose your services to AI agents over MCP.',
    modules: ['juneau-rest-server-mcp', 'juneau-rest-server-mcp-v20250618', 'juneau-rest-server-mcp-v20260728', 'juneau-rest-client-mcp', 'juneau-rest-client-mcp-auth', 'juneau-rest-client-mcp-v20250618', 'juneau-rest-client-mcp-v20260728'],
    docs: ['JuneauMcp', 'JuneauMcpQuickstart'],
  },
  {
    slug: 'commons',
    name: 'Juneau Commons',
    group: 'foundations',
    tagline: 'The reflection, collection and settings utilities Juneau is built on.',
    modules: ['juneau-commons'],
    docs: ['JuneauCommons'],
  },
  {
    slug: 'config',
    name: 'Juneau Config',
    group: 'foundations',
    tagline: 'Configuration files as POJOs, with variables and live reload.',
    modules: ['juneau-config'],
    docs: ['JuneauConfig'],
  },
  {
    slug: 'bct',
    name: 'Juneau BCT',
    group: 'foundations',
    tagline: 'Bean-Centric Testing: assert on whole beans in one line.',
    modules: ['juneau-bct'],
    docs: ['BeanCentricTesting'],
  },
  {
    slug: 'terminal',
    name: 'Juneau Terminal',
    group: 'foundations',
    tagline: 'Live terminal boards for builds, tests and scripts.',
    modules: ['juneau-terminal', 'juneau-terminal-report', 'juneau-terminal-region', 'juneau-terminal-bus'],
    docs: ['JuneauTerminal'],
  },
];

// Module name -> topic slug, for the modules whose topic page clearly covers them.  Unmapped modules render as plain code.
export const MODULE_DOCS: Record<string, string> = {
  'juneau-marshall': 'JuneauMarshall',
  'juneau-marshall-rdf': 'JuneauMarshallRdf',
  'juneau-bean-openapi-v3': 'JuneauBeanOpenApi3',
  'juneau-bean-swagger-v2': 'JuneauBeanSwagger2',
  'juneau-bean-jsonschema': 'JuneauBeanJsonSchema',
  'juneau-bean-html5': 'JuneauBeanHtml5',
  'juneau-bean-atom': 'JuneauBeanAtom',
  'juneau-bean-hal': 'JuneauBeanHal',
  'juneau-bean-jsonapi': 'JuneauBeanJsonApi',
  'juneau-bean-jsonrpc': 'JuneauBeanJsonRpc',
  'juneau-bean-jsonpatch': 'JuneauBeanJsonPatch',
  'juneau-bean-rfc7807': 'JuneauBeanRfc7807',
  'juneau-bean-mcp-v20250618': 'JuneauBeanMcp',
  'juneau-bean-mcp-v20260728': 'JuneauBeanMcp',
  'juneau-bean-common': 'JuneauBeanCommon',
  'juneau-commons': 'JuneauCommons',
  'juneau-rest-server': 'JuneauRestServer',
  'juneau-rest-common': 'JuneauRestCommon',
  'juneau-microservice': 'JuneauMicroservice',
  'juneau-microservice-jetty': 'JuneauMicroserviceJetty',
  'juneau-microservice-tomcat': 'JuneauMicroserviceTomcat',
  'juneau-rest-server-springboot': 'JuneauRestServerSpringboot',
  'juneau-rest-mock': 'JuneauRestMock',
  'juneau-rest-server-auth-oauth': 'OAuthAuthSupport',
  'juneau-rest-server-auth-oidc-rp': 'OidcRelyingParty',
  'juneau-rest-server-auth-saml': 'SamlAuthSupport',
  'juneau-rest-server-metrics-micrometer': 'RestServerMicrometerMetrics',
  'juneau-rest-server-tracing-otel': 'RestServerOtelTracing',
  'juneau-rest-client': 'JuneauRestClient',
  'juneau-rest-client-apache-httpclient-45': 'RestClientApacheHttpClient45',
  'juneau-rest-client-apache-httpclient-50': 'RestClientApacheHttpClient50',
  'juneau-rest-client-jetty': 'RestClientJetty',
  'juneau-rest-client-okhttp': 'RestClientOkHttp',
  'juneau-console-datatables': 'DataTables',
  'juneau-console-bus': 'ConsoleMessageBus',
  'juneau-rest-server-view-jsp': 'JspViewSupport',
  'juneau-rest-server-view-thymeleaf': 'ThymeleafViewSupport',
  'juneau-rest-server-view-mustache': 'MustacheViewSupport',
  'juneau-rest-server-view-freemarker': 'FreemarkerViewSupport',
  'juneau-rest-server-mcp': 'JuneauRestServerMcp',
  'juneau-rest-client-mcp': 'JuneauRestClientMcp',
  'juneau-config': 'JuneauConfig',
  'juneau-bct': 'JuneauBct',
  'juneau-terminal': 'JuneauTerminal',
  'juneau-terminal-region': 'TerminalRegion',
};

export function topicForModule(module: string): string | undefined {
  return MODULE_DOCS[module];
}

export function projectBySlug(slug: string): Project | undefined {
  return PROJECTS.find((p) => p.slug === slug);
}
