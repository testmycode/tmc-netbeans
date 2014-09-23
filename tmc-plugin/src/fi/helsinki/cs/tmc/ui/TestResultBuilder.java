package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.data.Exercise;
import fi.helsinki.cs.tmc.data.TestCaseResult;
import fi.helsinki.cs.tmc.model.SourceFileLookup;

import java.util.ArrayList;
import java.util.List;

public final class TestResultBuilder {

    private TestResultBuilder() {}

    public static List<ResultCell> buildCells(final Exercise exercise, final List<TestCaseResult> testCaseResults, final boolean showAll) {

        final SourceFileLookup sourceFileLookup = SourceFileLookup.getDefault();
        final List<ResultCell> resultCells = new ArrayList<ResultCell>();

        for (TestCaseResult result : testCaseResults) {

            if (showAll || !result.isSuccessful()) {

                resultCells.add(new TestCaseResultCell(exercise, result, sourceFileLookup, result.getValgrindFailed()).getCell());

                // Show only first result
                if(!showAll) {
                    break;
                }
            }
        }

        return resultCells;
    }
}
