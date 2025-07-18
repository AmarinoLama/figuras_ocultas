package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.dto.CartaDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Carta;
import edu.badpals.FigurasOcultas.model.entity.CartasUsuario;
import edu.badpals.FigurasOcultas.model.entity.Usuario;
import edu.badpals.FigurasOcultas.model.repository.CartaUsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;
import java.time.ZoneId;
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
    public boolean comprarCarta(Long usuarioId, Long cartaId) {

        UsuarioDTO usuario = usuarioService.getUserById(usuarioId);
        CartaDTO carta = cartaService.getCartaById(cartaId);

        if (usuario.getTarjetaAlumno().getElectronios() < carta.getPrecio()) {
            return false;
        } else {

            CartasUsuario cartasUsuario = new CartasUsuario();

            cartasUsuario.setAlumno(modelMapper.map(usuario, Usuario.class));
            cartasUsuario.setCarta(modelMapper.map(carta, Carta.class));
            cartasUsuario.setFechaAdquisicion(ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toInstant());
            cartasUsuario.setUsada(false);

            cartaUsuarioRepository.save(cartasUsuario);

            usuario.getTarjetaAlumno().setElectronios((byte) (usuario.getTarjetaAlumno().getElectronios() - carta.getPrecio()));
            usuarioService.saveUser(usuario);

            return true;
        }
    }

    @Transactional
    public Map<Long, Integer> getCartasByAlumno(Long usuarioId) {
        List<Object[]> resultados = cartaUsuarioRepository.countByCartaIdAndUsuarioId(usuarioId);
        Map<Long, Integer> inventario = new HashMap<>();

        for (Object[] fila : resultados) {
            Long cartaId = (Long) fila[0];
            Long cantidad = (Long) fila[1];
            inventario.put(cartaId, cantidad.intValue());
        }

        return inventario;
    }

    public List<CartaDTO> getCartasDisponiblesByUsuario(Long usuarioId) {
        return cartaUsuarioRepository.findByAlumnoIdAndUsadaFalse(usuarioId).stream()
                .map(CartasUsuario::getCarta)
                .map(carta -> modelMapper.map(carta, CartaDTO.class))
                .distinct()
                .toList();
    }

    public boolean usarCarta(Long idCarta, Long idUsuario) {
        List<CartasUsuario> cartasUsuarios = cartaUsuarioRepository.findByCartaIdAndAlumnoId(idCarta, idUsuario);
        for (CartasUsuario cartaUsuario : cartasUsuarios) {
            if (!cartaUsuario.getUsada()) {
                cartaUsuario.setUsada(true);
                cartaUsuarioRepository.save(cartaUsuario);
                return true;
            }
        }
        return false;
    }
}