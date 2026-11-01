package com.vibelang.vocabulary;

import com.vibelang.common.NotFoundException;
import com.vibelang.subject.Subject;
import com.vibelang.subject.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class VocabularyService {

    private final VocabularyRepository vocabulary;
    private final SubjectService subjectService;

    public VocabularyService(VocabularyRepository vocabulary, SubjectService subjectService) {
        this.vocabulary = vocabulary;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<VocabularyDto> findBySubject(Long subjectId) {
        subjectService.getEntity(subjectId);
        return vocabulary.findBySubject(subjectId).stream().map(VocabularyDto::from).toList();
    }

    public VocabularyDto create(Long subjectId, VocabularyRequest request) {
        return createAll(subjectId, List.of(request)).getFirst();
    }

    public List<VocabularyDto> createAll(Long subjectId, List<VocabularyRequest> requests) {
        Subject subject = subjectService.getEntity(subjectId);
        int position = vocabulary.maxPosition(subjectId);
        List<Vocabulary> created = new ArrayList<>();
        for (VocabularyRequest request : requests) {
            Vocabulary v = new Vocabulary();
            v.setSubject(subject);
            v.setPosition(++position);
            apply(v, request);
            created.add(v);
        }
        return vocabulary.saveAll(created).stream().map(VocabularyDto::from).toList();
    }

    public VocabularyDto update(Long id, VocabularyRequest request) {
        Vocabulary v = getEntity(id);
        apply(v, request);
        return VocabularyDto.from(v);
    }

    public void delete(Long id) {
        vocabulary.delete(getEntity(id));
    }

    private Vocabulary getEntity(Long id) {
        return vocabulary.findById(id).orElseThrow(() -> NotFoundException.of("Vocabulary", id));
    }

    private static void apply(Vocabulary v, VocabularyRequest request) {
        v.setTerm(request.term().trim());
        v.setTranslation(request.translation().trim());
        v.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
    }
}
