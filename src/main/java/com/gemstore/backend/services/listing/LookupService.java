package com.gemstore.backend. services.listing;

import com. gemstore.backend. dtos.listing.lookup.LookupDTO;
import com.gemstore. backend.entities.listing.lookup.*;
import com.gemstore.backend.mappers.listing.LookupMapper;
import com.gemstore.backend.repositories. listing.lookup.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * LookupService provides access to all lookup/reference data. 
 * Results are cached since lookup data rarely changes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LookupService {

    private final GemstoneTypeRepository gemstoneTypeRepository;
    private final ColorRepository colorRepository;
    private final ColorQualityRepository colorQualityRepository;
    private final ClarityGradeRepository clarityGradeRepository;
    private final CutRepository cutRepository;
    private final OriginRepository originRepository;
    private final TreatmentRepository treatmentRepository;
    private final LookupMapper lookupMapper;

    /* ===================== Gemstone Types ===================== */

    @Cacheable(value = "gemstoneTypes")
    public List<LookupDTO> getAllGemstoneTypes() {
        List<GemstoneType> types = gemstoneTypeRepository.findByIsActiveTrue();
        return lookupMapper.toGemstoneTypeDTOList(types);
    }

    public GemstoneType getGemstoneTypeById(Integer id) {
        return gemstoneTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gemstone type not found: " + id));
    }

    /* ===================== Colors ===================== */

    @Cacheable(value = "colors")
    public List<LookupDTO> getAllColors() {
        List<Color> colors = colorRepository.findByIsActiveTrue();
        return lookupMapper.toColorDTOList(colors);
    }

    public Color getColorById(Integer id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Color not found: " + id));
    }

    /* ===================== Color Qualities ===================== */

    @Cacheable(value = "colorQualities")
    public List<LookupDTO> getAllColorQualities() {
        List<ColorQuality> qualities = colorQualityRepository.findByIsActiveTrueOrderByRankAsc();
        return lookupMapper.toColorQualityDTOList(qualities);
    }

    public ColorQuality getColorQualityById(Integer id) {
        return colorQualityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Color quality not found: " + id));
    }

    /* ===================== Clarity Grades ===================== */

    @Cacheable(value = "clarityGrades")
    public List<LookupDTO> getAllClarityGrades() {
        List<ClarityGrade> grades = clarityGradeRepository.findByIsActiveTrueOrderByRankAsc();
        return lookupMapper.toClarityGradeDTOList(grades);
    }

    public ClarityGrade getClarityGradeById(Integer id) {
        return clarityGradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Clarity grade not found: " + id));
    }

    /* ===================== Cuts ===================== */

    @Cacheable(value = "cuts")
    public List<LookupDTO> getAllCuts() {
        List<Cut> cuts = cutRepository.findByIsActiveTrue();
        return lookupMapper.toCutDTOList(cuts);
    }

    public Cut getCutById(Integer id) {
        return cutRepository. findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cut not found:  " + id));
    }

    /* ===================== Origins ===================== */

    @Cacheable(value = "origins")
    public List<LookupDTO> getAllOrigins() {
        List<Origin> origins = originRepository.findByIsActiveTrue();
        return lookupMapper.toOriginDTOList(origins);
    }

    public Origin getOriginById(Integer id) {
        return originRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Origin not found: " + id));
    }

    /* ===================== Treatments ===================== */

    @Cacheable(value = "treatments")
    public List<LookupDTO> getAllTreatments() {
        List<Treatment> treatments = treatmentRepository.findByIsActiveTrue();
        return lookupMapper. toTreatmentDTOList(treatments);
    }

    public Treatment getTreatmentById(Integer id) {
        return treatmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + id));
    }

    /* ===================== All Lookups Combined ===================== */

    /**
     * Returns all lookup data in a single call.
     * Useful for initializing frontend dropdowns.
     */
    public AllLookupsResponse getAllLookups() {
        return AllLookupsResponse.builder()
                .gemstoneTypes(getAllGemstoneTypes())
                .colors(getAllColors())
                .colorQualities(getAllColorQualities())
                .clarityGrades(getAllClarityGrades())
                .cuts(getAllCuts())
                .origins(getAllOrigins())
                .treatments(getAllTreatments())
                .build();
    }

    /* ===================== Response DTO ===================== */

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AllLookupsResponse {
        private List<LookupDTO> gemstoneTypes;
        private List<LookupDTO> colors;
        private List<LookupDTO> colorQualities;
        private List<LookupDTO> clarityGrades;
        private List<LookupDTO> cuts;
        private List<LookupDTO> origins;
        private List<LookupDTO> treatments;
    }
}