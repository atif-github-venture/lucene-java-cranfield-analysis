package com.aahmed.app;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

class Indexer {
    private IndexWriter writer;

    Indexer(String indexDirectoryPath) throws IOException {
        //this directory will contain the indexes
        Directory indexDirectory =
                FSDirectory.open(Paths.get(indexDirectoryPath));

        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        //create the indexer
        writer = new IndexWriter(indexDirectory,
                config);
    }

    void close() throws IOException {
        writer.close();
    }

    private Document getDocument(File file) throws IOException {
        Document document = new Document();

        //index file contents
        Field contentField = new Field(Constants.CONTENTS, new String(Files.readAllBytes(Paths.get(file.getPath()))), TextField.TYPE_STORED);
        //index file name
        Field fileNameField = new Field(Constants.FILE_NAME, file.getName(), TextField.TYPE_STORED);
        //index file path
        Field filePathField = new Field(Constants.FILE_PATH, file.getCanonicalPath(), TextField.TYPE_STORED);

        document.add(contentField);
        document.add(fileNameField);
        document.add(filePathField);

        return document;
    }

    private Document getDocument(String doc, int index) throws IOException {
        Document document = new Document();
        //index file contents
        Field contentField = new Field(Constants.CONTENTS, doc, TextField.TYPE_STORED);
        Field fileNameField = new Field(Constants.FILE_NAME, String.valueOf(index), TextField.TYPE_STORED);
        document.add(contentField);
        document.add(fileNameField);
        return document;
    }

    private void indexFile(File file) throws IOException {
        System.out.println("Indexing " + file.getCanonicalPath());
        Document document = getDocument(file);
        writer.addDocument(document);
    }

    private void indexFile(String doc, int index) throws IOException {
        Document document = getDocument(doc, index);
        writer.addDocument(document);
    }

    int createIndex(String dataDirPath, FileFilter filter) throws IOException {
        //get all files in the data directory
        File[] files = new File(dataDirPath).listFiles();

        assert files != null;
        for (File file : files) {
            if (!file.isDirectory()
                    && !file.isHidden()
                    && file.exists()
                    && file.canRead()
                    && filter.accept(file)
            ) {
                indexFile(file);
            }
        }
        return writer.getDocStats().numDocs;
    }

    int createIndex(String dataDirPath) throws IOException {
        File file = new File(dataDirPath);
        int counter = 0;
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        while ((line = br.readLine()) != null) {
            indexFile(line, counter);
            counter++;
        }
        return writer.getDocStats().numDocs;
    }
}
