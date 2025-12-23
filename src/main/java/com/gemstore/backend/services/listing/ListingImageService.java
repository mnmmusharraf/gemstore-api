package com.gemstore.backend.services.listing;

import com.gemstore.backend.entities. listing.Listing;
import com. gemstore.backend.entities.listing.ListingImage;
import com.gemstore.backend.repositories.listing.ListingImageRepository;
import com.gemstore.backend.services.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.stereotype.Service;
import org.springframework.transaction.annotation. Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ListingImageService handles listing image operations. 
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListingImageService {

    private final ListingImageRepository listingImageRepository;
    private final FileStorageService fileStorageService;


    private static final int MAX_IMAGES = 10;

    @Transactional
    public void addImages(Listing listing, List<String> imageUrls, Integer primaryIndex) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        if (imageUrls.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("Maximum " + MAX_IMAGES + " images allowed");
        }

        int index = 0;
        for (String url : imageUrls) {
            ListingImage image = ListingImage.builder()
                    .listing(listing)
                    .imageUrl(url)
                    . displayOrder(index)
                    . isPrimary(index == (primaryIndex != null ? primaryIndex :  0))
                    .build();

            listing.addImage(image);
            index++;
        }

        log.info("Added {} images to listing:  {}", imageUrls.size(), listing.getId());
    }

    @Transactional
    public void updateImages(Listing listing, List<String> imageUrls, Integer primaryIndex) {
        // Remove existing images
        listingImageRepository.deleteByListingId(listing.getId());
        listing.getImages().clear();

        // Add new images
        addImages(listing, imageUrls, primaryIndex);

        log.info("Updated images for listing: {}", listing.getId());
    }

    @Transactional
    public void setPrimaryImage(Long listingId, Long imageId) {
        // Clear current primary
        listingImageRepository.clearPrimaryImage(listingId);

        // Set new primary
        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));

        if (! image.getListing().getId().equals(listingId)) {
            throw new IllegalArgumentException("Image does not belong to this listing");
        }

        image.setIsPrimary(true);
        listingImageRepository.save(image);

        log.info("Set primary image:  {} for listing: {}", imageId, listingId);
    }

    @Transactional
    public void deleteImage(Long listingId, Long imageId, Long userId) {
        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));

        if (!image.getListing().getId().equals(listingId)) {
            throw new IllegalArgumentException("Image does not belong to this listing");
        }

        if (! image.getListing().getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete images from your own listings");
        }

        // If deleting primary, set first remaining as primary
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        listingImageRepository.delete(image);

        if (wasPrimary) {
            List<ListingImage> remaining = listingImageRepository
                    .findByListingIdOrderByDisplayOrderAsc(listingId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsPrimary(true);
                listingImageRepository. save(remaining.get(0));
            }
        }

        log.info("Deleted image: {} from listing: {}", imageId, listingId);
    }

    @Transactional(readOnly = true)
    public List<ListingImage> getImagesForListing(Long listingId) {
        return listingImageRepository.findByListingIdOrderByDisplayOrderAsc(listingId);
    }

    // ListingService.java
    @Transactional
    public String uploadListingImage(Long userId, MultipartFile file) {
        // -- Validate file --
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }

        // -- Use the storage service --
        return fileStorageService.uploadFile(file, "listing-images/" + userId);
    }
}