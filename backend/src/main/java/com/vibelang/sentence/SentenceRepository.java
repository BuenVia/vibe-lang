package com.vibelang.sentence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    @Query("select s from Sentence s where s.subject.id = :subjectId order by s.position, s.id")
    List<Sentence> findBySubject(@Param("subjectId") Long subjectId);

    long countBySubjectId(Long subjectId);

    @Query("select coalesce(max(s.position), 0) from Sentence s where s.subject.id = :subjectId")
    int maxPosition(@Param("subjectId") Long subjectId);

    /** Rows of [subjectId (Long), count (Long)]. */
    @Query("""
            select s.subject.id, count(s) from Sentence s
            where s.subject.category.course.id = :courseId
            group by s.subject.id
            """)
    List<Object[]> countBySubjectForCourse(@Param("courseId") Long courseId);
}
