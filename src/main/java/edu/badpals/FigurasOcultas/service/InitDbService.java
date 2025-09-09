package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.entity.RolUsuario;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

@Service
@Profile({"production", "dev"})
public class InitDbService {

    @Autowired UsuarioService usuarioService;

    @Autowired WebConfigService webConfigService;

    @Transactional
    @PostConstruct
    public void initDatabase() {

        webConfigService.updateWebConfig("Docker Ocultas");

//        Usuario susiAccount = new Usuario();
//        susiAccount.setNombre("Susi");
//        susiAccount.setEmail("susiveiga@figurasocultas");
//        susiAccount.setPassword("123");
//        susiAccount.setRol(RolUsuario.ADMIN);
//        usuarioService.saveAdmin(susiAccount);
//
//        Usuario andreaAccount = new Usuario();
//        andreaAccount.setNombre("Andrea");
//        andreaAccount.setEmail("admin@ua");
//        andreaAccount.setPassword("123");
//        andreaAccount.setRol(RolUsuario.ADMIN);
//        usuarioService.saveAdmin(andreaAccount);
    }
}