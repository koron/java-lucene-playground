package net.kaoriya.lucene_playground;

import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;

public class Main {

    public static IntsRef toIntsRefs(String s) {
        int len = s.length();
        int[] ints = new int[len];
        for (int i = 0; i < len; ++i) {
            ints[i] = (int)s.charAt(i);
        }
        return new IntsRef(ints, 0, len);
    }

    public static void main(String[] args) throws Exception {
        // build a FST.
        Builder<CharsRef> b = new Builder<>(FST.INPUT_TYPE.BYTE1,
                CharSequenceOutputs.getSingleton());
        b.add(toIntsRefs("key1"), new CharsRef("value1"));
        b.add(toIntsRefs("key2"), new CharsRef("value2"));
        b.add(toIntsRefs("key3"), new CharsRef("value3"));
        FST<CharsRef> f = b.finish();

        System.out.println("f=" + f);
    }

}
