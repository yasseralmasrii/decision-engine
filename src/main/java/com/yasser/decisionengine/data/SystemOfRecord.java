package com.yasser.decisionengine.data;

import com.yasser.decisionengine.data.Records.CiRun;
import com.yasser.decisionengine.data.Records.Customer;
import com.yasser.decisionengine.data.Records.Order;
import com.yasser.decisionengine.data.Records.Service;
import com.yasser.decisionengine.data.Records.Ticket;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The engine's source of truth. Agents say WHAT they want to do;
 * the facts (order totals, refund history, CI status...) come from here,
 * so an agent cannot lie its way to an EXECUTE.
 *
 * Backed by synthetic JSON in src/main/resources/data/.
 */
@Component
public class SystemOfRecord {

    private final Map<String, Order> orders;
    private final Map<String, Customer> customers;
    private final Map<String, Ticket> tickets;
    private final Map<String, Service> services;
    private final Map<String, CiRun> ciRunsByCommit;

    public SystemOfRecord() {
        JsonMapper mapper = JsonMapper.builder().build();
        this.orders = load(mapper, "orders.json", Order[].class, Order::id);
        this.customers = load(mapper, "customers.json", Customer[].class, Customer::id);
        this.tickets = load(mapper, "tickets.json", Ticket[].class, Ticket::id);
        this.services = load(mapper, "services.json", Service[].class, Service::name);
        this.ciRunsByCommit = load(mapper, "ci-runs.json", CiRun[].class, CiRun::commit);
    }

    public Optional<Order> order(String id) {
        return Optional.ofNullable(id == null ? null : orders.get(id));
    }

    public Optional<Customer> customer(String id) {
        return Optional.ofNullable(id == null ? null : customers.get(id));
    }

    public Optional<Ticket> ticket(String id) {
        return Optional.ofNullable(id == null ? null : tickets.get(id));
    }

    public Optional<Service> service(String name) {
        return Optional.ofNullable(name == null ? null : services.get(name));
    }

    public Optional<CiRun> ciRun(String commit) {
        return Optional.ofNullable(commit == null ? null : ciRunsByCommit.get(commit));
    }

    public List<Ticket> tickets() {
        return List.copyOf(tickets.values());
    }

    public List<Order> orders() {
        return List.copyOf(orders.values());
    }

    public List<Customer> customers() {
        return List.copyOf(customers.values());
    }

    public List<Service> services() {
        return List.copyOf(services.values());
    }

    public List<CiRun> ciRuns() {
        return List.copyOf(ciRunsByCommit.values());
    }

    private static <T> Map<String, T> load(JsonMapper mapper, String file, Class<T[]> type,
            Function<T, String> key) {
        String path = "/data/" + file;
        try (InputStream in = SystemOfRecord.class.getResourceAsStream(path)) {
            if (in == null)
                throw new IllegalStateException("Missing resource " + path);
            T[] rows = mapper.readValue(in, type);
            return Arrays.stream(rows).collect(Collectors.toMap(
                    key, Function.identity(),
                    (a, b) -> {
                        throw new IllegalStateException("Duplicate key in " + path + ": " + key.apply(a));
                    },
                    LinkedHashMap::new));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load " + path + ": " + e.getMessage(), e);
        }
    }
}
