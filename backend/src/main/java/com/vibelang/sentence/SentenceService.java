package com.vibelang.sentence;

import com.vibelang.common.NotFoundException;
import com.vibelang.subject.Subject;
import com.vibelang.subject.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SentenceService {

    private final SentenceRepository sentences;
    private final SubjectService subjectService;

    public SentenceService(SentenceRepository sentences, SubjectService subjectService) {
        this.sentences = sentences;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<SentenceDto> findBySubject(Long subjectId) {
        subjectService.getEntity(subjectId);
        return sentences.findBySubject(subjectId).stream().map(SentenceDto::from).toList();
    }

    public SentenceDto create(Long subjectId, SentenceRequest request) {
        return createAll(subjectId, List.of(request)).getFirst();
    }

    public List<SentenceDto> createAll(Long subjectId, List<SentenceRequest> requests) {
        Subject subject = subjectService.getEntity(subjectId);
        int position = sentences.maxPosition(subjectId);
        List<Sentence> created = new ArrayList<>();
        for (SentenceRequest request : requests) {
            Sentence s = new Sentence();
            s.setSubject(subject);
            s.setPosition(++position);
            apply(s, request);
            created.add(s);
        }
        return sentences.saveAll(created).stream().map(SentenceDto::from).toList();
    }

    public SentenceDto update(Long id, SentenceRequest request) {
        Sentence s = getEntity(id);
        apply(s, request);
        return SentenceDto.from(s);
    }

    public void delete(Long id) {
        sentences.delete(getEntity(id));
    }

    private Sentence getEntity(Long id) {
        return sentences.findById(id).orElseThrow(() -> NotFoundException.of("Sentence", id));
    }

    private static void apply(Sentence s, SentenceRequest request) {
        s.setText(request.text().trim());
        s.setTranslation(blankToNull(request.translation()));
        s.setHint(blankToNull(request.hint()));
        s.setExplanation(blankToNull(request.explanation()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
