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
package org.dkpro.tc.core.task;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.dkpro.tc.core.Constants.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.storage.StorageService.AccessMode;
import org.dkpro.lab.task.Discriminator;
import org.dkpro.lab.uima.task.impl.UimaTaskBase;
import org.dkpro.tc.api.exception.TextClassificationException;
import org.dkpro.tc.api.features.meta.MetaCollector;
import org.dkpro.tc.core.feature.SequenceContextMetaCollector;
import org.dkpro.tc.core.feature.UnitContextMetaCollector;
import org.dkpro.tc.core.task.uima.MetaCollectionLogger;
import org.dkpro.tc.core.util.TaskUtils;

import de.tudarmstadt.ukp.dkpro.core.io.bincas.BinaryCasReader;

/**
 * Iterates over all documents and stores required collection-level meta data, e.g. which n-grams
 * appear in the documents.
 * 
 */
public class MetaInfoTask
    extends UimaTaskBase
{

    /**
     * Public name of the task key
     */
    public static final String META_KEY = "meta";
    /**
     * Public name of the folder where meta information will be stored within the task
     */
    public static final String INPUT_KEY = "input";

    private List<String> operativeViews;

    @Discriminator(name=DIM_FEATURE_SET)
    protected List<String> featureSet;

    @Discriminator(name=DIM_FEATURE_MODE)
    private String featureMode;

    @Discriminator(name=DIM_PIPELINE_PARAMS)
    protected List<Object> pipelineParameters;

    private Set<Class<? extends MetaCollector>> metaCollectorClasses;

    @Discriminator(name=DIM_FILES_ROOT)
    private File filesRoot;

    @Discriminator(name=DIM_FILES_TRAINING)
    private Collection<String> files_training;

    @Discriminator(name=DIM_RECORD_CONTEXT)
    private boolean recordContext;

    @Override
    public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
        throws ResourceInitializationException, IOException
    {
        // TrainTest setup: input files are set as imports
        if (filesRoot == null || files_training == null) {
            File root = aContext.getFolder(INPUT_KEY, AccessMode.READONLY);
            Collection<File> files = FileUtils.listFiles(root, new String[] { "bin" }, true);

            return createReaderDescription(BinaryCasReader.class, BinaryCasReader.PARAM_PATTERNS,
                    files);
        }
        // CV setup: filesRoot and files_atrining have to be set as dimension
        else {
            return createReaderDescription(BinaryCasReader.class, BinaryCasReader.PARAM_PATTERNS,
                    files_training);
        }
    }

    @Override
    public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
        throws ResourceInitializationException, IOException
    {

        // check for error conditions
        if (featureSet == null) {
            throw new ResourceInitializationException(new TextClassificationException(
                    "No feature extractors have been added to the experiment."));
        }

        // automatically determine the required metaCollector classes from the provided feature
        // extractors
        try {
            metaCollectorClasses = TaskUtils.getMetaCollectorsFromFeatureExtractors(featureSet);
        }
        catch (ClassNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
        catch (InstantiationException e) {
            throw new ResourceInitializationException(e);
        }
        catch (IllegalAccessException e) {
            throw new ResourceInitializationException(e);
        }

        if (recordContext) {
            addContextCollector();
        }

        // collect parameter/key pairs that need to be set
        Map<String, String> parameterKeyPairs = new HashMap<String, String>();
        for (Class<? extends MetaCollector> metaCollectorClass : metaCollectorClasses) {
            try {
                parameterKeyPairs.putAll(metaCollectorClass.newInstance().getParameterKeyPairs());
            }
            catch (InstantiationException e) {
                throw new ResourceInitializationException(e);
            }
            catch (IllegalAccessException e) {
                throw new ResourceInitializationException(e);
            }
        }

        List<Object> parameters = new ArrayList<Object>();
        if (pipelineParameters != null) {
            parameters.addAll(pipelineParameters);
        }

        // make sure that the meta key import can be resolved (even when no meta features have been
        // extracted, as in the regression demo)
        // TODO better way to do this?
        if (parameterKeyPairs.size() == 0) {
            File file = new File(aContext.getFolder(META_KEY, AccessMode.READONLY).getPath());
            file.mkdir();
        }

        for (Entry<String, String> entry : parameterKeyPairs.entrySet()) {
            File file = new File(aContext.getFolder(META_KEY, AccessMode.READONLY),
                    entry.getValue());
            parameters.addAll(Arrays.asList(entry.getKey(), file.getAbsolutePath()));
        }

        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createEngineDescription(MetaCollectionLogger.class));

        for (Class<? extends MetaCollector> metaCollectorClass : metaCollectorClasses) {
            if (operativeViews != null) {
                for (String viewName : operativeViews) {
                    builder.add(createEngineDescription(metaCollectorClass, parameters.toArray()),
                            CAS.NAME_DEFAULT_SOFA, viewName);
                }
            }
            else {
                builder.add(createEngineDescription(metaCollectorClass, parameters.toArray()));
            }
        }
        return builder.createAggregateDescription();
    }

    private void addContextCollector()
    {
        // Records the context i.e. as debugging help turned off by default set
        // Dimension.create("recordContext", true) into your experiment to enable it

        if (featureMode.equals(FM_UNIT)) {
            metaCollectorClasses.add(UnitContextMetaCollector.class);
        }

        if (featureMode.equals(FM_SEQUENCE)) {
            metaCollectorClasses.add(SequenceContextMetaCollector.class);
        }
    }

    public void setOperativeViews(List<String> operativeViews)
    {
        this.operativeViews = operativeViews;
    }
    
}