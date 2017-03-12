/**
 * Created by gregory on 3/11/2017.
 */

import java.lang.*;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Bot {

    private BufferedReader from_exchange;
    private PrintWriter to_exchange;
    private int orderID = 1;
    private int fairValue;
    private String dataStream, instrument;
    private List<Integer> buyPrice, buyAmount, sellPrice, sellAmount;

    public Bot() {
        initializeConnection();

        buyPrice = new ArrayList<Integer>();
        buyAmount = new ArrayList<Integer>();
        sellPrice = new ArrayList<Integer>();
        sellAmount = new ArrayList<Integer>();
    }

    public void initializeConnection() {
        try {

            //Socket skt = new Socket("production", 20000);
            Socket skt = new Socket("production", 20000);
            from_exchange = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            to_exchange = new PrintWriter(skt.getOutputStream(), true);

            to_exchange.println("HELLO ABCDE");
            String reply = from_exchange.readLine().trim();
            System.err.printf("The exchange replied: %s\n", reply);
        } catch (Exception e) {
            //e.printStackTrace(System.out);
            initializeConnection();
        }
    }

    public void readData() {
        int bookStart = dataStream.indexOf("BOOK");
        if (bookStart != -1) {
            int buyStart = dataStream.indexOf("BUY");
            int sellStart = dataStream.indexOf("SELL");

            instrument = dataStream.substring(bookStart + 5, buyStart - 1);

            String buyTemp = "";
            List<Integer> buyData = new ArrayList<Integer>();
            //System.out.println(dataStream);
            if (!dataStream.contains("BUY SELL")) {
                char[] buyList = dataStream.substring(buyStart + 4, sellStart - 1).toCharArray();
                for (char c : buyList) {
                    if (c == ':' || c == ' ') {
                        buyData.add(Integer.parseInt(buyTemp));
                        buyTemp = "";
                    } else {
                        buyTemp += c;
                    }
                }
                buyData.add(Integer.parseInt(buyTemp));
                //System.out.println("buyData: " + buyData);
                for (int i = 0; i < buyData.size(); i++) {
                    if (i % 2 == 0) {
                        buyPrice.add(buyData.get(i));
                    } else {
                        buyAmount.add(buyData.get(i));
                    }
                }
            }

            String sellTemp = "";
            List<Integer> sellData = new ArrayList<Integer>();
            if (sellStart + 4 != dataStream.length()) {
                char[] sellList = dataStream.substring(sellStart + 5).toCharArray();
                //System.out.println("sellList: " + dataStream.substring(sellStart + 5));
                if (sellList.length > 0) {
                    for (char c : sellList) {
                        if (c == ':' || c == ' ') {
                            sellData.add(Integer.parseInt(sellTemp));
                            sellTemp = "";
                        } else {
                            sellTemp += c;
                        }
                    }
                    sellData.add(Integer.parseInt(sellTemp));
                    //System.out.println("sellData: " + sellData);
                    for (int i = 0; i < sellData.size(); i++) {
                        if (i % 2 == 0) {
                            sellPrice.add(sellData.get(i));
                        } else {
                            sellAmount.add(sellData.get(i));
                        }
                    }
                }
            }
        }
    }

    public void clearData() {
        instrument = "";
        buyPrice.clear();
        buyAmount.clear();
        sellPrice.clear();
        sellAmount.clear();
    }

    public void trade() {
        try {
            int fairValueVALBZ = -1, fairValueVALE = -1, fairValueBOND = 1000, fairValueGS = -1, fairValueMS = -1,
                    fairValueWFC = -1, fairValueXLF = -1;
            while(true) {
                //System.out.println("buyPrice: " + buyPrice);
                //System.out.println("buyAmount: " + buyAmount);
                //System.out.println("sellPrice: " + sellPrice);
                //System.out.println("sellAmount: " + sellAmount);

                clearData();
                dataStream = from_exchange.readLine().trim();
                readData();

                fairValue = calcFairValue();
                //System.out.println("Fair Value: " + fairValue);
                //System.err.printf("The exchange replied: %s\n", replyStream);
                if(dataStream.contains("BOOK") && fairValue != -1 && dataStream.contains("BOND")) {

                    buy(1000);
                    sell(1000);
                }

                else if(dataStream.contains("BOOK") && fairValue != -1) {
                    if(dataStream.contains("VALBZ")){
                        fairValueVALBZ = fairValue;
                    }
                    if(dataStream.contains("VALE")){
                        if(fairValueVALBZ != -1) {
                            buy(fairValueVALBZ);
                            sell(fairValueVALBZ);
                        }

                    }
                    if(dataStream.contains("GS")){
                        fairValueGS = fairValue;
                    }
                    if(dataStream.contains("MS")){
                        fairValueMS = fairValue;
                    }
                    if(dataStream.contains("WFC")){
                        fairValueWFC = fairValue;
                    }
//                    if(dataStream.contains("XLF")){
//                        if(fairValueGS != -1 || fairValueMS != -1 || fairValueWFC != -1) {
//                            fairValueXLF = fairValue;
//                            int predFairValue = (3 * fairValueBOND + 2 * fairValueGS + 3 * fairValueMS + 2 * fairValueWFC) / 11;
//                            if (predFairValue < fairValueXLF) {
//                                sell(predFairValue);
//                            }
//                            if (predFairValue > fairValueXLF) {
//                                buy(predFairValue);
//                            }
//                        }
//                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            initializeConnection();
        }
    }

    public int calcFairValue(){
        if(!buyPrice.isEmpty() && !sellPrice.isEmpty()) {
            int maxBuy = Collections.max(buyPrice);
            int minSell = Collections.min(sellPrice);
            return (maxBuy + minSell) / 2;
        }
        return -1;
    }

    public void buy(int fair) {
        int cost = Collections.min(sellPrice);
        int amount = sellAmount.get(sellPrice.indexOf(cost));
        while(cost < fair) {
            String send = "ADD " + orderID + " " + instrument + " BUY " + cost + " " + amount;
            System.out.println("Sending: " + send);
            to_exchange.println(send);
            orderID++;

            sellPrice.remove(sellPrice.indexOf(cost));
            sellAmount.remove(sellAmount.indexOf(amount));
            if(!sellPrice.isEmpty()) {
                cost = Collections.min(sellPrice);
                amount = sellAmount.indexOf(cost);
            } else {
                break;
            }
        }
    }

    public void sell(int fair) {
        int cost = Collections.max(buyPrice);
        int amount = buyAmount.get(buyPrice.indexOf(cost));
        while(cost > fair) {
            String send = "ADD " + orderID + " " + instrument + " SELL " + cost + " " + amount;
            System.out.println("Sending: " + send);
            to_exchange.println(send);
            orderID++;

            buyPrice.remove(buyPrice.indexOf(cost));
            buyAmount.remove(buyAmount.indexOf(amount));
            if(!buyPrice.isEmpty()) {
                cost = Collections.min(buyPrice);
                amount = buyAmount.indexOf(cost);
            } else {
                break;
            }
        }
    }

    public static void main(String[] args) {
        Bot b = new Bot();
        while(true) {
            b.trade();
        }
    }
}
