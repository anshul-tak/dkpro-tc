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
package org.dkpro.tc.features.length;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.dkpro.tc.testing.FeatureTestUtil.assertFeature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.type.TextClassificationTarget;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import junit.framework.Assert;

public class NrOfTokensTest
{
    @Test
    public void nrOfTokensFeatureExtractorTest()
        throws Exception
    {
        AnalysisEngine engine = createEngine(BreakIteratorSegmenter.class);

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText("This is a test.");
        engine.process(jcas);

        TextClassificationTarget target = new TextClassificationTarget(jcas, 0,
                jcas.getDocumentText().length());

        NrOfTokens extractor = new NrOfTokens();
        List<Feature> features = new ArrayList<Feature>(extractor.extract(jcas, target));

        Assert.assertEquals(1, features.size());

        Iterator<Feature> iter = features.iterator();
        assertFeature(NrOfTokens.FN_NR_OF_TOKENS, 5., iter.next());
    }

    @Test
    public void nrOfTokensExteremeFeatureExtractorTest()
        throws Exception
    {
        AnalysisEngine engine = createEngine(BreakIteratorSegmenter.class);

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText("");
        engine.process(jcas);

        TextClassificationTarget target = new TextClassificationTarget(jcas, 0,
                jcas.getDocumentText().length());

        NrOfTokens extractor = new NrOfTokens();
        List<Feature> features = new ArrayList<Feature>(extractor.extract(jcas, target));

        Assert.assertEquals(1, features.size());

        Iterator<Feature> iter = features.iterator();
        assertFeature(NrOfTokens.FN_NR_OF_TOKENS, 0., iter.next());
    }
}