package com.esi.repo;

import com.esi.model.EmployeeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, Long> {

    List<EmployeeSkill> findByEmployeeId(Long employeeId);

    List<EmployeeSkill> findBySkillId(Long skillId);

    Optional<EmployeeSkill> findByEmployeeIdAndSkillId(Long employeeId, Long skillId);

    List<EmployeeSkill> findBySkillNameIgnoreCase(String skillName);

    @Query("SELECT es FROM EmployeeSkill es WHERE LOWER(es.skill.name) = LOWER(:skillName) AND LOWER(es.proficiencyLevel) = LOWER(:proficiencyLevel)")
    List<EmployeeSkill> findBySkillNameAndProficiency(@Param("skillName") String skillName, @Param("proficiencyLevel") String proficiencyLevel);

    @Query("SELECT es FROM EmployeeSkill es WHERE es.expiryDate IS NOT NULL AND es.expiryDate <= :targetDate")
    List<EmployeeSkill> findCertificationsExpiringBefore(@Param("targetDate") LocalDate targetDate);
}
