package br.ifsp.guesthouse.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
@Tag(name = "Quartos", description = "Operações relacionadas aos usuarios")
public class RoomController {

}
