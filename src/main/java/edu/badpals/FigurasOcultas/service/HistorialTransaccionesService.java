package edu.badpals.FigurasOcultas.service;
import edu.badpals.FigurasOcultas.model.dto.CartaDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.*;
import edu.badpals.FigurasOcultas.model.repository.CartaUsuarioRepository;
import edu.badpals.FigurasOcultas.model.repository.HistorialTransaccionesRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class HistorialTransaccionesService {

    @Autowired
    private HistorialTransaccionesRepository htr;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CartaService cartaService;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public List<HistorialTransacciones> getHistorialTransaccionesByUsuario(Long usuarioId) {
        return htr.findByAlumnoId(usuarioId);
    }

    @Transactional
    public void addCompraToHistorial(Long usuarioId, Long idCarta) {

        UsuarioDTO usuarioDTO = usuarioService.getUserById(usuarioId);
        CartaDTO cartaDTO = cartaService.getCartaById(idCarta);

        HistorialTransacciones historial = new HistorialTransacciones();
        historial.setAlumno(modelMapper.map(usuarioDTO, Usuario.class));
        historial.setTipo(TipoHistorial.COMPRA);
        historial.setDescripcion("Has comprado \"" + cartaDTO.getTitulo() + "\"");
        historial.setFecha(ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toInstant());
        historial.setElectroniosEnMomento(Integer.valueOf(usuarioDTO.getTarjetaAlumno().getElectronios()));

        htr.save(historial);
    }

    @Transactional
    public void addUsoToHistorial(Long usuarioId, Long idCarta) {

        UsuarioDTO usuarioDTO = usuarioService.getUserById(usuarioId);
        CartaDTO cartaDTO = cartaService.getCartaById(idCarta);

        HistorialTransacciones historial = new HistorialTransacciones();
        historial.setAlumno(modelMapper.map(usuarioDTO, Usuario.class));
        historial.setTipo(TipoHistorial.USO);
        historial.setDescripcion("Has usado \"" + cartaDTO.getTitulo() + "\"");
        historial.setFecha(ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toInstant());
        historial.setElectroniosEnMomento(Integer.valueOf(usuarioDTO.getTarjetaAlumno().getElectronios()));

        htr.save(historial);
    }

    @Transactional
    public void addMoreElectroniosToHistorial(Long usuarioId, int electronios) {

        UsuarioDTO usuarioDTO = usuarioService.getUserById(usuarioId);

        HistorialTransacciones historial = new HistorialTransacciones();
        historial.setAlumno(modelMapper.map(usuarioDTO, Usuario.class));
        historial.setTipo(TipoHistorial.GANAR_ELECTRONIOS);
        historial.setDescripcion("Has recibido " + electronios + " " + (electronios == 1 ? "electronio" : "electronios"));
        historial.setFecha(ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toInstant());
        historial.setElectroniosEnMomento(Integer.valueOf(usuarioDTO.getTarjetaAlumno().getElectronios()));

        htr.save(historial);
    }

    @Transactional
    public void addLessElectroniosToHistorial(Long usuarioId, int electronios) {

        UsuarioDTO usuarioDTO = usuarioService.getUserById(usuarioId);

        HistorialTransacciones historial = new HistorialTransacciones();
        historial.setAlumno(modelMapper.map(usuarioDTO, Usuario.class));
        historial.setTipo(TipoHistorial.PERDER_ELECTRONIOS);
        historial.setDescripcion("Has perdido " + electronios + " " + (electronios == 1 ? "electronio" : "electronios"));
        historial.setFecha(ZonedDateTime.now(ZoneId.of("Europe/Madrid")).toInstant());
        historial.setElectroniosEnMomento(Integer.valueOf(usuarioDTO.getTarjetaAlumno().getElectronios()));

        htr.save(historial);
    }
}