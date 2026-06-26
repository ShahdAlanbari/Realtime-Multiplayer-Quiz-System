package quizgame;
/**
 * 2108516
 * 2206145
 * 2006728
 */  

/**
 * Represents a single quiz question, including its text,
 * answer options, correct answer, time limit, and points value.
 * This class is immutable: all fields are final and assigned once.
 */
public class Question {

    // Core question text shown to the player
    private final String questionText;

    // Multiple-choice answer options
    private final String optionA;
    private final String optionB;
    private final String optionC;
    private final String optionD;

    // Correct answer key (must match one of the options)
    private final String correctAnswer;

    // Time allowed to answer this question (in seconds)
    private final int timeLimit;

    // Points awarded for answering correctly
    private final int pointsValue;

    /**
     * Constructor initializes all question properties.
     * Values should be validated before being passed here.
     */
    public Question(String questionText,
                    String optionA,
                    String optionB,
                    String optionC,
                    String optionD,
                    String correctAnswer,
                    int timeLimit,
                    int pointsValue) {

        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.timeLimit = timeLimit;
        this.pointsValue = pointsValue;
    }

    // Getter methods used by GUI, DB, and quiz logic
    public String getQuestionText() { return questionText; }
    public String getOptionA()      { return optionA; }
    public String getOptionB()      { return optionB; }
    public String getOptionC()      { return optionC; }
    public String getOptionD()      { return optionD; }
    public String getCorrectAnswer(){ return correctAnswer; }
    public int getTimeLimit()       { return timeLimit; }
    public int getPointsValue()     { return pointsValue; }

    /**
     * Returns a readable representation of the question,
     * mainly for debugging or console logging.
     */
    @Override
    public String toString() {
        return "Question{" + "questionText='" + questionText + '\'' + '}';
    }
}
