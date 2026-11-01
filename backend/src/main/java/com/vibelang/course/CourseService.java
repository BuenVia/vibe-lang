package com.vibelang.course;

import com.vibelang.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courses;

    public CourseService(CourseRepository courses) {
        this.courses = courses;
    }

    @Transactional(readOnly = true)
    public List<CourseDto> findAll() {
        return courses.findAllByOrderByPositionAscIdAsc().stream().map(CourseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CourseDto get(Long id) {
        return CourseDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Course getEntity(Long id) {
        return courses.findById(id).orElseThrow(() -> NotFoundException.of("Course", id));
    }

    public CourseDto create(CourseRequest request) {
        Course course = new Course();
        apply(course, request);
        course.setPosition(courses.maxPosition() + 1);
        return CourseDto.from(courses.save(course));
    }

    public CourseDto update(Long id, CourseRequest request) {
        Course course = getEntity(id);
        apply(course, request);
        return CourseDto.from(course);
    }

    /** Categories, subjects, lessons and vocabulary are removed by ON DELETE CASCADE. */
    public void delete(Long id) {
        courses.delete(getEntity(id));
    }

    private static void apply(Course course, CourseRequest request) {
        course.setLanguage(request.language().trim());
        course.setLevel(request.level().trim());
        course.setDescription(request.description());
    }
}
