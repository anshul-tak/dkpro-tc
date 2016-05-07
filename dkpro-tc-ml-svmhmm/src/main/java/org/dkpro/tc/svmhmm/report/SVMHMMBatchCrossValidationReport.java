/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dkpro.tc.svmhmm.report;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dkpro.lab.reporting.BatchReportBase;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.task.TaskContextMetadata;
import org.dkpro.lab.task.impl.ExecutableTaskBase;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.svmhmm.task.SVMHMMTestTask;
import org.dkpro.tc.svmhmm.util.ConfusionMatrix;
import org.dkpro.tc.svmhmm.util.SVMHMMUtils;
@Deprecated
public class SVMHMMBatchCrossValidationReport
        extends BatchReportBase
{
    static Log log = LogFactory.getLog(SVMHMMBatchCrossValidationReport.class);

    protected void aggregateResults(String testTaskCSVFile, String outputPrefix)
            throws Exception
    {
        StorageService storageService = getContext().getStorageService();

        // aggregate rows from all CSVs from all folds
        List<List<String>> allOutcomes = new ArrayList<>();

        List<TaskContextMetadata> testTasks = collectTestTasks();

        // we need test tasks!
        if (testTasks.isEmpty()) {
            throw new IllegalStateException("No test tasks found. Make sure you properly " +
                    "define the test task in getTestTaskClass() (currently: " +
                    getTestTaskClass().getName());
        }

        // iterate over all sub tasks
        for (TaskContextMetadata subContext : testTasks) {
            // locate CSV file with outcomes (gold, predicted, token, etc.)
        	File csvFile = storageService.locateKey(subContext.getId(),
                    Constants.TEST_TASK_OUTPUT_KEY + File.separator
                    + testTaskCSVFile);

            // load the CSV
            CSVParser csvParser = new CSVParser(new FileReader(csvFile),
                    CSVFormat.DEFAULT.withCommentMarker('#'));

            // and add the all rows
            for (CSVRecord csvRecord : csvParser) {
                // row for particular instance
                List<String> row = new ArrayList<>();
                for (String value : csvRecord) {
                    row.add(value);
                }
                allOutcomes.add(row);
            }

            IOUtils.closeQuietly(csvParser);
        }

        // store aggregated outcomes again to CSV
        File evaluationFolder = getContext().getFolder(Constants.TEST_TASK_OUTPUT_KEY,
                StorageService.AccessMode.READWRITE);
        File evaluationFile = new File(evaluationFolder,
                testTaskCSVFile);
        log.debug("Evaluation file: " + evaluationFile.getAbsolutePath());

        CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(evaluationFile),
                SVMHMMUtils.CSV_FORMAT);
        csvPrinter.printComment(SVMHMMUtils.CSV_COMMENT);
        csvPrinter.printRecords(allOutcomes);
        IOUtils.closeQuietly(csvPrinter);

        // compute confusion matrix
        ConfusionMatrix cm = new ConfusionMatrix();

        for (List<String> singleInstanceOutcomeRow : allOutcomes) {
            
            String string = singleInstanceOutcomeRow.get(0);
            String entry = string.split("=")[1];
            String[] split = entry.split(SVMHMMOutcomeIDReport.SEPARATOR_CHAR);
            String gold = split[0];
            String predicted = split[1];

            cm.increaseValue(gold, predicted);
        }

        // and write all reports
        SVMHMMUtils.writeOutputResults(getContext(), cm, outputPrefix);

        // and print detailed results
        log.info(outputPrefix + "; " + cm.printNiceResults());
        log.info(outputPrefix + "; " + cm.printLabelPrecRecFm());
    }

    @Override
    public void execute()
            throws Exception
    {
        aggregateResults(Constants.ID_OUTCOME_KEY, "seq");
    }

    /**
     * Collects all sub-tasks that correspond to the test task
     *
     * @return list of test tasks
     */
    protected List<TaskContextMetadata> collectTestTasks()
    {
        List<TaskContextMetadata> result = new ArrayList<>();
        for (TaskContextMetadata subContext : getSubtasks()) {
            // but only test tasks are important
            if (subContext.getLabel().startsWith(getTestTaskClass().getSimpleName())) {
                result.add(subContext);
            }
        }

        return result;
    }

    /**
     * Returns class implementing the test tasks in this scenario (required for determining
     * directory names for collecting results from test tasks)
     *
     * @return class
     */
    protected Class<? extends ExecutableTaskBase> getTestTaskClass()
    {
        return SVMHMMTestTask.class;
    }
}
