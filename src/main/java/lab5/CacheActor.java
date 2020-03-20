package lab5;
import java.util.Map;
import java.util.HashMap;
import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

public class CacheActor extends AbstractActor {
    private HashMap<String, Map<Integer, Integer>> data = new HashMap<>();
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GMSG.class,
                        msg -> {
                            String url = msg.gURL();
                            int count = msg.gCount();
                            if (data.containsKey(url) && data.get(url).containsKey(count)) {
                                getSender().tell(data.get(url).get(count), ActorRef.noSender());
                            }
                            else {
                                getSender().tell(-1, ActorRef.noSender());
                            }
                        })
                .match(
                        PMSG.class,
                        msg -> {
                            Map<Integer, Integer> temp;
                            if (data.containsKey(msg.gURL())) {
                                temp = data.get(msg.gURL());
                            } else {
                                temp = new HashMap<>();
                            }
                            temp.put(msg.gCount(), msg.gTime());
                            data.put(msg.gURL(), temp);
                        }
                ).build();
    }
}
