package com.diguage.truman.runner;

import com.diguage.truman.mapper.EmployeeMapper;
import com.diguage.truman.model.Employee;

import java.io.IOException;
import java.io.InputStream;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class TestMain {

  // tag::test0101[]
  /**
   * @author D瓜哥 · https://www.diguage.com
   *
   * @since 2023-12-30 09:37:33
   */
  @Test
  public void test() throws IOException {
    InputStream resource = Resources.getResourceAsStream("truman/mybatis-config.xml");
    DefaultSqlSessionFactory sessionFactory = (DefaultSqlSessionFactory) new SqlSessionFactoryBuilder().build(resource);
    Configuration configuration = sessionFactory.getConfiguration();
    Environment environment = configuration.getEnvironment();
    DataSource dataSource = environment.getDataSource();

    Resource schemaResource = new ClassPathResource("truman/database/employees/schema.sql");
    Resource dataResource = new ClassPathResource("truman/database/employees/data.sql");
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schemaResource, dataResource);
    populator.execute(dataSource);

    SqlSession session = sessionFactory.openSession();
    EmployeeMapper mapper = session.getMapper(EmployeeMapper.class);
    Employee employee = mapper.selectById(10001L);
  }
  // end::test0101[]

}
