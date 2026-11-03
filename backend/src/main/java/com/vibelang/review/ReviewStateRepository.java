package com.vibelang.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewStateRepository extends JpaRepository<ReviewState, Long> {

    Optional<ReviewState> findByVocabularyId(Long vocabularyId);

    Optional<ReviewState> findBySentenceId(Long sentenceId);

    List<ReviewState> findByVocabularyIdIn(Collection<Long> ids);

    List<ReviewState> findBySentenceIdIn(Collection<Long> ids);
}
