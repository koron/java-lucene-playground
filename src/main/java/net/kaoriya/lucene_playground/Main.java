package net.kaoriya.lucene_playground;

import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

public class Main {

    public static IntsRef toUTF16(String s) {
        return Util.toUTF16(s, new IntsRefBuilder());
    }

    public static void query(FST<CharsRef> fst, String key) throws Exception {
        CharsRef v = Util.get(fst, toUTF16(key));
        System.out.println("query: " + key + " -> " + v);
    }

    public static void main(String[] args) throws Exception {
        // build a FST.
        Builder<CharsRef> b = new Builder<>(FST.INPUT_TYPE.BYTE1,
                CharSequenceOutputs.getSingleton());
        // order to add() is too important.
        b.add(toUTF16("bar"), new CharsRef("quux"));
        b.add(toUTF16("foo"), new CharsRef("baz"));
        b.add(toUTF16("foobar"), new CharsRef("waldo"));
        FST<CharsRef> f = b.finish();
        System.out.println("f=" + f);

        query(f, "foo");
        query(f, "bar");
        query(f, "foobar");
        query(f, "f");
        query(f, "fo");
        query(f, "b");
        query(f, "ba");
    }

}
