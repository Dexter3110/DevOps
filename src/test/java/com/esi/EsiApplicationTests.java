package com.esi;

import com.esi.model.Skill;
import com.esi.repo.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EsiApplicationTests {

    @Autowired(required = false)
    private SkillRepository skillRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testCreateSkill() {
        if (skillRepository != null) {
            Skill skill = new Skill("Python", "Programming", "Python 3 core language", "ACTIVE");
            Skill saved = skillRepository.save(skill);
            assertNotNull(saved.getId());
        }
    }
}
