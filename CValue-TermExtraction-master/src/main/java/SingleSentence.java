import edu.illinois.cs.cogcomp.lbjava.nlp.Sentence;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;

import java.util.LinkedList;

public class SingleSentence implements Parser {

    protected LinkedList<Sentence> sentences;

    public SingleSentence(String sentenceText) {
        super();
        this.sentences = new LinkedList<>();
        String[] dotSplitted = sentenceText.split("(?<=[a-z])\\.\\s+");

        for(String s : dotSplitted) {
//            System.out.println(s);
            this.sentences.add(new Sentence(s));
        }

    }

    @Override
    public Object next() {
        return this.sentences.size() == 0 ? null : this.sentences.removeFirst();
    }

    @Override
    public void reset() {

    }

    @Override
    public void close() {

    }
}
