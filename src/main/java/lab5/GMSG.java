package lab5;
import javafx.util.Pair;
public class GMSG {
    Pair<String, Integer> msgPair;
    //Возвращаем ключ
    public String gURL() {
        return msgPair.getKey();
    }
    //Возвращаем значение
    public int gCount() {
        return msgPair.getValue();
    }
    //Возвращаем пару
    public Pair<String, Integer> gMsgPair() {
        return msgPair;
    }
    //Задаем пару
    public GMSG(Pair<String, Integer> pair) {
        this.msgPair = pair;
    }
}
