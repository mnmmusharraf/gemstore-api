package com.gemstore.backend.controllers.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemstore.backend.dtos.report.CreateReportRequest;
import com.gemstore.backend.dtos.report.UpdateReportRequest;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.listing.ListingStatus;
import com.gemstore.backend.entities.listing.lookup.GemstoneType;
import com.gemstore.backend.entities.report.Report;
import com.gemstore.backend.entities.report.ReportReason;
import com.gemstore.backend.entities.report.ReportStatus;
import com.gemstore.backend.entities.report.ReportType;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.EmailVerificationOtpRepository;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.listing.ListingViewRepository;
import com.gemstore.backend.repositories.listing.lookup.GemstoneTypeRepository;
import com.gemstore.backend.repositories.report.ReportRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.auth.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmailVerificationOtpRepository otpRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private ListingViewRepository listingViewRepository; // ✅ added
    @Autowired private GemstoneTypeRepository gemstoneTypeRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JWTService jwtService;

    private User reporter;
    private User reportedUser;
    private User admin;
    private String reporterToken;
    private String adminToken;
    private String reportedUserToken;
    private Listing listing;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        listingViewRepository.deleteAll(); // ✅ added (must be before listingRepository.deleteAll())
        listingRepository.deleteAll();
        otpRepository.deleteAll(); // ✅ Added to prevent foreign key constraint violation
        userRepository.deleteAll();
        gemstoneTypeRepository.deleteAll();

        // Gemstone type
        GemstoneType type = new GemstoneType();
        type.setName("Ruby");
        type = gemstoneTypeRepository.save(type);

        // Reporter
        reporter = User.builder()
                .displayName("Reporter")
                .email("reporter@example.com")
                .username("reporter")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        reporter = userRepository.save(reporter);
        reporterToken = jwtService.generateToken(reporter.getId(), reporter.getUsername(), reporter.getRole());

        // Reported user (listing owner)
        reportedUser = User.builder()
                .displayName("Reported")
                .email("reported@example.com")
                .username("reported")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        reportedUser = userRepository.save(reportedUser);
        reportedUserToken = jwtService.generateToken(reportedUser.getId(), reportedUser.getUsername(), reportedUser.getRole());

        // Admin
        admin = User.builder()
                .displayName("Admin")
                .email("admin@example.com")
                .username("admin")
                .passwordHash(passwordEncoder.encode("adminpass"))
                .provider("LOCAL")
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        admin = userRepository.save(admin);
        adminToken = jwtService.generateToken(admin.getId(), admin.getUsername(), admin.getRole());

        // Listing
        listing = Listing.builder()
                .seller(reportedUser)
                .title("Suspicious Gem")
                .gemstoneType(type)
                .caratWeight(new BigDecimal("1.50"))
                .price(new BigDecimal("30000"))
                .currency("LKR")
                .status(ListingStatus.ACTIVE)
                .build();
        listing = listingRepository.save(listing);
    }

    private Report createTestReport() {
        Report report = Report.builder()
                .reporter(reporter)
                .reportedListing(listing)
                .reportType(ReportType.LISTING)
                .reason(ReportReason.values()[0])
                .description("Test report")
                .status(ReportStatus.PENDING)
                .build();
        return reportRepository.save(report);
    }

    // ==================== CREATE REPORT ====================

    @Test
    void createReport_validListingReport_returnsCreated() throws Exception {
        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportType.LISTING);
        request.setListingId(listing.getId());
        request.setReason(ReportReason.values()[0]);
        request.setDescription("This listing seems fraudulent");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportType").value("LISTING"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createReport_ownListing_returnsBadRequest() throws Exception {
        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportType.LISTING);
        request.setListingId(listing.getId());
        request.setReason(ReportReason.values()[0]);

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reportedUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createReport_unauthenticated_returnsUnauthorized() throws Exception {
        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportType.LISTING);
        request.setListingId(listing.getId());
        request.setReason(ReportReason.values()[0]);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReport_duplicateReport_returnsBadRequest() throws Exception {
        createTestReport();

        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportType.LISTING);
        request.setListingId(listing.getId());
        request.setReason(ReportReason.values()[0]);

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createReport_missingReportType_returnsBadRequest() throws Exception {
        CreateReportRequest request = new CreateReportRequest();
        request.setListingId(listing.getId());
        request.setReason(ReportReason.values()[0]);

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReport_missingReason_returnsBadRequest() throws Exception {
        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportType.LISTING);
        request.setListingId(listing.getId());

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReport_userReport_returnsCreated() throws Exception {
        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportType.USER);
        request.setUserId(reportedUser.getId());
        request.setReason(ReportReason.values()[0]);
        request.setDescription("Suspicious user behavior");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportType").value("USER"));
    }

    @Test
    void createReport_reportSelf_returnsBadRequest() throws Exception {
        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportType.USER);
        request.setUserId(reporter.getId());
        request.setReason(ReportReason.values()[0]);

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createReport_nonExistentListing_returns4xx() throws Exception {
        CreateReportRequest request = new CreateReportRequest();
        request.setReportType(ReportType.LISTING);
        request.setListingId(99999L);
        request.setReason(ReportReason.values()[0]);

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ==================== MY REPORTS ====================

    @Test
    void getMyReports_authenticated_returnsOwnReports() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/my-reports")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyReports_noReports_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/reports/my-reports")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyReports_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/my-reports"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== CHECK REPORTED LISTING ====================

    @Test
    void hasReportedListing_hasReported_returnsTrue() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/check/listing/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReported").value(true));
    }

    @Test
    void hasReportedListing_notReported_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/v1/reports/check/listing/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReported").value(false));
    }

    @Test
    void hasReportedListing_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/check/listing/{listingId}", listing.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET SINGLE REPORT ====================

    @Test
    void getReport_ownReport_returnsOk() throws Exception {
        Report report = createTestReport();

        mockMvc.perform(get("/api/v1/reports/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(report.getId()))
                .andExpect(jsonPath("$.data.reportType").value("LISTING"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getReport_otherUsersReport_returnsForbidden() throws Exception {
        Report report = createTestReport();

        mockMvc.perform(get("/api/v1/reports/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + reportedUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReport_nonExistent_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/reports/{reportId}", 99999L)
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getReport_unauthenticated_returnsUnauthorized() throws Exception {
        Report report = createTestReport();

        mockMvc.perform(get("/api/v1/reports/{reportId}", report.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ==================== ADMIN: GET ALL REPORTS ====================

    @Test
    void getAllReports_asAdmin_returnsOk() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllReports_filteredByStatus_returnsFiltered() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllReports_filteredByType_returnsFiltered() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("type", "LISTING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllReports_asRegularUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllReports_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== ADMIN: PENDING COUNT ====================

    @Test
    void getPendingReportsCount_asAdmin_returnsCount() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin/pending-count")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void getPendingReportsCount_noReports_returnsZero() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin/pending-count")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void getPendingReportsCount_asRegularUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin/pending-count")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isForbidden());
    }

    // ==================== ADMIN: GET REPORT BY ID ====================

    @Test
    void getReportAsAdmin_returnsWithAdminFields() throws Exception {
        Report report = createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(report.getId()))
                .andExpect(jsonPath("$.data.reporterUsername").value("reporter"));
    }

    @Test
    void getReportAsAdmin_nonExistent_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin/{reportId}", 99999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getReportAsAdmin_asRegularUser_returnsForbidden() throws Exception {
        Report report = createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isForbidden());
    }

    // ==================== ADMIN: UPDATE REPORT ====================

    @Test
    void updateReport_asAdmin_returnsOk() throws Exception {
        Report report = createTestReport();

        UpdateReportRequest request = new UpdateReportRequest();
        request.setStatus(ReportStatus.RESOLVED);
        request.setAdminNotes("Reviewed and resolved");

        mockMvc.perform(put("/api/v1/reports/admin/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report updated successfully"))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.adminNotes").value("Reviewed and resolved"));

        Report updated = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(updated.getReviewedBy()).isNotNull();
        assertThat(updated.getReviewedAt()).isNotNull();
    }

    @Test
    void updateReport_dismissReport_returnsOk() throws Exception {
        Report report = createTestReport();

        UpdateReportRequest request = new UpdateReportRequest();
        request.setStatus(ReportStatus.DISMISSED);
        request.setAdminNotes("No violation found");

        mockMvc.perform(put("/api/v1/reports/admin/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISMISSED"));
    }

    @Test
    void updateReport_missingStatus_returnsBadRequest() throws Exception {
        Report report = createTestReport();

        UpdateReportRequest request = new UpdateReportRequest();
        request.setAdminNotes("Notes without status");

        mockMvc.perform(put("/api/v1/reports/admin/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReport_asRegularUser_returnsForbidden() throws Exception {
        Report report = createTestReport();

        UpdateReportRequest request = new UpdateReportRequest();
        request.setStatus(ReportStatus.RESOLVED);

        mockMvc.perform(put("/api/v1/reports/admin/{reportId}", report.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReport_nonExistent_returns4xx() throws Exception {
        UpdateReportRequest request = new UpdateReportRequest();
        request.setStatus(ReportStatus.RESOLVED);

        mockMvc.perform(put("/api/v1/reports/admin/{reportId}", 99999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ==================== ADMIN: GET BY STATUS ====================

    @Test
    void getReportsByStatus_asAdmin_returnsFiltered() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin/status/{status}", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getReportsByStatus_noMatches_returnsEmptyPage() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin/status/{status}", "RESOLVED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getReportsByStatus_asRegularUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin/status/{status}", "PENDING")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isForbidden());
    }

    // ==================== ADMIN: GET BY TYPE ====================

    @Test
    void getReportsByType_asAdmin_returnsFiltered() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin/type/{type}", "LISTING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getReportsByType_noMatches_returnsEmptyPage() throws Exception {
        createTestReport();

        mockMvc.perform(get("/api/v1/reports/admin/type/{type}", "USER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getReportsByType_asRegularUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin/type/{type}", "LISTING")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReportsByType_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/admin/type/{type}", "LISTING"))
                .andExpect(status().isUnauthorized());
    }
}