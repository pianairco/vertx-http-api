package ir.piana.dev.common.starter;

import io.vertx.core.Vertx;
import io.vertx.ext.web.common.template.TemplateEngine;
import ir.piana.dev.vertxthymeleaf.ThymeleafTemplateEngine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication(scanBasePackages = {"ir.piana.dev"})
@EnableScheduling
public class StarterApplication {

	public static void main(String[] args) {
		new SpringApplication(StarterApplication.class).run(args);
	}

	@Bean
	public Executor taskExecutor() {
		return Executors.newFixedThreadPool(10);
	}

	/*@Bean
	TemplateEngine thymeleafTemplateEngine(Vertx vertx) {
		return ThymeleafTemplateEngine.create(vertx);
	}*/
}
