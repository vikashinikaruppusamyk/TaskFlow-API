package com.example.taskflow.controller;

import com.example.taskflow.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsApiTest extends IntegrationTestSupport {

    private String alex;

    @BeforeEach
    void logIn() throws Exception {
        alex = registerAndLogin("alex@example.com");
    }

    @Test
    void everyChangeIsRecordedAsAnEventInOrder() throws Exception {
        long id = createTask(alex, "Write README");
        clock.advance(Duration.ofHours(1));
        changeStatus(alex, id, "IN_PROGRESS");
        changeStatus(alex, id, "IN_PROGRESS"); // same status again: no new event
        clock.advance(Duration.ofHours(1));
        changeStatus(alex, id, "COMPLETED");

        mockMvc.perform(get("/api/v1/tasks/{id}/events", id).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].activity").value(contains("CREATED", "STARTED", "COMPLETED")))
                .andExpect(jsonPath("$[*].activityLabel").value(contains("Create Task", "Start Work", "Complete Task")))
                .andExpect(jsonPath("$[1].fromStatus").value("TODO"))
                .andExpect(jsonPath("$[1].toStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[2].timestamp").value("2026-01-05T11:00:00Z"));
    }

    @Test
    void putRecordsDetailUpdateAndStatusChangeSeparately() throws Exception {
        long id = createTask(alex, "Write README");

        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, alex)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Write README v2", "status": "IN_PROGRESS"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tasks/{id}/events", id).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(jsonPath("$[*].activity").value(contains("CREATED", "DETAILS_UPDATED", "STARTED")));
    }

    @Test
    void historyOfDeletedTaskIsKept() throws Exception {
        long id = createTask(alex, "Write README");
        mockMvc.perform(delete("/api/v1/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{id}/events", id).header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].activity").value(contains("CREATED", "DELETED")))
                .andExpect(jsonPath("$[1].fromStatus").value("TODO"));
    }

    @Test
    void cycleTimeIsMeasuredFromCreationToCompletion() throws Exception {
        long twoHours = createTask(alex, "Two hours");
        long fourHours = createTask(alex, "Four hours");
        createTask(alex, "Never finished");

        clock.advance(Duration.ofHours(2));
        changeStatus(alex, twoHours, "COMPLETED");
        clock.advance(Duration.ofHours(2));
        changeStatus(alex, fourHours, "COMPLETED");

        mockMvc.perform(get("/api/v1/analytics/cycle-time").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCases").value(2))
                .andExpect(jsonPath("$.averageSeconds").value(3 * 3600))
                .andExpect(jsonPath("$.medianSeconds").value(3 * 3600))
                .andExpect(jsonPath("$.minSeconds").value(2 * 3600))
                .andExpect(jsonPath("$.maxSeconds").value(4 * 3600))
                .andExpect(jsonPath("$.average").value("PT3H"));
    }

    @Test
    void cycleTimeIsEmptyWhenNothingIsCompleted() throws Exception {
        createTask(alex, "Open task");

        mockMvc.perform(get("/api/v1/analytics/cycle-time").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(jsonPath("$.completedCases").value(0))
                .andExpect(jsonPath("$.averageSeconds").doesNotExist());
    }

    @Test
    void variantsGroupCasesByActivitySequence() throws Exception {
        for (int i = 0; i < 2; i++) {
            long id = createTask(alex, "Happy path " + i);
            changeStatus(alex, id, "IN_PROGRESS");
            changeStatus(alex, id, "COMPLETED");
        }
        long reworked = createTask(alex, "Reworked");
        changeStatus(alex, reworked, "COMPLETED");
        changeStatus(alex, reworked, "IN_PROGRESS");
        changeStatus(alex, reworked, "COMPLETED");

        mockMvc.perform(get("/api/v1/analytics/variants").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].activities").value(contains("Create Task", "Start Work", "Complete Task")))
                .andExpect(jsonPath("$[0].caseCount").value(2))
                .andExpect(jsonPath("$[0].percentage").value(66.7))
                .andExpect(jsonPath("$[1].activities")
                        .value(contains("Create Task", "Complete Task", "Reopen Task", "Complete Task")))
                .andExpect(jsonPath("$[1].caseCount").value(1));
    }

    @Test
    void summaryCountsCasesAndReworkRate() throws Exception {
        long done = createTask(alex, "Done");
        changeStatus(alex, done, "COMPLETED");
        long reopened = createTask(alex, "Reopened");
        changeStatus(alex, reopened, "COMPLETED");
        changeStatus(alex, reopened, "TODO");
        long started = createTask(alex, "Started");
        changeStatus(alex, started, "IN_PROGRESS");
        long deleted = createTask(alex, "Deleted");
        mockMvc.perform(delete("/api/v1/tasks/{id}", deleted).header(HttpHeaders.AUTHORIZATION, alex));

        mockMvc.perform(get("/api/v1/analytics/summary").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(4))
                .andExpect(jsonPath("$.openCases").value(2))
                .andExpect(jsonPath("$.completedCases").value(2))
                .andExpect(jsonPath("$.reopenedCases").value(1))
                .andExpect(jsonPath("$.deletedCases").value(1))
                .andExpect(jsonPath("$.reworkRate").value(50.0))
                .andExpect(jsonPath("$.tasksByStatus.TODO").value(1))
                .andExpect(jsonPath("$.tasksByStatus.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.tasksByStatus.COMPLETED").value(1));
    }

    @Test
    void eventLogExportsAsCsv() throws Exception {
        long id = createTask(alex, "Write README");
        clock.advance(Duration.ofMinutes(30));
        changeStatus(alex, id, "COMPLETED");

        String csv = mockMvc.perform(get("/api/v1/analytics/event-log/export").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("taskflow-event-log.csv")))
                .andReturn().getResponse().getContentAsString();

        assertThat(csv.lines()).containsExactly(
                "case_id,activity,timestamp,from_status,to_status",
                id + ",Create Task,2026-01-05T09:00:00Z,,TODO",
                id + ",Complete Task,2026-01-05T09:30:00Z,TODO,COMPLETED");
    }

    @Test
    void analyticsOnlyCoverTheCallersOwnTasks() throws Exception {
        long id = createTask(alex, "Alex's task");
        changeStatus(alex, id, "COMPLETED");
        String sam = registerAndLogin("sam@example.com");

        mockMvc.perform(get("/api/v1/analytics/event-log").header(HttpHeaders.AUTHORIZATION, sam))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/analytics/summary").header(HttpHeaders.AUTHORIZATION, sam))
                .andExpect(jsonPath("$.totalCases").value(0))
                .andExpect(jsonPath("$.reworkRate").value(0.0));
        mockMvc.perform(get("/api/v1/analytics/event-log").header(HttpHeaders.AUTHORIZATION, alex))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
