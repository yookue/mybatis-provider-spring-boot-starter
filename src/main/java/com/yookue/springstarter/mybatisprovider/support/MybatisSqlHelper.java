/*
 * Mybatis SqlHelper of Mybatis_Utils
 * @author abel533
 *
 * https://gitee.com/free/Mybatis_Utils/tree/master/SqlHelper
 */


package com.yookue.springstarter.mybatisprovider.support;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;


/**
 * Mybatis SqlHelper
 *
 * @author abel533
 * @reference "https://blog.csdn.net/isea533/article/details/40044417"
 * @reference "https://blog.csdn.net/dalinsi/article/details/51919160"
 */
@SuppressWarnings({"unused", "WeakerAccess", "JavadocDeclaration", "JavadocLinkAsPlainText"})
public abstract class MybatisSqlHelper {
    @Nullable
    public static String getMapperSql(@Nullable Object mapper, @Nullable String methodName, @Nullable Object... args) {
        if (mapper == null || StringUtils.isBlank(methodName)) {
            return null;
        }
        MetaObject metaObject = SystemMetaObject.forObject(mapper);
        SqlSession sqlSession = (SqlSession) metaObject.getValue("h.sqlSession");    // $NON-NLS-1$
        Class<?> mapperClazz = (Class<?>) metaObject.getValue("h.mapperInterface");    // $NON-NLS-1$
        String fullMethodName = mapperClazz.getCanonicalName() + "." + methodName;    // $NON-NLS-1$
        return ArrayUtils.isEmpty(args) ? getNamespaceSql(sqlSession, fullMethodName, null) : getMapperSql(sqlSession, mapperClazz, methodName, args);
    }

    @Nullable
    public static String getMapperSql(@Nullable SqlSession session, @Nullable String fullName, @Nullable Object... args) {
        if (session == null || StringUtils.isBlank(fullName)) {
            return null;
        }
        if (ArrayUtils.isEmpty(args)) {
            return getNamespaceSql(session, fullName, null);
        }
        String method = StringUtils.substring(fullName, fullName.lastIndexOf('.') + 1);
        try {
            Class<?> clazz = ClassUtils.getClass(StringUtils.substring(fullName, 0, fullName.lastIndexOf('.')));
            return getMapperSql(session, clazz, method, args);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions"})
    public static String getMapperSql(@Nullable SqlSession session, @Nullable Class<?> mapperClazz, @Nullable String methodName, @Nullable Object... args) {
        if (ObjectUtils.anyNull(session, mapperClazz) || StringUtils.isBlank(methodName)) {
            return null;
        }
        String fullName = mapperClazz.getCanonicalName() + "." + methodName;
        if (ArrayUtils.isEmpty(args)) {
            return getNamespaceSql(session, fullName, null);
        }
        Method method = ReflectionUtils.findMethod(mapperClazz, methodName);
        if (method == null) {
            return null;
        }
        final Class<?>[] argTypes = method.getParameterTypes();
        Map<String, Object> argValues = new HashMap<>();
        for (int i = 0; i < argTypes.length; i++) {
            if (!RowBounds.class.isAssignableFrom(argTypes[i]) && !ResultHandler.class.isAssignableFrom(argTypes[i])) {
                String paramName = "param" + (argValues.size() + 1);    // $NON-NLS-1$
                paramName = getAnnotationParamName(method, i, paramName);
                argValues.put(paramName, (i >= args.length) ? null : args[i]);
            }
        }
        if (ArrayUtils.getLength(args) == 1) {
            Object firstParam = wrapCollectionObject(args[0]);
            if (firstParam instanceof Map) {
                argValues.putAll((Map) firstParam);
            }
        }
        return getNamespaceSql(session, fullName, argValues);
    }

    public static String getNamespaceSql(@Nullable SqlSession session, @Nullable String namespace) {
        return getNamespaceSql(session, namespace, null);
    }

    @Nullable
    public static String getNamespaceSql(@Nullable SqlSession session, @Nullable String namespace, @Nullable Object params) {
        if (session == null || StringUtils.isBlank(namespace)) {
            return null;
        }
        params = wrapCollectionObject(params);
        Configuration configuration = session.getConfiguration();
        MappedStatement statement = configuration.getMappedStatement(namespace);
        TypeHandlerRegistry registry = statement.getConfiguration().getTypeHandlerRegistry();
        BoundSql boundSql = statement.getBoundSql(params);
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql();
        if (CollectionUtils.isEmpty(mappings)) {
            return null;
        }
        for (ParameterMapping mapping : mappings) {
            if (mapping.getMode() == ParameterMode.OUT) {
                continue;
            }
            Object value;
            String property = mapping.getProperty();
            if (boundSql.hasAdditionalParameter(property)) {
                value = boundSql.getAdditionalParameter(property);
            } else if (params == null) {
                value = null;
            } else if (registry.hasTypeHandler(params.getClass())) {
                value = params;
            } else {
                MetaObject metaObject = configuration.newMetaObject(params);
                value = metaObject.getValue(property);
            }
            JdbcType jdbcType = mapping.getJdbcType();
            if (value == null && jdbcType == null) {
                jdbcType = configuration.getJdbcTypeForNull();
            }
            sql = replaceParameter(sql, value, jdbcType, mapping.getJavaType());
        }
        return sql;
    }

    private static String replaceParameter(@Nullable String sql, @Nullable Object value, @Nullable JdbcType jdbcType, @Nullable Class<?> javaType) {
        String strValue = String.valueOf(value);
        if (jdbcType != null) {
            switch (jdbcType) {
                case BIT:
                case TINYINT:
                case SMALLINT:
                case INTEGER:
                case BIGINT:
                case FLOAT:
                case REAL:
                case DOUBLE:
                case NUMERIC:
                case DECIMAL:
                    break;
                case DATE:
                case TIME:
                case TIMESTAMP:
                default:
                    strValue = StringUtils.join("'", strValue, "'");    // $NON-NLS-1$ // $NON-NLS-2$
                    break;
            }
        } else if (javaType != null && !Number.class.isAssignableFrom(javaType)) {
            strValue = StringUtils.join("'", strValue, "'");    // $NON-NLS-1$ // $NON-NLS-2$
        }
        return RegExUtils.replaceFirst(sql, "\\?", strValue);    // $NON-NLS-1$
    }

    @Nullable
    private static String getAnnotationParamName(@Nullable Method method, int index, @Nullable String paramName) {
        if (method == null || index < 0) {
            return null;
        }
        final Object[] paramAnnos = method.getParameterAnnotations()[index];
        if (ArrayUtils.isEmpty(paramAnnos)) {
            return null;
        }
        for (Object paramAnno : paramAnnos) {
            if (paramAnno instanceof Param) {
                paramName = ((Param) paramAnno).value();
            }
        }
        return paramName;
    }

    @Nullable
    private static Object wrapCollectionObject(@Nullable Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof List) {
            Map<String, Object> map = new HashMap<>();
            map.put("list", object);    // $NON-NLS-1$
            return map;
        } else if (object.getClass().isArray()) {
            Map<String, Object> map = new HashMap<>();
            map.put("array", object);    // $NON-NLS-1$
            return map;
        }
        return object;
    }
}
