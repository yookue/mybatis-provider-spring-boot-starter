/*
 * Copyright (c) 2020 Yookue Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yookue.springstarter.mybatisprovider.config;


import javax.annotation.Nonnull;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.session.SqlSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.yookue.springstarter.mybatisprovider.property.MybatisProviderProperties;
import com.yookue.springstarter.mybatisprovider.util.MybatisProviderUtils;


/**
 * Configuration for {@link org.apache.ibatis.mapping.DatabaseIdProvider}
 *
 * @author David Hsing
 * @reference "http://www.mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/"
 * @reference "http://www.mybatis.org/mybatis-3/zh/configuration.html#databaseIdProvider"
 * @reference "http://www.mybatis.org/mybatis-3/dynamic-sql.html#Multi-db vendor support"
 * @see org.apache.ibatis.mapping.DatabaseIdProvider
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = MybatisProviderAutoConfiguration.PROPERTIES_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(value = SqlSession.class)
@EnableConfigurationProperties(value = MybatisProviderProperties.class)
@SuppressWarnings({"JavadocDeclaration", "JavadocLinkAsPlainText"})
public class MybatisProviderAutoConfiguration {
    public static final String PROPERTIES_PREFIX = "spring.mybatis-provider";    // $NON-NLS-1$

    @Bean
    @ConditionalOnMissingBean
    public DatabaseIdProvider databaseIdProvider(@Nonnull MybatisProviderProperties properties) throws Exception {
        DatabaseIdProvider provider = new VendorDatabaseIdProvider();
        provider.setProperties(MybatisProviderUtils.getProviderProperties(properties.getConfigFile()));
        return provider;
    }
}
