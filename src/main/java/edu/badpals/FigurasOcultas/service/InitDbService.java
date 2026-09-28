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

//        webConfigService.updateWebConfig("Docker Ocultas");

        crearAdminSiNoExiste("Susi", "susiveiga@ua", "123");
        crearAdminSiNoExiste("Andrea", "andreavazquez@ua", "123");
    }

    /** Crea la cuenta de administrador solo si todavía no existe (evita duplicados). */
    private void crearAdminSiNoExiste(String nombre, String email, String password) {
        if (!usuarioService.checkValidEmail(email)) {
            return;
        }
        Usuario cuenta = new Usuario();
        cuenta.setNombre(nombre);
        cuenta.setEmail(email);
        cuenta.setPassword(password);
        cuenta.setRol(RolUsuario.ADMIN);
        usuarioService.saveAdmin(cuenta);
    }
}