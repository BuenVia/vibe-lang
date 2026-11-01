package com.vibelang.subject;

import com.vibelang.category.Category;
import com.vibelang.category.CategoryService;
import com.vibelang.common.NotFoundException;
import com.vibelang.common.Positions;
import com.vibelang.course.CourseService;
import com.vibelang.lesson.LessonRepository;
import com.vibelang.vocabulary.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjects;
    private final CategoryService categoryService;
    private final CourseService courseService;
    private final LessonRepository lessons;
    private final VocabularyRepository vocabulary;

    public SubjectService(SubjectRepository subjects, CategoryService categoryService, CourseService courseService,
                          LessonRepository lessons, VocabularyRepository vocabulary) {
        this.subjects = subjects;
        this.categoryService = categoryService;
        this.courseService = courseService;
        this.lessons = lessons;
        this.vocabulary = vocabulary;
    }

    /** All subjects of a course in learning order. */
    @Transactional(readOnly = true)
    public List<SubjectDto> findByCourse(Long courseId) {
        courseService.getEntity(courseId);
        Set<Long> withLesson = new HashSet<>(lessons.findSubjectIdsByCourse(courseId));
        Map<Long, Long> vocabCounts = new HashMap<>();
        for (Object[] row : vocabulary.countBySubjectForCourse(courseId)) {
            vocabCounts.put((Long) row[0], (Long) row[1]);
        }
        return subjects.findByCourse(courseId).stream()
                .map(s -> SubjectDto.from(s, withLesson.contains(s.getId()), vocabCounts.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectDto get(Long id) {
        return toDto(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Subject getEntity(Long id) {
        return subjects.findById(id).orElseThrow(() -> NotFoundException.of("Subject", id));
    }

    public SubjectDto create(SubjectRequest request) {
        Category category = categoryService.getEntity(request.categoryId());
        Subject subject = new Subject();
        apply(subject, request, category);
        subject.setPosition(subjects.maxPosition(category.getCourse().getId()) + 1);
        return toDto(subjects.save(subject));
    }

    public SubjectDto update(Long id, SubjectRequest request) {
        Subject subject = getEntity(id);
        Category category = categoryService.getEntity(request.categoryId());
        Long oldCourseId = subject.getCategory().getCourse().getId();
        Long newCourseId = category.getCourse().getId();
        if (!Objects.equals(oldCourseId, newCourseId)) {
            // Moved to another course: put it at the end of that course's learning order.
            subject.setPosition(subjects.maxPosition(newCourseId) + 1);
        }
        apply(subject, request, category);
        return toDto(subject);
    }

    /** The lesson and vocabulary are removed by ON DELETE CASCADE. */
    public void delete(Long id) {
        subjects.delete(getEntity(id));
    }

    public List<SubjectDto> reorder(Long courseId, List<Long> orderedIds) {
        courseService.getEntity(courseId);
        Positions.apply(subjects.findByCourse(courseId), orderedIds, Subject::getId, Subject::setPosition);
        subjects.flush();
        return findByCourse(courseId);
    }

    private SubjectDto toDto(Subject s) {
        return SubjectDto.from(s, lessons.existsBySubjectId(s.getId()), vocabulary.countBySubjectId(s.getId()));
    }

    private static void apply(Subject subject, SubjectRequest request, Category category) {
        subject.setCategory(category);
        subject.setTitle(request.title().trim());
        subject.setDescription(request.description());
    }
}
