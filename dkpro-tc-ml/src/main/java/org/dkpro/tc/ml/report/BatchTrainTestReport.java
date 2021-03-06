/*******************************************************************************
 * Copyright 2016
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
 ******************************************************************************/
package org.dkpro.tc.ml.report;

import static org.dkpro.tc.core.util.ReportUtils.getDiscriminatorValue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dkpro.lab.reporting.BatchReportBase;
import org.dkpro.lab.reporting.FlexTable;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.storage.impl.PropertiesAdapter;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.task.TaskContextMetadata;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.util.ReportUtils;
import org.dkpro.tc.evaluation.Id2Outcome;
import org.dkpro.tc.evaluation.evaluator.EvaluatorBase;
import org.dkpro.tc.evaluation.evaluator.EvaluatorFactory;

/**
 * Collects the final evaluation results in a train/test setting.
 */
public class BatchTrainTestReport
    extends BatchReportBase
    implements Constants
{
    private final List<String> discriminatorsToExclude = Arrays.asList(new String[] {
            DIM_FILES_VALIDATION, DIM_FILES_TRAINING });
  	private boolean softEvaluation = true;
	private boolean individualLabelMeasures = false;

	
    @Override
    public void execute()
        throws Exception
    {
        StorageService store = getContext().getStorageService();
        FlexTable<String> table = FlexTable.forClass(String.class);

        for (TaskContextMetadata subcontext : getSubtasks()) {
            // FIXME this is a bad hack
            if (subcontext.getType().contains("TestTask")) {
                Map<String, String> discriminatorsMap = store.retrieveBinary(subcontext.getId(),
                        Task.DISCRIMINATORS_KEY, new PropertiesAdapter()).getMap();
                String mode = getDiscriminatorValue(discriminatorsMap, DIM_LEARNING_MODE);
                File id2outcomeFile = getContext().getStorageService().locateKey(subcontext.getId(), ID_OUTCOME_KEY);
                Id2Outcome id2outcome = new Id2Outcome(id2outcomeFile, mode);
                EvaluatorBase evaluator = EvaluatorFactory.createEvaluator(id2outcome, softEvaluation, individualLabelMeasures);
                Map<String, Double> resultTempMap = evaluator.calculateEvaluationMeasures();
                Map<String, String> resultMap = new HashMap<String, String>();
                for (String key : resultTempMap.keySet()) {
                	Double value = resultTempMap.get(key);
					resultMap.put(key, String.valueOf(value));
				}

                Map<String, String> values = new HashMap<String, String>();
                Map<String, String> cleanedDiscriminatorsMap = new HashMap<String, String>();

                for (String disc : discriminatorsMap.keySet()) {
                    if (!ReportUtils.containsExcludePattern(disc, discriminatorsToExclude)) {
                        cleanedDiscriminatorsMap.put(disc, discriminatorsMap.get(disc));
                    }
                }
                values.putAll(cleanedDiscriminatorsMap);
                values.putAll(resultMap);

                table.addRow(subcontext.getLabel(), values);
            }
        }
        
        getContext().getLoggingService().message(getContextLabel(),
                ReportUtils.getPerformanceOverview(table));
        // Excel cannot cope with more than 255 columns
        if (table.getColumnIds().length <= 255) {
            getContext()
                    .storeBinary(EVAL_FILE_NAME + "_compact" + SUFFIX_EXCEL, table.getExcelWriter());
        }
        getContext().storeBinary(EVAL_FILE_NAME + "_compact" + SUFFIX_CSV, table.getCsvWriter());
        table.setCompact(false);
        // Excel cannot cope with more than 255 columns
        if (table.getColumnIds().length <= 255) {
            getContext().storeBinary(EVAL_FILE_NAME + SUFFIX_EXCEL, table.getExcelWriter());
        }
        getContext().storeBinary(EVAL_FILE_NAME + SUFFIX_CSV, table.getCsvWriter());

        // output the location of the batch evaluation folder
        // otherwise it might be hard for novice users to locate this
        File dummyFolder = store.locateKey(getContext().getId(), "dummy");
        // TODO can we also do this without creating and deleting the dummy folder?
        getContext().getLoggingService().message(getContextLabel(),
                "Storing detailed results in:\n" + dummyFolder.getParent() + "\n");
        dummyFolder.delete();
    }
}