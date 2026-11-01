package com.vibelang.subject;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Query("""
            select s from Subject s
            join fetch s.category c
            where c.course.id = :courseId
            order by s.position, s.id
            """)
    List<Subject> findByCourse(@Param("courseId") Long courseId);

    @Query("select coalesce(max(s.position), 0) from Subject s where s.category.course.id = :courseId")
    int maxPosition(@Param("courseId") Long courseId);
}
