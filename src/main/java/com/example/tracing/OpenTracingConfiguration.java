package com.example.tracing;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import brave.propagation.B3Propagation;
import brave.propagation.ExtraFieldPropagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.BoundarySampler;
import brave.sampler.CountingSampler;
import brave.sampler.Sampler;
import io.opentracing.util.GlobalTracer;
import lombok.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import static zipkin2.codec.SpanBytesEncoder.JSON_V1;
import static zipkin2.codec.SpanBytesEncoder.JSON_V2;
import static zipkin2.reporter.BytesMessageEncoder.PROTO3;

@EnableConfigurationProperties(ZipkinConfigurationProperties.class)
@Configuration
public class OpenTracingConfiguration {

//    @Bean
//    public Tracing doSomething(){
//        Tracing build = Tracing.newBuilder().build();
//        BraveTracer braveTracer = BraveTracer.create(build);
//        GlobalTracer.register(braveTracer);
//        return build;
//    }

    private String serviceName ="pizza-order-taking";

    @Bean("pizza-order-taking")
    public io.opentracing.Tracer tracer1(Reporter<Span> reporter, Sampler sampler) {

        final Tracing.Builder builder = Tracing.newBuilder()
                .sampler(sampler)
                .localServiceName("pizza-order-taking")
//                .propagationFactory(ExtraFieldPropagation.newFactory(B3Propagation.FACTORY, "user-name"))
//                .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
//                        .addScopeDecorator(MDCScopeDecorator.create()) // puts trace IDs into logs
//                        .build()
//                )
                .spanReporter(reporter);


        return BraveTracer.create(builder.build());
    }

    @Bean("pizza-making")
    public io.opentracing.Tracer tracer2(Reporter<Span> reporter, Sampler sampler) {

        final Tracing.Builder builder = Tracing.newBuilder()
                .sampler(sampler)
                .localServiceName("pizza-order-making")
                .spanReporter(reporter);

        return BraveTracer.create(builder.build());
    }

    @Bean("pizza-delivery")
    public io.opentracing.Tracer tracer3(Reporter<Span> reporter, Sampler sampler) {

        final Tracing.Builder builder = Tracing.newBuilder()
                .sampler(sampler)
                .localServiceName("pizza-delivery")
                .spanReporter(reporter);

        return BraveTracer.create(builder.build());
    }



    @Bean
    @ConditionalOnMissingBean
    public Reporter<Span> reporter(ZipkinConfigurationProperties properties) {
        String url = properties.getHttpSender().getBaseUrl();
        if (properties.getHttpSender().getEncoder().equals(JSON_V2) || properties.getHttpSender().getEncoder().equals(PROTO3)) {
            url += (url.endsWith("/") ? "" : "/") + "api/v2/spans";
        } else if (properties.getHttpSender().getEncoder().equals(JSON_V1)) {
            url += (url.endsWith("/") ? "" : "/") + "api/v1/spans";
        }
        OkHttpSender sender = OkHttpSender.newBuilder()
                .endpoint(url)
                .encoding(properties.getHttpSender().getEncoder().encoding())
                .build();

        return AsyncReporter.builder(sender).build(properties.getHttpSender().getEncoder());
    }


    @Bean
    @ConditionalOnMissingBean
    public Sampler sampler(ZipkinConfigurationProperties properties) {
        if (properties.getBoundarySampler().getRate() != null) {
            return BoundarySampler.create(properties.getBoundarySampler().getRate());
        }

        if (properties.getCountingSampler().getRate() != null) {
            return CountingSampler.create(properties.getCountingSampler().getRate());
        }

        return Sampler.ALWAYS_SAMPLE;
    }
}
