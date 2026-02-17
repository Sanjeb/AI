import java.net.URI;
import java.net.http.*;
import java.io.*;
import java.time.Duration;
import java.util.regex.*;
import java.time.LocalDateTime;

public class AI {

    static final String API_KEY = "AIzaSyBW-IJfx_NcqWr-810mP55cYTq9pvZwrAU";

    static final String URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

    static final String LOG_FILE = "chat_log.txt";

    public static void main(String[] args) throws Exception {

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        System.out.println("AI Ready (type exit to quit)\n");

        while (true) {

            System.out.print("> ");
            String prompt = input.readLine();

            if (prompt == null || prompt.equalsIgnoreCase("exit"))
                break;

            String json = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "%s"
                    }
                  ]
                }
              ]
            }
            """.formatted(escape(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("x-goog-api-key", API_KEY)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            try {

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                String body = response.body();

                String text = extractText(body);

                if (text == null) {
                    System.out.println("\n[Invalid response]");
                    System.out.println(body);
                } else {
                    System.out.println("\n" + text + "\n");
                    log(prompt, text);
                }

            } catch (Exception e) {
                System.out.println("Request failed. " + e.getMessage());
            }
        }
    }

    // escape prompt for JSON
    static String escape(String s){
        return s.replace("\\","\\\\")
                .replace("\"","\\\"")
                .replace("\n"," ");
    }

    // extract full text block safely
    static String extractText(String json){

    String key = "\"text\":";
    int start = json.indexOf(key);

    if(start == -1)
        return null;

    start += key.length();

    // skip whitespace + quotes
    while(start < json.length() && (json.charAt(start)==' ' || json.charAt(start)=='\"'))
        start++;

    StringBuilder out = new StringBuilder();

    boolean escape = false;

    for(int i = start; i < json.length(); i++){
        char c = json.charAt(i);

        if(escape){
            switch(c){
                case 'n': out.append('\n'); break;
                case 't': out.append('\t'); break;
                case 'r': out.append('\r'); break;
                case '"': out.append('"'); break;
                case '\\': out.append('\\'); break;
                default: out.append(c);
            }
            escape=false;
            continue;
        }

        if(c=='\\'){
            escape=true;
            continue;
        }

        if(c=='"') break;

        out.append(c);
    }

    String text = out.toString();

    // remove markdown fences if present
    text = text.replaceAll("```[a-zA-Z]*","")
               .replace("```","");

    return text.trim();
}

    // log chat to file
    static void log(String prompt, String response){
        try(FileWriter fw = new FileWriter(LOG_FILE, true)){
            fw.write("\n============================\n");
            fw.write(LocalDateTime.now()+"\n");
            fw.write("USER: " + prompt + "\n\n");
            fw.write("AI:\n" + response + "\n");
        } catch(Exception ignored){}
    }
}