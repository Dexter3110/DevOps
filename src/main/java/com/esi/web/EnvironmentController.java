package com.esi.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes which environment / Spring profile this running instance was
 * deployed with, so the dashboard can show it in the ENV badge.
 * This is what Week 8's "parameterize at least one environment setting"
 * looks like at runtime: the same JAR, started with a different
 * --spring.profiles.active value by Jenkins, reports a different value here.
 */
@RestController
@RequestMapping("/api/env")
@CrossOrigin(origins = "*")
public class EnvironmentController {

    @Value("${esi.environment:local}")
    private String environment;

    @Value("${server.port:8085}")
    private String port;

    @GetMapping
    public Map<String, String> getEnvironment() {
        Map<String, String> info = new HashMap<>();
        info.put("environment", environment);
        info.put("port", port);
        return info;
    }
}
