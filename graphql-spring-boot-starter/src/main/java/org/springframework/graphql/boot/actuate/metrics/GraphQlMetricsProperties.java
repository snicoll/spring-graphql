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

package org.springframework.graphql.boot.actuate.metrics;

import org.springframework.boot.actuate.autoconfigure.metrics.AutoTimeProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * {@link ConfigurationProperties properties} for Spring GraphQL.
 * <p>
 * This class could be later merged with
 * {@link org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties}.
 *
 * @author Brian Clozel
 * @since 1.0.0
 */
@ConfigurationProperties("management.metrics.graphql")
public class GraphQlMetricsProperties {

	/**
	 * Auto-timed queries settings.
	 */
	@NestedConfigurationProperty
	private final AutoTimeProperties autotime = new AutoTimeProperties();

	public AutoTimeProperties getAutotime() {
		return this.autotime;
	}

}
