package com.socialpulse.app.common.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@Profile("dev")
public class ScalarApiDocsController {

    @GetMapping("/scalar")
    public void scalarApiDocs(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().write("""
            <!doctype html>
            <html>
              <head>
                <title>Social Pulse API Documentation</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
              </head>
              <body>
                <script
                  id="api-reference"
                  data-url="/v3/api-docs"
                  data-configuration='{"theme":"purple","darkMode":true}'></script>
                <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
              </body>
            </html>
            """);
    }
}
