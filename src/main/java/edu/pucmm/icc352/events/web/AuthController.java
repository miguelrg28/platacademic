package edu.pucmm.icc352.events.web;

import edu.pucmm.icc352.events.application.security.AuthGuard;
import edu.pucmm.icc352.events.application.service.AuthService;
import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public final class AuthController {
    private final AuthService authService;
    private final AuthGuard authGuard;

    public AuthController(AuthService authService, AuthGuard authGuard) {
        this.authService = authService;
        this.authGuard = authGuard;
    }

    public void register(Javalin app) {
        app.unsafe.routes.apiBuilder(() -> {
            post("/api/auth/register", ctx -> {
                ApiDtos.RegisterRequest request = ctx.bodyAsClass(ApiDtos.RegisterRequest.class);
                var user = authService.register(new AuthService.RegisterCommand(
                        request.username(),
                        request.fullName(),
                        request.email(),
                        request.password()
                ));
                ctx.status(201).json(ApiDtos.toUserResponse(user));
            });

            post("/api/auth/login", ctx -> {
                ApiDtos.LoginRequest request = ctx.bodyAsClass(ApiDtos.LoginRequest.class);
                var user = authService.login(new AuthService.LoginCommand(request.username(), request.password()));
                authGuard.startSession(ctx, user);
                ctx.json(ApiDtos.toUserResponse(authService.getUserById(user.id())));
            });

            post("/api/auth/logout", ctx -> {
                authGuard.endSession(ctx);
                ctx.status(204);
            });

            get("/api/auth/me", ctx -> {
                var actor = authGuard.requireAuthenticated(ctx);
                var user = authService.getUserById(actor.id());
                ctx.json(ApiDtos.toUserResponse(user));
            });
        });
    }
}
