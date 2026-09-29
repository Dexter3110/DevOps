-- ==========================================================
-- ESI (Employee Skill Inventory) Database Schema (Reference DDL)
-- Compatible with H2 and standard SQL databases (MySQL/PostgreSQL)
-- ==========================================================

-- 1. Skills Table
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 2. Employees Table
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    hire_date DATE
);

-- 3. Employee Skills Association Table
CREATE TABLE IF NOT EXISTS employee_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level VARCHAR(50) NOT NULL,
    years_of_experience INT DEFAULT 0,
    certification_name VARCHAR(200),
    expiry_date DATE,
    verified BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_emp_skill_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_emp_skill_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT uq_employee_skill UNIQUE (employee_id, skill_id)
);

-- Initial Seed Data
INSERT INTO skills (name, category, description, status) VALUES 
('Java', 'Backend', 'Core Java and Enterprise development', 'ACTIVE'),
('Spring Boot', 'Framework', 'Spring Boot microservices framework', 'ACTIVE'),
('Docker', 'DevOps', 'Containerization and orchestration', 'ACTIVE'),
('Kubernetes', 'DevOps', 'Container orchestration platform', 'ACTIVE'),
('React', 'Frontend', 'Frontend UI library', 'ACTIVE');

INSERT INTO employees (first_name, last_name, email, department, designation, hire_date) VALUES 
('John', 'Doe', 'john.doe@example.com', 'Engineering', 'Senior Software Engineer', '2022-01-15'),
('Jane', 'Smith', 'jane.smith@example.com', 'DevOps', 'DevOps Lead', '2021-06-01');

INSERT INTO employee_skills (employee_id, skill_id, proficiency_level, years_of_experience, certification_name, expiry_date, verified) VALUES 
(1, 1, 'ADVANCED', 5, 'Oracle Certified Java Professional', '2027-01-01', TRUE),
(1, 2, 'ADVANCED', 4, 'Spring Certified Professional', '2026-12-31', TRUE),
(2, 3, 'EXPERT', 6, 'Docker Certified Associate', '2025-05-10', TRUE),
(2, 4, 'INTERMEDIATE', 2, 'CKA - Certified Kubernetes Administrator', '2024-11-20', TRUE);
