package naivebayes;

/**
 * helper class for constructing an object to remember the word's class and counts
 * @author Zhaokun Xue
 *
 */
public class Word {
	private String wordClass;
	private int wordCounter;
	
	public Word(String wordClass, int wordCounter) {
		this.wordClass = wordClass;
		this.wordCounter = wordCounter;
	}
	public String getWordClass() {
		return wordClass;
	}
	public void setWordClass(String wordClass) {
		this.wordClass = wordClass;
	}
	public int getWordCounter() {
		return wordCounter;
	}
	public void setWordCounter(int wordCounter) {
		this.wordCounter = wordCounter;
	}
	
}
