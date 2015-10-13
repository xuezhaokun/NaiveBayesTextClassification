package naivebayes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
/**
 * 
 * @author Zhaokun Xue
 * implement NaiveBayes for Text Classification
 */
public class NaiveBayes {
	private static int docCounter = 0;//# of training files
	private static double yesProbability = 0;//p yes
	private static double noProbability = 0;//p no
	private static int yesWordsCounter = 0;// total yes word
	private static int noWordsCounter = 0;// total no word
	private static HashMap<String, String> docClassification = new HashMap<String, String>();// <doc, yes/no>
	private static HashMap<String, Integer> docClassCounter = new HashMap<String, Integer>(); // <yes/no counter>
	private static List<List<String>> yesTenFolds = new ArrayList<List<String>>(); //yes folds
	private static List<List<String>> noTenFolds = new ArrayList<List<String>>(); //no folds
	private static List<List<String>> tenFolds = new ArrayList<List<String>>(); // ten folds stratified 
	private static HashMap<Integer, List<List<String>>> kTestTrainSplits = new HashMap<Integer, List<List<String>>>(); //ith test and train samples List[0]:test; List[1]:train

	/**
	 * parse the index file
	 * @param filename name for the index file
	 * @param dataPath the path of the file
	 * @throws IOException
	 */
	public static void parseIndexFile(String filename, String dataPath) throws IOException{
		String indexLine;
		String indexTokens[];
		BufferedReader indexFileBuffer;
		filename = dataPath + filename;
		indexFileBuffer = new BufferedReader(new FileReader(filename));
		while((indexLine = indexFileBuffer.readLine()) != null){
			indexTokens = indexLine.split("\\|");
			if(!docClassification.containsKey(indexTokens[0])){
				docCounter++;
				docClassification.put(indexTokens[0], indexTokens[1]);
			}
			if(!docClassCounter.containsKey(indexTokens[1])){
				docClassCounter.put(indexTokens[1], 1);
			}else{
				int currentCount = docClassCounter.get(indexTokens[1]);
				currentCount++;
				docClassCounter.put(indexTokens[1], currentCount);
			}
		}
		int yesDocCounter = docClassCounter.get("yes");
		int noDocCounter = docClassCounter.get("no");
		yesProbability = (double) yesDocCounter/docCounter;
		noProbability = (double) noDocCounter/docCounter;
		indexFileBuffer.close();
	}

	/**
	 * parse training set docs 
	 * @param trainSamples a list of training set docs' names
	 * @param dataPath path to reach these docs
	 * @return a hash map which has key:docs name; value: <wordname, Word>
	 * @throws IOException
	 */
	public static HashMap<String, HashMap<String, Word>> parseTrainDoc(List<String> trainSamples, String dataPath) throws IOException{

		HashMap<String, HashMap<String, Word>> docVocabulary = new HashMap<String, HashMap<String, Word>>();
		
		for(String file : trainSamples){//iterate each file
			String docClassifier = docClassification.get(file);//get doc's class
			String docFileName = dataPath+file+".clean";//doc path name
			String docLine;
			String words[];
			HashMap<String, Word> wordCount = new HashMap<String, Word>();//create for each file
			BufferedReader docBuffer;//doc reader buffer
			docBuffer = new BufferedReader(new FileReader(docFileName));
			//build vocabulary for each doc
			while((docLine = docBuffer.readLine()) != null){
				words = docLine.split(" ");
		    	for(String word : words){
			    	if(!wordCount.containsKey(word)){
			    		Word newWord = new Word(docClassifier, 1);
			    		wordCount.put(word, newWord);
			    	}else{
			    		Word currentWord = wordCount.get(word);
			    		int wordCurrentCount = currentWord.getWordCounter();
			    		wordCurrentCount++;
			    		currentWord.setWordCounter(wordCurrentCount);
			    		wordCount.put(word, currentWord);
			    	}
			    	if(docClassifier.equals("yes")){
			    		yesWordsCounter++;
			    	}else{
			    		noWordsCounter++;
			    	}
		    	}
			}
			docVocabulary.put(file, wordCount);
			docBuffer.close();
		}
		return docVocabulary;
	}
	
	/**
	 * build a hashmap for vocabulary of training set
	 * @param docVocabulary the doc vocabulary built from previous funtion
	 * @return the vocabulary based on traing set
	 */
	public static HashMap<String, HashMap<String, Integer>> countWords(HashMap<String, HashMap<String, Word>> docVocabulary){
		HashMap<String, HashMap<String, Integer>> wordClassCounter = new HashMap<String, HashMap<String, Integer>>();
		Collection<HashMap<String, Word>> allValues = docVocabulary.values();
		for(HashMap<String, Word> words : allValues){
			for(Map.Entry<String, Word> word : words.entrySet()){
				String wordname = word.getKey();
				String wordclass = word.getValue().getWordClass();
				int wordcounter = word.getValue().getWordCounter();
				HashMap<String, Integer> classcounter = new HashMap<String, Integer>();
				if(!wordClassCounter.containsKey(wordname)){
					classcounter.put(wordclass, wordcounter);
					wordClassCounter.put(wordname, classcounter);
				}else{
					HashMap<String, Integer> currentWordClassCounter = wordClassCounter.get(wordname);
					if(!currentWordClassCounter.containsKey(wordclass)){
						currentWordClassCounter.put(wordclass, wordcounter);
						wordClassCounter.put(wordname, currentWordClassCounter);
					}else{
						int currentClassCounter = currentWordClassCounter.get(wordclass);
						currentClassCounter += wordcounter;
						currentWordClassCounter.put(wordclass, currentClassCounter);
						wordClassCounter.put(wordname, currentWordClassCounter);
					}
				}
			}
		}
		return wordClassCounter;
	}

	/**
	 * read the content for a test file
	 * @param filename filename of test file
	 * @return list of string contains all words in this test file
	 * @throws IOException
	 */
	public static List<String> readTestFile(String filename) throws IOException{
		List<String> content = new ArrayList<String>();
		String[] words;
		String line;
		BufferedReader docBuffer;
		docBuffer = new BufferedReader(new FileReader(filename));
		while((line = docBuffer.readLine()) != null){//iterate lines
			words = line.split(" ");
		    for(String word : words){
		    	content.add(word);//add each line's word
		    }
		}
		docBuffer.close();
		return content;
	}

	/**
	 * make prediction for test
	 * @param content the test file's content
	 * @param vocabulary built based on training set
	 * @param m the smoothing parameter
	 * @return the prediction yes/no
	 */
	public static String predict(List<String> content, HashMap<String, HashMap<String, Integer>> vocabulary, double m){
		double yesScore = 0;
		double noScore = 0;
		int vocabularySize = vocabulary.size();//vocabulary size, V
		String result = null;

		for(String word : content){//go through each word in test file words' list
			if(vocabulary.containsKey(word)){// if word in vocabulary
				HashMap<String, Integer> wordCount = vocabulary.get(word);
				
				if(wordCount.containsKey("yes")){
					double tempProb = (double) (wordCount.get("yes") + m)/(yesWordsCounter + m * vocabularySize);
					tempProb = Math.log(tempProb);
					yesScore = yesScore + tempProb;
				}else{
					double tempProb = m/(yesWordsCounter + m * vocabularySize);
					yesScore = yesScore +  Math.log(tempProb);
				}
				if(wordCount.containsKey("no")){
					double tempProb = (double) (wordCount.get("no") + m)/(noWordsCounter + m * vocabularySize);
					tempProb = Math.log(tempProb);
					noScore = noScore + tempProb;
				}else{
					double tempProb = m/(noWordsCounter + m * vocabularySize);
					noScore = noScore +  Math.log(tempProb);
				}

			}else{// if word not in vocabulary, skip
				continue;
			}
		}
		
		yesScore = yesScore + Math.log(yesProbability);
		noScore = noScore + Math.log(noProbability);
		List<String> results = new ArrayList<String>();
		Random randomizer = new Random();
		results.add("yes");
		results.add("no");
		if(yesScore == Double.NEGATIVE_INFINITY && noScore == Double.NEGATIVE_INFINITY){
			result = results.get(randomizer.nextInt(results.size()));
		}else{
			if(yesScore >= noScore){
				result = "yes";
			}else{
				result = "no";
			}
		}
		return result;
	}
	
	/**
	 * implement stratified ten folds cross validation
	 * @param filename index file
	 * @param dataPath path to the file
	 * @throws IOException
	 */
	public static void tenFoldCrossValidation(String filename, String dataPath) throws IOException{
		BufferedReader indexFileBuffer;
		String indexLine;
		String[] indexTokens;
		List<String> yesDocs = new ArrayList<String>();
		List<String> noDocs = new ArrayList<String>();
		filename = dataPath + filename;
		indexFileBuffer = new BufferedReader(new FileReader(filename));
		
		while((indexLine = indexFileBuffer.readLine()) != null){
			indexTokens = indexLine.split("\\|");
			if(indexTokens[1].equals("yes")){
				yesDocs.add(indexTokens[0]);
			}else{
				noDocs.add(indexTokens[0]);
			}
		}
		Collections.shuffle(yesDocs);
		Collections.shuffle(noDocs);
		int yesDocsSize = yesDocs.size();
		int noDocsSize = noDocs.size();
		int yesFoldSize = yesDocsSize/10;
		int noFoldSize = noDocsSize/10;
		//System.out.println("yesFoldSize" + yesFoldSize);
		
		for(int i = 0; i < yesDocsSize; i = i + yesFoldSize){
			int j = i + yesFoldSize;
			if(j >= yesDocsSize){
				j = yesDocsSize;
			}
			List<String> tempFold = yesDocs.subList(i, j);
			yesTenFolds.add(tempFold);
		}
		
		for(int i = 0; i < noDocsSize; i = i + noFoldSize){
			int j = i + noFoldSize;
			if(j >= noDocsSize){
				j = noDocsSize;
			}
			List<String> tempFold = noDocs.subList(i, j);
			noTenFolds.add(tempFold);
		}
		
		for (int k = 0; k < 10; k++){
			List<String> yesktemp = yesTenFolds.get(k);
			List<String> noktemp = noTenFolds.get(k);
			List<String> temp = new ArrayList<String>(yesktemp);
			temp.addAll(noktemp);
			tenFolds.add(temp);
		}
		
		indexFileBuffer.close();
	}
	
	/**
	 * split train and test data based on the ten folds cross validation splits 
	 */
	public static void splitFolds(){
		for(int i = 0; i < 10; i++){
			List<List<String>> allFolds = new ArrayList<List<String>>();
			allFolds.addAll(tenFolds);
			List<List<String>> currentTestTrainSet = new ArrayList<List<String>>();
			List<String> trainSet = new ArrayList<String>();
			List<String> testSet = allFolds.get(i);
			currentTestTrainSet.add(testSet);
			allFolds.remove(i);
			for(List<String> fold : allFolds){
				trainSet.addAll(fold);
			}
			Collections.shuffle(trainSet);
			currentTestTrainSet.add(trainSet);
			if(!kTestTrainSplits.containsKey(i)){
				kTestTrainSplits.put(i, currentTestTrainSet);
			}
		}
	}
	
	/**
	 * implement Experiment 1 and Experiment 2
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("------------------ manually change datePath for different datasets ------------------");
		String dataPath = "data/ibmmac/"; // CHANGE THIS VARIABLE FOR DIFFERENT DATASET
		NaiveBayes.parseIndexFile("index.full", dataPath);
		NaiveBayes.tenFoldCrossValidation("index.full", dataPath);
		NaiveBayes.splitFolds();
		HashMap<Double, List<Double>> mData = new HashMap<Double, List<Double>>();
		
		// Experiment1
		System.out.println("*********** Experiment1 : " + dataPath  +" ***********");
		for(int m1 = 0; m1 < 2; m1++){
			HashMap<Double, List<Double>> iData = new HashMap<Double, List<Double>>();
		for (Map.Entry<Integer, List<List<String>>> entry : NaiveBayes.kTestTrainSplits.entrySet()) {
			List<List<String>> testTrain = entry.getValue();
			List<String> testSet = testTrain.get(0);
			List<String> trainSet = testTrain.get(1);
			int trainSize = trainSet.size();
			double[] trainSizePortion = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1};
			for(double portion : trainSizePortion){
				int endIndex = (int) (portion * trainSize);
				List<String> cutTrainSet = trainSet.subList(0, endIndex);
				HashMap<String, HashMap<String, Word>> docVocabulary = NaiveBayes.parseTrainDoc(cutTrainSet, dataPath);
				HashMap<String, HashMap<String, Integer>> vocabulary = NaiveBayes.countWords(docVocabulary);
				int accuracy = 0;
				for(String testfile : testSet){
					String testFileName = dataPath + testfile+".clean";
					List<String> currentContent = NaiveBayes.readTestFile(testFileName);
					String result = NaiveBayes.predict(currentContent, vocabulary, m1);
					String check = NaiveBayes.docClassification.get(testfile);
					if(check.equals(result)){
						accuracy++;
					}
				}
				double accuracyRate = (double) accuracy/testSet.size();
				if(!iData.containsKey(portion)){
					List<Double> iaccuracy = new ArrayList<Double>();
					iaccuracy.add(accuracyRate);
					iData.put(portion, iaccuracy);
				}else{
					List<Double> iaccuracy =iData.get(portion);
					iaccuracy.add(accuracyRate);
					iData.put(portion, iaccuracy);
				}
			}	
		}
		System.out.println("m1 = " + m1);
		for(Map.Entry<Double, List<Double>> iacc : iData.entrySet()){
			double iValue = iacc.getKey();
			List<Double> accs = iacc.getValue();
			double iaverage = StatistcCalculation.getMean(accs);
			double istd = StatistcCalculation.getStdDev(accs);
			System.out.println(iValue + " " + iaverage + " " + istd);
		}
		}
		// Experiment 2
		System.out.println("*********** Experiment2 : " + dataPath  +" ***********");
		for (Map.Entry<Integer, List<List<String>>> entry : NaiveBayes.kTestTrainSplits.entrySet()) {
			List<List<String>> testTrain = entry.getValue();
			List<String> testSet = testTrain.get(0);
			List<String> trainSet = testTrain.get(1);
			int halfSize = trainSet.size()/2;
			List<String> halfTrainSet = trainSet.subList(0, halfSize);
			HashMap<String, HashMap<String, Word>> docVocabulary = NaiveBayes.parseTrainDoc(halfTrainSet, dataPath);
			HashMap<String, HashMap<String, Integer>> vocabulary = NaiveBayes.countWords(docVocabulary);
			double[] mlist = new double[]{0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
			for(double m:mlist){
				int accuracy = 0;
				for(String testfile : testSet){
					String testFileName = dataPath + testfile+".clean";
					List<String> currentContent = NaiveBayes.readTestFile(testFileName);
					String result = NaiveBayes.predict(currentContent, vocabulary, m);
					String check = NaiveBayes.docClassification.get(testfile);
					if(check.equals(result)){
						accuracy++;
					}
				}
				double accuracyRate = (double) accuracy/testSet.size();
				if(!mData.containsKey(m)){
					List<Double> maccuracy = new ArrayList<Double>();
					maccuracy.add(accuracyRate);
					mData.put(m, maccuracy);
				}else{
					List<Double> maccuracy = mData.get(m);
					maccuracy.add(accuracyRate);
					mData.put(m, maccuracy);
				}
			}
		}
		for(Map.Entry<Double, List<Double>> macc : mData.entrySet()){
			double mValue = macc.getKey();
			List<Double> accs = macc.getValue();
			double maverage = StatistcCalculation.getMean(accs);
			double mstd = StatistcCalculation.getStdDev(accs);
			System.out.println(mValue + " " + maverage + " " + mstd);
		}
	}

}