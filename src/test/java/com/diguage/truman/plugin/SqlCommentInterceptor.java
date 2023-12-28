/*
 *    Copyright 2009-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.diguage.truman.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Properties;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { java.sql.Connection.class,
    Integer.class }) })
public class SqlCommentInterceptor implements Interceptor, EnvironmentAware {

  private String applicationName = System.getProperty("spring.application.name", "unknown-app");

  // 统一定义项目的根包名，用于过滤堆栈
  private static final String BUSINESS_PACKAGE_PREFIX = "com.diguage.";

  // 定义需要过滤掉的框架级包名前缀列表
  private static final String[] FRAMEWORK_PREFIXES = { "java.", "jdk.", "javax.", "sun.", "com.sun.", "org.aspectj.",
      "org.springframework.", "org.apache.ibatis.", "org.mybatis.", "com.baomidou.", "p6spy.", "com.mysql.",
      "org.postgresql.", "com.alibaba.druid.", "com.zaxxer.hikari.", "com.diguage.truman.plugin." };

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    // 1. 拿到当前拦截到的目标对象（可能是被多层插件代理后的 Proxy 对象）
    Object target = invocation.getTarget();

    // 2. 使用 JDK 公开 API 循环剥离动态代理，直到拿到最底层的真实 StatementHandler
    while (Proxy.isProxyClass(target.getClass())) {
      InvocationHandler handler = Proxy.getInvocationHandler(target);
      if (handler instanceof Plugin) {
        // Plugin 是 MyBatis 的类，反射访问它的 target 属性是安全的，不会触发 JDK 强封装报错
        target = SystemMetaObject.forObject(handler).getValue("target");
      } else {
        // 如果被非 MyBatis 插件的其它 JDK 代理包装，按需处理或直接跳出
        break;
      }
    }

    // 3. 此时的 target 已经是真实的 StatementHandler 实现类（通常是 RoutingStatementHandler）
    MetaObject metaObject = SystemMetaObject.forObject(target);

    // 4. 安全地获取 BoundSql 并修改 SQL
    BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
    String originalSql = boundSql.getSql();

    // 5. 获取 Trace ID
    String traceId = TraceIdHelper.getTraceId();

    // 6. 核心：提取 MappedStatement 获取 Mapper 方法信息
    // MappedStatement 存储在 delegate 中
    MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

    // mappedStatement.getId() 返回形如: com.example.mapper.UserMapper.selectByStatus
    String mapperMethodId = mappedStatement.getId();

    // 【可选美化】如果你嫌全路径太长，可以只截取 类名.方法名
    String shortMethodName = mapperMethodId
        .substring(mapperMethodId.lastIndexOf(".", mapperMethodId.lastIndexOf(".") - 1) + 1);

    // 7. 核心：获取上层业务调用者
    String businessCaller = findUniversalCaller();

    // 8. 组装注释信息，采用标准的 SQL 块注释符，注意末尾留一个空格
    String comment = String.format("/* app=%s, caller=%s, method=%s, traceId=%s */ ", applicationName, businessCaller,
        shortMethodName, traceId);

    // 9. 拼接新 SQL
    String newSql = comment + originalSql;

    // 10. 使用反射将新 SQL 写回 BoundSql
    metaObject.setValue("delegate.boundSql.sql", newSql);

    // 11. 继续执行下一个拦截器或目标方法
    return invocation.proceed();
  }

  /**
   * 自适应堆栈搜索：排除法寻找非框架代码的第一个调用者
   */
  private String findUniversalCaller() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    for (StackTraceElement element : stackTrace) {
      String className = element.getClassName();

      // 1. 跳过当前拦截器类自身
      if (className.equals(this.getClass().getName()) || className.contains("$Proxy")) {
        continue;
      }

      // 2. 检查是否属于需要过滤的底层中间件/框架包
      boolean isFramework = false;
      for (String prefix : FRAMEWORK_PREFIXES) {
        if (className.startsWith(prefix)) {
          isFramework = true;
          break;
        }
      }

      // 3. 如果不是框架包，说明踩到了“用户真正的业务起始调用点”
      if (!isFramework) {
        String shortClassName = className.substring(className.lastIndexOf(".") + 1);
        // 返回格式：类名.方法名:行号 (例如: OrderJob.execute:24 或 UserListener.onMessage:45)
        return shortClassName + "." + element.getMethodName() + ":" + element.getLineNumber();
      }
    }
    return "UnknownCaller";
  }

  @Override
  public Object plugin(Object target) {
    // 使用 MyBatis 提供的 Plugin.wrap 生成代理对象
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
    // 可以接收 mybatis-config.xml 中配置的属性
    // 从配置文件中读取参数，例如 <property name="appName" value="myApp"/>
    if (properties != null) {
      // 优先查找用户显式命名的 applicationName
      String appName = properties.getProperty("applicationName");
      // 兼容有些用户习惯在 XML 里直接写明 spring.application.name
      if (appName == null) {
        appName = properties.getProperty("spring.application.name");
      }

      // 如果 XML 中有显式配置，则覆盖 Spring 的默认环境变量（配置优先原则）
      if (appName != null && !appName.isEmpty()) {
        this.applicationName = appName;
      }
    }
  }

  @Override
  public void setEnvironment(Environment environment) {
    if (environment != null) {
      // 从 Spring 统一的环境变量（application.yml / 环境变量 / 命令行参数）中获取应用名
      String springAppName = environment.getProperty("spring.application.name");
      if (springAppName != null && !springAppName.isEmpty()) {
        this.applicationName = springAppName;
      }
    }
  }

  // // 可以从配置中传入应用名，也可以通过其他方式动态获取
  // private String appName;
  //
  // @Override
  // public Object intercept(Invocation invocation) throws Throwable {
  // StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
  // BoundSql boundSql = statementHandler.getBoundSql();
  // String originalSql = boundSql.getSql();
  //
  // // 如果 SQL 已经包含注释，可以跳过（可选）
  // if (originalSql.trim().startsWith("/*")) {
  // return invocation.proceed();
  // }
  //
  // // 生成带注释的 SQL
  // String commentedSql = buildCommentedSql(originalSql);
  //
  // // 通过反射修改 BoundSql 中的 sql 字段
  // setFieldValue(boundSql, "sql", commentedSql);
  //
  // return invocation.proceed();
  // }
  //
  // private String buildCommentedSql(String originalSql) {
  // // 可以添加时间戳、应用名、请求ID等，从 ThreadLocal 中获取（见后续说明）
  // String comment = String.format("/* app=%s, time=%d */ ", appName, System.currentTimeMillis());
  // return comment + originalSql;
  // }
  //
  // /**
  // * 利用反射修改私有字段的值
  // */
  // private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
  // Field field = target.getClass().getDeclaredField(fieldName);
  // field.setAccessible(true);
  // field.set(target, value);
  // }
  //
  // @Override
  // public Object plugin(Object target) {
  // // 使用 MyBatis 自带的 Plugin.wrap 方法生成代理对象
  // return Plugin.wrap(target, this);
  // }
  //
  // @Override
  // public void setProperties(Properties properties) {
  // // 从配置文件中读取参数，例如 <property name="appName" value="myApp"/>
  // this.appName = properties.getProperty("appName", "unknown");
  // }
}
