
import edu.ehu.galan.cvalue.CValueAlgortithm;
import edu.ehu.galan.cvalue.filters.english.AdjNounFilter;
import edu.ehu.galan.cvalue.model.Document;
import edu.ehu.galan.cvalue.model.Term;
import edu.ehu.galan.cvalue.model.Token;
import edu.illinois.cs.cogcomp.chunker.main.lbjava.Chunker;
import edu.illinois.cs.cogcomp.lbjava.nlp.SentenceSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.WordSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.PlainToTokenParser;
import edu.illinois.cs.cogcomp.pos.lbjava.POSTagger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("This is a custom layer for C-Value implementation by Angel Conde " +
                "(Github.com/Neuw84/CValue-TermExtraction). This implementation adds time data for temporal analysis" +
                " and is intended to be used with textual sources. In this specific case, data.json contains information" +
                " extracted from MINECO-GRANT RTI2018-093336-B-C21 project. If you want to test this code with other" +
                " sources, you should adapt it.");
        System.out.println("Starting parser with filter: (AdjNounFilter)...");
        System.out.println("reading data.json. Please wait.");

        File jsonFile = new File("./data.json");
        boolean exists = jsonFile.exists();

        if(exists) {
            System.out.println("data.json found ("+jsonFile.length() / 1000 +" KB)");
        }else{
            System.out.println("File not found... program will stop now.");
            return;
        }
        JSONObject parsedResults = new JSONObject();
        String pattern = "yyyy/MM/dd HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);
        String defaultDateString = df.format(Calendar.getInstance().getTime());

        try {
            JSONParser jsonParser = new JSONParser();
            Object obj = jsonParser.parse(new FileReader(
                    "./data.json"));
            JSONObject jsonObject = (JSONObject) obj;
            jsonObject.keySet().forEach( k -> {
                Object root = jsonObject.get(k);
                Object value = ((JSONObject) root).get("individual");
                if (value instanceof JSONObject){
                    System.out.println("Analysing data. This could take a while depending on the size of data.json");
                    String userID = (String) k;
                    Boolean isPositive = (Boolean)((JSONObject) value).get("isPositive");
                    JSONArray userResults = new JSONArray();

                    if( ((JSONObject) value).get("writing") instanceof JSONArray) {
                        JSONArray writing = (JSONArray) ((JSONObject) value).get("writing");
                        int count = writing.size();
                        System.out.println("Parsing posts for user " + userID + "(" + count + " posts)");

                        JSONArray simple = new JSONArray();
                        // IMPORTANT: update this part if you want to process all files
                        System.out.println("For permorance reasons we are only processing a small subset of posts." +
                                "you can disable this updating the source code");
                        int limit_posts = 20;
                        for(int i = 0; i< limit_posts; i++) {
                            if(i < count) {
                                simple.add(writing.get(i));
                            }
                        }




                        simple.forEach(w -> {
                            JSONObject item = (JSONObject) w;
                            System.out.println(item.toJSONString());
                            String text = null;
                            String date = null;
                            try{
                                text = (String) item.get("text");
                            }catch (ClassCastException e) {
                                //e.printStackTrace();
                                text = (String) item.get("title");
                            }
                            try{
                                date = (String) item.get("date");
                            }catch (ClassCastException e) {
                                //e.printStackTrace();
                                date = defaultDateString;
                            }


                            List<LinkedList<Token>> tokenizedSentenceList = new ArrayList<>();
                            List<String> sentenceList = new ArrayList<>();
                            POSTagger tagger = new POSTagger();
                            Chunker chunker = new Chunker();
                            boolean first = true;

//                            FileWriter file = null;
//                            try {
//                                file = new FileWriter("./temp");
//                                file.write(text);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }


                            PlainToTokenParser parser = new PlainToTokenParser(new WordSplitter(new SingleSentence(text)));
                            String sentence = "";
                            LinkedList<Token> tokenList = null;

                            for (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token word = (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token) parser.next(); word != null;
                                 word = (edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token) parser.next()) {
                                String chunked = chunker.discreteValue(word);
                                tagger.discreteValue(word);
                                if (first) {
                                    tokenList = new LinkedList<>();
                                    tokenizedSentenceList.add(tokenList);
                                    first = false;
                                }
                                tokenList.add(new Token(word.form, word.partOfSpeech, null, chunked));
                                sentence = sentence + " " + (word.form);
                                if (word.next == null) {
                                    sentenceList.add(sentence);
                                    first = true;
                                    sentence = "";
                                }
                            }
                            parser.reset();
                            Document doc = new Document("./result","result.txt");
                            doc.setSentenceList(sentenceList);
                            doc.setTokenList(tokenizedSentenceList);
                            CValueAlgortithm cvalue = new CValueAlgortithm();
                            cvalue.init(doc); // initializes the algorithm for processing the desired document.
                            cvalue.addNewProcessingFilter(new AdjNounFilter()); //for example the AdjNounFilter
                            cvalue.runAlgorithm(); //process the CValue algorithm with the provided filters
                            List<Term> terms = doc.getTermList(); //get the results
                            JSONObject results = new JSONObject();
                            for(Term term : terms){
//                                System.out.println(term.toString());
                                results.put(term.getTerm(), term.getScore());
                            }
                            JSONObject resultWithTime = new JSONObject();
                            resultWithTime.put("date", date);
                            resultWithTime.put("terms", results);
                            userResults.add(resultWithTime);

//                            if(file!=null){
//                                try {
//                                    file.flush();
//                                    file.close();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
                        });
                        parsedResults.put(userID, userResults);
                    }else{
                        System.out.println("Error reading JSON structure! Please verify source format.");
                        System.out.println("writing object class: "+ value.getClass().getName());
                    }

                }else{
                    System.out.println("Error reading JSON structure! Please verify source format.");
                    System.out.println("Root object class: "+ value.getClass().getName());
                }

            });

            System.out.println("Process completed. Exporting data to results.json");
            FileWriter file = new FileWriter("./results.json");
            file.write(parsedResults.toJSONString());
            if(file!=null){
                try {
                    file.flush();
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            System.out.println("File successfully created. Please check results.json");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

}
