package edu.badpals.FigurasOcultas.service;

import edu.badpals.FigurasOcultas.model.dto.CartaDTO;
import edu.badpals.FigurasOcultas.model.dto.UsuarioDTO;
import edu.badpals.FigurasOcultas.model.entity.Carta;
import edu.badpals.FigurasOcultas.model.repository.CartaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CartaService {

    @Autowired
    private CartaRepository cartaRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public List<Carta> getAllCartas() {
        return StreamSupport.stream(cartaRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Carta> getAllCartasDTO() {
        return StreamSupport.stream(cartaRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, Carta.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public void guardarCarta(String titulo, Integer precio, String descripcion,
                             Boolean activa, MultipartFile imagenFile) {
        try {
            Carta carta = new Carta();
            carta.setTitulo(titulo);
            carta.setPrecio(precio);
            carta.setDescripcion(descripcion);
            carta.setActiva(activa != null && activa);

            if (!imagenFile.isEmpty()) {
                carta.setImagen(imagenFile.getBytes());
            }

            cartaRepository.save(carta);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] obtenerImagenCarta(Long id) {
        Carta carta = cartaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carta no encontrada"));
        if (carta.getImagen() == null) {
            throw new RuntimeException("La carta no tiene imagen");
        }
        return carta.getImagen();
    }
}