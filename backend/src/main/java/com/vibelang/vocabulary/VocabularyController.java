package com.vibelang.vocabulary;

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
public class VocabularyController {

    private final VocabularyService service;

    public VocabularyController(VocabularyService service) {
        this.service = service;
    }

    @GetMapping("/subjects/{subjectId}/vocabulary")
    public List<VocabularyDto> list(@PathVariable Long subjectId) {
        return service.findBySubject(subjectId);
    }

    @PostMapping("/subjects/{subjectId}/vocabulary")
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularyDto create(@PathVariable Long subjectId, @Valid @RequestBody VocabularyRequest request) {
        return service.create(subjectId, request);
    }

    @PostMapping("/subjects/{subjectId}/vocabulary/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<VocabularyDto> createAll(@PathVariable Long subjectId, @Valid @RequestBody BulkVocabularyRequest request) {
        return service.createAll(subjectId, request.items());
    }

    @PutMapping("/vocabulary/{id}")
    public VocabularyDto update(@PathVariable Long id, @Valid @RequestBody VocabularyRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/vocabulary/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
