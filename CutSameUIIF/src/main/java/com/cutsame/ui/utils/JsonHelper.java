package com.cutsame.ui.utils;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 类似getString的get...函数可以一次性取出多层的值
 * 如下的json格式
 * {
 * "a":{
 * "b":[
 * {
 * "e":"222"
 * },
 * {
 * "c":"1"
 * }
 * ]
 * }
 * }
 * 则getString(json,"a/b/1/c")返回"1"
 * getBoolean(json,"a/b/1/c")返回true
 * get系列函数会自动转换值到对应的格式,
 * 另外永远不会返回null对象，如果对应的路径不存在，则构造一个默认值为空的对象返回
 */
public class JsonHelper {

    public static Gson gson = new Gson();
    private static final Object lockObj = new Object();

    public static String toJsonString(Object o) {
        return gson.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T fromJson(JsonElement json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static Map<String, Object> toMap(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface MapProvider {
        Map<String, Object> createMap();
    }

    public static Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        return jsonObjectToMap(jsonObject, LinkedHashMap::new);
    }

    public static Map<String, Object> jsonObjectToMap(JSONObject jsonObject, MapProvider provider) {
        if (jsonObject == null) {
            return null;
        }
        JSONArray names = jsonObject.names();
        if (names != null) {
            Map<String, Object> map = provider.createMap();
            int count = names.length();
            for (int i = 0; i < count; ++i) {
                try {
                    String name = names.getString(i);
                    map.put(name, jsonObject.get(name));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            return map;
        }
        return null;
    }

    public static JSONObject toJSONObject(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject toJSONObject(JsonObject json) {
        if (json == null) {
            return null;
        }
        try {
            return new JSONObject(gson.toJson(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsonObject toJsonObject(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return new JsonParser().parse(json).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsonObject toJsonObject(JsonReader reader) {
        if (reader == null) {
            return null;
        }
        try {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray toJSONArray(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray toJSONArray(JsonArray json) {
        if (json == null) {
            return null;
        }
        try {
            return new JSONArray(gson.toJson(json));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsonArray toJsonArray(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return new JsonParser().parse(json).getAsJsonArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * obj may be JSONObject or JSONArray object
     * path为路径，类似"a/3/b"，可以一次性取出多层的值
     */
    public static Object get(Object jsonObj, String path) {

        if (jsonObj == null || path == null)
            return null;

        // 获取key
        String key;
        int index = path.indexOf('/');
        if (index < 0) {
            key = path;
            path = null;
        } else {
            key = path.substring(0, index);
            path = path.substring(index + 1);
        }

        // 获取第一部分
        Object value = null;
        try {
            if (jsonObj instanceof JSONObject) {
                if (((JSONObject) jsonObj).has(key)) {
                    value = ((JSONObject) jsonObj).get(key);
                } else {
                    return null;
                }
            } else if (jsonObj instanceof JsonObject) {
                if (((JsonObject) jsonObj).has(key)) {
                    value = ((JsonObject) jsonObj).get(key);
                } else {
                    return null;
                }
            } else if (jsonObj instanceof JSONArray) {
                int n = Integer.parseInt(key);
                JSONArray ary = (JSONArray) jsonObj;
                value = ary.get(n);
            } else if (jsonObj instanceof JsonArray) {
                int n = Integer.parseInt(key);
                JsonArray ary = (JsonArray) jsonObj;
                value = ary.get(n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (value != null && path != null) {
            value = get(value, path);
        }

        return value;
    }

    public static String getString(Object obj, String path) {
        Object value = get(obj, path);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof JsonPrimitive) {
            return ((JsonPrimitive) value).getAsString();
        } else if (value instanceof JsonNull) {
            return null;
        }
        return String.valueOf(value);
    }

    public static int getInt(Object obj, String path, int defaultValue) {
        Object value = get(obj, path);
        if (value == null)
            return defaultValue;
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else if (value instanceof JsonPrimitive) {
            try {
                return ((JsonPrimitive) value).getAsInt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }

    public static long getLong(Object obj, String path, long defaultValue) {
        Object value = get(obj, path);
        if (value == null)
            return defaultValue;
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else if (value instanceof JsonPrimitive) {
            try {
                return ((JsonPrimitive) value).getAsLong();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }

    public static double getDouble(Object obj, String path, double defaultValue) {
        Object value = get(obj, path);
        if (value == null)
            return defaultValue;
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else if (value instanceof JsonPrimitive) {
            try {
                return ((JsonPrimitive) value).getAsDouble();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }

    public static float getFloat(Object obj, String path, float defaultValue) {
        Object value = get(obj, path);
        if (value == null)
            return defaultValue;
        if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else if (value instanceof JsonPrimitive) {
            try {
                return ((JsonPrimitive) value).getAsFloat();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }

    public static boolean getBoolean(Object obj, String path, boolean defaultValue) {
        Object value = get(obj, path);
        if (value == null)
            return defaultValue;

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            String stringValue = (String) value;
            if ("true".equalsIgnoreCase(stringValue)) {
                return true;
            } else if ("false".equalsIgnoreCase(stringValue)) {
                return false;
            }
            return !("0".equalsIgnoreCase(stringValue) || stringValue.length() == 0);
        } else if (value instanceof Number) {
            return (((Number) value).intValue()) != 0;
        } else if (value instanceof JsonPrimitive) {
            try {
                return ((JsonPrimitive) value).getAsBoolean();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }

    public static JSONObject getJSONObject(Object obj, String path) {
        Object value = get(obj, path);
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        } else {
            return null;
        }
    }

    public static JsonObject getJsonObject(Object obj, String path) {
        Object value = get(obj, path);
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        } else {
            return null;
        }
    }

    public static JSONArray getJSONArray(Object obj, String path) {
        Object value = get(obj, path);
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        } else {
            return null;
        }
    }

    public static JsonArray getJsonArray(Object obj, String path) {
        Object value = get(obj, path);
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        } else {
            return null;
        }
    }

    /**
     * 不抛出异常的
     */
    public static JSONObject put(JSONObject json, String name, boolean value) {
        try {
            json.put(name, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONObject put(JSONObject json, String name, int value) {
        try {
            json.put(name, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONObject put(JSONObject json, String name, long value) {
        try {
            json.put(name, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONObject put(JSONObject json, String name, double value) {
        try {
            json.put(name, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONObject put(JSONObject json, String name, Object value) {
        try {
            json.put(name, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * JSONArray
     */
    public static JSONArray put(JSONArray json, boolean value) {
        try {
            json.put(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONArray put(JSONArray json, int value) {
        try {
            json.put(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONArray put(JSONArray json, long value) {
        try {
            json.put(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONArray put(JSONArray json, double value) {
        try {
            json.put(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONArray put(JSONArray json, Object value) {
        try {
            json.put(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
}
