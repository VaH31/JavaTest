import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TicketAnalyzer {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Использование: java TicketAnalyzer tickets.json");
            return;
        }

        String filename = args[0];
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(filename));
        JsonNode tickets = root.get("tickets");

        List<Map<String, Object>> filteredTickets = new ArrayList<>();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        for (JsonNode ticket : tickets) {
            String originName = ticket.get("origin_name").asText();
            String destinationName = ticket.get("destination_name").asText();
            if (originName.equals("Владивосток") && destinationName.equals("Тель-Авив")) {
                String carrier = ticket.get("carrier").asText();
                int price = ticket.get("price").asInt();
                LocalTime departure = LocalTime.parse(ticket.get("departure_time").asText(), timeFormatter);
                LocalTime arrival = LocalTime.parse(ticket.get("arrival_time").asText(), timeFormatter);

                int duration = (int) java.time.Duration.between(departure, arrival).toMinutes();
                if (duration < 0) { // если перелет через полночь
                    duration += 24 * 60;
                }

                Map<String, Object> t = new HashMap<>();
                t.put("carrier", carrier);
                t.put("price", price);
                t.put("duration", duration);
                filteredTickets.add(t);
            }
        }

        // Минимальное время полета по авиаперевозчикам
        Map<String, Optional<Map<String, Object>>> minDurationMap = filteredTickets.stream()
                .collect(Collectors.groupingBy(t -> (String) t.get("carrier"),
                        Collectors.minBy(Comparator.comparingInt(t -> (int) t.get("duration")))));

        System.out.println("Минимальное время полета для каждого авиаперевозчика:");
        for (Map.Entry<String, Optional<Map<String, Object>>> entry : minDurationMap.entrySet()) {
            entry.getValue().ifPresent(ticket ->
                    System.out.println(entry.getKey() + ": " + ticket.get("duration") + " минут"));
        }

        // Цены для статистики
        List<Integer> prices = filteredTickets.stream()
                .map(t -> (Integer) t.get("price"))
                .sorted()
                .collect(Collectors.toList());

        if (!prices.isEmpty()) {
            double average = prices.stream().mapToInt(Integer::intValue).average().orElse(0);

            double median;
            int n = prices.size();
            if (n % 2 == 0) {
                median = (prices.get(n / 2 - 1) + prices.get(n / 2)) / 2.0;
            } else {
                median = prices.get(n / 2);
            }

            System.out.println("\nСредняя цена: " + average);
            System.out.println("Медиана цены: " + median);
            System.out.println("Разница (среднее - медиана): " + (average - median));
        } else {
            System.out.println("Нет билетов для Владивосток -> Тель-Авив");
        }
    }
}
