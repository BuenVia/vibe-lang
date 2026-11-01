package com.vibelang.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("select c from Category c where c.course.id = :courseId order by c.position, c.id")
    List<Category> findByCourse(@Param("courseId") Long courseId);

    @Query("select coalesce(max(c.position), 0) from Category c where c.course.id = :courseId")
    int maxPosition(@Param("courseId") Long courseId);
}
