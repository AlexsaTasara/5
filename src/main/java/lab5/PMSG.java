package lab5;
import javafx.util.Pair;
public class PMSG {
    private Pair<String, Pair<Integer, Integer>> msg;
    //Получаем ключ
    public String gURL() {
        return msg.getKey();
    }
    //Получаем счетчик
    public int gCount() {
        return msg.getValue().getKey();
    }
    //Получаем время
    public int gTime() {
        return msg.getValue().getValue();
    }
    //Кладем значения
    public PMSG(Pair<String, Pair<Integer, Integer>> msg) {
        this.msg = msg;
    }
}
