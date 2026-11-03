package com.vibelang.review;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping("/reviews")
    public ReviewResultDto review(@Valid @RequestBody ReviewRequest request) {
        return service.review(request);
    }

    @GetMapping("/courses/{courseId}/due")
    public DueResponse due(@PathVariable Long courseId,
                           @RequestParam(defaultValue = "50") int limit,
                           @RequestParam(defaultValue = "10") int newLimit,
                           @RequestParam(required = false) String zone) {
        return service.due(courseId, limit, newLimit, zone);
    }

    @GetMapping("/courses/{courseId}/progress")
    public List<SubjectProgressDto> progress(@PathVariable Long courseId,
                                             @RequestParam(required = false) String zone) {
        return service.progress(courseId, zone);
    }
}
