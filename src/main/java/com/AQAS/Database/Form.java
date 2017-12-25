package com.AQAS.Database;

import com.AQAS.Document_ranking.ConfigDR;
import com.AQAS.Document_ranking.DocumentRanking;
import com.AQAS.Document_ranking.HelpersDR;
import com.AQAS.answer_extraction.ConfigAE;
import com.AQAS.keyphrase_extraction.ConfigKE;
import com.AQAS.keyphrase_extraction.HelpersKE;
import com.AQAS.main.HelpersM;
import com.AQAS.question_type.ConfigQT;
import com.AQAS.synonyms.FindSynonyms;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.*;

import static com.AQAS.Database.HelpersDB.props;

public class Form {

    public int id;
    public int question_id;
    public String text;
    public int question_type;
    public String normalizedText;
    public ArrayList<Document> documents = new ArrayList<Document>();
    private String[] keyPhrases = null;
    public ArrayList<Segment> topSegmentsByRank = new ArrayList<>();// top segments by rank
    public ArrayList<Segment> topSegmentsByOrder = new ArrayList<>();//top segments by order of document
    public ArrayList<Answer> answers = new ArrayList<Answer>();// the segments ordered by Rank


    private static Form singletonForm;
    public static Form getInstance(){
        if(singletonForm == null){
            singletonForm = new Form();
        }
        return singletonForm;
    }



    public Form() {
    }

    public ArrayList<Segment> getTopSegmentsByOrder() {
        return topSegmentsByOrder;
    }

    public void setTopSegmentsByOrder(ArrayList<Segment> topSegmentsByOrder) {
        this.topSegmentsByOrder = topSegmentsByOrder;
    }

    public Form(int id) {
        this.id = id;
    }

    public Form(String text) {
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestion_id() {
        return question_id;
    }

    public void setQuestion_id(int question_id) {
        this.question_id = question_id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getQuestion_type() {
        return question_type;
    }

    public void setKeyPhrases(String[] keyPhrases) {
        this.keyPhrases = keyPhrases;
    }

    public void setKeyPhrases() {
        String[] queryKeyPhrases = HelpersKE.getKeyPhrases(this.normalizedText);

        if (ConfigKE.VERBOS) {
            System.out.println("Keyphrases List is :");
            System.out.println(Arrays.toString(queryKeyPhrases));
        }
        ArrayList<String> queryKeyPhraseArrayList = new ArrayList<String>(Arrays.asList(queryKeyPhrases));
        for (String queryKeyPhrase : queryKeyPhrases) {
            String[] keyPhraseSynonyms = FindSynonyms.getWordSynonyms(queryKeyPhrase);
            if (ConfigKE.VERBOS) {
                System.out.println("Synonyms for keyphrase \"" + queryKeyPhrase + "\" are: ");
                System.out.println(Arrays.asList(keyPhraseSynonyms));
            }
            System.out.println("all keyphrases and synonyms");
            queryKeyPhraseArrayList.addAll(Arrays.asList(keyPhraseSynonyms));
        }
        queryKeyPhrases = queryKeyPhraseArrayList.toArray(new String[queryKeyPhraseArrayList.size()]);

        this.keyPhrases = queryKeyPhrases;
    }
    public String[] getKeyPhrases() {
        if (this.keyPhrases == null) {
            this.setKeyPhrases();
        }
        return this.keyPhrases;
    }
    public ArrayList<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(ArrayList<Answer> answers) {
        this.answers = answers;
    }

    public int store() {
        try {
            org.jsoup.nodes.Document doc = Jsoup.connect(props.getProperty("LOCAL_SERVER_IP") + "forms/" + this.question_id)
                    .data("text", this.text)
                    .userAgent("Mozilla")
                    .post();
            return Integer.parseInt(doc.text());

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static Form getFormById(int id) {
        return null;
    }

    public ArrayList<Document> getDocuments() {
        try {
            String json = Jsoup.connect(props.getProperty("LOCAL_SERVER_IP") + "/forms/document/" + this.id).ignoreContentType(true).execute().body();
            // System.out.println("JSON: "+json);
            JSONParser parser = new JSONParser();
            Object obj = null;
            obj = parser.parse(json.toString());

            JSONArray jsonArray = (JSONArray) obj;
            Iterator<JSONObject> iterator = jsonArray.iterator();
            while (iterator.hasNext()) {
                JSONObject tmp = iterator.next();
                String link = (String) tmp.get("link");
                String text = StringEscapeUtils.unescapeJava((String) tmp.get("text"));
                int document_id = Integer.parseInt(tmp.get("id") + "");
                JSONObject pivot = (JSONObject) tmp.get("pivot");

                double urltRank = Double.parseDouble(pivot.get("urlRank") + "");
                this.documents.add(new Document(document_id, link, text, this.id, urltRank));

            }

            return this.documents;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Document> getDocumentsForRank() {
        try {
            String json = Jsoup.connect(props.getProperty("LOCAL_SERVER_IP") + "/forms/documentRank/" + this.id).ignoreContentType(true).execute().body();
            // System.out.println("JSON: "+json);
            JSONParser parser = new JSONParser();
            Object obj = null;
            obj = parser.parse(json.toString());

            JSONArray jsonArray = (JSONArray) obj;
            Iterator<JSONObject> iterator = jsonArray.iterator();
            while (iterator.hasNext()) {

                JSONObject tmp = iterator.next();
                int document_id = Integer.parseInt(tmp.get("id") + "");
                double contentRank = Double.parseDouble(tmp.get("contentRank") + "");
                double urlRank = Double.parseDouble(tmp.get("urlRank") + "");
                this.documents.add(new Document(document_id, this.id, urlRank, contentRank));

            }

            return this.documents;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void removeIrrelevantDocuments() {
        double relevancyThreshold = getRelevancyThreshold();
        System.out.println("Choosen Threshold is :" + relevancyThreshold);
        for (Iterator<Document> document = documents.iterator(); document.hasNext(); ) {
            if (document.next().overAllRank() < relevancyThreshold) {
                document.remove();
            }
        }
    }

    private double getRelevancyThreshold() {

        if (ConfigDR.THRESHOLD_SOURCE == ConfigDR.STATIC_THRESHOLD) {
            return ConfigDR.RELEVANCY_THRESHOLD;
        } else if (ConfigDR.THRESHOLD_SOURCE == ConfigDR.STATISTICAL_THRESHOLD) {
            double avg = getDocumentsRanksAvg();
            double standardDeviation = getDocumentsRankStandardDeviation();
            if (avg <= standardDeviation) {
                return avg;
            } else {
                return avg - standardDeviation;
            }
        }
        return 1;//TODO
    }


    @Override
    public String toString() {
        return "Form{" +
                "id=" + id +
                ", question_id=" + question_id +
                ", text='" + text + '\'' +
                ", documents=" + documents +
                '}';
    }

    public double getDocumentsRanksAvg() {
        double sum = 0;
        for (Document document : this.documents) {
            sum += document.overAllRank();
        }
        return sum / this.documents.size();
    }

    public double getDocumentsRankStandardDeviation() {
        double mean = getDocumentsRanksAvg();
        double sum = 0;
        for (Document document : this.documents) {
            sum += Math.pow((document.overAllRank() - mean), 2);
        }
        return Math.sqrt(sum / this.documents.size());
    }


    public void setQuestion_type(int question_type) {
        this.question_type = question_type;
    }




    /*
   *
   * this will generate the segments for each document in a form
    */
    public void generateFormDocumentsSegments() {

        ArrayList<Segment> tempTopSegmentsByOrder = new ArrayList<>();
        String[] questionKeyPhrases = this.getKeyPhrases();
        for (Document document : this.documents) {
            document.generateDocumentSegments(questionKeyPhrases, null);
            document.calculateSegmentsRanks(this);
            document.setSegmentsOrder();
            document.removeIrrelevantSegments();

            //Get top N segments according to their order in the document
            Collections.sort(document.segments, (o1, o2) -> {
                double diff = (o2.getSegmentOrder() - o1.getSegmentOrder());
                return diff != 0 ? (diff > 0 ? -1 : 1) : 0;
            });

//            tempTopSegmentsByOrder.addAll(document.segments.subList(0, ConfigAE.TOP_SEGMENTS_BY_ORDER));
            tempTopSegmentsByOrder.addAll(document.segments.subList(0, (int) Math.ceil((double)ConfigAE.topN.DEFINITION/(double)this.documents.size())));

//            //just printing
//            System.out.println("*************After segmentation process*************");
//            PrintWriter writer = null;
//            try {
//                writer = new PrintWriter(new FileOutputStream(
//                        new File("out.txt"),
//                        true /* append = true */));
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            for (Segment segment: document.segments) {
//                writer.println(segment.text);
//                writer.println("Rank is :" + segment.getRank());
//                writer.println("types are is :" + segment.findSegmentTypes());
//
//
//                writer.println("==================================================");
//            }
//            writer.println("*****************Finshed the current Document************");
//            writer.close();
        }

        //filling documents in form.topPassages ArrayList
        ArrayList<Segment> tempTopSegments = new ArrayList<Segment>();
        for (Document document : this.documents) {
            tempTopSegments.addAll(document.getSegments());
        }
        Collections.sort(tempTopSegments);
        this.setTopSegmentsByRank(tempTopSegments);//best segments in all documents

        Collections.sort(tempTopSegmentsByOrder);
        this.setTopSegmentsByOrder(tempTopSegmentsByOrder);


    }

    public void setNormalizedText(String normalizedText) {
        this.normalizedText = normalizedText;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public ArrayList<Segment> getTopSegmentsByRank() {
        return topSegmentsByRank;
    }

    public void setTopSegmentsByRank(ArrayList<Segment> topSegmentsByRank) {
        this.topSegmentsByRank = topSegmentsByRank;
    }


    public void extractAnswer() {
        int topSegmentsSize = this.topSegmentsByRank.size();
        switch (this.question_type) {

            case ConfigQT.QT_LIST:
                for (int i = 0; i < ConfigAE.topN.LIST; i++) {
                    try {
                        this.answers.add(new Answer(this.topSegmentsByRank.get(i).getText()));
                    } catch (Exception e) {
                        break;
                    }
                }
                break;

            case ConfigQT.QT_NUMERIC:
                for (int i = 0; i < ConfigAE.topN.NUMERIC; i++) {

                    try {
                        Segment segment = this.topSegmentsByRank.get(i);
                        String segmentText = segment.getText();
                        String[] segmentSentences = segmentText.split("\\.");

                        Answer bestAnswer = new Answer();
                        double bestCosine = -1;
                        for (String sentence : segmentSentences) {
                            //Answer dummyAnswer = Answer(sentence);
                            if (HelpersM.regexCount("\\d+", sentence) > 1) {// the sentence has a number
                                double cosSim = HelpersDR.cosineSimilarity(sentence, this.text);
                                if (cosSim > bestCosine) {
                                    bestAnswer.setText(sentence);
                                    bestAnswer.setRank(cosSim);
                                }
                            }
                        }
                        this.answers.add(bestAnswer);

                    } catch (Exception e) {
                        break;
                    }

                }
                Collections.sort(this.answers);
                break;

            case ConfigQT.QT_PARAGRAPH:
                for (int i = 0; i < ConfigAE.topN.DEFINITION; i++) {
                    try {
                        this.answers.add(new Answer(this.topSegmentsByOrder.get(i).getText()));
                    } catch (Exception e) {
                        break;
                    }
                }
                break;
        }

        this.printAnswers();
    }

    public void printAnswers() {
        System.out.println(this.answers);
    }

    public void setDocuments(ArrayList<Document> documents) {
        this.documents = documents;
    }

    public void calculateDocumentsRanks() {
        for (Document document : this.documents) {
            double contentRank = DocumentRanking.getDocumentRank(document.text, this.normalizedText);
            document.setContentRank(contentRank);
        }
    }
}
