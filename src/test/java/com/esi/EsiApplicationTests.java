package com.esi;

import com.esi.model.Employee;
import com.esi.model.EmployeeSkill;
import com.esi.model.Skill;
import com.esi.repo.EmployeeRepository;
import com.esi.repo.EmployeeSkillRepository;
import com.esi.repo.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EsiApplicationTests {

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeSkillRepository employeeSkillRepository;

    @Autowired
    private DataSeeder dataSeeder;

    @Test
    void contextLoads() {
        assertNotNull(skillRepository);
        assertNotNull(employeeRepository);
        assertNotNull(employeeSkillRepository);
    }

    @Test
    void testCreateSkill() {
        Skill skill = new Skill("Go", "Programming", "Go programming language", "ACTIVE");
        Skill saved = skillRepository.save(skill);
        assertNotNull(saved.getId());
    }

    @Test
    void testDataSeederSkillsInserted() {
        List<String> requiredSkills = List.of("Java", "Python", "Docker", "SQL", "Communication");
        for (String skillName : requiredSkills) {
            assertTrue(skillRepository.findByNameIgnoreCase(skillName).isPresent(),
                    "Expected skill " + skillName + " to be present");
            Skill skill = skillRepository.findByNameIgnoreCase(skillName).get();
            assertNotNull(skill.getCategory(), "Expected category for skill " + skillName);
            assertFalse(skill.getCategory().isBlank(), "Expected non-blank category for skill " + skillName);
        }
    }

    @Test
    void testDataSeederEmployeesAndDepartments() {
        List<Employee> employees = employeeRepository.findAll();
        assertEquals(4, employees.size(), "Expected 4 seeded employees");

        Set<String> departments = employees.stream()
                .map(Employee::getDepartment)
                .collect(Collectors.toSet());
        assertEquals(2, departments.size(), "Expected 2 departments across employees");
    }

    @Test
    void testDataSeederEmployeeSkillAssignmentsAndProficiency() {
        List<EmployeeSkill> assignments = employeeSkillRepository.findAll();
        assertEquals(6, assignments.size(), "Expected 6 employee-skill assignments");

        Set<String> proficiencies = assignments.stream()
                .map(EmployeeSkill::getProficiencyLevel)
                .collect(Collectors.toSet());
        assertTrue(proficiencies.size() >= 3, "Expected varied proficiencies among assignments");
    }

    @Test
    void testDataSeederExpiringCertificationsWithin20Days() {
        LocalDate within20Days = LocalDate.now().plusDays(20);
        List<EmployeeSkill> expiring = employeeSkillRepository.findCertificationsExpiringBefore(within20Days);
        assertEquals(2, expiring.size(), "Expected exactly 2 certifications expiring within 20 days");
    }

    @Test
    void testDataSeederDoesNotDuplicateWhenSkillsExist() {
        long skillsCountBefore = skillRepository.count();
        long employeesCountBefore = employeeRepository.count();
        long assignmentsCountBefore = employeeSkillRepository.count();

        dataSeeder.run();

        assertEquals(skillsCountBefore, skillRepository.count(), "Skills count should not change when seeder is re-run");
        assertEquals(employeesCountBefore, employeeRepository.count(), "Employees count should not change when seeder is re-run");
        assertEquals(assignmentsCountBefore, employeeSkillRepository.count(), "Assignments count should not change when seeder is re-run");
    }
}
