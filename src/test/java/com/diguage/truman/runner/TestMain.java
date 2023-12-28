/*
 *    Copyright 2009-2023 the original author or authors.
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
