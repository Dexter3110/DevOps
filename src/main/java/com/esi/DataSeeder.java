package com.esi;

import com.esi.model.Employee;
import com.esi.model.EmployeeSkill;
import com.esi.model.Skill;
import com.esi.repo.EmployeeRepository;
import com.esi.repo.EmployeeSkillRepository;
import com.esi.repo.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SkillRepository skillRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;

    public DataSeeder(SkillRepository skillRepository,
                      EmployeeRepository employeeRepository,
                      EmployeeSkillRepository employeeSkillRepository) {
        this.skillRepository = skillRepository;
        this.employeeRepository = employeeRepository;
        this.employeeSkillRepository = employeeSkillRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (skillRepository.count() > 0) {
            log.info("Skills table is not empty ({} skills found), skipping data seeding.", skillRepository.count());
            return;
        }

        log.info("Skills table is empty. Seeding initial data...");

        // 1. Insert 5 skills with categories
        Skill java = new Skill("Java", "Backend Development", "Enterprise Java and Spring Boot ecosystem", "ACTIVE");
        Skill python = new Skill("Python", "Data Science & Scripting", "Python for automation, data science, and scripting", "ACTIVE");
        Skill docker = new Skill("Docker", "DevOps & Cloud", "Container packaging, management, and orchestration", "ACTIVE");
        Skill sql = new Skill("SQL", "Database", "Relational database querying, optimization, and modeling", "ACTIVE");
        Skill communication = new Skill("Communication", "Soft Skills", "Technical writing, cross-team collaboration, and presentation", "ACTIVE");

        skillRepository.saveAll(List.of(java, python, docker, sql, communication));

        // 2. Insert 4 employees across 2 departments
        Employee emp1 = new Employee("Alice", "Johnson", "alice.johnson@example.com", "Engineering", "Senior Software Engineer", LocalDate.now().minusYears(3));
        Employee emp2 = new Employee("Bob", "Smith", "bob.smith@example.com", "Engineering", "Software Engineer", LocalDate.now().minusYears(2));
        Employee emp3 = new Employee("Carol", "Williams", "carol.williams@example.com", "DevOps", "DevOps Lead", LocalDate.now().minusYears(4));
        Employee emp4 = new Employee("David", "Brown", "david.brown@example.com", "DevOps", "Cloud Operations Engineer", LocalDate.now().minusYears(1));

        employeeRepository.saveAll(List.of(emp1, emp2, emp3, emp4));

        // 3. Insert 6 employee-skill assignments with varied proficiency
        // Make 2 certifications expire within 20 days so Exception Alerts and dashboard show data.
        LocalDate now = LocalDate.now();

        EmployeeSkill es1 = new EmployeeSkill(
                emp1, java, "EXPERT", 6,
                "Oracle Certified Professional: Java SE 17",
                now.plusDays(8), true
        );

        EmployeeSkill es2 = new EmployeeSkill(
                emp1, sql, "ADVANCED", 4,
                null, null, true
        );

        EmployeeSkill es3 = new EmployeeSkill(
                emp2, python, "INTERMEDIATE", 2,
                "Certified Associate in Python Programming",
                now.plusDays(15), true
        );

        EmployeeSkill es4 = new EmployeeSkill(
                emp3, docker, "EXPERT", 5,
                "Docker Certified Associate",
                now.plusMonths(10), true
        );

        EmployeeSkill es5 = new EmployeeSkill(
                emp3, communication, "ADVANCED", 4,
                null, null, true
        );

        EmployeeSkill es6 = new EmployeeSkill(
                emp4, docker, "BEGINNER", 1,
                null, null, false
        );

        employeeSkillRepository.saveAll(List.of(es1, es2, es3, es4, es5, es6));

        log.info("Data seeding completed successfully: 5 skills, 4 employees, 6 employee-skill assignments (2 certifications expiring within 20 days).");
    }
}
