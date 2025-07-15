package edu.badpals.FigurasOcultas.service;
import edu.badpals.FigurasOcultas.model.entity.Carta;
import edu.badpals.FigurasOcultas.model.entity.CartasUsuario;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.repository.CartaUsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartaUsuarioService {

    @Autowired
    private CartaUsuarioRepository cartaUsuarioRepository;

    @Autowired
    private CartaService cartaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public void comprarCarta(Long usuarioId, Long cartaId) {

       CartasUsuario cartasUsuario = new CartasUsuario();

       cartasUsuario.setAlumno(modelMapper.map(usuarioService.getUserById(usuarioId), Usuario.class));
       cartasUsuario.setCarta(modelMapper.map(cartaService.getCartaById(cartaId), Carta.class));
       cartasUsuario.setFechaAdquisicion(new Date().toInstant());

       cartaUsuarioRepository.save(cartasUsuario);
    }

    @Transactional
    public Map<Long, Integer> getCantidadPorCartaParaAlumno(Long usuarioId) {
        List<Object[]> resultados = cartaUsuarioRepository.countByCartaIdAndUsuarioId(usuarioId);
        Map<Long, Integer> inventario = new HashMap<>();

        for (Object[] fila : resultados) {
            Long cartaId = (Long) fila[0];
            Long cantidad = (Long) fila[1]; // o Integer, depende de tu query
            inventario.put(cartaId, cantidad.intValue());
        }

        return inventario;
    }
}