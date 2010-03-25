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
package ch.ymc.lucehbase;

import java.io.IOException;
import java.util.List;
import java.util.NavigableMap;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;

public class LucandraTermDocs implements TermDocs, TermPositions {

    private IndexReader indexReader;
    private LucandraTermEnum termEnum;
    private List<byte[][]> termDocs;
    private int docPosition;
    private int[] termPositionArray;
    private int termPosition;
    private static final Logger logger = Logger.getLogger(LucandraTermDocs.class);

    public LucandraTermDocs(IndexReader indexReader) {
        this.indexReader = indexReader;
        termEnum = new LucandraTermEnum(indexReader);
    }

    public void close() throws IOException {
        // TODO Auto-generated method stub
    }

    public int doc() {
        if ( docPosition == -1 )
        {
            docPosition = 0;
        }

        int docid = indexReader.addDocument( termDocs.get(docPosition)[0] );
        return docid;
    }

    public int freq() {
        termPositionArray = HBaseUtils.byteArrayToIntArray( termDocs.get(docPosition)[1] );
        termPosition = 0;

        return termPositionArray.length;
    }

    public boolean next() throws IOException {
        if (termDocs == null)
        {
          return false;
        }

        return ++docPosition < termDocs.size();
    }

    public int read(int[] docs, int[] freqs) throws IOException {
        int i=0;
        if( termDocs == null )
        {
          return 0;
        }
        for (; i < termDocs.size() && next() ; ++i) // (termDocs != null && docPosition < termDocs.size() && i < docs.length); i++, docPosition++) {
        {
          docs[i] = doc();
          freqs[i] = freq();
        }
      
        return i;
    }

    public void seek(Term term) throws IOException {
        // on a new term so check cached
        LucandraTermEnum tmp = indexReader.checkTermCache(term);
        if (tmp == null) {

            if (termEnum.skipTo(term)) {
                if (termEnum.term().compareTo(term) == 0) {
                    termDocs = termEnum.getTermDocFreq();
                } else {
                    termDocs = null;
                }
            }
        } else {
            termEnum = tmp;
            if(termEnum.skipTo(term)){

                if (termEnum.term().equals(term)) {
                    termDocs = termEnum.getTermDocFreq();
                } else {
                    termDocs = null;
                }
            }else{
                termDocs = null;
            }
        }

        docPosition = -1;
    }

    public void seek(TermEnum termEnum) throws IOException {
        if (termEnum instanceof LucandraTermEnum) {
            this.termEnum = (LucandraTermEnum) termEnum;
            termDocs = this.termEnum.getTermDocFreq();
            docPosition = -1;
        } else {
            throw new RuntimeException("TermEnum is not compatable with Lucandra");
        }
    }

    public boolean skipTo(int target) throws IOException {
        do {
            if (!next())
                return false;
        } while (target > doc());

        return true;
    }

    public byte[] getPayload(byte[] data, int offset) throws IOException {
        return null;
    }

    public int getPayloadLength() {
        return 0;
    }

    public boolean isPayloadAvailable() {
        return false;
    }

    public int nextPosition() throws IOException {
        int pos = termPositionArray[termPosition];
        termPosition++;

        return pos;
    }

}
