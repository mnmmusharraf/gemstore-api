package com.gemstore.backend.controllers.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemstore.backend.dtos.listing.request.CreateListingRequest;
import com.gemstore.backend.dtos.listing.request.ListingSearchRequest;
import com.gemstore.backend.dtos.listing.request.UpdateListingRequest;
import com.gemstore.backend.entities.listing.Listing;
import com.gemstore.backend.entities.listing.ListingStatus;
import com.gemstore.backend.entities.listing.lookup.GemstoneType;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.listing.*;
import com.gemstore.backend.repositories.listing.lookup.GemstoneTypeRepository;
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
class ListingControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private GemstoneTypeRepository gemstoneTypeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JWTService jwtService;

    // Child table repositories — must be cleared BEFORE listings
    @Autowired private ListingViewRepository listingViewRepository;
    @Autowired private ListingImageRepository listingImageRepository;
    @Autowired private ListingPriceHistoryRepository listingPriceHistoryRepository;
    @Autowired private LikeRepository likeRepository;
    @Autowired private FavoriteRepository favoriteRepository;

    private User seller;
    private User otherUser;
    private String sellerToken;
    private String otherToken;
    private GemstoneType gemstoneType;

    @BeforeEach
    void setUp() {
        // Delete child tables FIRST (order matters for FK constraints)
        listingViewRepository.deleteAll();
        listingImageRepository.deleteAll();
        listingPriceHistoryRepository.deleteAll();
        likeRepository.deleteAll();
        favoriteRepository.deleteAll();

        // Now safe to delete parent tables
        listingRepository.deleteAll();
        userRepository.deleteAll();
        gemstoneTypeRepository.deleteAll();

        // Create gemstone type lookup
        gemstoneType = new GemstoneType();
        gemstoneType.setName("Sapphire");
        gemstoneType = gemstoneTypeRepository.save(gemstoneType);

        // Create seller
        seller = User.builder()
                .displayName("Seller")
                .email("seller@example.com")
                .username("seller")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        seller = userRepository.save(seller);
        sellerToken = jwtService.generateToken(seller.getId(), seller.getUsername(), seller.getRole());

        // Create other user
        otherUser = User.builder()
                .displayName("Other")
                .email("other@example.com")
                .username("other")
                .passwordHash(passwordEncoder.encode("password123"))
                .provider("LOCAL")
                .role("USER")
                .status("ACTIVE")
                .build();
        otherUser = userRepository.save(otherUser);
        otherToken = jwtService.generateToken(otherUser.getId(), otherUser.getUsername(), otherUser.getRole());
    }

    private Listing createTestListing(User owner, String title, ListingStatus status) {
        Listing listing = Listing.builder()
                .seller(owner)
                .title(title)
                .gemstoneType(gemstoneType)
                .caratWeight(new BigDecimal("2.50"))
                .price(new BigDecimal("50000"))
                .currency("LKR")
                .status(status)
                .build();
        listing = listingRepository.save(listing);
        listing.setListingNumber("GEM-2026-" + String.format("%06d", listing.getId()));
        return listingRepository.save(listing);
    }

    private CreateListingRequest buildCreateRequest() {
        return CreateListingRequest.builder()
                .title("Beautiful Blue Sapphire")
                .description("A stunning natural sapphire")
                .gemstoneTypeId(gemstoneType.getId())
                .caratWeight(new BigDecimal("3.50"))
                .price(new BigDecimal("75000"))
                .currency("LKR")
                .build();
    }

    // ==================== CREATE ====================

    @Test
    void createListing_validRequest_returnsCreated() throws Exception {
        CreateListingRequest request = buildCreateRequest();

        mockMvc.perform(post("/api/v1/listings")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Listing created successfully"))
                .andExpect(jsonPath("$.data.title").value("Beautiful Blue Sapphire"))
                .andExpect(jsonPath("$.data.caratWeight").value(3.50));
    }

    @Test
    void createListing_unauthenticated_returnsUnauthorized() throws Exception {
        CreateListingRequest request = buildCreateRequest();

        mockMvc.perform(post("/api/v1/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createListing_missingTitle_returnsBadRequest() throws Exception {
        CreateListingRequest request = buildCreateRequest();
        request.setTitle(null);

        mockMvc.perform(post("/api/v1/listings")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createListing_missingPrice_returnsBadRequest() throws Exception {
        CreateListingRequest request = buildCreateRequest();
        request.setPrice(null);

        mockMvc.perform(post("/api/v1/listings")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET BY ID ====================

    @Test
    void getListingById_existingListing_returnsOk() throws Exception {
        Listing listing = createTestListing(seller, "Test Sapphire", ListingStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/listings/{id}", listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Sapphire"));
    }

    @Test
    void getListingById_nonExistent_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/listings/{id}", 99999L))
                .andExpect(status().is4xxClientError());
    }

    // ==================== GET LISTING DETAIL ====================

    @Test
    void getListingDetail_existingListing_returnsOk() throws Exception {
        Listing listing = createTestListing(seller, "Detail Sapphire", ListingStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/listings/{id}/detail", listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Detail Sapphire"));
    }

    @Test
    void getListingDetail_authenticated_setsUserContext() throws Exception {
        Listing listing = createTestListing(seller, "My Gem", ListingStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/listings/{id}/detail", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwner").value(true));
    }

    // ==================== GET ACTIVE LISTINGS ====================

    @Test
    void getActiveListings_returnsPagedResults() throws Exception {
        createTestListing(seller, "Gem 1", ListingStatus.ACTIVE);
        createTestListing(seller, "Gem 2", ListingStatus.ACTIVE);
        createTestListing(seller, "Archived Gem", ListingStatus.ARCHIVED);

        mockMvc.perform(get("/api/v1/listings")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    // ==================== SEARCH (GET) ====================

    @Test
    void searchListings_byQuery_returnsMatchingResults() throws Exception {
        createTestListing(seller, "Blue Sapphire Premium", ListingStatus.ACTIVE);
        createTestListing(seller, "Red Ruby Gem", ListingStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/listings/search")
                        .param("query", "Sapphire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void searchListings_withPriceRange_returnsFiltered() throws Exception {
        createTestListing(seller, "Cheap Gem", ListingStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/listings/search")
                        .param("minPrice", "10000")
                        .param("maxPrice", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== SEARCH (POST) ====================

    @Test
    void searchListingsPost_withBody_returnsResults() throws Exception {
        createTestListing(seller, "Post Search Gem", ListingStatus.ACTIVE);

        ListingSearchRequest request = ListingSearchRequest.builder()
                .gemstoneTypeId(gemstoneType.getId())
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/v1/listings/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== SELLER LISTINGS ====================

    @Test
    void getListingsBySeller_returnsSellerListings() throws Exception {
        createTestListing(seller, "Seller Gem 1", ListingStatus.ACTIVE);
        createTestListing(seller, "Seller Gem 2", ListingStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/listings/seller/{sellerId}", seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    // ==================== MY LISTINGS ====================

    @Test
    void getMyListings_authenticated_returnsOwnListings() throws Exception {
        createTestListing(seller, "My Gem 1", ListingStatus.ACTIVE);
        createTestListing(seller, "My Gem 2", ListingStatus.ARCHIVED);
        createTestListing(otherUser, "Not My Gem", ListingStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/listings/my")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void getMyListings_filteredByStatus_returnsFiltered() throws Exception {
        createTestListing(seller, "Active Gem", ListingStatus.ACTIVE);
        createTestListing(seller, "Archived Gem", ListingStatus.ARCHIVED);

        mockMvc.perform(get("/api/v1/listings/my")
                        .header("Authorization", "Bearer " + sellerToken)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void getMyListings_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/listings/my"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== UPDATE (PUT) ====================

    @Test
    void updateListing_asOwner_returnsUpdated() throws Exception {
        Listing listing = createTestListing(seller, "Original Title", ListingStatus.ACTIVE);

        UpdateListingRequest request = UpdateListingRequest.builder()
                .title("Updated Title")
                .price(new BigDecimal("80000"))
                .build();

        mockMvc.perform(put("/api/v1/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    void updateListing_asNonOwner_returnsForbiddenOrUnauthorized() throws Exception {
        Listing listing = createTestListing(seller, "Not Yours", ListingStatus.ACTIVE);

        UpdateListingRequest request = UpdateListingRequest.builder()
                .title("Hijacked")
                .build();

        mockMvc.perform(put("/api/v1/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ==================== PATCH ====================

    @Test
    void patchListing_partialUpdate_returnsUpdated() throws Exception {
        Listing listing = createTestListing(seller, "Patch Test", ListingStatus.ACTIVE);

        UpdateListingRequest request = UpdateListingRequest.builder()
                .description("New description only")
                .build();

        mockMvc.perform(patch("/api/v1/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== MARK AS SOLD ====================

    @Test
    void markAsSold_asOwner_returnsOk() throws Exception {
        Listing listing = createTestListing(seller, "Sell Me", ListingStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/listings/{id}/sold", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken)
                        .param("soldPrice", "60000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Listing marked as sold"));

        Listing updated = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(updated.getIsSold()).isTrue();
        assertThat(updated.getStatus()).isEqualTo(ListingStatus.SOLD);
    }

    @Test
    void markAsSold_asNonOwner_returns4xx() throws Exception {
        Listing listing = createTestListing(seller, "Not Yours Sold", ListingStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/listings/{id}/sold", listing.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().is4xxClientError());
    }

    // ==================== ARCHIVE ====================

    @Test
    void archiveListing_asOwner_returnsOk() throws Exception {
        Listing listing = createTestListing(seller, "Archive Me", ListingStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/listings/{id}/archive", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Listing updated = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ListingStatus.ARCHIVED);
    }

    // ==================== REACTIVATE ====================

    @Test
    void reactivateListing_asOwner_returnsOk() throws Exception {
        Listing listing = createTestListing(seller, "Reactivate Me", ListingStatus.ARCHIVED);

        mockMvc.perform(post("/api/v1/listings/{id}/reactivate", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Listing updated = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ListingStatus.ACTIVE);
    }

    @Test
    void reactivateListing_soldListing_returns4xx() throws Exception {
        Listing listing = createTestListing(seller, "Sold Gem", ListingStatus.SOLD);
        listing.setIsSold(true);
        listingRepository.save(listing);

        mockMvc.perform(post("/api/v1/listings/{id}/reactivate", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().is4xxClientError());
    }

    // ==================== DELETE ====================

    @Test
    void deleteListing_asOwner_returnsOk() throws Exception {
        Listing listing = createTestListing(seller, "Delete Me", ListingStatus.ACTIVE);

        mockMvc.perform(delete("/api/v1/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(listingRepository.findById(listing.getId())).isEmpty();
    }

    @Test
    void deleteListing_asNonOwner_returns4xx() throws Exception {
        Listing listing = createTestListing(seller, "Not Deletable", ListingStatus.ACTIVE);

        mockMvc.perform(delete("/api/v1/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().is4xxClientError());
    }

    // ==================== UPLOAD IMAGE ====================

    @Test
    void uploadListingImage_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/listings/upload"))
                .andExpect(status().isUnauthorized());
    }
}