package com.gemstore.backend.controllers.listing;

import com.gemstore.backend.dtos.common.ApiResponse;
import com. gemstore.backend.dtos. listing.lookup.LookupDTO;
import com.gemstore. backend.services.listing.LookupService;
import com.gemstore.backend.services. listing.LookupService. AllLookupsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework. web.bind.annotation.*;

import java.util.List;

/**
 * LookupController provides endpoints for all lookup/reference data. 
 * Used by frontend to populate dropdowns and filters.
 */
@RestController
@RequestMapping("/api/v1/lookups")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    /* ===================== Get All Lookups ===================== */

    /**
     * Get all lookup data in a single call.
     * Useful for initializing frontend. 
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AllLookupsResponse>> getAllLookups() {
        AllLookupsResponse lookups = lookupService.getAllLookups();
        return ResponseEntity. ok(ApiResponse.success(lookups));
    }

    /* ===================== Individual Lookups ===================== */

    @GetMapping("/gemstone-types")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getGemstoneTypes() {
        List<LookupDTO> types = lookupService.getAllGemstoneTypes();
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    @GetMapping("/colors")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getColors() {
        List<LookupDTO> colors = lookupService.getAllColors();
        return ResponseEntity.ok(ApiResponse.success(colors));
    }

    @GetMapping("/color-qualities")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getColorQualities() {
        List<LookupDTO> qualities = lookupService.getAllColorQualities();
        return ResponseEntity.ok(ApiResponse.success(qualities));
    }

    @GetMapping("/clarity-grades")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getClarityGrades() {
        List<LookupDTO> grades = lookupService.getAllClarityGrades();
        return ResponseEntity.ok(ApiResponse.success(grades));
    }

    @GetMapping("/cuts")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getCuts() {
        List<LookupDTO> cuts = lookupService.getAllCuts();
        return ResponseEntity.ok(ApiResponse.success(cuts));
    }

    @GetMapping("/origins")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getOrigins() {
        List<LookupDTO> origins = lookupService.getAllOrigins();
        return ResponseEntity.ok(ApiResponse.success(origins));
    }

    @GetMapping("/treatments")
    public ResponseEntity<ApiResponse<List<LookupDTO>>> getTreatments() {
        List<LookupDTO> treatments = lookupService.getAllTreatments();
        return ResponseEntity.ok(ApiResponse.success(treatments));
    }
}