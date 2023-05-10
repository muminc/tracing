package com.example.tracing;


import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
public class PizzaController {

    private final @Qualifier("pizza-order-taking") Tracer orderTracer;

    private final @Qualifier("pizza-making") Tracer makingTracer;

    private final @Qualifier("pizza-delivery") Tracer deliveryTracer;




    @PostMapping("/pizza")
    public Object order(@RequestBody PizzaOrder pizzaOrder) throws Exception{

        Span parentSpan = orderTracer.buildSpan("Pizza Order").start();
        String traceId = parentSpan.context().toTraceId();
        parentSpan.setTag("toppings",pizzaOrder.toppings().toString());

        parentSpan.setTag(Tags.HTTP_URL,"/pizza");
        orderTracer.activateSpan(parentSpan);

        String responseMessage = "traceId=" + traceId;
        System.out.println("responseMessage = " + responseMessage);
        try {
            // in real code these would be different services
            pickUpOrder(pizzaOrder);

            // in real code these would be different services
            makeOrder(pizzaOrder);

            // in real code these would be different services
            deliverOrder(pizzaOrder);
            parentSpan.setTag(Tags.HTTP_STATUS,200);


            return responseMessage;
        }
        catch (Exception e){
            parentSpan.setTag(Tags.HTTP_STATUS,500);
            return new ResponseEntity<>(responseMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finally {
            parentSpan.finish();
        }

    }

    private void pickUpOrder(PizzaOrder pizzaOrder) throws InterruptedException {
        Span childSpan = orderTracer.buildSpan("Pick Up Order").start();
        String pickUpServicePerson = Math.random() > 0.5 ? "Daisy" : "Toadstool";
        childSpan.setTag("Server",pickUpServicePerson);
        orderTracer.activateSpan(childSpan);
        try {
            Thread.sleep(100);
        }
        finally {
            childSpan.finish();
        }
    }

    private void makeOrder(PizzaOrder pizzaOrder) throws InterruptedException {
        Span childSpan = makingTracer.buildSpan("Make Order").start();

        String baker = Math.random() > 0.5 ? "Mario" : "Luigi";
        childSpan.setTag("Baker",baker);
        makingTracer.activateSpan(childSpan);
        try{
            if (pizzaOrder.toppings().contains("pineapple")){
                throw new DisgustingPizzaException(baker+" says - Dude pizza with pineapple is horrible, no way i'm making that!");
            }
            Thread.sleep(300);
        }
        catch (Exception e){
            childSpan.setTag("error",e.getMessage());
            throw e;
        }
        finally {
            childSpan.finish();
        }

    }

    private void deliverOrder(PizzaOrder pizzaOrder) throws InterruptedException {
        Span childSpan = deliveryTracer.buildSpan("Deliver Order").start();
        String deliveryDriver = Math.random() > 0.5 ? "Donkey Kong" : "Yoshi";
        childSpan.setTag("Delivery Person",deliveryDriver);

        childSpan.setTag(Tags.COMPONENT,"delivery");
        try {
            deliveryTracer.activateSpan(childSpan);
            Thread.sleep(600);
        }
        finally {
            childSpan.finish();
        }

    }

    class DisgustingPizzaException extends RuntimeException {
        public DisgustingPizzaException(String message) {
            super(message);
        }
    }

}
