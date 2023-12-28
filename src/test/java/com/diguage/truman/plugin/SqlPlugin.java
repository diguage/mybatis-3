package com.diguage.truman.plugin;

import java.sql.Statement;
import java.util.Properties;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 打印填充完参数的 SQL 语句
 * <p>
 * TODO： 可以考虑格式化一下
 */
@Intercepts({
    // @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = StatementHandler.class, method = "query", args = { Statement.class, ResultHandler.class }) })
public class SqlPlugin implements Interceptor {
  private static final Logger logger = LoggerFactory.getLogger(SqlPlugin.class);
  private Properties properties = new Properties();

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    logger.info("{}.{}({})", invocation.getTarget().getClass().getName(), invocation.getMethod().getName(),
        invocation.getArgs());
    // implement pre-processing if needed
    Object target = invocation.getTarget();
    StatementHandler statementHandler = (StatementHandler) target;
    Object result = invocation.proceed();
    // implement post-processing if needed
    return result;
  }

  @Override
  public Object plugin(Object target) {
    logger.info("class: {}", target.getClass().getName());
    return Interceptor.super.plugin(target);
  }

  @Override
  public void setProperties(Properties properties) {
    this.properties = properties;
  }
}
