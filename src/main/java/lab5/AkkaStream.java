package lab5;
import akka.NotUsed;
import akka.japi.Pair;
import akka.actor.Props;
import akka.actor.ActorRef;
import akka.util.ByteString;
import akka.pattern.Patterns;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.http.javadsl.model.*;
import akka.stream.javadsl.Source;
import akka.http.javadsl.ConnectHttp;
import akka.stream.ActorMaterializer;
import akka.http.javadsl.ServerBinding;
import org.slf4j.Logger;
import java.time.Duration;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import static org.asynchttpclient.Dsl.asyncHttpClient;

public class AkkaStream {
    private static ActorRef controlActor;
    private static final String HOME_DIR = "/", COUNT = "count", EMPTY_STRING = "", ROUTES = "routes";
    private static final String TEST_URL = "testUrl", WELCOME_MSG = "Start!", PATH_ERROR = "BAD PATH";
    private static final String LOCALHOST = "localhost", GET_ERROR = "ONLY GET METHOD!";
    private static final String NUMBER_ERROR = "NUMBER EXCEPTION", URL_ERROR = "URL PARAMETER IS EMPTY";
    private static final String COUNT_ERROR = "COUNT PARAMETER IS EMPTY", FINAL_ANSWER = "Medium response is in MS ->";
    private static final String SERVER_WELCOME_MSG = "Server online at http://localhost:8080/\nPress RETURN to stop...";
    private static final int ZERO = 0, PARALLELISM = 1, NO_SUCH_DATA = -1, LOCALHOST_PORT = 8080;
    private static final long TIME_MILLIS = 5000;
    private static final Logger logger = LoggerFactory.getLogger(AkkaStream.class);

    public static void main(String[] args) throws IOException {
        System.out.println(WELCOME_MSG);
        ActorSystem system = ActorSystem.create(ROUTES);
        controlActor = system.actorOf(Props.create(CacheActor.class));
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = Flow.of(HttpRequest.class).map(
            req -> {
                if (req.method() == HttpMethods.GET) {
                    if (req.getUri().path().equals(HOME_DIR)) {
                        String url = req.getUri().query().get(TEST_URL).orElse(EMPTY_STRING);
                        String count = req.getUri().query().get(COUNT).orElse(EMPTY_STRING);
                        if (url.isEmpty()) {
                            return HttpResponse.create().withEntity(ByteString.fromString(URL_ERROR));
                        }
                        if (url.isEmpty()) {
                            return HttpResponse.create().withEntity(ByteString.fromString(COUNT_ERROR));
                        }
                        try {
                            Integer countInteger = Integer.parseInt(count);
                            Pair<String, Integer> data = new Pair<>(url, countInteger);
                            Source<Pair<String, Integer>, NotUsed> source = Source.from(Collections.singletonList(data));
                            Flow<Pair<String, Integer>, HttpResponse, NotUsed> testSink = Flow.<Pair<String, Integer>>create()
                                .map(pair -> new Pair<>(HttpRequest.create().withUri(pair.first()), pair.second()))
                                .mapAsync(1, pair -> {
                                    return Patterns.ask(controlActor,
                                            new GMSG(new javafx.util.Pair<>(data.first(), data.second())),
                                            Duration.ofMillis(TIME_MILLIS)
                                        ).thenCompose(r -> {
                                            if ((int) r != NO_SUCH_DATA) {
                                                return CompletableFuture.completedFuture((int) r);
                                            }
                                            // fold for counting all time
                                            Sink<CompletionStage<Long>, CompletionStage<Integer>> fold = Sink
                                                .fold(ZERO, (ac, el) -> {
                                                    int testEl = (int) (ZERO + el.toCompletableFuture().get());
                                                    return ac + testEl;
                                                });
                                            return Source.from(Collections.singletonList(pair))
                                                .toMat(
                                                    Flow.<Pair<HttpRequest, Integer>>create()
                                                    .mapConcat(p -> Collections.nCopies(p.second(), p.first()))
                                                    .mapAsync(PARALLELISM, req2 -> {
                                                        return CompletableFuture.supplyAsync(() ->
                                                            System.currentTimeMillis()
                                                        ).thenCompose(start -> CompletableFuture.supplyAsync(() -> {
                                                            CompletionStage<Long> whenResponse = asyncHttpClient()
                                                            .prepareGet(req2.getUri().toString()).execute()
                                                            .toCompletableFuture().thenCompose(answer ->
                                                                CompletableFuture.completedFuture(System.currentTimeMillis() - start));
                                                            return whenResponse;
                                                        }));
                                                    }).toMat(fold, Keep.right()), Keep.right()).run(materializer);
                                            }
                                        ).thenCompose(sum -> {
                                            Patterns.ask(controlActor, new PMSG(new javafx.util.Pair<>(data.first(), new javafx.util.Pair<>(data.second(), sum))), 5000);
                                            Double middleValue = (double) sum / (double) countInteger;
                                            return CompletableFuture.completedFuture(HttpResponse.create().withEntity(ByteString.fromString(FINAL_ANSWER + middleValue.toString())));
                                        });
                                }
                                );
                            CompletionStage<HttpResponse> result = source.via(testSink).toMat(Sink.last(), Keep.right()).run(materializer);
                            return result.toCompletableFuture().get();
                        }
                        catch(NumberFormatException e){
                            e.printStackTrace();
                            return HttpResponse.create().withEntity(ByteString.fromString(NUMBER_ERROR));
                        }
                    }
                    else {
                        req.discardEntityBytes(materializer);
                        return HttpResponse.create().withStatus(StatusCodes.NOT_FOUND).withEntity(PATH_ERROR);
                    }
                }
                else{
                    req.discardEntityBytes(materializer);
                    return HttpResponse.create().withStatus(StatusCodes.NOT_FOUND).withEntity(GET_ERROR);
                }
            });

        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost(LOCALHOST, LOCALHOST_PORT), materializer);
        //Выводим приветствие
        System.out.println(SERVER_WELCOME_MSG);
        System.in.read();
        binding.thenCompose(ServerBinding::unbind).thenAccept(unbound ->system.terminate());
    }
}