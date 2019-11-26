package io.branch.search.widget;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Asset Utilities.
 */
public class AssetUtils {
    public static String readAssetFile(Context context, String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
        } catch (IOException e) {
        }
        return sb.toString();
    }

    public static JSONObject readJsonAsset(Context context, String filename) throws JSONException {
        String jsonString = readAssetFile(context, filename);
        return new JSONObject(jsonString);
    }

    public static JSONArray readJsonArrayAsset(Context context, String filename) throws JSONException {
        String jsonString = readAssetFile(context, filename);
        return new JSONArray(jsonString);
    }
}
