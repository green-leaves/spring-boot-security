package uet.hungnh.metrics;

import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class ApplicationContextMetrics implements PublicMetrics {

    private ApplicationContext applicationContext;

    public ApplicationContextMetrics(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Collection<Metric<?>> metrics() {
        Collection<Metric<?>> metrics = new ArrayList<>();

        Metric<Integer> beanCounts = new Metric<>("counter.spring.beans.definitions", applicationContext.getBeanDefinitionCount());
        metrics.add(beanCounts);

        Metric<Integer> controllerCounts = new Metric<Integer>("counter.spring.controllers", applicationContext.getBeanNamesForAnnotation(Controller.class).length);
        metrics.add(controllerCounts);

        return metrics;
    }
}
