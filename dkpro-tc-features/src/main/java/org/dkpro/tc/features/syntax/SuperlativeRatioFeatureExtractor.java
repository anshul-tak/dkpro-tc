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
package org.dkpro.tc.features.syntax;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.tc.api.exception.TextClassificationException;
import org.dkpro.tc.api.features.FeatureExtractor;
import org.dkpro.tc.api.features.Feature;
import org.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;
import org.dkpro.tc.api.type.TextClassificationTarget;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ADJ;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ADV;

/**
 * Extracts the ratio adjectives and adverbs to the total number of adjectives and adverbs.
 */
public class SuperlativeRatioFeatureExtractor
    extends FeatureExtractorResource_ImplBase
    implements FeatureExtractor
{
    public static final String FN_SUPERLATIVE_RATIO_ADJ = "SuperlativeRatioAdj";
    public static final String FN_SUPERLATIVE_RATIO_ADV = "SuperlativeRatioAdv";

    @Override
    public Set<Feature> extract(JCas jcas, TextClassificationTarget target)
        throws TextClassificationException
    {
        double adjRatio = 0;
        int superlativeAdj = 0;
        int adjectives = 0;
        for (ADJ tag : JCasUtil.selectCovered(jcas, ADJ.class, target)) {
            adjectives++;
            // FIXME Issue 123: depends on tagset
            if (tag.getPosValue().contains("JJS")) {
                superlativeAdj++;
            }
        }
        if (adjectives > 0) {
            adjRatio = (double) superlativeAdj / adjectives;
        }

        double advRatio = 0;
        int superlativeAdv = 0;
        int adverbs = 0;
        for (ADV tag : JCasUtil.selectCovered(jcas, ADV.class, target)) {
            adverbs++;
            // FIXME Issue 123: depends on tagset
            if (tag.getPosValue().contains("RBS")) {
                superlativeAdv++;
            }
        }
        if (adverbs > 0) {
            advRatio = (double) superlativeAdv / adverbs;
        }

        Set<Feature> features = new HashSet<Feature>();
        features.add(new Feature(FN_SUPERLATIVE_RATIO_ADJ, adjRatio));
        features.add(new Feature(FN_SUPERLATIVE_RATIO_ADV, advRatio));

        return features;
    }
}
