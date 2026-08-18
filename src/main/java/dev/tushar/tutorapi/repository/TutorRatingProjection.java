package dev.tushar.tutorapi.repository;

/**
 * Per-tutor aggregate of {@link dev.tushar.tutorapi.entity.Review} rows, produced by
 * {@link ReviewRepository#ratingSummaries}. Lets the catalog enrich each tutor with a star
 * average + count in a single grouped query instead of one query per tutor.
 */
public interface TutorRatingProjection {

    Long getTutorId();

    Double getAverageRating();

    long getReviewCount();
}
