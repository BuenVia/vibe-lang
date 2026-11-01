package com.vibelang.category;

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
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping("/courses/{courseId}/categories")
    public List<CategoryDto> list(@PathVariable Long courseId) {
        return service.findByCourse(courseId);
    }

    @PostMapping("/courses/{courseId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@PathVariable Long courseId, @Valid @RequestBody CategoryRequest request) {
        return service.create(courseId, request);
    }

    @PutMapping("/courses/{courseId}/categories/order")
    public List<CategoryDto> reorder(@PathVariable Long courseId, @Valid @RequestBody ReorderRequest request) {
        return service.reorder(courseId, request.ids());
    }

    @GetMapping("/categories/{id}")
    public CategoryDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/categories/{id}")
    public CategoryDto update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
