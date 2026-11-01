package com.vibelang.lesson;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    Optional<Lesson> findBySubjectId(Long subjectId);

    boolean existsBySubjectId(Long subjectId);

    @Query("select l.subject.id from Lesson l where l.subject.category.course.id = :courseId")
    List<Long> findSubjectIdsByCourse(@Param("courseId") Long courseId);
}
