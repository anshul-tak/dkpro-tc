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
package org.dkpro.tc.mallet;

import java.util.Collection;

import org.dkpro.lab.reporting.ReportBase;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.impl.DimensionBundle;
import org.dkpro.lab.task.impl.ExecutableTaskBase;
import org.dkpro.lab.task.impl.FoldDimensionBundle;
import org.dkpro.tc.core.io.DataWriter;
import org.dkpro.tc.core.ml.ModelSerialization_ImplBase;
import org.dkpro.tc.core.ml.TCMachineLearningAdapter;
import org.dkpro.tc.core.task.ModelSerializationTask;
import org.dkpro.tc.fstore.simple.DenseFeatureStore;
import org.dkpro.tc.mallet.report.MalletOutcomeIDReport;
import org.dkpro.tc.mallet.task.MalletTestTask;
import org.dkpro.tc.mallet.writer.MalletDataWriter;
import org.dkpro.tc.ml.report.InnerBatchUsingTCEvaluationReport;

public class MalletAdapter 
	implements TCMachineLearningAdapter
{

	public static TCMachineLearningAdapter getInstance() {
		return new MalletAdapter();
	}
	
	@Override
	public ExecutableTaskBase getTestTask() {
		return new MalletTestTask();
	}

	@Override
	public Class<? extends ReportBase> getOutcomeIdReportClass() {
		return MalletOutcomeIDReport.class;
	}

	@Override
	public Class<? extends ReportBase> getBatchTrainTestReportClass() {
		return InnerBatchUsingTCEvaluationReport.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DimensionBundle<Collection<String>> getFoldDimensionBundle(
			String[] files, int folds) {
	    return new FoldDimensionBundle<String>("files", Dimension.create("", files), folds);
	}
	
	@Override
	public String getFrameworkFilename(AdapterNameEntries name) {

        switch (name) {
            case featureVectorsFile:  return "training-data.txt";
            case predictionsFile      :  return "predictions.txt";
            case featureSelectionFile :  return "attributeEvaluationResults.txt";
        }
        
        return null;
	}

	@Override
	public Class<? extends DataWriter> getDataWriterClass() {
		return MalletDataWriter.class;
	}
	
	@Override
	public Class<? extends ModelSerialization_ImplBase> getLoadModelConnectorClass() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<? extends ModelSerializationTask> getSaveModelTask() {
		throw new UnsupportedOperationException();
	}

    @Override
    public String getFeatureStore()
    {
        return DenseFeatureStore.class.getName();
    }
}
