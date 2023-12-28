package com.diguage.truman.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceIdHelper {

  private static final Logger log = LoggerFactory.getLogger(TraceIdHelper.class);

  // 预先动态检测各大链路追踪组件是否存在于当前项目的 classpath 中
  private static final boolean IS_OTEL_PRESENT;
  private static final boolean IS_SKYWALKING_PRESENT;

  static {
    IS_OTEL_PRESENT = isClassPresent("io.opentelemetry.api.trace.Span");
    IS_SKYWALKING_PRESENT = isClassPresent("org.apache.skywalking.apm.toolkit.trace.TraceContext");
  }

  /**
   * 自适应获取当前线程的 TraceId
   */
  public static String getTraceId() {
    try {
      // 1. 优先判定并从 OpenTelemetry 中获取
      if (IS_OTEL_PRESENT) {
        String otelTraceId = OpenTelemetryExecutor.getTraceId();
        if (isValidTraceId(otelTraceId)) {
          return otelTraceId;
        }
      }

      // 2. 备选方案：从 Apache SkyWalking 中获取
      if (IS_SKYWALKING_PRESENT) {
        String swTraceId = SkyWalkingExecutor.getTraceId();
        if (isValidTraceId(swTraceId)) {
          return swTraceId;
        }
      }

    } catch (Throwable t) {
      // 开源防线：绝对不因链路监控组件的异常导致业务主流程崩溃
      log.debug("Failed to auto-fetch traceId via APM component", t);
    }

    return "NoTraceId";
  }

  /**
   * 判定 ClassPath 中是否存在特定类
   */
  private static boolean isClassPresent(String className) {
    try {
      Class.forName(className, false, TraceIdHelper.class.getClassLoader());
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  /**
   * 校验 TraceId 的合法性（过滤掉未初始化或全 0 的无效 ID）
   */
  private static boolean isValidTraceId(String traceId) {
    return traceId != null && !traceId.isEmpty() && !"Ignored_Trace".equals(traceId)
        && !traceId.contains("00000000000000000000000000000000");
  }

  /**
   * 【核心设计】：利用静态内部类隔离 OpenTelemetry 的类加载。 只有当外部调用 OpenTelemetryExecutor.getTraceId() 时，JVM 才会去加载 io.opentelemetry.*
   * 相关的类。 如果外部不调用，即使没导 OTel 的包，整个系统也绝不会报错。
   */
  private static class OpenTelemetryExecutor {
    public static String getTraceId() {
      try {
        io.opentelemetry.api.trace.Span currentSpan = io.opentelemetry.api.trace.Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
          return currentSpan.getSpanContext().getTraceId();
        }
      } catch (Throwable t) {
        // 再次防御
      }
      return null;
    }
  }

  /**
   * 隔离 SkyWalking 依赖的内部类
   */
  private static class SkyWalkingExecutor {
    public static String getTraceId() {
      // try {
      // return org.apache.skywalking.apm.toolkit.trace.TraceContext.traceId();
      // } catch (Throwable t) {
      // // 防御
      // }
      return null;
    }
  }
}
