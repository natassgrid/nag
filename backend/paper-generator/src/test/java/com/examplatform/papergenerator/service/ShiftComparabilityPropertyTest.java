package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.exception.ShiftComparabilityViolationException;
import com.examplatform.papergenerator.repository.PaperRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Property test 7.5: Shift Paper Statistical Comparability.
 *
 * <p><strong>Invariant:</strong>
 * {@code |difficultyScore(pi) - difficultyScore(pj)| / totalMarks ≤ 0.02}
 * must hold for every pair of shift papers in the same exam.
 * When violated, {@link ShiftComparabilityViolationException} is thrown.
 *
 * <p>Two sub-properties are tested:
 * <ol>
 *   <li>If all paper pairs are within the 2% tolerance → validation passes.</li>
 *   <li>If any paper pair exceeds the 2% tolerance → validation throws.</li>
 * </ol>
 *
 * Validates: Requirements 8.9
 */
class ShiftComparabilityPropertyTest {

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /**
     * Generates a pair of difficulty scores that are within the 2% relative tolerance.
     * Returns (score1, score2, totalMarks) where |score1-score2|/totalMarks ≤ 0.02.
     *
     * Strategy: generate delta as integer thousandths strictly below the 2% boundary.
     * Using (maxDeltaThousandths - 1) ensures floating-point subtraction cancellation
     * cannot push the reconstructed diff above the threshold.
     */
    @Provide
    Arbitrary<Tuple.Tuple3<Double, Double, Integer>> scorePairsWithinTolerance() {
        return Arbitraries.integers().between(50, 500)
                .flatMap(marks -> {
                    // Max allowed delta in thousandths: floor(0.02 * marks * 1000) - 1
                    // The -1 leaves a gap so floating-point cancellation can't create a violation
                    int maxDeltaThousandths = (int) Math.floor(0.02 * marks * 1000) - 1;
                    if (maxDeltaThousandths < 0) maxDeltaThousandths = 0;
                    // Use score1=1.0, score2=1.0+delta — small base avoids cancellation
                    return Arbitraries.integers().between(0, maxDeltaThousandths)
                            .map(deltaThousandths -> {
                                double delta = deltaThousandths / 1000.0;
                                return Tuple.of(1.0, 1.0 + delta, marks);
                            });
                });
    }

    /**
     * Generates a pair of difficulty scores that strictly exceed the 2% relative tolerance.
     * Returns (score1, score2, totalMarks) where |score1-score2|/totalMarks > 0.02.
     *
     * Strategy: delta = (floor(0.02 * marks * 1000) + 1 + extra) / 1000,
     * guaranteed strictly > 0.02 * totalMarks.
     */
    @Provide
    Arbitrary<Tuple.Tuple3<Double, Double, Integer>> scorePairsExceedingTolerance() {
        return Arbitraries.integers().between(50, 500)
                .flatMap(marks -> {
                    // Minimum violation delta-thousandths: floor(0.02 * marks * 1000) + 1
                    int minViolationThousandths = (int) Math.floor(0.02 * marks * 1000) + 1;
                    // Cap extra at 5% of marks to keep score additions reasonable
                    int maxExtraThousandths = (int) Math.floor(0.05 * marks * 1000);
                    double base = marks / 4.0; // base = 25% of marks, leaves room for delta
                    return Arbitraries.integers().between(0, Math.max(0, maxExtraThousandths))
                            .map(extraThousandths -> {
                                double delta = (minViolationThousandths + extraThousandths) / 1000.0;
                                return Tuple.of(base, base + delta, marks);
                            });
                });
    }

    /**
     * Generates a list of 2–5 papers all within tolerance of each other,
     * along with totalMarks.
     *
     * Strategy: all scores are 1.0 + k/1000 for k in [0, maxHalf-1], so the
     * max pairwise diff is safely below 0.02 * marks with a 1-thousandth gap
     * that absorbs any floating-point cancellation from subtraction.
     */
    @Provide
    Arbitrary<Tuple.Tuple2<List<Double>, Integer>> multiplePapersWithinTolerance() {
        return Combinators.combine(
                Arbitraries.integers().between(100, 500),
                Arbitraries.integers().between(2, 5)
        ).flatAs((marks, count) -> {
            // Max total spread in thousandths (score_max - score_min), kept 1 below the 2% boundary
            int maxSpreadThousandths = (int) Math.floor(0.02 * marks * 1000) - 1;
            if (maxSpreadThousandths < 0) maxSpreadThousandths = 0;
            return Arbitraries.integers().between(0, maxSpreadThousandths)
                    .map(spreadThousandths -> {
                        double spread = spreadThousandths / 1000.0;
                        List<Double> scores = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            // Evenly space from 1.0 to 1.0+spread
                            double fraction = count == 1 ? 0.0 : (double) i / (count - 1);
                            scores.add(1.0 + spread * fraction);
                        }
                        return Tuple.of(scores, marks);
                    });
        });
    }

    // -----------------------------------------------------------------------
    // Properties
    // -----------------------------------------------------------------------

    /**
     * Property 1: When all papers are within the 2% tolerance, validation passes.
     */
    @Property(tries = 300)
    @Label("Validation passes when all shift papers are within 2% difficulty tolerance")
    void validationPassesWhenAllPapersWithinTolerance(
            @ForAll("scorePairsWithinTolerance") Tuple.Tuple3<Double, Double, Integer> scorePairAndMarks) {

        double score1 = scorePairAndMarks.get1();
        double score2 = scorePairAndMarks.get2();
        int totalMarks = scorePairAndMarks.get3();

        // Sanity-check the generator: delta is 1 thousandth below boundary, so strictly < 0.02
        double relativeDiff = Math.abs(score1 - score2) / totalMarks;
        assertThat(relativeDiff).isLessThan(0.02);

        UUID examId = UUID.randomUUID();
        List<Paper> papers = List.of(
                buildPaper(examId, "SHIFT-A", score1),
                buildPaper(examId, "SHIFT-B", score2)
        );

        PaperRepository repo = Mockito.mock(PaperRepository.class);
        when(repo.findByExamIdAndTenantId(examId, null)).thenReturn(papers);

        ShiftComparabilityService service = new ShiftComparabilityService(repo);

        assertThatCode(() -> service.validateComparability(examId, totalMarks))
                .as("|diff|/totalMarks=%.4f ≤ 0.02 should not throw", relativeDiff)
                .doesNotThrowAnyException();
    }

    /**
     * Property 2: When any paper pair exceeds the 2% tolerance, validation throws.
     */
    @Property(tries = 300)
    @Label("Validation throws when any shift paper pair exceeds 2% difficulty tolerance")
    void validationThrowsWhenAnyPairExceedsTolerance(
            @ForAll("scorePairsExceedingTolerance") Tuple.Tuple3<Double, Double, Integer> scorePairAndMarks) {

        double score1 = scorePairAndMarks.get1();
        double score2 = scorePairAndMarks.get2();
        int totalMarks = scorePairAndMarks.get3();

        // Sanity-check the generator
        double relativeDiff = Math.abs(score1 - score2) / totalMarks;
        assertThat(relativeDiff).isGreaterThan(0.02);

        UUID examId = UUID.randomUUID();
        List<Paper> papers = List.of(
                buildPaper(examId, "SHIFT-A", score1),
                buildPaper(examId, "SHIFT-B", score2)
        );

        PaperRepository repo = Mockito.mock(PaperRepository.class);
        when(repo.findByExamIdAndTenantId(examId, null)).thenReturn(papers);

        ShiftComparabilityService service = new ShiftComparabilityService(repo);

        assertThatThrownBy(() -> service.validateComparability(examId, totalMarks))
                .as("|diff|/totalMarks=%.4f > 0.02 should throw", relativeDiff)
                .isInstanceOf(ShiftComparabilityViolationException.class)
                .satisfies(ex -> {
                    ShiftComparabilityViolationException scEx = (ShiftComparabilityViolationException) ex;
                    assertThat(scEx.getExamId()).isEqualTo(examId);
                    assertThat(scEx.getViolations()).isNotEmpty();
                });
    }

    /**
     * Property 3: Multiple papers all within tolerance — validation always passes.
     */
    @Property(tries = 200)
    @Label("Validation passes for multiple papers all within tolerance")
    void validationPassesForMultiplePapersWithinTolerance(
            @ForAll("multiplePapersWithinTolerance") Tuple.Tuple2<List<Double>, Integer> papersAndMarks) {

        List<Double> scores = papersAndMarks.get1();
        int totalMarks = papersAndMarks.get2();

        UUID examId = UUID.randomUUID();
        List<Paper> papers = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            papers.add(buildPaper(examId, "SHIFT-" + (char) ('A' + i), scores.get(i)));
        }

        PaperRepository repo = Mockito.mock(PaperRepository.class);
        when(repo.findByExamIdAndTenantId(examId, null)).thenReturn(papers);

        ShiftComparabilityService service = new ShiftComparabilityService(repo);

        // Verify generator invariant: max pairwise relative diff is strictly < 0.02
        // (generator guarantees a 1-thousandth gap below the boundary)
        double maxRelDiff = 0.0;
        for (int i = 0; i < scores.size(); i++) {
            for (int j = i + 1; j < scores.size(); j++) {
                double rel = Math.abs(scores.get(i) - scores.get(j)) / totalMarks;
                if (rel > maxRelDiff) maxRelDiff = rel;
            }
        }
        assertThat(maxRelDiff).isLessThan(0.02);

        assertThatCode(() -> service.validateComparability(examId, totalMarks))
                .doesNotThrowAnyException();
    }

    /**
     * Property 4: With only one paper, validation always passes (nothing to compare).
     */
    @Property(tries = 100)
    @Label("Validation always passes with a single paper")
    void validationAlwaysPassesWithSinglePaper(
            @ForAll("singlePaperScore") double score,
            @ForAll("totalMarks") int totalMarks) {

        UUID examId = UUID.randomUUID();
        List<Paper> papers = List.of(buildPaper(examId, "SHIFT-ONLY", score));

        PaperRepository repo = Mockito.mock(PaperRepository.class);
        when(repo.findByExamIdAndTenantId(examId, null)).thenReturn(papers);

        ShiftComparabilityService service = new ShiftComparabilityService(repo);

        assertThatCode(() -> service.validateComparability(examId, totalMarks))
                .doesNotThrowAnyException();
    }

    @Provide
    Arbitrary<Double> singlePaperScore() {
        return Arbitraries.doubles().between(1.0, 101.0).ofScale(4);
    }

    @Provide
    Arbitrary<Integer> totalMarks() {
        return Arbitraries.integers().between(50, 500);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Paper buildPaper(UUID examId, String shiftId, double difficultyScore) {
        Paper paper = Paper.builder()
                .examId(examId)
                .shiftId(shiftId)
                .difficultyScore(difficultyScore)
                .status("DRAFT")
                .build();
        try {
            var field = paper.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(paper, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return paper;
    }
}
