/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.graphql.boot;

import java.util.Collections;
import java.util.stream.Collectors;

import graphql.GraphQL;
import graphql.schema.idl.SchemaPrinter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.GraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.web.WebGraphQlHandler;
import org.springframework.graphql.web.WebInterceptor;
import org.springframework.graphql.web.webflux.GraphQlHttpHandler;
import org.springframework.graphql.web.webflux.GraphQlWebSocketHandler;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketUpgradeHandlerPredicate;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for enabling Spring GraphQL over
 * WebFlux.
 *
 * @author Brian Clozel
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(GraphQL.class)
@ConditionalOnBean(GraphQlSource.class)
@AutoConfigureAfter(GraphQlServiceAutoConfiguration.class)
public class GraphQlWebFluxAutoConfiguration {

	private static final Log logger = LogFactory.getLog(GraphQlWebFluxAutoConfiguration.class);

	@Bean
	@ConditionalOnBean(GraphQlService.class)
	@ConditionalOnMissingBean
	public WebGraphQlHandler webGraphQlHandler(GraphQlService service, ObjectProvider<WebInterceptor> interceptors) {
		return WebGraphQlHandler.builder(service)
				.interceptors(interceptors.orderedStream().collect(Collectors.toList())).build();
	}

	@Bean
	@ConditionalOnMissingBean
	public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
		return new GraphQlHttpHandler(webGraphQlHandler);
	}

	@Bean
	public RouterFunction<ServerResponse> graphQlEndpoint(GraphQlHttpHandler handler, GraphQlSource graphQlSource,
			GraphQlProperties properties, ResourceLoader resourceLoader) {
		String graphQLPath = properties.getPath();
		if (logger.isInfoEnabled()) {
			logger.info("GraphQL endpoint HTTP POST " + graphQLPath);
		}
		// @formatter:off
		RouterFunctions.Builder builder = RouterFunctions.route()
				.POST(graphQLPath, accept(MediaType.APPLICATION_JSON).and(contentType(MediaType.APPLICATION_JSON)), handler::handleRequest);
		if (properties.getGraphiql().isEnabled()) {
			Resource resource = resourceLoader.getResource("classpath:graphiql/index.html");
			GraphiQlWebFluxHandler graphiQlHandler = new GraphiQlWebFluxHandler(graphQLPath, resource);
			builder = builder.GET(properties.getGraphiql().getPath(), graphiQlHandler::showGraphiQlPage);
		}
		if (properties.getSchema().getPrinter().isEnabled()) {
			SchemaPrinter printer = new SchemaPrinter();
			builder = builder.GET(graphQLPath + properties.getSchema().getPrinter().getPath(),
					(req) -> ServerResponse.ok()
							.contentType(MediaType.TEXT_PLAIN)
							.bodyValue(printer.print(graphQlSource.schema())));
		}
		// @formatter:on
		return builder.build();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.graphql.websocket", name = "path")
	public static class WebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public GraphQlWebSocketHandler graphQlWebSocketHandler(WebGraphQlHandler webGraphQlHandler,
				GraphQlProperties properties, ServerCodecConfigurer configurer) {
			return new GraphQlWebSocketHandler(webGraphQlHandler, configurer,
					properties.getWebsocket().getConnectionInitTimeout());
		}

		@Bean
		public HandlerMapping graphQlWebSocketEndpoint(GraphQlWebSocketHandler graphQlWebSocketHandler,
				GraphQlProperties properties) {
			String path = properties.getWebsocket().getPath();
			if (logger.isInfoEnabled()) {
				logger.info("GraphQL endpoint WebSocket " + path);
			}
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setHandlerPredicate(new WebSocketUpgradeHandlerPredicate());
			mapping.setUrlMap(Collections.singletonMap(path, graphQlWebSocketHandler));
			mapping.setOrder(-2); // Ahead of HTTP endpoint ("routerFunctionMapping" bean)
			return mapping;
		}

	}

}
