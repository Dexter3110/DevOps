package com.esi.web;

import com.esi.model.Employee;
import com.esi.model.EmployeeSkill;
import com.esi.model.Skill;
import com.esi.repo.EmployeeRepository;
import com.esi.repo.EmployeeSkillRepository;
import com.esi.repo.SkillRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    private final SkillRepository skillRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;

    public SearchController(SkillRepository skillRepository,
                            EmployeeRepository employeeRepository,
                            EmployeeSkillRepository employeeSkillRepository) {
        this.skillRepository = skillRepository;
        this.employeeRepository = employeeRepository;
        this.employeeSkillRepository = employeeSkillRepository;
    }

    @GetMapping("/skills")
    public List<Skill> searchSkills(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return skillRepository.findByCategoryIgnoreCase(category);
        }
        if (keyword != null && !keyword.isBlank()) {
            return skillRepository.findByNameContainingIgnoreCase(keyword);
        }
        return skillRepository.findAll();
    }

    @GetMapping("/employees")
    public List<Employee> searchEmployees(@RequestParam(required = false) String department,
                                          @RequestParam(required = false) String skillName,
                                          @RequestParam(required = false) String proficiency) {
        if (skillName != null && !skillName.isBlank()) {
            List<EmployeeSkill> employeeSkills;
            if (proficiency != null && !proficiency.isBlank()) {
                employeeSkills = employeeSkillRepository.findBySkillNameAndProficiency(skillName, proficiency);
            } else {
                employeeSkills = employeeSkillRepository.findBySkillNameIgnoreCase(skillName);
            }

            List<Employee> employees = employeeSkills.stream()
                    .map(EmployeeSkill::getEmployee)
                    .distinct()
                    .collect(Collectors.toList());

            if (department != null && !department.isBlank()) {
                return employees.stream()
                        .filter(e -> department.equalsIgnoreCase(e.getDepartment()))
                        .collect(Collectors.toList());
            }
            return employees;
        }

        if (department != null && !department.isBlank()) {
            return employeeRepository.findByDepartmentIgnoreCase(department);
        }

        return employeeRepository.findAll();
    }
}
