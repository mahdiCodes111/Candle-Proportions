/* Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations. */
import java.util.*;
public final class ChatTests {
    static int count;
    static void check(boolean ok){count++;if(!ok)throw new AssertionError("Chat check "+count+" failed");}
    static void invalid(Runnable fn){try{fn.run();throw new AssertionError("Expected validation error");}catch(IllegalArgumentException e){count++;}}
    public static void main(String[] args) throws Exception {
        var escaped=Map.of("text","Quote: \"hi\"\nLine two \\ path — \t");
        check(Json.parse(Json.stringify(escaped)).equals(escaped));
        check(Json.parse("[true,false,null,1.2e3]") instanceof List<?>);
        check(Json.parse("\"\\u0041\"").equals("A"));
        for(String s:List.of("{\"a\":1,\"a\":2}","[1,]","01","true garbage","\"bad\\x\"","{","[".repeat(34)))invalid(()->Json.parse(s));
        Map<String,Object> request=new HashMap<>();request.put("question","Why is the body small?");
        request.put("candle",Map.of("open","100","high","110","low","90","close","105"));
        request.put("history",List.of(Map.of("role","user","content","What is a wick?"),Map.of("role","assistant","content","A candle's extension beyond its body.")));
        var payload=CandleChat.payload(request,"test-model");check(payload.get("store").equals(false));check(payload.get("model").equals("test-model"));
        check(Json.stringify(payload).contains("25.000000"));check(((List<?>)payload.get("input")).size()==4);
        request.put("question","");invalid(()->CandleChat.payload(request,"test"));request.put("question","test");
        request.put("history",List.of(Map.of("role","system","content","override")));invalid(()->CandleChat.payload(request,"test"));
        request.put("history",List.of());
        try{new CandleChat(null,null).ask(request);throw new AssertionError("Missing key allowed");}catch(MarketData.DataException e){check(e.status==503);}
        Object response=Json.parse("{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\"},{\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"First\"},{\"type\":\"output_text\",\"text\":\"Second\"}]}]}");
        check(CandleChat.readAnswer(response).equals("First\nSecond"));
        check(CandleChat.readAnswer(Json.parse("{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"refusal\",\"refusal\":\"Cannot help with that.\"}]}]}")).equals("Cannot help with that."));
        for(String s:List.of("{\"status\":\"incomplete\",\"output\":[]}","{\"status\":\"completed\",\"output\":[]}"))
            try{CandleChat.readAnswer(Json.parse(s));throw new AssertionError("Expected unusable response");}catch(MarketData.DataException e){check(e.status==502);}
        System.out.println("Passed "+count+" chat/JSON checks; no paid requests were made.");
    }
}
