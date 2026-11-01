package com.vibelang.subject;

import com.vibelang.common.ReorderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SubjectController {

    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @GetMapping("/courses/{courseId}/subjects")
    public List<SubjectDto> listByCourse(@PathVariable Long courseId) {
        return service.findByCourse(courseId);
    }

    @PutMapping("/courses/{courseId}/subjects/order")
    public List<SubjectDto> reorder(@PathVariable Long courseId, @Valid @RequestBody ReorderRequest request) {
        return service.reorder(courseId, request.ids());
    }

    @GetMapping("/subjects/{id}")
    public SubjectDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectDto create(@Valid @RequestBody SubjectRequest request) {
        return service.create(request);
    }

    @PutMapping("/subjects/{id}")
    public SubjectDto update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/subjects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
