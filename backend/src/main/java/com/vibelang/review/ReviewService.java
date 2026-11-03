package com.vibelang.review;

import com.vibelang.common.NotFoundException;
import com.vibelang.course.CourseService;
import com.vibelang.sentence.Sentence;
import com.vibelang.sentence.SentenceDto;
import com.vibelang.sentence.SentenceRepository;
import com.vibelang.vocabulary.Vocabulary;
import com.vibelang.vocabulary.VocabularyDto;
import com.vibelang.vocabulary.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    private final ReviewStateRepository states;
    private final ReviewLogRepository logs;
    private final VocabularyRepository vocabulary;
    private final SentenceRepository sentences;
    private final CourseService courseService;
    private final Clock clock;

    public ReviewService(ReviewStateRepository states, ReviewLogRepository logs, VocabularyRepository vocabulary,
                         SentenceRepository sentences, CourseService courseService, Clock clock) {
        this.states = states;
        this.logs = logs;
        this.vocabulary = vocabulary;
        this.sentences = sentences;
        this.courseService = courseService;
        this.clock = clock;
    }

    public ReviewResultDto review(ReviewRequest request) {
        Long vocabularyId = request.vocabularyId();
        Long sentenceId = request.sentenceId();
        if ((vocabularyId == null) == (sentenceId == null)) {
            throw new IllegalArgumentException("Set exactly one of vocabularyId and sentenceId");
        }
        ZoneId zone = Zones.parse(request.zone());
        Instant now = clock.instant();

        ReviewState state;
        if (vocabularyId != null) {
            if (!vocabulary.existsById(vocabularyId)) throw NotFoundException.of("Vocabulary", vocabularyId);
            state = states.findByVocabularyId(vocabularyId).orElseGet(() -> newState(vocabularyId, null, now));
        } else {
            if (!sentences.existsById(sentenceId)) throw NotFoundException.of("Sentence", sentenceId);
            state = states.findBySentenceId(sentenceId).orElseGet(() -> newState(null, sentenceId, now));
        }

        boolean isNew = state.getReviews() == 0;
        ReviewScheduler.Result result = ReviewScheduler.schedule(
                state.getBox(),
                isNew ? null : state.getDueAt(),
                DueSelector.sameDay(state.getLastReviewedAt(), now, zone),
                request.correct(),
                request.responseMs(),
                request.thresholdMs(),
                now);

        state.setBox(result.box());
        state.setDueAt(result.dueAt());
        state.setLastReviewedAt(now);
        state.setLastResponseMs(request.responseMs());
        state.setReviews(state.getReviews() + 1);
        if (result.outcome() == ReviewScheduler.Outcome.LAPSED && !isNew) state.setLapses(state.getLapses() + 1);
        states.save(state);

        logs.save(new ReviewLog(vocabularyId, sentenceId, request.mode(), request.correct(), request.responseMs(),
                result.outcome(), result.box(), now));
        return ReviewResultDto.from(state, result.outcome());
    }

    @Transactional(readOnly = true)
    public DueResponse due(Long courseId, int limit, int newLimit, String zone) {
        if (limit < 1 || newLimit < 0) throw new IllegalArgumentException("limit must be ≥ 1 and newLimit ≥ 0");
        DueSelector.Selection<DueItemDto> selection = DueSelector.select(
                candidates(courseId), clock.instant(), Zones.parse(zone), limit, newLimit);
        List<DueItemDto> items = selection.items().stream()
                .map(c -> c.state() == null ? c.item() : withState(c.item(), c.state()))
                .toList();
        return new DueResponse(selection.dueCount(), selection.newCount(), items);
    }

    @Transactional(readOnly = true)
    public List<SubjectProgressDto> progress(Long courseId, String zone) {
        Instant now = clock.instant();
        ZoneId zoneId = Zones.parse(zone);
        Instant endOfToday = now.atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant();

        Map<Long, int[]> boxes = new LinkedHashMap<>();
        Map<Long, Integer> due = new LinkedHashMap<>();
        for (DueSelector.Candidate<DueItemDto> c : candidates(courseId)) {
            Long subjectId = c.item().vocabulary() != null ? c.item().vocabulary().subjectId() : c.item().sentence().subjectId();
            int box = c.state() == null ? 0 : c.state().getBox();
            boxes.computeIfAbsent(subjectId, k -> new int[ReviewScheduler.MAX_BOX + 1])[box]++;
            boolean isDue = c.state() != null && c.state().getDueAt().isBefore(endOfToday);
            due.merge(subjectId, isDue ? 1 : 0, Integer::sum);
        }
        List<SubjectProgressDto> result = new ArrayList<>();
        boxes.forEach((subjectId, counts) -> result.add(
                new SubjectProgressDto(subjectId, java.util.Arrays.stream(counts).sum(), counts, due.get(subjectId))));
        return result;
    }

    /** Every vocabulary item and sentence of the course in learning-path order, with its review state. */
    private List<DueSelector.Candidate<DueItemDto>> candidates(Long courseId) {
        courseService.getEntity(courseId);
        List<Vocabulary> words = vocabulary.findByCourse(courseId);
        List<Sentence> sents = sentences.findByCourse(courseId);
        Map<Long, ReviewState> wordStates = byKey(states.findByVocabularyIdIn(words.stream().map(Vocabulary::getId).toList()),
                ReviewState::getVocabularyId);
        Map<Long, ReviewState> sentenceStates = byKey(states.findBySentenceIdIn(sents.stream().map(Sentence::getId).toList()),
                ReviewState::getSentenceId);

        record Ordered(int subjectPosition, long subjectId, int kind, int position, long id,
                       DueSelector.Candidate<DueItemDto> candidate) {}
        List<Ordered> all = new ArrayList<>();
        for (Vocabulary v : words) {
            DueItemDto dto = new DueItemDto(DueItemDto.Type.VOCABULARY, 0, null, v.getSubject().getTitle(),
                    VocabularyDto.from(v), null);
            all.add(new Ordered(v.getSubject().getPosition(), v.getSubject().getId(), 0, v.getPosition(), v.getId(),
                    new DueSelector.Candidate<>(dto, wordStates.get(v.getId()))));
        }
        for (Sentence s : sents) {
            DueItemDto dto = new DueItemDto(DueItemDto.Type.SENTENCE, 0, null, s.getSubject().getTitle(), null,
                    SentenceDto.from(s));
            all.add(new Ordered(s.getSubject().getPosition(), s.getSubject().getId(), 1, s.getPosition(), s.getId(),
                    new DueSelector.Candidate<>(dto, sentenceStates.get(s.getId()))));
        }
        // Learning order: subject, then its words before its sentences, then each list's own order.
        return all.stream()
                .sorted(Comparator.comparingInt(Ordered::subjectPosition).thenComparingLong(Ordered::subjectId)
                        .thenComparingInt(Ordered::kind).thenComparingInt(Ordered::position)
                        .thenComparingLong(Ordered::id))
                .map(Ordered::candidate)
                .toList();
    }

    private static DueItemDto withState(DueItemDto item, ReviewState state) {
        return new DueItemDto(item.type(), state.getBox(), state.getDueAt(), item.subjectTitle(), item.vocabulary(),
                item.sentence());
    }

    private static Map<Long, ReviewState> byKey(List<ReviewState> list, Function<ReviewState, Long> key) {
        return list.stream().collect(Collectors.toMap(key, Function.identity()));
    }

    private static ReviewState newState(Long vocabularyId, Long sentenceId, Instant now) {
        ReviewState s = new ReviewState();
        s.setVocabularyId(vocabularyId);
        s.setSentenceId(sentenceId);
        s.setBox(0);
        s.setDueAt(now);
        s.setFirstReviewedAt(now);
        return s;
    }
}
