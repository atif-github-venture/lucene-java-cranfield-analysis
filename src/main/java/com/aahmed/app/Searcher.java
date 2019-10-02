package com.aahmed.app;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

class Searcher {

    private IndexSearcher indexSearcher;
    private QueryParser queryParser;
    private Analyzer analyzer = new StandardAnalyzer();


    void setSimilarity(Similarity similarity) {
        indexSearcher.setSimilarity(similarity);
    }

    Searcher(String indexDirectoryPath) throws IOException {
        Directory indexDirectory = FSDirectory.open(Paths.get(indexDirectoryPath));
        DirectoryReader ireader = DirectoryReader.open(indexDirectory);
        indexSearcher = new IndexSearcher(ireader);
        queryParser = new QueryParser(Constants.CONTENTS, new StandardAnalyzer());
    }

    TopDocs search(String searchQuery) throws IOException, ParseException {
        Query query = queryParser.parse(searchQuery);
        return indexSearcher.search(query, Constants.MAX_SEARCH);
    }

    ArrayList<Integer> search(String queryStr, int topHitsCount) {
        String fields[] = new String[]{Constants.CONTENTS};
        queryParser = new MultiFieldQueryParser(fields, analyzer);

        try {
            Query query = queryParser.parse(queryStr);
            ScoreDoc[] hits = indexSearcher.search(query, topHitsCount).scoreDocs;
            ArrayList<Integer> docIds = new ArrayList<>();
            for (ScoreDoc hit : hits) {
                Document doc = indexSearcher.doc(hit.doc);
                int id = Integer.parseInt(doc.getFields().get(1).stringValue());
                docIds.add(id);
//                System.out.println("File: " + doc.getFields().get(1).stringValue() + ", its score -> "+ hit.score);
            }
            return docIds;
        } catch (ParseException e) {
            System.out.println("Can't parse query");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }

}