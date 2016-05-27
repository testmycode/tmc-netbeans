package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.langs.domain.TestResult;
import fi.helsinki.cs.tmc.model.SourceFileLookup;

import java.util.ArrayList;
import java.util.List;

public final class TestResultBuilder {

    private TestResultBuilder() {}

    public static List<ResultCell> buildCells(final Exercise exercise, final List<TestResult> testCaseResults, final boolean showAll) {

        final SourceFileLookup sourceFileLookup = SourceFileLookup.getDefault();
        final List<ResultCell> resultCells = new ArrayList<ResultCell>();

        for (TestResult result : testCaseResults) {

            if (showAll || !result.passed) {

                resultCells.add(new TestCaseResultCell(exercise, result, sourceFileLookup).getCell());

                // Show only first result
                if(!showAll) {
                    break;
                }
            }
        }

        return resultCells;
    }
}
