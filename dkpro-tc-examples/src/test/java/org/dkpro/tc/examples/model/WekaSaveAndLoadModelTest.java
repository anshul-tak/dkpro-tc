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
package org.dkpro.tc.examples.model;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.BatchTask.ExecutionPolicy;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.type.TextClassificationOutcome;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.examples.io.BrownCorpusReader;
import org.dkpro.tc.examples.io.ReutersCorpusReader;
import org.dkpro.tc.examples.io.STSReader;
import org.dkpro.tc.examples.io.TwentyNewsgroupsCorpusReader;
import org.dkpro.tc.examples.util.DemoUtils;
import org.dkpro.tc.features.length.NrOfTokens;
import org.dkpro.tc.features.ngram.LuceneCharacterNGram;
import org.dkpro.tc.features.ngram.LuceneNGram;
import org.dkpro.tc.features.ngram.base.NGramFeatureExtractorBase;
import org.dkpro.tc.features.pair.core.length.DiffNrOfTokensPairFeatureExtractor;
import org.dkpro.tc.ml.ExperimentSaveModel;
import org.dkpro.tc.ml.uima.TcAnnotator;
import org.dkpro.tc.weka.MekaClassificationAdapter;
import org.dkpro.tc.weka.WekaClassificationAdapter;
import org.dkpro.tc.weka.WekaRegressionAdapter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.tei.TeiReader;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import meka.classifiers.multilabel.MULAN;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.trees.RandomForest;

/**
 * Round-trip tests for save/load model experiments. Tests all feature modes (document, pair, unit),
 * as well as all learning models (single-label, multi-label, regression).
 *
 */
public class WekaSaveAndLoadModelTest
    implements Constants
{
    static String documentTrainFolder = "src/main/resources/data/twentynewsgroups/bydate-train";
    static String unitTrainFolder = "src/main/resources/data/brown_tei/";
    static String pairTrainFiles = "src/main/resources/data/sts2012/STS.input.MSRpar.txt";
    static String pairGoldFiles = "src/main/resources/data/sts2012/STS.gs.MSRpar.txt";
    static String documentTrainFolderReuters = "src/main/resources/data/reuters/training";
    static String documentGoldLabelsReuters = "src/main/resources/data/reuters/cats.txt";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup()
    {
        DemoUtils.setDkproHome(WekaSaveAndLoadModelTest.class.getSimpleName());
    }

    private ParameterSpace documentGetParameterSpaceSingleLabel()
        throws ResourceInitializationException
    {
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                TwentyNewsgroupsCorpusReader.class,
                TwentyNewsgroupsCorpusReader.PARAM_SOURCE_LOCATION, documentTrainFolder,
                TwentyNewsgroupsCorpusReader.PARAM_LANGUAGE, "en",
                TwentyNewsgroupsCorpusReader.PARAM_PATTERNS,
                Arrays.asList(TwentyNewsgroupsCorpusReader.INCLUDE_PREFIX + "*/*.txt"));
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new String[] { NaiveBayes.class.getName() }));

        @SuppressWarnings("unchecked")
        Dimension<List<Object>> dimPipelineParameters = Dimension.create(DIM_PIPELINE_PARAMS,
                Arrays.asList(new Object[] { NGramFeatureExtractorBase.PARAM_NGRAM_USE_TOP_K, 500,
                        NGramFeatureExtractorBase.PARAM_NGRAM_MIN_N, 1,
                        NGramFeatureExtractorBase.PARAM_NGRAM_MAX_N, 3 }));

        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimFeatureSets = Dimension.create(DIM_FEATURE_SET, Arrays.asList(
                new String[] { NrOfTokens.class.getName(), LuceneNGram.class.getName() }));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimPipelineParameters,
                dimFeatureSets, dimClassificationArgs);
        return pSpace;
    }

    private ParameterSpace documentGetParameterSpaceMultiLabel()
        throws ResourceInitializationException
    {
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        Object readerTrain = CollectionReaderFactory.createReaderDescription(
                ReutersCorpusReader.class, ReutersCorpusReader.PARAM_SOURCE_LOCATION,
                documentTrainFolderReuters, ReutersCorpusReader.PARAM_GOLD_LABEL_FILE,
                documentGoldLabelsReuters, ReutersCorpusReader.PARAM_LANGUAGE, "en",
                ReutersCorpusReader.PARAM_PATTERNS, ReutersCorpusReader.INCLUDE_PREFIX + "*.txt");
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new String[] { MULAN.class.getName(), "-S", "RAkEL2", "-W",
                        RandomForest.class.getName() }));

        @SuppressWarnings("unchecked")
        Dimension<List<Object>> dimPipelineParameters = Dimension.create(DIM_PIPELINE_PARAMS,
                Arrays.asList(new Object[] { NGramFeatureExtractorBase.PARAM_NGRAM_USE_TOP_K, 500,
                        NGramFeatureExtractorBase.PARAM_NGRAM_MIN_N, 1,
                        NGramFeatureExtractorBase.PARAM_NGRAM_MAX_N, 3 }));

        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimFeatureSets = Dimension.create(DIM_FEATURE_SET, Arrays.asList(
                new String[] { NrOfTokens.class.getName(), LuceneNGram.class.getName() }));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_MULTI_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimPipelineParameters,
                dimFeatureSets, Dimension.create(DIM_BIPARTITION_THRESHOLD, "0.5"),
                dimClassificationArgs);
        return pSpace;
    }

    @Test
    public void documentRoundTripWekaSingleLabel()
        throws Exception
    {

        DemoUtils.setDkproHome(WekaSaveAndLoadModelTest.class.getSimpleName());
        File modelFolder = folder.newFolder();

        ParameterSpace docParamSpace = documentGetParameterSpaceSingleLabel();
        documentWriteModel(docParamSpace, modelFolder, true);
        documentLoadModelSingleLabel(modelFolder);

        // verify created files

        File classifierFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_CLASSIFIER);
        assertTrue(classifierFile.exists());

        File usedFeaturesFile = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_EXTRACTORS);
        assertTrue(usedFeaturesFile.exists());

        File modelMetaFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_META);
        assertTrue(modelMetaFile.exists());

        File featureMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_MODE);
        assertTrue(featureMode.exists());

        File learningMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_LEARNING_MODE);
        assertTrue(learningMode.exists());

        File bipartitionThreshold = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_BIPARTITION_THRESHOLD);
        assertTrue(bipartitionThreshold.exists());

        modelFolder.deleteOnExit();
    }

    @Test
    public void documentRoundTripWekaMultiLabel()
        throws Exception
    {

        DemoUtils.setDkproHome(WekaSaveAndLoadModelTest.class.getSimpleName());
        File modelFolder = folder.newFolder();

        ParameterSpace docParamSpace = documentGetParameterSpaceMultiLabel();
        documentWriteModel(docParamSpace, modelFolder, false);
        documentLoadModelMultiLabel(modelFolder);

        // verify created files

        File classifierFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_CLASSIFIER);
        assertTrue(classifierFile.exists());

        File usedFeaturesFile = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_EXTRACTORS);
        assertTrue(usedFeaturesFile.exists());

        File modelMetaFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_META);
        assertTrue(modelMetaFile.exists());

        File featureMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_MODE);
        assertTrue(featureMode.exists());

        File learningMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_LEARNING_MODE);
        assertTrue(learningMode.exists());

        File bipartitionThreshold = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_BIPARTITION_THRESHOLD);
        assertTrue(bipartitionThreshold.exists());

        modelFolder.deleteOnExit();
    }

    @Test
    public void pairRoundTripWekaRegression()
        throws Exception
    {

        DemoUtils.setDkproHome(WekaSaveAndLoadModelTest.class.getSimpleName());
        File modelFolder = folder.newFolder();

        ParameterSpace pairParamSpace = pairGetParameterSpace();
        pairWriteModel(pairParamSpace, modelFolder);
        pairLoadModelRegression(modelFolder);

        // verify created files

        File classifierFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_CLASSIFIER);
        assertTrue(classifierFile.exists());

        File usedFeaturesFile = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_EXTRACTORS);
        assertTrue(usedFeaturesFile.exists());

        File modelMetaFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_META);
        assertTrue(modelMetaFile.exists());

        File featureMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_MODE);
        assertTrue(featureMode.exists());

        File learningMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_LEARNING_MODE);
        assertTrue(learningMode.exists());

        File bipartitionThreshold = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_BIPARTITION_THRESHOLD);
        assertTrue(bipartitionThreshold.exists());

        modelFolder.deleteOnExit();
    }

    /**
     * This test case trains a unit model for recognizing part of speech tags.
     */
    @Test
    public void unitRoundTripWeka()
        throws Exception
    {

        DemoUtils.setDkproHome(WekaSaveAndLoadModelTest.class.getSimpleName());
        File modelFolder = folder.newFolder();

        ParameterSpace unitParamSpace = unitGetParameterSpace();
        unitExecuteSaveModel(unitParamSpace, modelFolder);
        unitLoadModel(modelFolder);

        // verify that all expected files have been created
        File classifierFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_CLASSIFIER);
        assertTrue(classifierFile.exists());

        File usedFeaturesFile = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_EXTRACTORS);
        assertTrue(usedFeaturesFile.exists());

        File modelMetaFile = new File(modelFolder.getAbsolutePath() + "/" + MODEL_META);
        assertTrue(modelMetaFile.exists());

        File featureMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_FEATURE_MODE);
        assertTrue(featureMode.exists());

        File learningMode = new File(modelFolder.getAbsolutePath() + "/" + MODEL_LEARNING_MODE);
        assertTrue(learningMode.exists());

        File bipartitionThreshold = new File(
                modelFolder.getAbsolutePath() + "/" + MODEL_BIPARTITION_THRESHOLD);
        assertTrue(bipartitionThreshold.exists());

        modelFolder.deleteOnExit();
    }

    private static void documentWriteModel(ParameterSpace paramSpace, File modelFolder,
            boolean singlelabel)
                throws Exception
    {
        ExperimentSaveModel batch;
        if (singlelabel) {
            batch = new ExperimentSaveModel("TestSaveModel", WekaClassificationAdapter.class,
                    modelFolder);
        }
        else {
            batch = new ExperimentSaveModel("TestSaveModel", MekaClassificationAdapter.class,
                    modelFolder);
        }
        batch.setPreprocessing(
                createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class)));
        batch.setParameterSpace(paramSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        Lab.getInstance().run(batch);
    }

    private static void pairWriteModel(ParameterSpace paramSpace, File modelFolder)
        throws Exception
    {
        ExperimentSaveModel batch = new ExperimentSaveModel("TestSaveModel",
                WekaRegressionAdapter.class, modelFolder);
        batch.setPreprocessing(
                createEngineDescription(createEngineDescription(BreakIteratorSegmenter.class)));
        batch.setParameterSpace(paramSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        Lab.getInstance().run(batch);
    }

    private void unitExecuteSaveModel(ParameterSpace pSpace, File modelFolder)
        throws Exception
    {
        ExperimentSaveModel batch = new ExperimentSaveModel("TestSaveModel",
                WekaClassificationAdapter.class, modelFolder);
        batch.setPreprocessing(createEngineDescription(WekaUnitAnnotator.class));
        batch.setParameterSpace(pSpace);
        batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
        Lab.getInstance().run(batch);
    }

    private static ParameterSpace pairGetParameterSpace()
        throws ResourceInitializationException
    {
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        Object readerTrain = CollectionReaderFactory.createReaderDescription(
                STSReader.class, STSReader.PARAM_INPUT_FILE, pairTrainFiles,
                STSReader.PARAM_GOLD_FILE, pairGoldFiles);
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimClassificationArgs = Dimension.create(
                Constants.DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new String[] { SMOreg.class.getName() }));

        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimFeatureSets = Dimension.create(Constants.DIM_FEATURE_SET,
                Arrays.asList(new String[] { DiffNrOfTokensPairFeatureExtractor.class.getName() }));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_REGRESSION),
                Dimension.create(DIM_FEATURE_MODE, FM_PAIR), dimFeatureSets, dimClassificationArgs);

        return pSpace;
    }

    private static ParameterSpace unitGetParameterSpace() throws ResourceInitializationException
    {
        Map<String, Object> dimReaders = new HashMap<String, Object>();
        
        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                BrownCorpusReader.class, BrownCorpusReader.PARAM_SOURCE_LOCATION, unitTrainFolder,
                BrownCorpusReader.PARAM_LANGUAGE, "en", BrownCorpusReader.PARAM_PATTERNS,
                Arrays.asList("*.xml"));
        dimReaders.put(DIM_READER_TRAIN, readerTrain);
        
        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new String[] { SMO.class.getName() }));

        @SuppressWarnings("unchecked")
        Dimension<List<Object>> dimPipelineParameters = Dimension.create(DIM_PIPELINE_PARAMS, Arrays
                .asList(new Object[] { LuceneCharacterNGram.PARAM_CHAR_NGRAM_USE_TOP_K, 20 }));

        @SuppressWarnings("unchecked")
        Dimension<List<String>> dimFeatureSets = Dimension.create(DIM_FEATURE_SET,
                Arrays.asList(new String[] { LuceneCharacterNGram.class.getName() }));

        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_UNIT), dimPipelineParameters, dimFeatureSets,
                dimClassificationArgs);

        return pSpace;
    }

    private static void documentLoadModelSingleLabel(File modelFolder)
        throws Exception
    {

        AnalysisEngine tokenizer = AnalysisEngineFactory.createEngine(BreakIteratorSegmenter.class);

        AnalysisEngine tcAnno = AnalysisEngineFactory.createEngine(TcAnnotator.class,
                TcAnnotator.PARAM_TC_MODEL_LOCATION, modelFolder.getAbsolutePath());

        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("This is an example text");
        jcas.setDocumentLanguage("en");

        tokenizer.process(jcas);
        tcAnno.process(jcas);

        List<TextClassificationOutcome> outcomes = new ArrayList<>(
                JCasUtil.select(jcas, TextClassificationOutcome.class));
        assertEquals(1, outcomes.size());
        assertEquals("comp.graphics", outcomes.get(0).getOutcome());
    }

    private static void documentLoadModelMultiLabel(File modelFolder)
        throws Exception
    {

        AnalysisEngine tokenizer = AnalysisEngineFactory.createEngine(BreakIteratorSegmenter.class);

        AnalysisEngine tcAnno = AnalysisEngineFactory.createEngine(TcAnnotator.class,
                TcAnnotator.PARAM_TC_MODEL_LOCATION, modelFolder.getAbsolutePath());

        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("This is an example text");
        jcas.setDocumentLanguage("en");

        tokenizer.process(jcas);
        tcAnno.process(jcas);

        List<TextClassificationOutcome> outcomes = new ArrayList<>(
                JCasUtil.select(jcas, TextClassificationOutcome.class));
        assertEquals(2, outcomes.size());
        assertEquals("crude", outcomes.get(0).getOutcome());
        assertEquals("acq", outcomes.get(1).getOutcome());

    }

    private static void pairLoadModelRegression(File modelFolder)
        throws Exception
    {
        CollectionReader reader = CollectionReaderFactory.createReader(STSReader.class,
                STSReader.PARAM_INPUT_FILE, pairTrainFiles, STSReader.PARAM_GOLD_FILE,
                pairGoldFiles);

        AnalysisEngine tcAnno = AnalysisEngineFactory.createEngine(TcAnnotator.class,
                TcAnnotator.PARAM_TC_MODEL_LOCATION, modelFolder.getAbsolutePath());

        JCas jcas = JCasFactory.createJCas();

        reader.getNext(jcas.getCas());
        tcAnno.process(jcas);

        List<TextClassificationOutcome> outcomes = new ArrayList<>(
                JCasUtil.select(jcas, TextClassificationOutcome.class));
        assertEquals(1, outcomes.size());
        assertEquals("4.0958", outcomes.get(0).getOutcome());

    }

    private static void unitLoadModel(File modelFolder)
        throws Exception
    {
        CollectionReader reader = CollectionReaderFactory.createReader(TeiReader.class,
                TeiReader.PARAM_SOURCE_LOCATION, unitTrainFolder, TeiReader.PARAM_LANGUAGE, "en",
                TeiReader.PARAM_PATTERNS, "*.xml");

        AnalysisEngine tcAnno = AnalysisEngineFactory.createEngine(TcAnnotator.class,
                TcAnnotator.PARAM_TC_MODEL_LOCATION, modelFolder.getAbsolutePath(),
                TcAnnotator.PARAM_NAME_UNIT_ANNOTATION, Token.class.getName());

        JCas jcas = JCasFactory.createJCas();
        reader.getNext(jcas.getCas());
        assertEquals(163, JCasUtil.select(jcas, Token.class).size()); // sanity check i.e. check
                                                                      // number of tokens which
                                                                      // determine number of
                                                                      // outcomes that are expected

        tcAnno.process(jcas);

        Collection<TextClassificationOutcome> outcomes = JCasUtil.select(jcas,
                TextClassificationOutcome.class);
        assertEquals(163, outcomes.size()); // 168 tokens in the training file we expect for each an
                                            // outcome
        for (TextClassificationOutcome o : outcomes) {
            assertTrue(!o.getOutcome().isEmpty());
        }
    }
}