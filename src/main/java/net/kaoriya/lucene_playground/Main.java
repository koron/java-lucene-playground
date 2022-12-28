package net.kaoriya.lucene_playground;

import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FSTCompiler;
import org.apache.lucene.util.fst.Util;

public class Main {

    public static IntsRef toUTF16(String s) {
        return Util.toUTF16(s, new IntsRefBuilder());
    }

    public static void fstQuery(FST<CharsRef> fst, String key) throws Exception {
        CharsRef v = Util.get(fst, toUTF16(key));
        System.out.println("query: " + key + " -> " + v);
    }

    public static void fstTrial() throws Exception {
        System.out.println("fstTrial");
        System.out.println("build a FST:");
        // build a FST.
        FSTCompiler<CharsRef> c = new FSTCompiler<>(FST.INPUT_TYPE.BYTE1,
                CharSequenceOutputs.getSingleton());
        // order to add() is too important.
        c.add(toUTF16("bar"), new CharsRef("quux"));
        c.add(toUTF16("foo"), new CharsRef("baz"));
        c.add(toUTF16("foobar"), new CharsRef("waldo"));
        System.out.println("  c=" + c);
        FST<CharsRef> f = c.compile();
        System.out.println("  f=" + f);

        System.out.println("make some queries:");
        fstQuery(f, "foo");
        fstQuery(f, "bar");
        fstQuery(f, "foobar");
        fstQuery(f, "f");
        fstQuery(f, "fo");
        fstQuery(f, "b");
        fstQuery(f, "ba");
    }

    public static void main(String[] args) throws Exception {
        fstTrial();
    }
}
