package utils;

import dev.se1dhe.bot.conf.Config;
import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.enums.PrizeType;
import dev.se1dhe.bot.model.enums.RaffleType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.Util.parseDateTime;

public class XMLParser {
    public static List<Prize> createPrize() {
        List<Prize> prizeList = new ArrayList<>();
        try {
            File xmlFile = new File("config/dailyRafflePrize.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            NodeList prizes = doc.getElementsByTagName("prize");
            for (int i = 0; i < prizes.getLength(); i++) {
                Prize newPrize = new Prize();
                Element prize = (Element) prizes.item(i);
                newPrize.setPlace(Integer.parseInt(prize.getAttribute("place")));
                newPrize.setType(PrizeType.valueOf(prize.getElementsByTagName("type").item(0).getTextContent()));
                newPrize.setItemName(prize.getElementsByTagName("itemName").item(0).getTextContent());
                newPrize.setItemId(Integer.parseInt(prize.getElementsByTagName("itemId").item(0).getTextContent()));
                newPrize.setCount(Integer.parseInt(prize.getElementsByTagName("count").item(0).getTextContent()));
                prizeList.add(newPrize);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prizeList;
    }

    public static Raffle parseDelayedRaffle() {
        Raffle delayedRaffle = new Raffle();
        try {
            File xmlFile = new File("config/delayedRaffle.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Parse DelayedRaffle details
            Element delayedRaffleElement = doc.getDocumentElement();
            delayedRaffle.setName(getTagValue(delayedRaffleElement, "name"));
            delayedRaffle.setDesc(getTagValue(delayedRaffleElement, "description"));
            delayedRaffle.setStartDate(parseDateTime(getTagValue(delayedRaffleElement, "startDate")));
            delayedRaffle.setRaffleResultDate(parseDateTime(getTagValue(delayedRaffleElement, "endDate")));
            delayedRaffle.setImgPath(getTagValue(delayedRaffleElement, "imgPath"));
            delayedRaffle.setChannelForSub(getTagValue(delayedRaffleElement, "channelForSub"));
            delayedRaffle.setSiteUrl(getTagValue(delayedRaffleElement, "siteUrl"));
            delayedRaffle.setType(RaffleType.valueOf(getTagValue(delayedRaffleElement, "raffleType").toUpperCase()));
            delayedRaffle.setParticipationBonus(Config.DAILY_PARTICIPANT_BONUS);
            NodeList prizeList = doc.getElementsByTagName("prize");
            List<Prize> prizes = new ArrayList<>();
            for (int i = 0; i < prizeList.getLength(); i++) {
                Prize prize = new Prize();
                Element prizeElement = (Element) prizeList.item(i);
                prize.setPlace(Integer.parseInt(prizeElement.getAttribute("place")));
                prize.setType(PrizeType.valueOf(getTagValue(prizeElement, "type")));
                prize.setCount(Integer.parseInt(getTagValue(prizeElement, "count")));
                prize.setItemName(getTagValue(prizeElement, "itemName"));
                prize.setItemId(Integer.parseInt(getTagValue(prizeElement, "itemId")));
                prizes.add(prize);
            }
            delayedRaffle.setWinnerCount(prizes.size());

            delayedRaffle.setPrizes(prizes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return delayedRaffle;
    }
    private static String getTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        Node node = nodeList.item(0);
        if (node != null) {
            return node.getTextContent();
        }
        return "";
    }

    public static void modifyRaffleType(String newRaffleType) {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(new File("config/delayedRaffle.xml"));

            Node raffleTypeNode = document.getElementsByTagName("raffleType").item(0);

            raffleTypeNode.setTextContent(newRaffleType);

            // Записываем изменения обратно в файл
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File("config/delayedRaffle.xml"));
            transformer.transform(source, result);


        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.printStackTrace();
        }
    }
}