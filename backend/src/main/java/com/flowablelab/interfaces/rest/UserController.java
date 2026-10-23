package com.flowablelab.interfaces.rest;

import com.flowablelab.domain.user.DemoUser;
import com.flowablelab.interfaces.rest.dto.Responses.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários demonstrativos", description = "Perfis fictícios para o seletor do cabeçalho (sem autenticação)")
public class UserController {

    @GetMapping
    @Operation(summary = "Lista os perfis demonstrativos disponíveis")
    public List<UserResponse> list() {
        return Arrays.stream(DemoUser.values()).map(UserResponse::from).toList();
    }
}
