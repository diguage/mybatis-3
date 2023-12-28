--
--    Copyright 2009-2023 the original author or authors.
--
--    Licensed under the Apache License, Version 2.0 (the "License");
--    you may not use this file except in compliance with the License.
--    You may obtain a copy of the License at
--
--       https://www.apache.org/licenses/LICENSE-2.0
--
--    Unless required by applicable law or agreed to in writing, software
--    distributed under the License is distributed on an "AS IS" BASIS,
--    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--    See the License for the specific language governing permissions and
--    limitations under the License.
--

--DROP DATABASE IF EXISTS employees;


-- <1><2>
-- CREATE DATABASE IF NOT EXISTS employees
--    DEFAULT CHARACTER SET utf8mb4
--    COLLATE utf8mb4_0900_ai_ci;

-- USE employees;

DROP TABLE IF EXISTS dept_emp,
    dept_manager,
    titles,
    salaries,
    employees,
    departments;

CREATE TABLE employees
(
    emp_no     INT            NOT NULL,
    birth_date DATE           NOT NULL,
    first_name VARCHAR(14)    NOT NULL,
    last_name  VARCHAR(16)    NOT NULL,
    gender     ENUM ('M','F') NOT NULL,
    hire_date  DATE           NOT NULL,
    PRIMARY KEY (emp_no)
);

CREATE TABLE departments
(
    dept_no   CHAR(4)     NOT NULL,
    dept_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (dept_no),
    UNIQUE KEY (dept_name)
);

CREATE TABLE dept_manager
(
    emp_no    INT     NOT NULL,
    dept_no   CHAR(4) NOT NULL,
    from_date DATE    NOT NULL,
    to_date   DATE    NOT NULL,
    FOREIGN KEY (emp_no) REFERENCES employees (emp_no) ON DELETE CASCADE,
    FOREIGN KEY (dept_no) REFERENCES departments (dept_no) ON DELETE CASCADE,
    PRIMARY KEY (emp_no, dept_no)
);

CREATE TABLE dept_emp
(
    emp_no    INT     NOT NULL,
    dept_no   CHAR(4) NOT NULL,
    from_date DATE    NOT NULL,
    to_date   DATE    NOT NULL,
    FOREIGN KEY (emp_no) REFERENCES employees (emp_no) ON DELETE CASCADE,
    FOREIGN KEY (dept_no) REFERENCES departments (dept_no) ON DELETE CASCADE,
    PRIMARY KEY (emp_no, dept_no)
);

CREATE TABLE titles
(
    emp_no    INT         NOT NULL,
    title     VARCHAR(50) NOT NULL,
    from_date DATE        NOT NULL,
    to_date   DATE,
    FOREIGN KEY (emp_no) REFERENCES employees (emp_no) ON DELETE CASCADE,
    PRIMARY KEY (emp_no, title, from_date)
)
;

CREATE TABLE salaries
(
    emp_no    INT  NOT NULL,
    salary    INT  NOT NULL,
    from_date DATE NOT NULL,
    to_date   DATE NOT NULL,
    FOREIGN KEY (emp_no) REFERENCES employees (emp_no) ON DELETE CASCADE,
    PRIMARY KEY (emp_no, from_date)
)
;

CREATE OR REPLACE VIEW dept_emp_latest_date AS
    SELECT emp_no, MAX(from_date) AS from_date, MAX(to_date) AS to_date
    FROM dept_emp
    GROUP BY emp_no;

-- shows only the current department for each employee
CREATE OR REPLACE VIEW current_dept_emp AS
    SELECT l.emp_no, dept_no, l.from_date, l.to_date
    FROM dept_emp d
        INNER JOIN dept_emp_latest_date l
            ON d.emp_no = l.emp_no
                   AND d.from_date = l.from_date
                   AND l.to_date = d.to_date;