# Serialization bomb example

Полная статья: https://vk.com/@apploidxxx-warning-serialization

## Warning! Serialization!
О том, почему не стоит практиковать и использовать встроенную сериализацию Java…

## Сериализация
Сериализация (serialization) объектов в Java позволяет преобразовать любой объект, реализующий интерфейс Serializable, в последовательность байтов, по которой затем можно полностью восстановить исходный объект.

К примеру, мы можем создать объект на ОС Windows, сериализовать и отправить на машину с ОС UNIX, где объект корректно будет воссоздан.

## Применения
Сериализация имеет широкий спектр применения.

И если выбирать основное, то это скорее всего:

* RMI (удаленный вызов методов)
* Hibernate
* JavaBean

## Так чем же он плох?
Я ничего не имею против сериализации объектов Java касательно приведенных выше примеров. Это действительно прекрасный механизм, который только дополняет Java.

Но при всем этом, меня очень сильно беспокоит его обманчивая простота и кажущаяся безопасность. Лично я считаю, что использование сериализованных объектов, например, в сети — это очень грубая ошибка.

Начнем пожалуй от очевидных до потенциально опасных угроз.

ККроссплатформенность
У механизма сериализации объектов есть важное ограничение: он работает только с объектами Java, то есть десериализовать такие объекты можно только в программах Java.

В контексте клиент-серверной программы — это очень важный минус.

Для этого в книге Брюса Эккелея «Thinking in Java» предлагается использовать XML, как вариант. А Джошуа Блох (Effective Java) советует использовать JSON или Protobuf.

## Совместимость
В свою очередь, когда мне захотелось использовать сериализацию объектов Java для lightweight persistence (легковесное долговременное хранение) я столкнулся с неприятной проблемой. Я использовал класс для хранения объектов, который сериализовывал и сохранял в качестве объекта в локальной памяти, а затем в нужный момент восстанавливал. Но при этом, при изменении самого класса, мне приходилось все сериализовывать повторно, так как их версия уже отличалась от текущей.

Основное неудобство реализации интерфейса Serializable заключается в том, что это уменьшает возможности изменения реализации класса в последующих версиях.

Поэтому в какой-то момент (еще до знакомства с JPA, ORM) я перешел на сериализацию объектов через gson. Это существенно облегчило работу с совместимостью. Хотя эту проблему можно было бы решить с помощью пользовательских сериализованных форм.

## Угроза безопасности
Хочу начать эту часть со случая атаки на агентство железнодорожных перевозок Сан-Франциско (SFMTA Muni), которая отключила всю систему тарифных сборов на два дня в ноябре 2016 года.

Вот отрывок из этой новости:

> …an SFMTA Web-facing server was likely compromised by what is referred to as a "deserialization" attack after it was identified by a vulnerability scan.

Полная статья: https://arstechnica.com/information-technology/2016/11/san-francisco-transit-ransomware-attacker-likely-used-year-old-java-exploit/

[![](https://cdn.arstechnica.net/wp-content/uploads/2016/11/MUNI-N-San-Francisco-1-800x600.jpg)](https://arstechnica.com/information-technology/2016/11/san-francisco-transit-ransomware-attacker-likely-used-year-old-java-exploit/)

Лучше всего проблему объяснит сам Joshua Bloch:

> Фундаментальная проблема сериализации состоит в том, что область, в которой она может быть атакована (в смысле проблем безопасности) слишком велика для защиты и постоянно растет: графы объектов десериализуются с помощью вызовов метода readObject над ObjectInputStream. Этот метод, по существу, представляет собой волшебный конструктор, который может инстанцировать объекты практически любого типа на пути класса, при условии, что тип реализует интерфейс Serializable. В процессе десериализации потока байтов этот метод может выполнять код любого из этих типов, поэтому код всех этих типов может быть атакован.

По словам Роберта Сикорда (Robert Seacord), технического руководителя координационного центра CERT (ссылка на его работу будет ниже):

> Десериализация Java — прямая и явная угроза, поскольку она широко используется как непосредственно в приложениях, так и опосредованно подсистемами Java, такими как RMI (удаленный вызов методов), JMX (расширения управления Java) и JMS (системы обмена сообщениями Java). Десериализация ненадежных потоков может привести к удаленному выполнению кода (RCE), отказу в обслуживании (DoS) и целому ряду других нападений. Приложения могут быть уязвимыми для этих атак, даже если они не делают ничего
Поверхность атаки включает классы библиотек платформы Java, сторонних библиотек, таких как Apache Commons Collections, и самого приложения. Даже если вы придерживаетесь всех наилучших практик и преуспеваете в написании неуязвимых для атак сериализуемых классов, ваше приложение все равно может быть уязвимым.


## Гаджеты (gadgets)
Методы, вызываемые в процессе десериализации, которые могут выполнять потенциально опасные действия называют гаджетами (gadgets).

Несколько гаджетов могут использоваться совместно, формируя цепочку гаджетов (gadget chain). Время от времени обнаруживается цепочка гаджетов, достаточно мощная, чтобы позволить злоумышленнику выполнить произвольный машинный код, обладая только лишь возможностью представить тщательно сформированный байтовый поток для десериализации. Именно это произошло при нападении на SFMTA Muni.

## Простой пример
Даже без гаджетов мы можем создать так называемую бомбу десериализации.

Листинг исходников: https://github.com/AppLoidx/serialization-dos-example

Создадим класс Thing, который генерирует объекты DOSThing и простой Object:
```java
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
```
С методом `getSimpleObject`все понятно, он возвращает обычный сериализуемый объект с переопределенным методом toString().

`getDOSThing` — тоже более менее понятен. Граф этого объекта состоит из 201 экземпляра HashSet, каждый из которых содержит 3 или меньше ссылок на объекты.

Создадим класс Server, который будет имитировать сервер:
```java
public class Server {
    public static void main(String[] args) {
        System.out.println("Start test");
        try {
    
            Object mySimpleObject = deserialize(serialize( Thing.getSimpleObject() ));
            System.out.println("Yeah, we did it! " + mySimpleObject);
            
            Object myObject = deserialize(serialize( Thing.getDOSThing() ));
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
```

Здесь мы просто сериализуем и обратно десериализуем два объекта и выводим результат работы.

В первом случае, простой объект, который быстро и успешно десериализуется выведет:
```
Yeah, we did it! Tu-tu-ruu
```
А со вторым возникнут проблемы. Проблема заключается в том, что десериализация экземпляра HashSet требует вычисления хеш-кодов его элементов. Два элемента корневого множества являются хеш-множествами, каждое из которых содержит два хеш-множества элементов, каждый из которых содержит два хеш-множества элементов... и так далее до ста уровней в глубину. Таким образом, десериализация множества вызывает метод hashCode более 2^(100) раз. Помимо того, что десериализация длится вечно, десериализатор не видит никаких признаков того, что что-то неладно. Создается несколько объектов, а глубина стека ограничена.

Для большей убедительности приведу цитату из официального руководства по безопасному кодированию Java:

> Deserialization of untrusted data is inherently dangerous and should be avoided

Ссылка на полную статью: [Secure Code Guidelines for Java SE](https://www.oracle.com/technetwork/java/seccodeguide-139067.html#8)

**Так, краткий туториал будет таким:**

> Лучший способ избежать проблем, связанных с сериализацией, — никогда ничего не десериализовать.
И если уж все таки пришлось:

> Никогда не десериализовывайте непроверенные данные.

**Из Effective Java:**

> Сериализация опасна, и ее следует избегать. Если вы проектируете систему “с нуля”, используйте кроссплатформенное представление структурированных данных, например JSON или protobuf. Не десериализуйте ненадежные данные. Если же вы вынуждены это делать, то используйте фильтрацию десериализации (JEP 290 [Java 9])(но учтите, что она не гарантирует предотвращение всех атак). Избегайте написания сериализуемых классов. Если же вы вынуждены это делать, будьте осторожны!

Таким образом, использование сериализации является очень спорным и как видно, довольно небезопасным. За простотой лишь добавления implements Serializable мы можем подвергнуть себя большому риску. Разумеется, эти риски можно снизить (например, используя паттерн serialization proxy ), но стоит ли это потраченных на него средств?

## Использованная литература и полезные ссылки
«Thinking in Java» Bruce Eckel

«Effective Java» Joshua Bloch

JEP 290 : http://openjdk.java.net/jeps/290

OWASP, Deserialization of untrusted data

Oracle, Secure Coding Guidelines for Java SE

NCC Group Whitepaper, Combating Java Deserialization Vulnerabilities (LAOIS)

Oracle, Serialization Filtering

[billion-laughs-style DoS for java serialization](https://gist.github.com/coekie/a27cc406fc9f3dc7a70d)
