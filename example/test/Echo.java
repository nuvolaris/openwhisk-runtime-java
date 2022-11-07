package example;

import com.google.gson.JsonObject;

public class Echo {
     public static JsonObject main(JsonObject args) {
         System.out.println("hello stdout");
         System.err.println("hello stderr");
         return args;
     }
}
 
