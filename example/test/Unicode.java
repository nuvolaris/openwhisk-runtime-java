import com.google.gson.JsonObject;

public class Unicode {
     public static JsonObject main(JsonObject args) {
         String delimiter = args.getAsJsonPrimitive("delimiter").getAsString();
         JsonObject response = new JsonObject();
         String str = delimiter + " ☃ " + delimiter;
         System.out.println(str);
         response.addProperty("winter", str);
         return response;
     }
}
