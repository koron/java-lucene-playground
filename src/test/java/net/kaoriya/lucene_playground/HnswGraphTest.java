package net.kaoriya.lucene_playground;

// See also: https://stackoverflow.com/questions/70477808/howto-run-nearest-neighbour-search-with-lucene-hnswgraph

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.lucene94.Lucene94Codec;
import org.apache.lucene.codecs.lucene94.Lucene94HnswVectorsFormat;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.index.VectorValues;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.hnsw.HnswGraph;
import org.apache.lucene.util.hnsw.HnswGraphBuilder;
import org.apache.lucene.util.hnsw.HnswGraphSearcher;
import org.apache.lucene.util.hnsw.NeighborQueue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

// https://github.com/apache/lucene/blob/fc67d6aa6e2bf2ec8ff4b2b8e4a763f3f706de29/lucene/core/src/test/org/apache/lucene/util/hnsw/KnnGraphTester.java

public class HnswGraphTest {

    public static final int dim = 2;
    public static final float[] query = new float[] { 0.98f, 0.01f };

    static final int VISITED_LIMIT = 100;

    // The goal vector will be inserted into the graph which is very close to the actual query vector.
    public static final Vector2D goalVector = new Vector2D(query[0] - 0.01f, query[1] + 0.01f);
    public static final Path indexPath = Paths.get("target/index");
    public static final VectorSimilarityFunction similarityFunction = VectorSimilarityFunction.EUCLIDEAN;
    public static final int maxConn = 14;
    public static final int beamWidth = 5;
    public static final long seed = HnswGraphBuilder.randSeed;

    private VectorProvider vectors;

    @Before
    public void setupIndexDir() throws IOException {
        File file = indexPath.toFile();
        if (file.exists()) {
            FileUtils.deleteDirectory(file);
        }

        // Prepare the test data (10 entries)
        List<Vector2D> vectorData = createVectorData(10);

        // Randomize vector order before insertion
        Collections.shuffle(vectorData);

        // Print the test dataset
        System.out.println("Test vectors:");
        for (int i = 0; i < vectorData.size(); i++) {
            vectorData.get(i).print(i);
        }


        // Create the provider which will feed the vectors for the graph
        vectors = new VectorProvider(vectorData);

    }

    @Test
    public void testWriteAndQueryIndex() throws IOException {
        // Persist and read the data
        try (MMapDirectory dir = new MMapDirectory(indexPath)) {

            // Write index
            int indexedDoc = writeIndex(dir, vectors);

            // Read index
            readAndQuery(dir, vectors, indexedDoc);
        }
    }

    @Test
    //@Ignore("Does not return the expected values. I assume there are extra steps needed to create a correlation between graph nodeId and id of the vector.")
    public void testSearchViaHnswGraph() throws IOException {
        // Build the graph manually and run the query
        HnswGraphBuilder builder = HnswGraphBuilder.create(vectors, VectorEncoding.FLOAT32, similarityFunction, maxConn, beamWidth, seed);
        HnswGraph hnsw = builder.build(vectors.randomAccess());

        // Run a search
        NeighborQueue nn = HnswGraphSearcher.search(
                query,
                10, // search result size
                vectors.randomAccess(), // ? Why do I need to specify the graph values again?
                VectorEncoding.FLOAT32,
                similarityFunction, // ? Why can I specify a different similarityFunction for search. Should that not be the same that was used for graph creation?
                hnsw,
                null,
                VISITED_LIMIT);
                //new SplittableRandom(RandomUtils.nextLong())); // Random seed to entry vector of the search

        // Print the results
        System.out.println();
        System.out.println(String.format("Searching for NN of [%.2f | %.2f]", query[0], query[1]));
        System.out.println("Top: " + nn.topNode() + " - score: " + nn.topScore() + " Visited: " + nn.visitedCount());
        Vector2D topVec = vectors.get(nn.topNode());
        topVec.print(nn.topNode());
        System.out.println("---------");
        for (int i = 0; i < nn.size(); i++) {
            int id = nn.pop();
            Vector2D vec = vectors.get(id);
            vec.print(id);
        }
    }

    private void readAndQuery(MMapDirectory dir, VectorProvider vectorData, int indexedDoc) throws IOException {
        try (IndexReader reader = DirectoryReader.open(dir)) {
            for (LeafReaderContext ctx : reader.leaves()) {
                VectorValues values = ctx.reader().getVectorValues("field");
                assertEquals(dim, values.dimension());
                assertEquals(indexedDoc, values.size());
                assertEquals(vectorData.size(), ctx.reader().maxDoc());
                assertEquals(vectorData.size(), ctx.reader().numDocs());
                // KnnGraphValues graphValues = ((Lucene94HnswVectorsReader) ((PerFieldKnnVectorsFormat.FieldsReader) ((CodecReader) ctx.reader())
                // .getVectorReader())
                // .getFieldReader("field"))
                // .getGraphValues("field");

                TopDocs results = doKnnSearch(ctx.reader(), "field", query, 2, indexedDoc);
                System.out.println();
                System.out.println("Doc Based Search:");
                System.out.println(String.format("Searching for NN of [%.2f | %.2f]", query[0], query[1]));
                System.out.println("TotalHits: " + results.totalHits.value);
                for (int i = 0; i < results.scoreDocs.length; i++) {
                    ScoreDoc doc = results.scoreDocs[i];
                    // System.out.println("Matches: " + doc.doc + " = " + doc.score);
                    Vector2D vec = vectorData.get(doc.doc);
                    vec.print(doc.doc);
                }
            }
        }

    }

    private int writeIndex(MMapDirectory dir, VectorProvider vectorProvider) throws IOException {
        int indexedDoc = 0;
        IndexWriterConfig iwc = new IndexWriterConfig()
            .setCodec(
                    new Lucene94Codec() {
                        @Override
                        public KnnVectorsFormat getKnnVectorsFormatForField(String field) {
                            return new Lucene94HnswVectorsFormat(maxConn, beamWidth);
                        }
                    });
        try (IndexWriter iw = new IndexWriter(dir, iwc)) {
            while (vectorProvider.nextDoc() != NO_MORE_DOCS) {
                while (indexedDoc < vectorProvider.docID()) {
                    // increment docId in the index by adding empty documents
                    iw.addDocument(new Document());
                    indexedDoc++;
                }
                Document doc = new Document();
                // System.out.println("Got: " + v2.vectorValue()[0] + ":" + v2.vectorValue()[1] + "@" + v2.docID());
                doc.add(new KnnVectorField("field", vectorProvider.vectorValue(), similarityFunction));
                doc.add(new StoredField("id", vectorProvider.docID()));
                iw.addDocument(doc);
                indexedDoc++;
            }
        }
        return indexedDoc;
    }

    private TopDocs doKnnSearch(
            IndexReader reader, String field, float[] vector, int docLimit, int fanout) throws IOException
    {
        TopDocs[] results = new TopDocs[reader.leaves().size()];
        for (LeafReaderContext ctx : reader.leaves()) {
            Bits liveDocs = ctx.reader().getLiveDocs();
            results[ctx.ord] = ctx.reader().searchNearestVectors(field, vector, docLimit + fanout, liveDocs, VISITED_LIMIT);
            int docBase = ctx.docBase;
            for (ScoreDoc scoreDoc : results[ctx.ord].scoreDocs) {
                scoreDoc.doc += docBase;
            }
        }
        return TopDocs.merge(docLimit, results);
    }

    private List<Vector2D> createVectorData(int len) {
        // Just using a list for now to make it easier to matchup with document ids later on
        List<Vector2D> list = new ArrayList<>();

        // Add a custom vector which is very close to our target
        list.add(goalVector);

        for (int i = 0; i < len; i++) {
            /*
             * double piRadians = i / (double) len; float a = (float) Math.cos(Math.PI * piRadians); float b = (float) Math.sin(Math.PI * piRadians);
             */
            float a = (float) Math.random();
            float b = (float) Math.random();
            Vector2D vec = new Vector2D(a, b);
            list.add(vec);
        }
        return list;
    }

}
