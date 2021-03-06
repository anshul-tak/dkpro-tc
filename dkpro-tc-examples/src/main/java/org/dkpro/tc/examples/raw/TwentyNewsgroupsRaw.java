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
package org.dkpro.tc.examples.raw;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import org.dkpro.tc.core.Constants;
import org.dkpro.tc.core.task.uima.ExtractFeaturesConnector;
import org.dkpro.tc.examples.io.TwentyNewsgroupsCorpusReader;
import org.dkpro.tc.features.length.NrOfSentences;
import org.dkpro.tc.features.length.NrOfTokens;
import org.dkpro.tc.weka.writer.WekaDataWriter;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

/**
 * Example of "raw" usage of DKPro TC without DKPro Lab parts. This is for advanced users that do
 * want to have full control over what is going on without having to use the lab architecture.
 * 
 * The pipeline will run all DKPro TC components up to the creation of the input the for the Machine
 * Learning framework. No actual learning or evaluation will be done.
 */
public class TwentyNewsgroupsRaw
{
    public static void main(String[] args)
        throws Exception
    {
        String corpusFilePathTrain = "src/main/resources/data/twentynewsgroups/bydate-train/*/*.txt";

        runPipeline(
                // Reader
                createReaderDescription(TwentyNewsgroupsCorpusReader.class,
                        TwentyNewsgroupsCorpusReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
                        TwentyNewsgroupsCorpusReader.PARAM_LANGUAGE, "en"),
                // Preprocessing
                createEngineDescription(JCasIdSetter.class),
                createEngineDescription(BreakIteratorSegmenter.class),
                createEngineDescription(OpenNlpPosTagger.class),
                // Feature extraction
                createEngineDescription(ExtractFeaturesConnector.class,
                        ExtractFeaturesConnector.PARAM_OUTPUT_DIRECTORY, "target/tn_raw_output",
                        ExtractFeaturesConnector.PARAM_DATA_WRITER_CLASS, WekaDataWriter.class,
                        ExtractFeaturesConnector.PARAM_LEARNING_MODE, Constants.LM_SINGLE_LABEL,
                        ExtractFeaturesConnector.PARAM_FEATURE_MODE, Constants.FM_DOCUMENT,
                        ExtractFeaturesConnector.PARAM_ADD_INSTANCE_ID, true,
                        ExtractFeaturesConnector.PARAM_FEATURE_FILTERS, new String[] {},
                        ExtractFeaturesConnector.PARAM_IS_TESTING, false,
                        ExtractFeaturesConnector.PARAM_FEATURE_EXTRACTORS,
                        asList(createExternalResourceDescription(NrOfTokens.class),
                                createExternalResourceDescription(NrOfSentences.class))));
    }
}
