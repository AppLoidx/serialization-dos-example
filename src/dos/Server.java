package dos;

import java.io.*;

/**
 * @author Arthur Kupriyanov
 */
public class Server {

    public static void main(String[] args) {
        System.out.println("Start test");
        try {
            Object mySimpleObject = deserialize(serialize(Thing.getSimpleObject()));

            System.out.println("Yeah, we did it! " + mySimpleObject);

            Object myObject = deserialize(serialize(Thing.getDOSThing()));

            System.out.println("Yeah, we did it! " + myObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object deserialize(byte[] bytes) throws Exception {
        return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
    }
    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(ba);
        oos.writeObject(o);
        oos.close();
        return ba.toByteArray();
    }

}
