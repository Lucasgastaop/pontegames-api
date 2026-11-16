package br.com.fiap.pontegames.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	public static final String BEARER = "bearer";

	@Bean
	OpenAPI pontegamesOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Ponte Games API")
						.version("1.0")
						.description("""
								Backend da CitizenTech Ponte Games, consumido pelo app Mobile.
								Faça login em /api/auth/login e cole o token em Authorize.
								"""))
				.addSecurityItem(new SecurityRequirement().addList(BEARER))
				.components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}

}
