/*
 * Mybatis SqlMapper of Mybatis_Utils
 * @author abel533
 *
 * https://gitee.com/free/Mybatis_Utils/tree/master/SqlMapper
 */


package com.yookue.springstarter.mybatisprovider.support;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


/**
 * Mybatis SqlMapper
 *
 * @author abel533
 * @reference "https://blog.csdn.net/isea533/article/details/40044417"
 * @reference "https://blog.csdn.net/dalinsi/article/details/51919160"
 */
@SuppressWarnings({"unused", "WeakerAccess", "JavadocDeclaration", "JavadocLinkAsPlainText"})
public class MybatisSqlMapper {
    private final StatementUtils statementUtils;
    private final SqlSession sqlSession;

    public MybatisSqlMapper(@Nonnull SqlSession sqlSession) {
        this.sqlSession = sqlSession;
        if (sqlSession.getConfiguration() != null) {
            this.statementUtils = new StatementUtils(sqlSession.getConfiguration());
        } else {
            this.statementUtils = null;
        }
    }

    public Map<String, Object> selectOne(@Nonnull String sql) {
        return getOne(selectList(sql));
    }

    public Map<String, Object> selectOne(@Nonnull String sql, @Nullable Object param) {
        return getOne(selectList(sql, param));
    }

    public <T> T selectOne(@Nonnull String sql, @Nonnull Class<T> resultType) {
        return getOne(selectList(sql, resultType));
    }

    public <T> T selectOne(@Nonnull String sql, @Nullable Object param, @Nonnull Class<T> resultType) {
        return getOne(selectList(sql, param, resultType));
    }

    public List<Map<String, Object>> selectList(@Nonnull String sql) {
        return sqlSession.selectList(statementUtils.select(sql));
    }

    public List<Map<String, Object>> selectList(@Nonnull String sql, @Nullable Object param) {
        Class<?> parameterType = (param != null) ? param.getClass() : null;
        String statementId = statementUtils.selectDynamic(sql, parameterType);
        return sqlSession.selectList(statementId, param);
    }

    public <T> List<T> selectList(@Nonnull String sql, @Nonnull Class<T> resultType) {
        return sqlSession.selectList(statementUtils.select(sql, resultType));
    }

    public <T> List<T> selectList(@Nonnull String sql, @Nullable Object param, @Nonnull Class<T> resultType) {
        Class<?> parameterType = (param != null) ? param.getClass() : null;
        return sqlSession.selectList(statementUtils.selectDynamic(sql, parameterType, resultType), param);
    }

    public int insert(@Nonnull String sql) {
        return sqlSession.insert(statementUtils.insert(sql));
    }

    public int insert(@Nonnull String sql, @Nullable Object param) {
        Class<?> parameterType = (param != null) ? param.getClass() : null;
        String statementId = statementUtils.insertDynamic(sql, parameterType);
        return sqlSession.insert(statementId, param);
    }

    public int update(@Nonnull String sql) {
        return sqlSession.update(statementUtils.update(sql));
    }

    public int update(@Nonnull String sql, @Nullable Object param) {
        Class<?> parameterType = (param != null) ? param.getClass() : null;
        String statementId = statementUtils.updateDynamic(sql, parameterType);
        return sqlSession.update(statementId, param);
    }

    public int delete(@Nonnull String sql) {
        return sqlSession.delete(statementUtils.delete(sql));
    }

    public int delete(@Nonnull String sql, @Nullable Object param) {
        Class<?> parameterType = (param != null) ? param.getClass() : null;
        String statementId = statementUtils.deleteDynamic(sql, parameterType);
        return sqlSession.delete(statementId, param);
    }

    @Nullable
    private <T> T getOne(@Nonnull List<T> list) {
        int size = list.size();
        if (size == 1) {
            return list.get(0);
        } else if (size > 1) {
            throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found more.");
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static class StatementUtils {
        private final Configuration configuration;
        private final LanguageDriver languageDriver;

        private StatementUtils(@Nonnull Configuration configuration) {
            this.configuration = configuration;
            languageDriver = configuration.getDefaultScriptingLanguageInstance();
        }

        private String newStatementId(@Nonnull String sql, @Nonnull SqlCommandType commandType) {
            return StringUtils.join(commandType.toString(), '.', sql.hashCode());
        }

        private boolean hasMappedStatement(@Nonnull String statementId) {
            return configuration.hasStatement(statementId, false);
        }

        private void newSelectStatement(@Nonnull String statementId, @Nonnull SqlSource sqlSource, @Nonnull Class<?> resultType) {
            MappedStatement statement = new MappedStatement.Builder(configuration, statementId, sqlSource, SqlCommandType.SELECT).resultMaps(new ArrayList<>() {
                {
                    add(new ResultMap.Builder(configuration, "defaultResultMap", resultType, new ArrayList<>(0)).build());    // $NON-NLS-1$
                }
            }).build();
            configuration.addMappedStatement(statement);
        }

        private void newUpdateStatement(@Nonnull String statementId, @Nonnull SqlSource sqlSource, @Nonnull SqlCommandType commandType) {
            MappedStatement statement = new MappedStatement.Builder(configuration, statementId, sqlSource, commandType).resultMaps(new ArrayList<>() {
                {
                    add(new ResultMap.Builder(configuration, "defaultResultMap", int.class, new ArrayList<>(0)).build());    // $NON-NLS-1$
                }
            }).build();
            configuration.addMappedStatement(statement);
        }

        private String select(@Nonnull String sql) {
            String statementId = newStatementId(sql, SqlCommandType.SELECT);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            StaticSqlSource sqlSource = new StaticSqlSource(configuration, sql);
            newSelectStatement(statementId, sqlSource, Map.class);
            return statementId;
        }

        private String select(@Nonnull String sql, @Nonnull Class<?> resultType) {
            String statementId = newStatementId(StringUtils.join(resultType, sql), SqlCommandType.SELECT);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            StaticSqlSource sqlSource = new StaticSqlSource(configuration, sql);
            newSelectStatement(statementId, sqlSource, resultType);
            return statementId;
        }

        private String selectDynamic(@Nonnull String sql, @Nullable Class<?> parameterType) {
            String statementId = newStatementId(StringUtils.join(sql, parameterType), SqlCommandType.SELECT);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, parameterType);
            newSelectStatement(statementId, sqlSource, Map.class);
            return statementId;
        }

        private String selectDynamic(@Nonnull String sql, @Nullable Class<?> parameterType, @Nonnull Class<?> resultType) {
            String statementId = newStatementId(StringUtils.join(resultType, sql, parameterType), SqlCommandType.SELECT);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, parameterType);
            newSelectStatement(statementId, sqlSource, resultType);
            return statementId;
        }

        private String insert(@Nonnull String sql) {
            String statementId = newStatementId(sql, SqlCommandType.INSERT);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            StaticSqlSource sqlSource = new StaticSqlSource(configuration, sql);
            newUpdateStatement(statementId, sqlSource, SqlCommandType.INSERT);
            return statementId;
        }

        private String insertDynamic(@Nonnull String sql, @Nullable Class<?> parameterType) {
            String statementId = newStatementId(StringUtils.join(sql, parameterType), SqlCommandType.INSERT);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, parameterType);
            newUpdateStatement(statementId, sqlSource, SqlCommandType.INSERT);
            return statementId;
        }

        private String update(@Nonnull String sql) {
            String statementId = newStatementId(sql, SqlCommandType.UPDATE);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            StaticSqlSource sqlSource = new StaticSqlSource(configuration, sql);
            newUpdateStatement(statementId, sqlSource, SqlCommandType.UPDATE);
            return statementId;
        }

        private String updateDynamic(@Nonnull String sql, @Nullable Class<?> parameterType) {
            String statementId = newStatementId(StringUtils.join(sql, parameterType), SqlCommandType.UPDATE);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, parameterType);
            newUpdateStatement(statementId, sqlSource, SqlCommandType.UPDATE);
            return statementId;
        }

        private String delete(@Nonnull String sql) {
            String statementId = newStatementId(sql, SqlCommandType.DELETE);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            StaticSqlSource sqlSource = new StaticSqlSource(configuration, sql);
            newUpdateStatement(statementId, sqlSource, SqlCommandType.DELETE);
            return statementId;
        }

        private String deleteDynamic(@Nonnull String sql, @Nullable Class<?> parameterType) {
            String statementId = newStatementId(StringUtils.join(sql, parameterType), SqlCommandType.DELETE);
            if (hasMappedStatement(statementId)) {
                return statementId;
            }
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, parameterType);
            newUpdateStatement(statementId, sqlSource, SqlCommandType.DELETE);
            return statementId;
        }
    }
}
