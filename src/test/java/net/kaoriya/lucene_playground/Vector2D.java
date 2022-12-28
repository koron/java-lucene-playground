package net.kaoriya.lucene_playground;

// https://github.com/apache/lucene/blob/fc67d6aa6e2bf2ec8ff4b2b8e4a763f3f706de29/lucene/core/src/test/org/apache/lucene/util/hnsw/KnnGraphTester.java

class Vector2D {

    float a;
    float b;

    public Vector2D(float a, float b) {
        this.a = a;
        this.b = b;
    }

    public float[] toArray() {
        return new float[] { a, b };
    }

    public void print(int ord) {
        System.out.println(ord + " => [" + String.format("%.02f", a) + "|" + String.format("%.02f", b) + "]");
    }
}
