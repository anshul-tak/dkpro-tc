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
package org.dkpro.tc.features.style;

import static org.dkpro.tc.testing.FeatureTestUtil.assertFeature;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.util.FeatureUtil;
import org.dkpro.tc.api.type.TextClassificationTarget;
import org.dkpro.tc.features.style.LongWordsFeatureExtractor;

public class LongWordsFeatureExtractorTest
{
    @Test
    public void longWordsFeatureExtractorTest()
        throws Exception
    {
        AnalysisEngineDescription desc = createEngineDescription(BreakIteratorSegmenter.class);
        AnalysisEngine engine = createEngine(desc);

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText("This is a test of incredibly surprising long words.");
        engine.process(jcas);
        
        TextClassificationTarget target = new TextClassificationTarget(jcas, 0, jcas.getDocumentText().length());
        target.addToIndexes();

        LongWordsFeatureExtractor extractor = FeatureUtil.createResource(LongWordsFeatureExtractor.class);

        List<Feature> features = new ArrayList<Feature>(extractor.extract(jcas, target));

        Assert.assertEquals(2, features.size());
        Iterator<Feature> iter = features.iterator();
        assertFeature(LongWordsFeatureExtractor.FN_LW_RATIO, 0.2, iter.next());
        assertFeature(LongWordsFeatureExtractor.FN_SW_RATIO, 0.4, iter.next());
    }
}