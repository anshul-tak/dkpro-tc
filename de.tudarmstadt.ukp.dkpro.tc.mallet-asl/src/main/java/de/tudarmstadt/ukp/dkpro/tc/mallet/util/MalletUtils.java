package de.tudarmstadt.ukp.dkpro.tc.mallet.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureStore;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Instance;

/**
 * Utility class for the Mallet machine learning toolkit
 *
 * @author Krish Perumal
 *
 */
public class MalletUtils
{
	
	//TODO yet to decide when to call this method
	public static void writeFeatureNamesToFile(FeatureStore instanceList, File outputFile) throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile))));
		HashMap<String, Integer> featureOffsetIndex = new HashMap<String, Integer>();
		for(int i=0; i < instanceList.size(); i++) {
			Instance instance = instanceList.getInstance(i);
			for (Feature feature : instance.getFeatures()) {
				String featureName = feature.getName();
				if(!featureOffsetIndex.containsKey(featureName)) {
					featureOffsetIndex.put(featureName, featureOffsetIndex.size());
					bw.write(featureName + " ");
				}
			}
		}
		bw.write("Outcome");
		bw.close();
	}
	
	public static void writeFeatureValuesToFile(double featureValues[], String outcome, File outputFile) throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile, true))));
		bw.write("\n");
		for (double featureValue : featureValues) {
			bw.write(featureValue + " ");
		}
		bw.write(outcome);
		bw.flush();
		bw.close();
	}
	
	public static HashMap<String, Integer> getFeatureOffsetIndex(FeatureStore instanceList) {
		HashMap<String, Integer> featureOffsetIndex = new HashMap<String, Integer>();
		for(int i=0; i < instanceList.size(); i++) {
			Instance instance = instanceList.getInstance(i);
			for (Feature feature : instance.getFeatures()) {
				String featureName = feature.getName();
				if(!featureOffsetIndex.containsKey(featureName)) {
					featureOffsetIndex.put(featureName, featureOffsetIndex.size());
				}
			}
			
		}
		return featureOffsetIndex;
	}

	public static void instanceListToMalletFormatFile(File outputFile, FeatureStore instanceList,
			boolean useDenseInstances, boolean isRegressionExperiment)
					throws Exception
	{
		// check for error conditions
		if (instanceList.getUniqueOutcomes().isEmpty()) {
			throw new IllegalArgumentException("List of instance outcomes is empty.");
		}
		
		HashMap<String, Integer> featureOffsetIndex = getFeatureOffsetIndex(instanceList);
		
		writeFeatureNamesToFile(instanceList, outputFile);
		
		for (int i = 0; i < instanceList.size(); i++) {
			Instance instance = instanceList.getInstance(i);
			String outcome = instance.getOutcome();
			double featureValues[] = new double[featureOffsetIndex.size()];
			for (Feature feature : instance.getFeatures()) {
				String featureName = feature.getName();
				Object value = feature.getValue();
				double featureValue = 0.0;
				if (value instanceof Number) {
					featureValue = ((Number) value).doubleValue();
				}
				else if (value instanceof Boolean) {
					featureValue = (Boolean) value ? 1.0d : 0.0d;
				}
				else {
					//TODO nominal or string feature values
					featureValue = 0.0;
				}
				if(featureOffsetIndex.containsKey(featureName)) {
					featureValues[featureOffsetIndex.get(featureName)] = featureValue;
				}
			}
			writeFeatureValuesToFile(featureValues, outcome, outputFile);
		}
	}
}
