import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class Main {

    public static TreeMap<String, String> ownerMap = new TreeMap();

    public static void main(String[] args) {
        try {
            //Read config from DeFi Wallet
            Path path = Paths.get(System.getProperty("user.home") + "/.defi/defi.conf");
            Properties configProps = new Properties();
            try (FileInputStream i = new FileInputStream(path.toString())) {
                configProps.load(i);
            }

            //get all token addresses
            getListAccounts(configProps.getProperty("rpcport"), configProps.getProperty("rpcuser") + ":" + configProps.getProperty("rpcpassword"));

            //get all coin addresses with balance > 0.0
            getListAddressGroupings(configProps.getProperty("rpcport"), configProps.getProperty("rpcuser") + ":" + configProps.getProperty("rpcpassword"));

            //Convert TreeMap to StringBuilder
            StringBuilder sb = new StringBuilder();
            for (Map.Entry address : ownerMap.entrySet()) {
                sb.append(address.getValue()).append(",");
            }

            //Copy all addresses im clipboard
            if (ownerMap.size() > 0) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection(sb.substring(0, sb.toString().length() - 1)), null
                );
            } else {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new StringSelection("Something went wrong. Maybe DeFi Wallet is not running"), null
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getListAccounts(String rpcport, String auth) {

        try {
            //Connection to RPC Server
            HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + rpcport).openConnection();
            conn.setConnectTimeout((int) TimeUnit.MINUTES.toMillis(0L));
            conn.setReadTimeout((int) TimeUnit.MINUTES.toMillis(0L));
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Authorization", "Basic " + new String(Base64.getEncoder().encode((auth.getBytes()))));
            conn.setRequestProperty("Content-Type", "application/json-rpc");
            conn.getOutputStream().write("{\"method\":\"listaccounts\",\"params\":[{},false,false,true]}".getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().close();

            //Get response
            String jsonText = "";
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    jsonText = br.readLine();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonText);
                JSONArray transactionJson = (JSONArray) jsonObject.get("result");

                //Put all token addresses in ownerMap
                for (Object owner : transactionJson) {
                    JSONObject ownerObject = (JSONObject) owner;
                    if (!ownerMap.containsKey(ownerObject.get("owner").toString())) {
                        ownerMap.put(ownerObject.get("owner").toString(), ownerObject.get("owner").toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getListAddressGroupings(String rpcport, String auth) {

        try {
            //Connection to RPC Server
            HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + rpcport).openConnection();
            conn.setConnectTimeout((int) TimeUnit.MINUTES.toMillis(0L));
            conn.setReadTimeout((int) TimeUnit.MINUTES.toMillis(0L));
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Authorization", "Basic " + new String(Base64.getEncoder().encode((auth.getBytes()))));
            conn.setRequestProperty("Content-Type", "application/json-rpc");
            conn.getOutputStream().write("{\"method\":\"listaddressgroupings\"}".getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().close();

            //Get response
            String jsonText = "";
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    jsonText = br.readLine();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonText);
                JSONArray transactionJson = (JSONArray) jsonObject.get("result");
                JSONArray ownerArray = (JSONArray) transactionJson.get(0);

                //Put all coin addresses with balance > 0.0 in a treemap
                for (Object owner : ownerArray) {
                    JSONArray ownerObject = (JSONArray) owner;
                    if (!ownerMap.containsKey(ownerObject.get(0).toString()) && (Double) ownerObject.get(1) > 0.0) {
                        ownerMap.put(ownerObject.get(0).toString(), ownerObject.get(0).toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
