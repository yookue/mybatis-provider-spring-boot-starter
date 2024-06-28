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

package com.yookue.springstarter.mybatisprovider.util;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.yookue.commonplexus.javaseutil.constant.StringVariantConst;
import com.yookue.commonplexus.springutil.util.ResourceUtilsWraps;


/**
 * Utilities for mybatis {@link org.apache.ibatis.mapping.DatabaseIdProvider}
 *
 * @author David Hsing
 */
@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted", "UnusedReturnValue"})
public abstract class MybatisProviderUtils {
    @Nonnull
    public static Properties getProviderProperties(@Nullable String configFile) throws Exception {
        if (StringUtils.isBlank(configFile)) {
            throw new IllegalArgumentException("Config file is blank");
        }
        Resource resource = ResourceUtilsWraps.determineResource(configFile);
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("Config file is not exists or readable");
        }
        Properties result = new Properties();
        try (InputStream inputStream = resource.getInputStream()) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(inputStream);
            NodeList nodes = document.getDocumentElement().getElementsByTagName(StringVariantConst.PROPERTY);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }
                Element element = (Element) node;
                String name = element.getAttribute(StringVariantConst.NAME), value = element.getAttribute(StringVariantConst.VALUE);
                if (StringUtils.isNoneBlank(name, value)) {
                    result.put(name, value);
                }
            }
        }
        return result;
    }
}
