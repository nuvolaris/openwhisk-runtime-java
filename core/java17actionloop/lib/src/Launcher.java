/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.*;
import java.security.Permission;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

class Launcher {

    private static String mainClassName = "Main";
    private static String mainMethodName = "main";
    private static Class mainClass = null;
    private static Method mainMethod = null;


    private static void initMain(String[] args) throws Exception {
        if(args.length > 0)
            mainClassName = args[0];
        int pos = mainClassName.indexOf("#");
        if(pos != -1) {
            if(pos + 1 != mainClassName.length())
                mainMethodName = args[0].substring(pos+1);
            mainClassName = args[0].substring(0,pos);
        }

        mainClass = Class.forName(mainClassName);
        Method m = mainClass.getMethod(mainMethodName, new Class[] { JsonObject.class });
        m.setAccessible(true);
        int modifiers = m.getModifiers();
        if (m.getReturnType() != JsonObject.class || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new NoSuchMethodException(mainMethodName);
        }
        mainMethod = m;
    }

    private static JsonObject invokeMain(JsonObject arg, Map<String, String> env) throws Exception {
        for(var e: env.entrySet()) {
            arg.add(e.getKey(), new Gson().toJsonTree(e.getValue()));
        }
        return (JsonObject) mainMethod.invoke(null, arg);
    }

    public static void main(String[] args) throws Exception {

        initMain(args);

        // exit after main class loading if "exit" specified
        // used to check healthy launch after init
        if(args.length >1 && Objects.equals(args[1], "-exit"))
            System.exit(0);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, "UTF-8"));
        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream("/dev/fd/3"), "UTF-8"));
        JsonParser json = new JsonParser();
        JsonObject empty = json.parse("{}").getAsJsonObject();
        String input = "";
        while (true) {
            try {
                input = in.readLine();
                if (input == null)
                    break;
                JsonElement element = json.parse(input);
                JsonObject payload = empty.deepCopy();
                HashMap<String, String> env = new HashMap<>();
                if (element.isJsonObject()) {
                    // collect payload and environment
                    for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                        if (entry.getKey().equals("value")) {
                            if (entry.getValue().isJsonObject())
                                payload = entry.getValue().getAsJsonObject();
                        } else {
                            env.put(String.format("ow.%s", entry.getKey().toLowerCase().replace("_", "")),
                                    entry.getValue().getAsString());
                        }
                    }
                }
                JsonElement response = invokeMain(payload, env);
                out.println(response.toString());
            } catch(NullPointerException npe) {
                System.out.println("the action returned null");
                npe.printStackTrace(System.err);
                JsonObject error = new JsonObject();
                error.addProperty("error", "the action returned null");
                out.println(error);
                out.flush();
            } catch(InvocationTargetException ite) {
                Throwable ex = ite;
                if(ite.getCause() != null)
                    ex = ite.getCause();
                ex.printStackTrace(System.err);
                JsonObject error = new JsonObject();
                error.addProperty("error", ex.getMessage());
                out.println(error);
                out.flush();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                JsonObject error = new JsonObject();
                error.addProperty("error", ex.getMessage());
                out.println(error);
                out.flush();
            }
            out.flush();
            System.out.flush();
            System.err.flush();
        }
    }
}

