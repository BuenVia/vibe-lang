package com.vibelang.vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    @Query("select v from Vocabulary v where v.subject.id = :subjectId order by v.position, v.id")
    List<Vocabulary> findBySubject(@Param("subjectId") Long subjectId);

    long countBySubjectId(Long subjectId);

    /** All vocabulary of a course, with its subject loaded. */
    @Query("""
            select v from Vocabulary v
            join fetch v.subject s
            where s.category.course.id = :courseId
            """)
    List<Vocabulary> findByCourse(@Param("courseId") Long courseId);

    @Query("select coalesce(max(v.position), 0) from Vocabulary v where v.subject.id = :subjectId")
    int maxPosition(@Param("subjectId") Long subjectId);

    /** Rows of [subjectId (Long), count (Long)]. */
    @Query("""
            select v.subject.id, count(v) from Vocabulary v
            where v.subject.category.course.id = :courseId
            group by v.subject.id
            """)
    List<Object[]> countBySubjectForCourse(@Param("courseId") Long courseId);
}
