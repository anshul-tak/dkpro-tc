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
package org.dkpro.tc.ml;

import org.dkpro.lab.reporting.Report;
import org.dkpro.lab.task.impl.TaskBase;

import org.dkpro.tc.api.exception.TextClassificationException;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.ml.TCMachineLearningAdapter;
import org.dkpro.tc.core.task.ExtractFeaturesTask;
import org.dkpro.tc.core.task.InitTask;
import org.dkpro.tc.core.task.MetaInfoTask;

/**
 * Train-Test setup
 * 
 */
public class ExperimentTrainTest
    extends Experiment_ImplBase
{

    protected InitTask initTaskTrain;
    protected InitTask initTaskTest;
    protected MetaInfoTask metaTask;
    protected ExtractFeaturesTask featuresTrainTask;
    protected ExtractFeaturesTask featuresTestTask;
    protected TaskBase testTask;

    public ExperimentTrainTest()
    {/* needed for Groovy */
    }

    /**
     * Preconfigured train-test setup.
     * 
     */
    public ExperimentTrainTest(String aExperimentName, Class<? extends TCMachineLearningAdapter> mlAdapter)
            throws TextClassificationException
    {
        setExperimentName(aExperimentName);
        setMachineLearningAdapter(mlAdapter);
        // set name of overall batch task
        setType("Evaluation-" + experimentName);
    }

    /**
     * Initializes the experiment. This is called automatically before execution. It's not done
     * directly in the constructor, because we want to be able to use setters instead of the
     * arguments in the constructor.
     * 
     */
    @Override
    protected void init()
    {
        if (experimentName == null)

        {
            throw new IllegalStateException(
                    "You must set an experiment name");
        }

        // init the train part of the experiment
        initTaskTrain = new InitTask();
        initTaskTrain.setMlAdapter(mlAdapter);
        initTaskTrain.setPreprocessing(getPreprocessing());
        initTaskTrain.setOperativeViews(operativeViews);
        initTaskTrain.setTesting(false);
        initTaskTrain.setType(initTaskTrain.getType() + "-Train-" + experimentName);

        // init the test part of the experiment
        initTaskTest = new InitTask();
        initTaskTest.setTesting(true);
        initTaskTest.setMlAdapter(mlAdapter);
        initTaskTest.setPreprocessing(getPreprocessing());
        initTaskTest.setOperativeViews(operativeViews);
        initTaskTest.setType(initTaskTest.getType() + "-Test-" + experimentName);

        // get some meta data depending on the whole document collection that we need for training
        metaTask = new MetaInfoTask();
        metaTask.setOperativeViews(operativeViews);
        metaTask.setType(metaTask.getType() + "-" + experimentName);

        metaTask.addImport(initTaskTrain, InitTask.OUTPUT_KEY_TRAIN,
                MetaInfoTask.INPUT_KEY);

        // feature extraction on training data
        featuresTrainTask = new ExtractFeaturesTask();
        featuresTrainTask.setType(featuresTrainTask.getType() + "-Train-" + experimentName);
        featuresTrainTask.setMlAdapter(mlAdapter);
        featuresTrainTask.addImport(metaTask, MetaInfoTask.META_KEY);
        featuresTrainTask.addImport(initTaskTrain, InitTask.OUTPUT_KEY_TRAIN,
                ExtractFeaturesTask.INPUT_KEY);

        // feature extraction on test data
        featuresTestTask = new ExtractFeaturesTask();
        featuresTestTask.setType(featuresTestTask.getType() + "-Test-" + experimentName);
        featuresTestTask.setMlAdapter(mlAdapter);
        featuresTestTask.addImport(metaTask, MetaInfoTask.META_KEY);
        featuresTestTask.addImport(initTaskTest, InitTask.OUTPUT_KEY_TEST,
                ExtractFeaturesTask.INPUT_KEY);
        featuresTestTask.addImport(featuresTrainTask, ExtractFeaturesTask.OUTPUT_KEY);

        // test task operating on the models of the feature extraction train and test tasks
        testTask = mlAdapter.getTestTask();
        testTask.setType(testTask.getType() + "-" + experimentName);

        if (innerReports != null) {
            for (Class<? extends Report> report : innerReports) {
                testTask.addReport(report);
            }
        }

        // always add OutcomeIdReport
        testTask.addReport(mlAdapter.getOutcomeIdReportClass());

        testTask.addImport(featuresTrainTask, ExtractFeaturesTask.OUTPUT_KEY,
                Constants.TEST_TASK_INPUT_KEY_TRAINING_DATA);
        testTask.addImport(featuresTestTask, ExtractFeaturesTask.OUTPUT_KEY,
                Constants.TEST_TASK_INPUT_KEY_TEST_DATA);

        // DKPro Lab issue 38: must be added as *first* task
        addTask(initTaskTrain);
        addTask(initTaskTest);
        addTask(metaTask);
        addTask(featuresTrainTask);
        addTask(featuresTestTask);
        addTask(testTask);
    }
}
