package com.vibelang.sentence;

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
public class SentenceController {

    private final SentenceService service;

    public SentenceController(SentenceService service) {
        this.service = service;
    }

    @GetMapping("/subjects/{subjectId}/sentences")
    public List<SentenceDto> list(@PathVariable Long subjectId) {
        return service.findBySubject(subjectId);
    }

    @PostMapping("/subjects/{subjectId}/sentences")
    @ResponseStatus(HttpStatus.CREATED)
    public SentenceDto create(@PathVariable Long subjectId, @Valid @RequestBody SentenceRequest request) {
        return service.create(subjectId, request);
    }

    @PostMapping("/subjects/{subjectId}/sentences/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SentenceDto> createAll(@PathVariable Long subjectId, @Valid @RequestBody BulkSentenceRequest request) {
        return service.createAll(subjectId, request.items());
    }

    @PutMapping("/sentences/{id}")
    public SentenceDto update(@PathVariable Long id, @Valid @RequestBody SentenceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/sentences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
