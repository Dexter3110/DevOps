package com.esi.web;

import com.esi.model.Employee;
import com.esi.model.EmployeeSkill;
import com.esi.model.Skill;
import com.esi.repo.EmployeeRepository;
import com.esi.repo.EmployeeSkillRepository;
import com.esi.repo.SkillRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employee-skills")
@CrossOrigin(origins = "*")
public class EmployeeSkillController {

    private final EmployeeSkillRepository employeeSkillRepository;
    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;

    public EmployeeSkillController(EmployeeSkillRepository employeeSkillRepository,
                                   EmployeeRepository employeeRepository,
                                   SkillRepository skillRepository) {
        this.employeeSkillRepository = employeeSkillRepository;
        this.employeeRepository = employeeRepository;
        this.skillRepository = skillRepository;
    }

    @GetMapping
    public List<EmployeeSkill> getAllEmployeeSkills() {
        return employeeSkillRepository.findAll();
    }

    @GetMapping("/employee/{employeeId}")
    public List<EmployeeSkill> getSkillsByEmployeeId(@PathVariable Long employeeId) {
        return employeeSkillRepository.findByEmployeeId(employeeId);
    }

    @GetMapping("/skill/{skillId}")
    public List<EmployeeSkill> getEmployeesBySkillId(@PathVariable Long skillId) {
        return employeeSkillRepository.findBySkillId(skillId);
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assignSkillToEmployee(@RequestParam Long employeeId,
                                                   @RequestParam Long skillId,
                                                   @Valid @RequestBody EmployeeSkill request) {
        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        Optional<Skill> skillOpt = skillRepository.findById(skillId);

        if (employeeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found with id: " + employeeId);
        }
        if (skillOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Skill not found with id: " + skillId);
        }

        // Check if already mapped
        Optional<EmployeeSkill> existing = employeeSkillRepository.findByEmployeeIdAndSkillId(employeeId, skillId);
        EmployeeSkill employeeSkill;
        if (existing.isPresent()) {
            employeeSkill = existing.get();
            employeeSkill.setProficiencyLevel(request.getProficiencyLevel());
            employeeSkill.setYearsOfExperience(request.getYearsOfExperience());
            employeeSkill.setCertificationName(request.getCertificationName());
            employeeSkill.setExpiryDate(request.getExpiryDate());
            employeeSkill.setVerified(request.getVerified());
        } else {
            employeeSkill = new EmployeeSkill(
                    employeeOpt.get(),
                    skillOpt.get(),
                    request.getProficiencyLevel(),
                    request.getYearsOfExperience(),
                    request.getCertificationName(),
                    request.getExpiryDate(),
                    request.getVerified()
            );
        }

        EmployeeSkill saved = employeeSkillRepository.save(employeeSkill);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployeeSkill(@PathVariable Long id) {
        if (employeeSkillRepository.existsById(id)) {
            employeeSkillRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
