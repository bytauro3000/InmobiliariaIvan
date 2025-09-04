package com.Inmobiliaria.demo.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

//CONTROL PARA PUBLICACION LOCAL DEL PROYECTO UNIFICADO CON ANGULAR
@Controller
public class ForwardController {

    // Captura todas las rutas que no contengan punto (.) y las reenvía a index.html
	 @RequestMapping(value = "/**/{path:[^\\.]*}")
	    public String forward() {
	        return "forward:/index.html";
	    }
}