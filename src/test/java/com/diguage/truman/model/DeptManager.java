package com.diguage.truman.model;

import java.util.Date;

import lombok.Data;

/**
 * @author D瓜哥 · https://www.diguage.com
 *
 * @since 2023-12-30 09:37:33
 */
@Data
public class DeptManager {

  private Long empNo;

  private String deptNo;

  private Date fromDate;

  private Date toDate;

}
