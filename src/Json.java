/* Written collaboratively by Mahdi Chowdhury and OpenAI Codex.
Mahdi: product idea, requirements, and direction. Codex: generated implementation and explanations. */
import java.math.BigDecimal;
import java.util.*;

/** Small bounded JSON reader/writer for the API adapter; no external dependencies. */
public final class Json {
    private final String text;
    private int pos;
    private Json(String text) { this.text = text; }
    public static Object parse(String text) {
        if (text == null || text.length() > 1_000_000) throw new IllegalArgumentException("JSON is too large.");
        Json parser = new Json(text);
        Object value = parser.value(0);
        parser.space();
        if (parser.pos != text.length()) throw new IllegalArgumentException("Invalid JSON.");
        return value;
    }
    private void space() { while(pos < text.length() && " \n\r\t".indexOf(text.charAt(pos)) >= 0) pos++; }
    private char take() { if(pos >= text.length()) throw new IllegalArgumentException("Incomplete JSON."); return text.charAt(pos++); }
    private boolean accept(char c) { space(); if(pos < text.length() && text.charAt(pos)==c){pos++;return true;}return false; }
    private void expect(char c) { if(!accept(c))throw new IllegalArgumentException("Invalid JSON."); }
    private Object value(int depth) {
        if(depth > 32) throw new IllegalArgumentException("JSON nesting is too deep.");
        space(); if(pos >= text.length()) throw new IllegalArgumentException("Incomplete JSON.");
        char c=text.charAt(pos);
        if(c=='"')return string();
        if(c=='{') {
            pos++; Map<String,Object> map=new LinkedHashMap<>(); if(accept('}'))return map;
            do { space(); if(pos>=text.length()||text.charAt(pos)!='"')throw new IllegalArgumentException("Invalid JSON key.");
                String key=string();expect(':');if(map.containsKey(key))throw new IllegalArgumentException("Duplicate JSON key.");map.put(key,value(depth+1));
            }while(accept(','));expect('}');return map;
        }
        if(c=='[') { pos++;List<Object> list=new ArrayList<>();if(accept(']'))return list;do{list.add(value(depth+1));}while(accept(','));expect(']');return list; }
        for(String word:List.of("true","false","null"))if(text.startsWith(word,pos)){pos+=word.length();return word.equals("null")?null:Boolean.valueOf(word);}
        int start=pos;while(pos<text.length() && "-+0123456789.eE".indexOf(text.charAt(pos))>=0)pos++;
        String number=text.substring(start,pos);
        if(!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))throw new IllegalArgumentException("Invalid JSON value.");
        return new BigDecimal(number);
    }
    private String string() {
        if(take()!='"')throw new IllegalArgumentException("Expected a JSON string.");
        StringBuilder out=new StringBuilder();
        while(true){char c=take();if(c=='"')return out.toString();if(c<32)throw new IllegalArgumentException("Invalid JSON character.");
            if(c=='\\'){char escaped=take();switch(escaped){
                case '"','\\','/' -> out.append(escaped);case 'b' -> out.append('\b');case 'f' -> out.append('\f');case 'n' -> out.append('\n');case 'r' -> out.append('\r');case 't' -> out.append('\t');
                case 'u' -> {int code=0;for(int i=0;i<4;i++){int digit=Character.digit(take(),16);if(digit<0)throw new IllegalArgumentException("Invalid Unicode escape.");code=code*16+digit;}out.append((char)code);}
                default -> throw new IllegalArgumentException("Invalid JSON escape.");
            }}else out.append(c);
        }
    }
    public static String stringify(Object value) {
        if(value==null)return "null";
        if(value instanceof String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){if(c=='"'||c=='\\')b.append('\\').append(c);else if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}return b.append('"').toString();}
        if(value instanceof Boolean || value instanceof BigDecimal || value instanceof Integer)return value.toString();
        if(value instanceof Map<?,?> map){StringJoiner j=new StringJoiner(",","{","}");map.forEach((k,v)->j.add(stringify(k.toString())+":"+stringify(v)));return j.toString();}
        if(value instanceof List<?> list){StringJoiner j=new StringJoiner(",","[","]");list.forEach(v->j.add(stringify(v)));return j.toString();}
        throw new IllegalArgumentException("Unsupported JSON type.");
    }
}
