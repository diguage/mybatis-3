package com.diguage.truman.runner;

import com.diguage.truman.mapper.EmployeeMapper;
import com.diguage.truman.model.Employee;
import com.diguage.truman.plugin.SqlCommentInterceptor;
import com.diguage.truman.plugin.SqlLogInterceptor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication(scanBasePackages = "com.diguage.truman")
@MapperScan(basePackages = "com.diguage.truman.mapper")
public class SpringBootMain implements CommandLineRunner {

  @Autowired
  private EmployeeMapper mapper;

  public static void main(String[] args) {
    SpringApplication.run(SpringBootMain.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    Employee employee = mapper.selectById(10001L);
    System.out.println(employee);
  }

  @Configuration
  public static class BeanConfig {
    @Bean
    public SqlCommentInterceptor sqlCommentInterceptor() {
      return new SqlCommentInterceptor();
    }

    @Bean
    public SqlLogInterceptor sqlLogInterceptor() {
      return new SqlLogInterceptor();
    }
  }
}
