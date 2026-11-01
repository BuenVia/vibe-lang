package com.vibelang.lesson;

import com.vibelang.common.NotFoundException;
import com.vibelang.subject.Subject;
import com.vibelang.subject.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class LessonService {

    private final LessonRepository lessons;
    private final SubjectService subjectService;

    public LessonService(LessonRepository lessons, SubjectService subjectService) {
        this.lessons = lessons;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public LessonDto get(Long subjectId) {
        subjectService.getEntity(subjectId);
        return lessons.findBySubjectId(subjectId)
                .map(LessonDto::from)
                .orElseThrow(() -> new NotFoundException("Subject " + subjectId + " has no lesson"));
    }

    /** Creates the subject's lesson, or replaces its content if it already exists. */
    public LessonDto save(Long subjectId, LessonRequest request) {
        Subject subject = subjectService.getEntity(subjectId);
        Lesson lesson = lessons.findBySubjectId(subjectId).orElseGet(() -> {
            Lesson l = new Lesson();
            l.setSubject(subject);
            return l;
        });
        lesson.setContent(request.content());
        lesson.setUpdatedAt(Instant.now());
        return LessonDto.from(lessons.save(lesson));
    }

    public void delete(Long subjectId) {
        Lesson lesson = lessons.findBySubjectId(subjectId)
                .orElseThrow(() -> new NotFoundException("Subject " + subjectId + " has no lesson"));
        lessons.delete(lesson);
    }
}
