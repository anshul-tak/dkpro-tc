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
package org.dkpro.tc.features.ngram.base;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.frequency.tfidf.model.DfModel;
import de.tudarmstadt.ukp.dkpro.core.frequency.tfidf.util.TfidfUtils;
import org.dkpro.tc.api.features.meta.MetaCollector;
import org.dkpro.tc.features.ngram.meta.NGramMetaCollector;

@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token" })
public class FrequencyDistributionNGramFeatureExtractorBase
    extends NGramFeatureExtractorBase
{
    public static final String PARAM_NGRAM_FD_FILE = "ngramFdFile";
    @ConfigurationParameter(name = PARAM_NGRAM_FD_FILE, mandatory = true)
    private String ngramFdFile;

    public static final String PARAM_DFSTORE_FILE = "dfStoreFile";
    @ConfigurationParameter(name = PARAM_DFSTORE_FILE, mandatory = true)
    private String dfStoreFile;
    
    public static final String FD_NGRAM_FIELD = "ngram";

    private FrequencyDistribution<String> trainingFD;

    @Override
    public List<Class<? extends MetaCollector>> getMetaCollectorClasses()
    {
        List<Class<? extends MetaCollector>> metaCollectorClasses = new ArrayList<Class<? extends MetaCollector>>();
        metaCollectorClasses.add(NGramMetaCollector.class);

        return metaCollectorClasses;
    }
    
    
    @Override
    public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
        throws ResourceInitializationException
    {
        if (!super.initialize(aSpecifier, aAdditionalParams)) {
            return false;
        }
       
        dfStore = getDfStore();
        
        return true;
    }    
    

    @Override
    protected FrequencyDistribution<String> getTopNgrams()
        throws ResourceInitializationException
    {
        try {
            trainingFD = new FrequencyDistribution<String>();
            trainingFD.load(new File(ngramFdFile));
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        catch (ClassNotFoundException e) {
            throw new ResourceInitializationException(e);
        }

        FrequencyDistribution<String> topNGrams = new FrequencyDistribution<String>();

        // FIXME - this is a really bad hack, but currently no better FD method to return
        // topK samples each of size n or greater.

        Map<String, Long> map = new HashMap<String, Long>();
        int ngramVocabularySize = 0;
        
        for (String key : trainingFD.getKeys()) {
        	long count = trainingFD.getCount(key);
        	ngramVocabularySize += count;
            map.put(key, count);
        }

        Map<String, Long> sorted_map = new TreeMap<String, Long>(new ValueComparator(map));
        sorted_map.putAll(map);
        
        int i = 0;
        for (String key : sorted_map.keySet()) {
        	long absCount = trainingFD.getCount(key);
        	double relFrequency = ((double) absCount) / ngramVocabularySize;
        	
            if (i >= ngramUseTopK || relFrequency < ngramFreqThreshold) {
                break;
            }
            topNGrams.addSample(key, absCount);
            i++;
        }

        getLogger().log(Level.INFO, "+++ TAKING " + topNGrams.getKeys().size() + " NGRAMS");

        return topNGrams;
    }
    

    protected DfModel getDfStore() throws ResourceInitializationException{   	
    	DfModel dfModel;
    	try {
    		dfModel = TfidfUtils.getDfModel(dfStoreFile);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
    	return dfModel;
    };

    public class ValueComparator
        implements Comparator<String>
    {

        Map<String, Long> base;

        public ValueComparator(Map<String, Long> base)
        {
            this.base = base;
        }

        @Override
        public int compare(String a, String b)
        {

            if (base.get(a) < base.get(b)) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }

    @Override
    protected String getFeaturePrefix()
    {
        return "ngram";
    }

    @Override
    protected String getFieldName()
    {
        return FD_NGRAM_FIELD;
    }

    @Override
    protected int getTopN()
    {
        return ngramUseTopK;
    }
}