package com.example.taskflow.controller;

import com.example.taskflow.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskApiTest extends IntegrationTestSupport {

    private String alex;

    @BeforeEach
    void logIn() throws Exception {
        alex = registerAndLogin("alex@example.com");
    }

    // --- authentication -------------------------------------------------------------------------------------

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").header(HttpHeaders.AUTHORIZATION, alex + "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Access token is invalid"));
    }

    @Test
    void expiredTokenIsRejectedWithReason() throws Exception {
        clock.advance(Duration.ofDays(31));

        mockMvc.perform(get("/api/v1/tasks").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Access token has expired, log in again"));
    }

    @Test
    void tokenOfDeletedAccountIsRejected() throws Exception {
        userRepository.deleteAllInBatch();

        mockMvc.perform(get("/api/v1/tasks").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isUnauthorized());
    }

    // --- CRUD ---------------------------------------------------------------------------------------------

    @Test
    void createReturnsTaskInTodoWithLocation() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, alex)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  Write README  ", "description": "   "}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesRegex(".*/api/v1/tasks/\\d+")))
                .andExpect(jsonPath("$.title").value("Write README"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));
    }

    @Test
    void createValidatesBody() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, alex)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "description": "%s"}""".formatted("x".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists());
        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void getReturnsTaskOrNotFound() throws Exception {
        long id = createTask(alex, "Write README");

        mockMvc.perform(get("/api/v1/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/api/v1/tasks/{id}", 999_999).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Task not found"));

        mockMvc.perform(get("/api/v1/tasks/abc").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putReplacesFieldsAndCompletingSetsCompletedAt() throws Exception {
        long id = createTask(alex, "Write README");
        clock.advance(Duration.ofMinutes(5));

        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, alex)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Write README v2", "description": "With diagrams", "status": "COMPLETED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write README v2"))
                .andExpect(jsonPath("$.description").value("With diagrams"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").value("2026-01-05T09:05:00Z"));
    }

    @Test
    void putRequiresStatusAndRejectsUnknownStatus() throws Exception {
        long id = createTask(alex, "Write README");

        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, alex)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Write README"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());

        changeStatus(alex, id, "DONE_ISH").andExpect(status().isBadRequest());
    }

    @Test
    void reopeningClearsCompletedAt() throws Exception {
        long id = createTask(alex, "Write README");
        changeStatus(alex, id, "COMPLETED").andExpect(jsonPath("$.completedAt").isNotEmpty());

        changeStatus(alex, id, "IN_PROGRESS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));
    }

    @Test
    void deleteReturnsNoContentAndTaskIsGone() throws Exception {
        long id = createTask(alex, "Write README");

        mockMvc.perform(delete("/api/v1/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isNotFound());
    }

    // --- listing ------------------------------------------------------------------------------------------

    @Test
    void listIsPagedNewestFirstAndFilterableByStatus() throws Exception {
        long first = createTask(alex, "Task 1");
        clock.advance(Duration.ofMinutes(1));
        createTask(alex, "Task 2");
        clock.advance(Duration.ofMinutes(1));
        createTask(alex, "Task 3");
        changeStatus(alex, first, "COMPLETED");

        mockMvc.perform(get("/api/v1/tasks").param("page", "0").param("size", "2").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Task 3"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/v1/tasks").param("status", "COMPLETED").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"));
    }

    @Test
    void listRejectsInvalidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("size", "500").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").exists());
        mockMvc.perform(get("/api/v1/tasks").param("page", "-1").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").exists());
    }

    // --- ownership ----------------------------------------------------------------------------------------

    @Test
    void usersCannotSeeOrChangeEachOthersTasks() throws Exception {
        long alexTask = createTask(alex, "Alex's task");
        String sam = registerAndLogin("sam@example.com");

        mockMvc.perform(get("/api/v1/tasks").header(HttpHeaders.AUTHORIZATION, sam))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/tasks/{id}", alexTask).header(HttpHeaders.AUTHORIZATION, sam))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/tasks/{id}", alexTask)
                        .header(HttpHeaders.AUTHORIZATION, sam)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "hijacked", "status": "COMPLETED"}"""))
                .andExpect(status().isNotFound());
        changeStatus(sam, alexTask, "COMPLETED").andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/tasks/{id}", alexTask).header(HttpHeaders.AUTHORIZATION, sam))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/tasks/{id}/events", alexTask).header(HttpHeaders.AUTHORIZATION, sam))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/tasks/{id}", alexTask).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(jsonPath("$.title").value("Alex's task"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }
}
