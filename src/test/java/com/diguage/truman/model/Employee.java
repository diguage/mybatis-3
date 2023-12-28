package com.diguage.truman.model;

import lombok.Data;

/**
 * @author D瓜哥 · https://www.diguage.com
 *
 * @since 2023-12-30 09:37:33
 */
@Data
public class Employee {

  private Long empNo;

  private String birthDate;

  private String firstName;

  private String lastName;

  private String gender;

  private String hireDate;
}
