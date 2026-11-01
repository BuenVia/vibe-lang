package com.vibelang.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderByPositionAscIdAsc();

    @Query("select coalesce(max(c.position), 0) from Course c")
    int maxPosition();
}
