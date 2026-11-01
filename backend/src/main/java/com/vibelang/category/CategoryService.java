package com.vibelang.category;

import com.vibelang.common.NotFoundException;
import com.vibelang.common.Positions;
import com.vibelang.course.Course;
import com.vibelang.course.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categories;
    private final CourseService courseService;

    public CategoryService(CategoryRepository categories, CourseService courseService) {
        this.categories = categories;
        this.courseService = courseService;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> findByCourse(Long courseId) {
        courseService.getEntity(courseId);
        return categories.findByCourse(courseId).stream().map(CategoryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto get(Long id) {
        return CategoryDto.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Category getEntity(Long id) {
        return categories.findById(id).orElseThrow(() -> NotFoundException.of("Category", id));
    }

    public CategoryDto create(Long courseId, CategoryRequest request) {
        Course course = courseService.getEntity(courseId);
        Category category = new Category();
        category.setCourse(course);
        apply(category, request);
        category.setPosition(categories.maxPosition(courseId) + 1);
        return CategoryDto.from(categories.saveAndFlush(category));
    }

    public CategoryDto update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        apply(category, request);
        categories.flush();
        return CategoryDto.from(category);
    }

    /** Subjects (and their lessons and vocabulary) are removed by ON DELETE CASCADE. */
    public void delete(Long id) {
        categories.delete(getEntity(id));
    }

    public List<CategoryDto> reorder(Long courseId, List<Long> orderedIds) {
        List<Category> list = categories.findByCourse(courseId);
        Positions.apply(list, orderedIds, Category::getId, Category::setPosition);
        return findByCourse(courseId);
    }

    private static void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
    }
}
