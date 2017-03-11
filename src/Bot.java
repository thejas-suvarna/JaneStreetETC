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
            //System.out.println("Connect");
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
                if (sellList.length > 0) {
                    for (char c : sellList) {
                        if (c == ':' || c == ' ') {
                            sellData.add(Integer.parseInt(sellTemp));
                            sellTemp = "";
                        } else {
                            sellTemp += c;
                        }
                    }
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
            while(true) {
                clearData();
                dataStream = from_exchange.readLine().trim();
                readData();
                fairValue = calcFairValue();
                //System.out.println("Fair Value: " + fairValue);
                //System.err.printf("The exchange replied: %s\n", replyStream);
                if(fairValue != -1) {
                    if (dataStream.contains("BOND") && dataStream.contains("BOOK")) {
                        //System.err.printf("The exchange replied: %s\n", replyStream);
                        int buyPrice = buyBond(dataStream);
                        if (buyPrice != -1) {
                            String buyReply = from_exchange.readLine().trim();
                            System.err.printf("The exchange replied: %s\n", buyReply);
                        }
                        int sellPrice = sellBond(dataStream, buyPrice);
                        if (sellPrice != -1) {
                            String sellReply = from_exchange.readLine().trim();
                            System.err.printf("The exchange replied: %s\n", sellReply);
                        }
                    }

                    if (dataStream.contains("VALE") && dataStream.contains("BOOK")) {
                        //System.err.printf("The exchange replied: %s\n", replyStream);
                        int buyPrice = buyVale(dataStream, calcFairValue());
                        if (buyPrice != -1) {
                            String buyReply = from_exchange.readLine().trim();
                            System.err.printf("The exchange replied: %s\n", buyReply);
                        }
                        int sellPrice = sellVale(dataStream, buyPrice, calcFairValue());
                        if (sellPrice != -1) {
                            String sellReply = from_exchange.readLine().trim();
                            System.err.printf("The exchange replied: %s\n", sellReply);
                        }
                    }

                    if (dataStream.contains("VALBZ") && dataStream.contains("BOOK")) {
                        //System.err.printf("The exchange replied: %s\n", replyStream);
                        int buyPrice = buyValbz(dataStream, calcFairValue());
                        if (buyPrice != -1) {
                            String buyReply = from_exchange.readLine().trim();
                            System.err.printf("The exchange replied: %s\n", buyReply);
                        }
                        int sellPrice = sellValbz(dataStream, buyPrice, calcFairValue());
                        if (sellPrice != -1) {
                            String sellReply = from_exchange.readLine().trim();
                            System.err.printf("The exchange replied: %s\n", sellReply);
                        }
                    }
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
        int amount = sellAmount.indexOf(cost);
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
        int cost = Collections.min(sellPrice);
        int amount = sellAmount.indexOf(cost);
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

    //************************************************************************************************************************************************
    //VALE

    public int buyVale(String stream, int Fair_val) {
        //int sellStart = replyStream.indexOf("SELL");
        int pos = stream.lastIndexOf("SELL");
        if(pos != -1) {
            String sellInfo = stream.substring(pos);
            int end = stream.indexOf(':', pos);
            if(end != -1) {
                String highestBuy = stream.substring(pos + 5, end);
                //System.out.println(lowestSell);
                int infoend = stream.indexOf(' ', end);
                String numtoSell = "";
                if (infoend == -1) {
                    numtoSell = stream.substring(end + 1);
                } else {
                    numtoSell = stream.substring(end + 1, infoend);
                }
                int position = Integer.parseInt(numtoSell);
                int cost = Integer.parseInt(highestBuy);
                int sellprice;
                String trans;

                if (cost < Fair_val) {
                    String sendBuy = "ADD " + orderID + " VALE BUY " + cost + " " + position;
                    System.out.println("Sending: " + sendBuy);
                    to_exchange.println(sendBuy);
                    orderID++;
                    return cost;
                }
            }
        }
        return -1;
    }

    public int sellVale(String stream, int buyPrice, int Fair_val) {
        int pos = stream.lastIndexOf("BUY");
        if(pos != -1 && !stream.contains("BUY SELL")) {
            int end = stream.indexOf(':', pos);
            String lowestSell = stream.substring(pos + 4, end);
            //System.out.println(lowestSell);
            int infoend = stream.indexOf(' ', end);
            String numtoSell = stream.substring(end + 1, infoend);
            int position = Integer.parseInt(numtoSell);
            int lowprice = Integer.parseInt(lowestSell);
            int sellprice;
            String trans;
            if (lowprice >= Fair_val+1) {
                sellprice = lowprice - 1;
                trans = "ADD " + orderID + " VALE SELL " + sellprice + " " + position;
                System.out.println("Sending: " + trans);
                to_exchange.println(trans);
                orderID++;
                return sellprice;
            }
        }
        return -1;
    }


    //************************************************************************************************************************************************
    //VALBZ
    public int buyValbz(String stream, int Fair_val) {
        //int sellStart = replyStream.indexOf("SELL");
        int pos = stream.lastIndexOf("SELL");
        if(pos != -1) {
            String sellInfo = stream.substring(pos);
            int end = stream.indexOf(':', pos);
            if(end != -1) {
                String highestBuy = stream.substring(pos + 5, end);
                //System.out.println(lowestSell);
                int infoend = stream.indexOf(' ', end);
                String numtoSell = "";
                if (infoend == -1) {
                    numtoSell = stream.substring(end + 1);
                } else {
                    numtoSell = stream.substring(end + 1, infoend);
                }
                int position = Integer.parseInt(numtoSell);
                int cost = Integer.parseInt(highestBuy);
                int sellprice;
                String trans;

                if (cost < Fair_val) {
                    String sendBuy = "ADD " + orderID + " VALBZ BUY " + cost + " " + position;
                    System.out.println("Sending: " + sendBuy);
                    to_exchange.println(sendBuy);
                    orderID++;
                    return cost;
                }
            }
        }
        return -1;
    }

    public int sellValbz(String stream, int buyPrice, int Fair_val) {
        int pos = stream.lastIndexOf("BUY");
        if(pos != -1 && !stream.contains("BUY SELL")) {
            int end = stream.indexOf(':', pos);
            String lowestSell = stream.substring(pos + 4, end);
            //System.out.println(lowestSell);
            int infoend = stream.indexOf(' ', end);
            String numtoSell = stream.substring(end + 1, infoend);
            int position = Integer.parseInt(numtoSell);
            int lowprice = Integer.parseInt(lowestSell);
            int sellprice;
            String trans;
            if (lowprice >= Fair_val+1) {
                sellprice = lowprice - 1;
                trans = "ADD " + orderID + " VALBZ SELL " + sellprice + " " + position;
                System.out.println("Sending: " + trans);
                to_exchange.println(trans);
                orderID++;
                return sellprice;
            }
        }
        return -1;
    }

    //************************************************************************************************************************************************

    public int buyBond(String stream) {
        //int sellStart = replyStream.indexOf("SELL");
        int pos = stream.lastIndexOf("SELL");
        if(pos != -1) {
            String sellInfo = stream.substring(pos);
            int end = stream.indexOf(':', pos);
            if(end != -1) {
                String highestBuy = stream.substring(pos + 5, end);
                //System.out.println(lowestSell);
                int infoend = stream.indexOf(' ', end);
                String numtoSell = "";
                if (infoend == -1) {
                    numtoSell = stream.substring(end + 1);
                } else {
                    numtoSell = stream.substring(end + 1, infoend);
                }
                int position = Integer.parseInt(numtoSell);
                int cost = Integer.parseInt(highestBuy);
                int sellprice;
                String trans;

                if (cost < 1000) {
                    String sendBuy = "ADD " + orderID + " BOND BUY " + cost + " " + position;
                    System.out.println("Sending: " + sendBuy);
                    to_exchange.println(sendBuy);
                    orderID++;
                    return cost;
                }
            }
        }
        return -1;
    }

    public int sellBond(String stream, int buyPrice) {
        int pos = stream.lastIndexOf("BUY");
        if(pos != -1 && !stream.contains("BUY SELL")) {
            int end = stream.indexOf(':', pos);
            String lowestSell = stream.substring(pos + 4, end);
            //System.out.println(lowestSell);
            int infoend = stream.indexOf(' ', end);
            String numtoSell = stream.substring(end + 1, infoend);
            int position = Integer.parseInt(numtoSell);
            int lowprice = Integer.parseInt(lowestSell);
            int sellprice;
            String trans;
            if (lowprice >= 1001) {
                sellprice = lowprice - 1;
                trans = "ADD " + orderID + " BOND SELL " + sellprice + " " + position;
                System.out.println("Sending: " + trans);
                to_exchange.println(trans);
                orderID++;
                return sellprice;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        Bot b = new Bot();
        while(true) {
            b.trade();
        }
    }
}
