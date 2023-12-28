package com.diguage.truman.mapper;

import com.diguage.truman.model.Employee;

/**
 * @author D瓜哥 · https://www.diguage.com
 *
 * @since 2023-12-30 09:37:33
 */
public interface EmployeeMapper {
  Employee selectById(long id);
}
