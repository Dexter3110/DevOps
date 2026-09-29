package com.esi.web;

import com.esi.model.EmployeeSkill;
import com.esi.repo.EmployeeSkillRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final EmployeeSkillRepository employeeSkillRepository;

    public AlertController(EmployeeSkillRepository employeeSkillRepository) {
        this.employeeSkillRepository = employeeSkillRepository;
    }

    @GetMapping("/expiring-certifications")
    public List<EmployeeSkill> getExpiringCertifications(@RequestParam(defaultValue = "30") int days) {
        LocalDate targetDate = LocalDate.now().plusDays(days);
        return employeeSkillRepository.findCertificationsExpiringBefore(targetDate);
    }

    @GetMapping("/summary")
    public Map<String, Object> getAlertSummary() {
        LocalDate next30Days = LocalDate.now().plusDays(30);
        List<EmployeeSkill> expiring = employeeSkillRepository.findCertificationsExpiringBefore(next30Days);

        Map<String, Object> summary = new HashMap<>();
        summary.put("expiringWithin30DaysCount", expiring.size());
        summary.put("expiringCertifications", expiring);
        summary.put("generatedAt", LocalDate.now());
        return summary;
    }
}
