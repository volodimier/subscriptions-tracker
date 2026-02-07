package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.response.JobRunResponse;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.entity.JobStatus;
import com.subscriptiontracker.entity.TriggerType;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.JobRunService;
import com.subscriptiontracker.service.JwtService;
import com.subscriptiontracker.service.TwoFactorAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AdminController}.
 *
 * <p>Tests role-based access control ensuring only ADMIN users
 * can access administrative endpoints.</p>
 *
 * @author Generated
 * @since 1.0
 */
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminController")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobRunService jobRunService;

    @MockBean
    private TwoFactorAuthService twoFactorAuthService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private JobRunResponse jobRunResponse;
    private static final Long JOB_RUN_ID = 1L;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        jobRunResponse = JobRunResponse.builder()
                .id(JOB_RUN_ID)
                .jobName("FX_RATE_REFRESH")
                .status(JobStatus.SUCCESS)
                .triggerType(TriggerType.SCHEDULED)
                .startTime(now.minusMinutes(5))
                .finishTime(now)
                .errorMessage(null)
                .createdAt(now)
                .build();
    }

    @Nested
    @DisplayName("GET /admin/job-runs")
    class GetJobRuns {

        @Test
        @DisplayName("should return 200 with paginated job runs")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200WithPaginatedJobRuns() throws Exception {
            PaginatedResponse<JobRunResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(jobRunResponse), 1, 20, 1
            );

            when(jobRunService.getAllJobRuns(1, 20)).thenReturn(paginatedResponse);

            mockMvc.perform(get("/admin/job-runs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(JOB_RUN_ID))
                    .andExpect(jsonPath("$.data[0].jobName").value("FX_RATE_REFRESH"))
                    .andExpect(jsonPath("$.data[0].status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data[0].triggerType").value("SCHEDULED"))
                    .andExpect(jsonPath("$.pagination.page").value(1))
                    .andExpect(jsonPath("$.pagination.limit").value(20))
                    .andExpect(jsonPath("$.pagination.total").value(1));
        }

        @Test
        @DisplayName("should return 200 with custom pagination")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200WithCustomPagination() throws Exception {
            PaginatedResponse<JobRunResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(jobRunResponse), 2, 10, 15
            );

            when(jobRunService.getAllJobRuns(2, 10)).thenReturn(paginatedResponse);

            mockMvc.perform(get("/admin/job-runs")
                            .param("page", "2")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pagination.page").value(2))
                    .andExpect(jsonPath("$.pagination.limit").value(10))
                    .andExpect(jsonPath("$.pagination.total").value(15));
        }

        @Test
        @DisplayName("should return 200 with empty list when no job runs")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200WithEmptyListWhenNoJobRuns() throws Exception {
            PaginatedResponse<JobRunResponse> emptyResponse = PaginatedResponse.of(
                    List.of(), 1, 20, 0
            );

            when(jobRunService.getAllJobRuns(1, 20)).thenReturn(emptyResponse);

            mockMvc.perform(get("/admin/job-runs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.pagination.total").value(0));
        }

        @Test
        @DisplayName("should return 200 with multiple job runs")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200WithMultipleJobRuns() throws Exception {
            LocalDateTime now = LocalDateTime.now();
            JobRunResponse jobRun2 = JobRunResponse.builder()
                    .id(2L)
                    .jobName("PAYMENT_GENERATOR")
                    .status(JobStatus.FAILURE)
                    .triggerType(TriggerType.MANUAL)
                    .startTime(now.minusMinutes(10))
                    .finishTime(now.minusMinutes(5))
                    .errorMessage("Connection timeout")
                    .createdAt(now.minusMinutes(5))
                    .build();

            PaginatedResponse<JobRunResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(jobRunResponse, jobRun2), 1, 20, 2
            );

            when(jobRunService.getAllJobRuns(1, 20)).thenReturn(paginatedResponse);

            mockMvc.perform(get("/admin/job-runs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].jobName").value("FX_RATE_REFRESH"))
                    .andExpect(jsonPath("$.data[1].jobName").value("PAYMENT_GENERATOR"))
                    .andExpect(jsonPath("$.data[1].status").value("FAILURE"))
                    .andExpect(jsonPath("$.data[1].errorMessage").value("Connection timeout"))
                    .andExpect(jsonPath("$.pagination.total").value(2));
        }
    }

    @Nested
    @DisplayName("GET /admin/job-runs/{id}")
    class GetJobRunById {

        @Test
        @DisplayName("should return 200 with job run details")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200WithJobRunDetails() throws Exception {
            when(jobRunService.getJobRunById(JOB_RUN_ID)).thenReturn(jobRunResponse);

            mockMvc.perform(get("/admin/job-runs/{id}", JOB_RUN_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(JOB_RUN_ID))
                    .andExpect(jsonPath("$.jobName").value("FX_RATE_REFRESH"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.triggerType").value("SCHEDULED"));
        }

        @Test
        @DisplayName("should return 404 when job run not found")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenJobRunNotFound() throws Exception {
            when(jobRunService.getJobRunById(999L))
                    .thenThrow(new ResourceNotFoundException("JobRun", "id", 999L));

            mockMvc.perform(get("/admin/job-runs/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 200 with failed job run details including error message")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200WithFailedJobRunDetailsIncludingErrorMessage() throws Exception {
            LocalDateTime now = LocalDateTime.now();
            JobRunResponse failedJobRun = JobRunResponse.builder()
                    .id(2L)
                    .jobName("FX_RATE_REFRESH")
                    .status(JobStatus.FAILURE)
                    .triggerType(TriggerType.SCHEDULED)
                    .startTime(now.minusMinutes(5))
                    .finishTime(now)
                    .errorMessage("External API returned 503")
                    .createdAt(now)
                    .build();

            when(jobRunService.getJobRunById(2L)).thenReturn(failedJobRun);

            mockMvc.perform(get("/admin/job-runs/{id}", 2L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2L))
                    .andExpect(jsonPath("$.status").value("FAILURE"))
                    .andExpect(jsonPath("$.errorMessage").value("External API returned 503"));
        }
    }
}
