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
package org.dkpro.tc.features.ngram;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.dkpro.tc.api.exception.TextClassificationException;
import org.dkpro.tc.api.features.FeatureExtractor;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.type.TextClassificationTarget;
import org.dkpro.tc.features.ngram.base.KeywordNGramFeatureExtractorBase;
import org.dkpro.tc.features.ngram.util.KeywordNGramUtils;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;

@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" })
public class KeywordNGram
    extends KeywordNGramFeatureExtractorBase
    implements FeatureExtractor
{

    @Override
    public Set<Feature> extract(JCas jcas, TextClassificationTarget target)
        throws TextClassificationException
    {
        Set<Feature> features = new HashSet<Feature>();

        FrequencyDistribution<String> documentNgrams = KeywordNGramUtils.getDocumentKeywordNgrams(
                jcas, target, keywordMinN, keywordMaxN, markSentenceBoundary, markSentenceLocation,
                includeCommas, keywords);

        for (String topNgram : topKSet.getKeys()) {
            if (documentNgrams.getKeys().contains(topNgram)) {
                features.add(new Feature(getFeaturePrefix() + "_" + topNgram, 1));
            }
            else {
                features.add(new Feature(getFeaturePrefix() + "_" + topNgram, 0, true));
            }
        }
        return features;
    }
}