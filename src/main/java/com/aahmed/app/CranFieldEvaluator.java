package com.aahmed.app;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import static com.aahmed.app.Constants.indexDir;
import static com.aahmed.app.Constants.dataDir;
import static com.aahmed.app.Constants.relQuerl;
import static com.aahmed.app.Constants.quer;

public class CranFieldEvaluator {

    public static void main(String[] args) {
        CranFieldEvaluator tester;
        try {
            tester = new CranFieldEvaluator();
            tester.createIndex();
            tester.evaluate(new BM25Similarity());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createIndex() throws IOException {
        Indexer indexer = new Indexer(indexDir);
        int numIndexed;
        long startTime = System.currentTimeMillis();
        numIndexed = indexer.createIndex(dataDir);
        long endTime = System.currentTimeMillis();
        indexer.close();
        System.out.println(numIndexed + " File indexed, time taken: " + (endTime - startTime) + " ms");
    }

    private void evaluate(Similarity similarity) throws Exception {
        Searcher searcher = new Searcher(indexDir);
        long startTime = System.currentTimeMillis();

        searcher.setSimilarity(similarity);


        double sumAveragePrecision = 0.0;
        double sumRecall = 0.0;

        ArrayList<String> queries = loadQueries();
        ArrayList<HashSet<Integer>> relevance = parseRelavance();

        for (int i = 0; i < queries.size(); i++) {
            ArrayList<Integer> hitDocIds = searcher.search(queries.get(i).replace("?", ""), Constants.MAX_SEARCH);
            HashSet<Integer> standardDocIds = relevance.get(i);

            int numTruePositive = 0;
            double sumPrecision = 0.0;
            for (int j = 0; j < hitDocIds.size(); j++) {
                int hitID = hitDocIds.get(j);
                if (standardDocIds.contains(hitID)) {
                    numTruePositive++;
                    double precision = (double) numTruePositive / (j + 1);
                    sumPrecision += precision;
                }
            }

            // Per query, average precision and recall.

            double averagePrecision = 0.0;
            double recall = 0.0;
            if (standardDocIds.size() == 0) { // if standard answer is 0, set MAP and recall to 1.0
                averagePrecision = 1.0;
                recall = 1.0;
            } else {
                averagePrecision = (numTruePositive == 0 ? 0.0 : sumPrecision / numTruePositive);
                recall = (double) numTruePositive / standardDocIds.size();
            }

            sumAveragePrecision += averagePrecision;
            sumRecall += recall;

            System.out.println(String.format("Query: %d, MAP: %.4f, Recall: %.4f", i + 1, averagePrecision, recall));

        }

        double meanAveragePrecision = sumAveragePrecision / queries.size();
        double meanRecall = sumRecall / queries.size();

        System.out.println(String.format("MAP: %.4f, Recall: %.4f", meanAveragePrecision, meanRecall));
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);

    }

    private ArrayList<HashSet<Integer>> parseRelavance() {
        ArrayList relevance_base = new ArrayList();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(relQuerl));
            String line = null;
            int oldQueryId = 1;
            HashSet<Integer> set = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\s+");
                int queryId = Integer.parseInt(items[0]);
                int documentId = Integer.parseInt(items[1]);
                int relevance = Integer.parseInt(items[2]);

                // New queryId starts or not
                if (queryId != oldQueryId) {
                    oldQueryId = queryId;
                    relevance_base.add(set);
                    set = new HashSet<>();
                }
                if (relevance <= 3) {
                    set.add(documentId);
                }
            }
            // last queryId
            relevance_base.add(set);
        } catch (IOException e) {
            System.out.println("Read baseline file failed");
        }

        return relevance_base;
    }

    private ArrayList<String> loadQueries() throws IOException {
        return (ArrayList<String>) Files.readAllLines(Paths.get(quer));
    }
}
