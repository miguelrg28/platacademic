package edu.pucmm.icc352.events.web;

import edu.pucmm.icc352.events.shared.error.AppException;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;

public final class ApiExceptionMapper {
    private ApiExceptionMapper() {
    }

    public static void register(Javalin app) {
        app.unsafe.routes.exception(AppException.class, (exception, ctx) -> ctx
                .status(exception.statusCode())
                .json(new ApiDtos.ErrorResponse(exception.code(), exception.getMessage())));

        app.unsafe.routes.exception(BadRequestResponse.class, (exception, ctx) -> ctx
                .status(400)
                .json(new ApiDtos.ErrorResponse("bad_request", exception.getMessage())));

        app.unsafe.routes.exception(Exception.class, (exception, ctx) -> {
            ctx.status(500).json(new ApiDtos.ErrorResponse("internal_error", "Ocurrió un error interno."));
            exception.printStackTrace();
        });
    }
}
