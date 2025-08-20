package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

@Service
@Profile("production")
public class InitDbService {

    @Autowired UsuarioService usuarioService;

    @Autowired WebConfigService webConfigService;

    @Transactional
    @PostConstruct
    public void initDatabase() {

        webConfigService.updateWebConfig("Docker Ocultas");

//        UsuarioDTO admin = new UsuarioDTO();
//        admin.setId(1L);
//        admin.setNombre("admin");
//        admin.setEmail("admin@ua");
//        admin.setPassword("123");
//        admin.setRol(RolUsuario.ADMIN);
//
//        usuarioService.saveUser(admin);
    }
}