package com.AQAS.question_type;


import com.AQAS.document_retrieval.HelpersD;
import opennlp.tools.util.StringList;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import weka.core.Instance;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;


import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;

public class Driver {


    /**
     * Generates the Instances object and outputs it in ARFF format to stdout.
     *
     * @param args ignored
     * @throws Exception if generation of instances fails
     */
    public static void main(String[] args) throws Exception {



         Instances ngrammedVectores =  HelpersQT.initializeStringToWordVector();
//
//        int n = 2; //n-gram
//        String[] s = "I saw the fox saw the fox".split(" ");
//        Collection<StringList> nGrams = NGramUtils.getNGrams(new StringList(s), n);
//        Collection<StringList> nGrams_cloned = NGramUtils.getNGrams(new StringList(s), n);
//
//
//        Set<StringList> hs = new HashSet<>();
//        hs.addAll(nGrams_cloned);
//        nGrams_cloned.clear();
//        nGrams_cloned.addAll(hs);
//
//        System.out.println(nGrams_cloned);
//        for (StringList stringList : nGrams_cloned) {
//            if (n == 1) {
//                System.out.println(stringList.toString() + ": " + NGramUtils.calculateUnigramMLProbability(stringList.getToken(0), nGrams));
//            } else if (n == 2) {
//                System.out.println(stringList.toString() + ": " + NGramUtils.calculateNgramMLProbability(stringList, nGrams));
//            }
//        }

    }
}