/**
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.dkpro.tc.weka;

import java.io.File;

import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.task.Task;

import org.dkpro.tc.api.exception.TextClassificationException;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.ml.TCMachineLearningAdapter;
import org.dkpro.tc.core.task.ExtractFeaturesTask;
import org.dkpro.tc.core.task.MetaInfoTask;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.weka.task.serialization.WekaModelSerializationDescription;

/**
 * A subclass of the ExperiementTrainTest batch task, which
 * will store the trained classifier to the output directory
 * that is provided in the constructor or via setter.
 * 
 */
public class WekaTrainTestStore extends ExperimentTrainTest {

	File outputDirectory = null;

	public WekaTrainTestStore() {
		/* needed for Groovy */
	}

	public WekaTrainTestStore(String aExperimentName, Class<? extends TCMachineLearningAdapter> mlAdapter,
            File outputDirectory)
            throws TextClassificationException {

		super(aExperimentName, mlAdapter);

		this.outputDirectory = outputDirectory;
    }

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	/**
     * Initializes the experiment. This is called automatically before execution. It's not done
     * directly in the constructor, because we want to be able to use setters instead of the
     * four-argument constructor.
     *
     * @throws IllegalStateException
     *             if not all necessary arguments have been set.
     */
	@Override
    protected void init() {
    	super.init();

    	if (outputDirectory == null) {
            throw new IllegalStateException("You must set the outputdirectory.");
        }

        WekaModelSerializationDescription saveModelTask = new WekaModelSerializationDescription();
    	String type = saveModelTask.getType() + "-" + experimentName;
    	saveModelTask.setType(type);
    	saveModelTask.setOutputFolder(outputDirectory.getAbsoluteFile());

    	saveModelTask.addImport(this.getMetaTask(), MetaInfoTask.META_KEY);
        saveModelTask.addImport(this.getFeatureExtractionTask(), ExtractFeaturesTask.OUTPUT_KEY, Constants.TEST_TASK_INPUT_KEY_TRAINING_DATA);

        this.addTask(saveModelTask);
    }

	@Override
	public void initialize(TaskContext aContext)
	{
        super.initialize(aContext);
        init();
	}

	/**
	 * Private helper function that returns the ExtractFeaturesTask of
	 * the super class. Required, because all tasks are private in
	 * ExperimentTrainTest.
	 *
	 * @return The ExtractFeaturesTask
	 */
	private ExtractFeaturesTask getFeatureExtractionTask() {
		for(Task task : this.getTasks()) {
            if(task instanceof ExtractFeaturesTask) {
                return (ExtractFeaturesTask) task;
            }
        }

		return null;
	}

	/**
	 * Private helper function that returns the MetaTask of
	 * the super class. Required, because all tasks are private in
	 * ExperimentTrainTest.
	 *
	 * @return The MetaTask
	 */
	private MetaInfoTask getMetaTask() {
		for(Task task : this.getTasks()) {
            if(task instanceof MetaInfoTask) {
                return (MetaInfoTask) task;
            }
        }

		return null;
	}
}
