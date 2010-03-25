/**
 * Copyright 2009 T Jake Luciani
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lucehbase.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ch.ymc.lucehbase.IndexReader;
import ch.ymc.lucehbase.IndexWriter;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import org.apache.hadoop.conf.Configuration;
/**
 * Simple demo showing Lucandra in action
 */
public class BookmarksDemo {

    // Connect to casssssssssandra
    private static HTable table;
    static {
      Configuration c = new Configuration(false);
      c.addResource( new Path("conf/hbase-default.xml"));
      HBaseConfiguration config = new HBaseConfiguration(c);    
      try {
        table = new HTable(config, "lucehbase");
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    private static IndexWriter indexWriter = new IndexWriter("bookmarks", "url", table);
    private static IndexReader indexReader = new IndexReader("bookmarks", table);
    private static IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    private static Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);

    public static void usage() {
        System.out.println("Usage: BookmarkDemo [-index file.tsv] [-search query]");
        System.exit(1);
    }

    public static void loadTSVFile(File file) throws Exception {
        long t0 = System.currentTimeMillis();
        
        FileReader input = new FileReader(file);
        BufferedReader buf = new BufferedReader(input);

        String line;
        Integer number = 1;

        while ((line = buf.readLine()) != null) {
            String[] arr = line.split("\t");
            addBookmark(arr[0], arr[1], arr[2]);
            System.out.println("Indexed "+number);
            number++;
        }

        input.close();

        long t1 = System.currentTimeMillis();

        System.out.println("*Indexed file in: " + (t1 - t0) + "ms*\n");
    }

    public static void addBookmark(String url, String title, String tags) throws Exception {

        Document doc = new Document();
        doc.add(new Field("url", url, Store.YES, Index.NO));
        doc.add(new Field("title", title, Store.YES, Index.ANALYZED));
        doc.add(new Field("tags", title, Store.NO, Index.ANALYZED));

        indexWriter.addDocument(doc, analyzer);
    }

    public static void search(String query) throws IOException, org.apache.lucene.queryParser.ParseException {
        QueryParser qp = new QueryParser(Version.LUCENE_30, "title", analyzer);
        Query q = qp.parse(query);

        TopDocs docs = indexSearcher.search(q, 10);
        System.out.println("Searching for: "+query);
        System.out.println("Search matched: "+docs.totalHits+" item(s)");
        Integer number = 0;
        for( ScoreDoc score: docs.scoreDocs ){
            Document doc = indexSearcher.doc(score.doc);         
            number++;
            String title =  doc.get("title");
            String url   =  doc.get("url");
            
            System.out.println(number+". "+title+"\n\t"+url);          
        }       
    }

    public static void main(String[] args) {
        if (args.length < 2)
            usage();

        if (!args[0].equals("-index") && !args[0].equals("-search"))
            usage();

        try {
            if (args[0].equals("-index")) {

                File file = new File(args[1]);

                if (!file.exists())
                    usage();

                loadTSVFile(file);
            }

            
            if(args[0].equals("-search")){
                search(args[1]);
            }
        } catch (Throwable t) {
            System.err.println(t.getLocalizedMessage());
        }
    }

}
