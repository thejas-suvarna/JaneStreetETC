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

public class Bot {

    private BufferedReader from_exchange;
    private PrintWriter to_exchange;
    private int orderID = 1;
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
        int buyStart = dataStream.indexOf("BUY");
        int sellStart = dataStream.indexOf("SELL");

        instrument = dataStream.substring(bookStart + 5, buyStart - 1);

        String buyTemp = "";
        List<Integer> buyData = new ArrayList<Integer>();
        char[] buyList = dataStream.substring(buyStart + 4, sellStart - 1).toCharArray();
        if(buyList.length > 0) {
            for (char c : buyList) {
                if(c == ':' || c == ' ') {
                    buyData.add(Integer.parseInt(buyTemp));
                }
                buyTemp += c;
            }
            for(int i = 0; i < buyData.size(); i++) {
                if(i % 2 == 0) {
                    buyPrice.add(buyData.get(i));
                } else {
                    buyAmount.add(buyData.get(i));
                }
            }
        }

        String sellTemp = "";
        List<Integer> sellData = new ArrayList<Integer>();
        if(sellStart + 3 != dataStream.length()) {
            char[] sellList = dataStream.substring(sellStart + 5).toCharArray();
            if (sellList.length > 0) {
                for (char c : sellList) {
                    if (c == ':' || c == ' ') {
                        buyData.add(Integer.parseInt(sellTemp));
                    }
                    sellTemp += c;
                }
                for (int i = 0; i < sellData.size(); i++) {
                    if (i % 2 == 0) {
                        buyPrice.add(sellData.get(i));
                    } else {
                        buyAmount.add(sellData.get(i));
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
                //System.err.printf("The exchange replied: %s\n", replyStream);
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
            }
        } catch (Exception e) {
            //e.printStackTrace(System.out);
            initializeConnection();
        }
    }

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
