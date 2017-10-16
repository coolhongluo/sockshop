package works.weave.socks.orders.controllers;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.TypeReferences;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.servicecomb.provider.rest.common.RestSchema;
import io.servicecomb.serviceregistry.RegistryUtils;
import io.servicecomb.serviceregistry.api.registry.Microservice;
import io.servicecomb.serviceregistry.api.registry.MicroserviceInstance;
import io.servicecomb.serviceregistry.client.ServiceRegistryClient;
import works.weave.socks.orders.config.OrdersConfigurationProperties;
import works.weave.socks.orders.entities.Address;
import works.weave.socks.orders.entities.Card;
import works.weave.socks.orders.entities.Customer;
import works.weave.socks.orders.entities.CustomerOrder;
import works.weave.socks.orders.entities.Item;
import works.weave.socks.orders.entities.Shipment;
import works.weave.socks.orders.repositories.CustomerOrderRepository;
import works.weave.socks.orders.resources.NewOrderResource;
import works.weave.socks.orders.services.AsyncGetService;
import works.weave.socks.orders.values.PaymentRequest;
import works.weave.socks.orders.values.PaymentResponse;

@RepositoryRestController
@RestSchema(schemaId = "orders")
@RequestMapping(path = "/orders")
public class OrdersController {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrdersConfigurationProperties config;

    @Autowired
    private AsyncGetService asyncGetService;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Value(value = "${http.timeout:5}")
    private long timeout;

    @RequestMapping(path = "customerId", method = RequestMethod.GET)
    public List<CustomerOrder> findByCustomerId(
            @RequestParam("custId") String id) throws InterruptedException, IOException, ExecutionException, TimeoutException {
        List<CustomerOrder> orderList = customerOrderRepository.findByCustomerId(id);
        return orderList;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public @ResponseBody CustomerOrder newOrder(@RequestBody NewOrderResource item) {
        try {
            if (item.address == null || item.customer == null || item.card == null || item.items == null) {
                throw new InvalidOrderException(
                        "Invalid order request. Order requires customer, address, card and items.");
            }

            LOG.info("Starting calls");
            LOG.info(item.address.getPath());
            LOG.info(item.customer.getPath());
            LOG.info(item.card.getPath());
            LOG.info(item.items.getPath());
            Future<Resource<Address>> addressFuture =
                asyncGetService.getResource(item.address, new TypeReferences.ResourceType<Address>() {
                });

            Future<Resource<Customer>> customerFuture =
                asyncGetService.getResource(item.customer, new TypeReferences.ResourceType<Customer>() {
                });

            Future<Resource<Card>> cardFuture =
                asyncGetService.getResource(item.card, new TypeReferences.ResourceType<Card>() {
                });

            Future<List<Item>> itemsFuture =
                asyncGetService.getDataList(item.items, new ParameterizedTypeReference<List<Item>>() {
                });

            LOG.info("End of calls.");

            float amount = calculateTotal(itemsFuture.get(timeout, TimeUnit.SECONDS));

            LOG.info("amount :" + amount);
            // payment
            String shippingUri = null;
            String paymentUri = null;
            ServiceRegistryClient client = RegistryUtils.getServiceRegistryClient();
            List<Microservice> services = client.getAllMicroservices();
            for (Microservice service : services) {
                String name = service.getServiceName();
                if (name.equalsIgnoreCase("shipping")) {
                    String appId = service.getAppId();
                    String cseServiceID = client.getMicroserviceId(appId, name, "0.0.1");
                    List<MicroserviceInstance> instances = client.getMicroserviceInstance(cseServiceID, cseServiceID);
                    if (null != instances && !instances.isEmpty()) {
                        for (MicroserviceInstance instance : instances) {
                            List<String> eps = instance.getEndpoints();
                            for (String ep : eps) {
                                if (ep.startsWith("rest")) {
                                    shippingUri = ep.substring(7);
                                    break;
                                }
                            }
                        }
                    }
                } else if (name.equalsIgnoreCase("payment")) {
                    String appId = service.getAppId();
                    String cseServiceID = client.getMicroserviceId(appId, name, "0.0.1");
                    List<MicroserviceInstance> instances = client.getMicroserviceInstance(cseServiceID, cseServiceID);
                    if (null != instances && !instances.isEmpty()) {
                        for (MicroserviceInstance instance : instances) {
                            List<String> eps = instance.getEndpoints();
                            for (String ep : eps) {
                                if (ep.startsWith("rest")) {
                                    paymentUri = ep.substring(7);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // Call payment service to make sure they've paid
            PaymentRequest paymentRequest = new PaymentRequest(
                    addressFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                    cardFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                    customerFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                    amount);

            Future<PaymentResponse> paymentFuture = asyncGetService.postResource(
                    config.getPaymentUri(paymentUri),
                    paymentRequest,
                    new ParameterizedTypeReference<PaymentResponse>() {
                    });
            PaymentResponse paymentResponse = paymentFuture.get(timeout, TimeUnit.SECONDS);

            if (paymentResponse == null) {
                throw new PaymentDeclinedException("Unable to parse authorisation packet");
            }
            if (!paymentResponse.isAuthorised()) {
                throw new PaymentDeclinedException(paymentResponse.getMessage());
            }

            String customerId = parseId(customerFuture.get(timeout, TimeUnit.SECONDS).getId().getHref());
            Future<Shipment> shipmentFuture = asyncGetService.postResource(config.getShippingUri(shippingUri),
                    new Shipment(customerId),
                    new ParameterizedTypeReference<Shipment>() {
                    });
            CustomerOrder order = new CustomerOrder(
                    null,
                    customerId,
                    customerFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                    addressFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                    cardFuture.get(timeout, TimeUnit.SECONDS).getContent(),
                    itemsFuture.get(timeout, TimeUnit.SECONDS),
                    shipmentFuture.get(timeout, TimeUnit.SECONDS),
                    Calendar.getInstance().getTime(),
                    amount);

            LOG.info("Received data: " + order.toString());

            CustomerOrder savedOrder = customerOrderRepository.save(order);
            LOG.info("Saved order: " + savedOrder);

            return savedOrder;

        } catch (TimeoutException e) {
            throw new IllegalStateException("Unable to create order due to timeout from one of the services.", e);
        } catch (InterruptedException | IOException | ExecutionException e) {
            throw new IllegalStateException("Unable to create order due to unspecified IO error.", e);
        }
    }

    private String parseId(String href) {

        Pattern idPattern = Pattern.compile("[\\w-]+$");

        Matcher matcher = idPattern.matcher(href);
        if (!matcher.find()) {
            throw new IllegalStateException("Could not parse user ID from: " + href);
        }

        return matcher.group(0);

    }

    private float calculateTotal(List<Item> items) {
        float amount = 0F;
        float shipping = 4.99F;
        amount += items.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();
        amount += shipping;
        return amount;
    }

    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    public class PaymentDeclinedException extends IllegalStateException {
        public PaymentDeclinedException(String s) {
            super(s);
        }
    }

    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    public class InvalidOrderException extends IllegalStateException {
        public InvalidOrderException(String s) {
            super(s);
        }
    }
}
