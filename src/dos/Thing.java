package dos;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Arthur Kupriyanov
 */
public class Thing implements Serializable {
    public static Set getDOSThing(){
        Set root = new HashSet();
        Set s1 = root;
        Set s2 = new HashSet();
        for (int i = 0; i < 100; i++) {
            Set t1 = new HashSet();
            Set t2 = new HashSet();
            t1.add("foo"); // make it not equal to t2
            s1.add(t1);
            s1.add(t2);
            s2.add(t1);
            s2.add(t2);
            s1 = t1;
            s2 = t2;
        }

        return root;
    }

    public static Object getSimpleObject(){
        class Mayushii implements Serializable{
            @Override
            public String toString() {
                return "Tu-tu-ruu";
            }
        }
        return new Mayushii();
    }
}
