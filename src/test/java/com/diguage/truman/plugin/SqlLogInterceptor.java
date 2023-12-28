package com.diguage.truman.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class }) })
public class SqlLogInterceptor implements Interceptor {

  private static final Logger log = LoggerFactory.getLogger(SqlLogInterceptor.class);

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object result;
    long start = System.currentTimeMillis();

    try {
      // 1. 让数据库先执行 SQL
      result = invocation.proceed();
    } finally {
      // 2. 无论成功还是失败，都在 finally 中计算耗时，确保异常时也能打印 SQL 和耗时
      long timing = System.currentTimeMillis() - start;
      try {
        // 3. 安全剥离 JDK 动态代理，获取真实的 StatementHandler
        Object target = invocation.getTarget();
        while (Proxy.isProxyClass(target.getClass())) {
          InvocationHandler handler = Proxy.getInvocationHandler(target);
          if (handler instanceof Plugin) {
            target = SystemMetaObject.forObject(handler).getValue("target");
          } else {
            break;
          }
        }

        MetaObject metaObject = SystemMetaObject.forObject(target);
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        Configuration configuration = (Configuration) metaObject.getValue("delegate.configuration");

        // 4. 组装完整 SQL（此时前一个插件添加的注释已经在这条 SQL 里面了）
        String completeSql = generateCompleteSql(configuration, boundSql);

        // 5. 打印 SQL 和 耗时
        log.info("\n================  MyBatis SQL Execution  ================\n" + "Execution Time: {} ms\n"
            + "SQL: {}\n" + "=========================================================", timing, completeSql);
      } catch (Exception e) {
        log.error("SqlLogInterceptor 解析完整 SQL 失败", e);
      }
    }

    return result;
  }

  /**
   * 将带有 ? 占位符的 SQL 还原为填充了真实参数的完整 SQL（保持不变）
   */
  private String generateCompleteSql(Configuration configuration, BoundSql boundSql) {
    String sql = boundSql.getSql();
    if (sql == null || sql.isEmpty()) {
      return "";
    }
    sql = sql.replaceAll("[\\s\n\r\t]+", " ").trim();

    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    Object parameterObject = boundSql.getParameterObject();

    if (parameterMappings != null && !parameterMappings.isEmpty() && parameterObject != null) {
      TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
      if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
        sql = replacePlaceholder(sql, getParameterValue(parameterObject));
      } else {
        MetaObject metaObject = configuration.newMetaObject(parameterObject);
        for (ParameterMapping parameterMapping : parameterMappings) {
          String propertyName = parameterMapping.getProperty();
          Object value;
          if (metaObject.hasGetter(propertyName)) {
            value = metaObject.getValue(propertyName);
          } else if (boundSql.hasAdditionalParameter(propertyName)) {
            value = boundSql.getAdditionalParameter(propertyName);
          } else {
            value = "N/A";
          }
          sql = replacePlaceholder(sql, getParameterValue(value));
        }
      }
    }
    return sql;
  }

  private String replacePlaceholder(String sql, String propertyValue) {
    return sql.replaceFirst("\\?", Matcher.quoteReplacement(propertyValue));
  }

  private String getParameterValue(Object obj) {
    if (obj == null)
      return "null";
    if (obj instanceof String)
      return "'" + obj + "'";
    if (obj instanceof Date) {
      DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
      return "'" + formatter.format((Date) obj) + "'";
    }
    return obj.toString();
  }

  // @Override
  // public Object intercept(Invocation invocation) throws Throwable {
  // try {
  // // 1. 安全剥离 JDK 动态代理，获取真实的 StatementHandler
  // Object target = invocation.getTarget();
  // while (Proxy.isProxyClass(target.getClass())) {
  // InvocationHandler handler = Proxy.getInvocationHandler(target);
  // if (handler instanceof Plugin) {
  // target = SystemMetaObject.forObject(handler).getValue("target");
  // } else {
  // break;
  // }
  // }
  //
  // // 2. 获取 MetaObject 以读取内部属性
  // MetaObject metaObject = SystemMetaObject.forObject(target);
  //
  // // 3. 提取出核心对象：BoundSql 和 Configuration
  // BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
  // // 通过 RoutingStatementHandler 内部的 delegate 拿到了代理前的真正的 Configuration
  // Configuration configuration = (Configuration) metaObject.getValue("delegate.configuration");
  //
  // // 4. 组装并格式化完整的 SQL
  // String completeSql = generateCompleteSql(configuration, boundSql);
  //
  // // 5. 打印 SQL 日志
  // log.info("\n================ MyBatis Executing SQL ================\n" +
  // "{}\n" +
  // "=========================================================", completeSql);
  //
  // } catch (Exception e) {
  // // 打印日志本身不能影响主业务流程的执行
  // log.error("SqlLogInterceptor 还原完整 SQL 失败", e);
  // }
  //
  // // 继续执行 MyBatis 原有逻辑，不干涉数据库操作
  // return invocation.proceed();
  // }
  //
  // /**
  // * 将带有 ? 占位符的 SQL 还原为填充了真实参数的完整 SQL
  // */
  // private String generateCompleteSql(Configuration configuration, BoundSql boundSql) {
  // String sql = boundSql.getSql();
  // // 如果没有 SQL 语句直接返回
  // if (sql == null || sql.isEmpty()) {
  // return "";
  // }
  //
  // // 美化 SQL：将多个连续的空格、换行符压缩为一个空格
  // sql = sql.replaceAll("[\\s\n\r\t]+", " ").trim();
  //
  // // 获取参数映射列表
  // List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
  // // 获取运行时传入的参数对象
  // Object parameterObject = boundSql.getParameterObject();
  //
  // if (parameterMappings != null && !parameterMappings.isEmpty() && parameterObject != null) {
  // TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
  //
  // // 如果参数对象本身有对应的 TypeHandler（说明是简单类型，如单个 String, Integer 等）
  // if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
  // sql = replacePlaceholder(sql, getParameterValue(parameterObject));
  // } else {
  // // 如果是复杂对象（如实体类、Map 等）
  // MetaObject metaObject = configuration.newMetaObject(parameterObject);
  // for (ParameterMapping parameterMapping : parameterMappings) {
  // String propertyName = parameterMapping.getProperty();
  // Object value;
  // if (metaObject.hasGetter(propertyName)) {
  // value = metaObject.getValue(propertyName);
  // } else if (boundSql.hasAdditionalParameter(propertyName)) {
  // value = boundSql.getAdditionalParameter(propertyName);
  // } else {
  // value = "N/A"; // 兜底处理
  // }
  // sql = replacePlaceholder(sql, getParameterValue(value));
  // }
  // }
  // }
  // return sql;
  // }
  //
  // /**
  // * 将原本的第一个 '?' 替换成具体格式化后的参数值
  // */
  // private String replacePlaceholder(String sql, String propertyValue) {
  // // 使用 Matcher.quoteReplacement 避免参数值内部含有特殊字符（如 $）导致正则报错
  // return sql.replaceFirst("\\?", Matcher.quoteReplacement(propertyValue));
  // }
  //
  // /**
  // * 核心逻辑：对不同类型的参数值进行格式化转换（如给字符串和日期加上单引号）
  // */
  // private String getParameterValue(Object obj) {
  // if (obj == null) {
  // return "null";
  // }
  // if (obj instanceof String) {
  // return "'" + obj + "'";
  // }
  // if (obj instanceof Date) {
  // DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
  // return "'" + formatter.format((Date) obj) + "'";
  // }
  // // 对于数字、布尔等类型，直接返回其字符串形式
  // return obj.toString();
  // }
  //
  // @Override
  // public Object plugin(Object target) {
  // return Plugin.wrap(target, this);
  // }
  //
  // @Override
  // public void setProperties(Properties properties) {
  // }
}
