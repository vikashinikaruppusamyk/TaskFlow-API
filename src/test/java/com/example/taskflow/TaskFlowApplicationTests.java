package com.example.taskflow;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskFlowApplicationTests extends IntegrationTestSupport {

    @Test
    void openApiDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("TaskFlow API"))
                .andExpect(jsonPath("$.paths['/api/v1/tasks']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/analytics/cycle-time']").exists());
    }

    @Test
    void frontendPagesArePublicButTheirDataIsNot() throws Exception {
        for (String page : new String[]{"/", "/login.html", "/register.html", "/tasks.html", "/script.js", "/style.css"}) {
            mockMvc.perform(get(page)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
