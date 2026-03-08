package com.gemstore.backend.services.report;

import com.gemstore.backend.dtos.report.CreateReportRequest;
import com.gemstore.backend.dtos.report.ReportResponse;
import com.gemstore.backend.dtos.report.UpdateReportRequest;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.report.*;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.exceptions.BadRequestException;
import com.gemstore.backend.exceptions.ResourceNotFoundException;
import com.gemstore.backend.repositories.listing.ListingRepository;
import com.gemstore.backend.repositories.message.MessageRepository;
import com.gemstore.backend.repositories.report.ReportRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService - Unit Tests")
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private MessageRepository messageRepository;

    @InjectMocks
    private ReportService reportService;

    private User reporter;
    private User reportedUser;
    private User adminUser;
    private Listing listing;

    @BeforeEach
    void setUp() {
        reporter = new User();
        reporter.setId(1L);
        reporter.setUsername("reporter");

        reportedUser = new User();
        reportedUser.setId(2L);
        reportedUser.setUsername("reported");

        adminUser = new User();
        adminUser.setId(3L);
        adminUser.setUsername("admin");
        adminUser.setRole("ADMIN");

        listing = new Listing();
        listing.setId(10L);
        listing.setTitle("Blue Sapphire");
        listing.setSeller(reportedUser);
    }

    @Nested
    @DisplayName("createReport()")
    class CreateReport {

        @Test
        @DisplayName("TC-RPT-001: Should create listing report")
        void shouldCreateListingReport() {
            CreateReportRequest req = new CreateReportRequest();
            req.setReportType(ReportType.LISTING);
            req.setReason(ReportReason.FAKE_LISTING);
            req.setDescription("Fake gemstone");
            req.setListingId(10L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
            when(reportRepository.existsByReporterIdAndReportedListingId(1L, 10L)).thenReturn(false);
            when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
                Report r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });
            when(reportRepository.countByReportedListingId(10L)).thenReturn(1L);

            ReportResponse result = reportService.createReport(1L, req);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(ReportStatus.PENDING);
            verify(reportRepository).save(any(Report.class));
        }

        @Test
        @DisplayName("TC-RPT-002: Should reject duplicate listing report")
        void shouldRejectDuplicateListingReport() {
            CreateReportRequest req = new CreateReportRequest();
            req.setReportType(ReportType.LISTING);
            req.setReason(ReportReason.FAKE_LISTING);
            req.setListingId(10L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
            when(reportRepository.existsByReporterIdAndReportedListingId(1L, 10L)).thenReturn(true);

            assertThatThrownBy(() -> reportService.createReport(1L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already reported");
        }

        @Test
        @DisplayName("TC-RPT-003: Should reject self-report on own listing")
        void shouldRejectSelfListingReport() {
            listing.setSeller(reporter); // reporter is the seller

            CreateReportRequest req = new CreateReportRequest();
            req.setReportType(ReportType.LISTING);
            req.setReason(ReportReason.FAKE_LISTING);
            req.setListingId(10L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
            when(reportRepository.existsByReporterIdAndReportedListingId(1L, 10L)).thenReturn(false);
            when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> reportService.createReport(1L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("own listing");
        }

        @Test
        @DisplayName("TC-RPT-004: Should reject self user report")
        void shouldRejectSelfUserReport() {
            CreateReportRequest req = new CreateReportRequest();
            req.setReportType(ReportType.USER);
            req.setReason(ReportReason.HARASSMENT);
            req.setUserId(1L); // reporting self

            when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
            when(reportRepository.existsByReporterIdAndReportedUserId(1L, 1L)).thenReturn(false);

            // findById for reported user returns reporter (same user)
            when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));

            assertThatThrownBy(() -> reportService.createReport(1L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("yourself");
        }

        @Test
        @DisplayName("TC-RPT-005: Should reject when listing ID missing for LISTING type")
        void shouldRejectMissingListingId() {
            CreateReportRequest req = new CreateReportRequest();
            req.setReportType(ReportType.LISTING);
            req.setReason(ReportReason.FAKE_LISTING);
            req.setListingId(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));

            assertThatThrownBy(() -> reportService.createReport(1L, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Listing ID is required");
        }
    }

    @Nested
    @DisplayName("getReport()")
    class GetReport {

        @Test
        @DisplayName("TC-RPT-006: Should return report by ID")
        void shouldReturnReport() {
            Report report = Report.builder()
                    .id(1L).reporter(reporter).reportType(ReportType.LISTING)
                    .reason(ReportReason.FAKE_LISTING).status(ReportStatus.PENDING)
                    .reportedListing(listing).build();

            when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

            ReportResponse result = reportService.getReport(1L, true);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC-RPT-007: Should throw when report not found")
        void shouldThrowWhenNotFound() {
            when(reportRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.getReport(999L, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPendingReportsCount()")
    class GetPendingCount {

        @Test
        @DisplayName("TC-RPT-008: Should return pending count")
        void shouldReturnPendingCount() {
            when(reportRepository.countByStatus(ReportStatus.PENDING)).thenReturn(5L);

            assertThat(reportService.getPendingReportsCount()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("hasUserReportedListing()")
    class HasUserReported {

        @Test
        @DisplayName("TC-RPT-009: Should return true when already reported")
        void shouldReturnTrue() {
            when(reportRepository.existsByReporterIdAndReportedListingId(1L, 10L)).thenReturn(true);

            assertThat(reportService.hasUserReportedListing(1L, 10L)).isTrue();
        }

        @Test
        @DisplayName("TC-RPT-010: Should return false when not reported")
        void shouldReturnFalse() {
            when(reportRepository.existsByReporterIdAndReportedListingId(1L, 10L)).thenReturn(false);

            assertThat(reportService.hasUserReportedListing(1L, 10L)).isFalse();
        }
    }
}