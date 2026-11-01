package com.vibelang.lesson;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subjects/{subjectId}/lesson")
public class LessonController {

    private final LessonService service;

    public LessonController(LessonService service) {
        this.service = service;
    }

    @GetMapping
    public LessonDto get(@PathVariable Long subjectId) {
        return service.get(subjectId);
    }

    @PutMapping
    public LessonDto save(@PathVariable Long subjectId, @Valid @RequestBody LessonRequest request) {
        return service.save(subjectId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long subjectId) {
        service.delete(subjectId);
    }
}
