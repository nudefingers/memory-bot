package il.nudefingers;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BotApplication.class, args);
	}


	@Bean
	public CommandLineRunner run(ApplicationContext appContext) {
		return args -> {

			String[] beans = appContext.getBeanDefinitionNames();
			//Arrays.stream(beans).sorted().forEach(System.out::println);

			//WordsService wordsService = appContext.getBean(WordsService.class);
			//System.out.println(wordsService.getWords());

		};
	}

}
