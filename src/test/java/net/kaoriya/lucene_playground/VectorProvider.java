package net.kaoriya.lucene_playground;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.RandomAccessVectorValues;
import org.apache.lucene.index.VectorValues;
import org.apache.lucene.util.BytesRef;

// https://github.com/apache/lucene/blob/fc67d6aa6e2bf2ec8ff4b2b8e4a763f3f706de29/lucene/core/src/test/org/apache/lucene/util/hnsw/KnnGraphTester.java

class VectorProvider extends VectorValues implements RandomAccessVectorValues {

    int doc = -1;
    private final List<Vector2D> data;

    public VectorProvider(List<Vector2D> data) {
        this.data = data;
    }

    public Vector2D get(int idx) {
        return data.get(idx);
    }

    public RandomAccessVectorValues randomAccess() {
        return new VectorProvider(data);
    }

    @Override
    public float[] vectorValue(int ord) throws IOException {
        Vector2D entry = data.get(ord);
        return entry.toArray();
    }

    @Override
    public BytesRef binaryValue(int targetOrd) throws IOException {
        return null;
    }

    @Override
    public int dimension() {
        return 2;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public float[] vectorValue() throws IOException {
        return vectorValue(doc);
    }

    @Override
    public int docID() {
        return doc;
    }

    @Override
    public int nextDoc() throws IOException {
        return advance(doc + 1);
    }

    @Override
    public int advance(int target) throws IOException {
        if (target >= 0 && target < data.size()) {
            doc = target;
        } else {
            doc = NO_MORE_DOCS;
        }
        return doc;
    }

    @Override
    public long cost() {
        return data.size();
    }

    public VectorProvider copy() {
        return new VectorProvider(data);
    }

}
