package edu.badpals.FigurasOcultas.service;
import edu.badpals.FigurasOcultas.model.dto.CartaDTO;
import edu.badpals.FigurasOcultas.model.entity.Carta;
import edu.badpals.FigurasOcultas.model.repository.CartaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public List<Carta> getAllCartasDTO() {
        return cartaRepository.findAll();
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "cartas", key = "'all-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<CartaDTO> getAllCartasDTO(Pageable pageable) {
        return cartaRepository.findAll(pageable)
                .map(c -> modelMapper.map(c, CartaDTO.class));
    }

    @Transactional(readOnly = true)
    public List<CartaDTO> getCartasDisponibles() {
        List<Carta> cartas = cartaRepository.findByActivaTrue();
        return cartas.stream()
                .map(carta -> modelMapper.map(carta, CartaDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "cartas", key = "'disponibles-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<CartaDTO> getCartasDisponibles(Pageable pageable) {
        return cartaRepository.findByActivaTrue(pageable)
                .map(c -> modelMapper.map(c, CartaDTO.class));
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "cartas", allEntries = true)
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

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"cartas", "cartaImagen"}, allEntries = true)
    public void actualizarCarta(CartaDTO cartaDTO, Long id, MultipartFile imagenFile) throws IOException {

        Carta carta = cartaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carta no encontrada"));

        carta.setTitulo(cartaDTO.getTitulo());
        carta.setPrecio(cartaDTO.getPrecio());
        carta.setDescripcion(cartaDTO.getDescripcion());
        carta.setActiva(cartaDTO.getActiva());
        if (imagenFile != null && !imagenFile.isEmpty()) {
            carta.setImagen(imagenFile.getBytes());
        }

        cartaRepository.save(carta);
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "cartaImagen", key = "#id")
    public byte[] obtenerImagenCarta(Long id) {
        Carta carta = cartaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carta no encontrada"));
        if (carta.getImagen() == null) {
            throw new RuntimeException("La carta no tiene imagen");
        }
        return carta.getImagen();
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"cartas", "cartaImagen"}, allEntries = true)
    public void borrarCarta(Long id) {
        Carta carta = cartaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carta no encontrada"));
        cartaRepository.delete(carta);
    }

    @Transactional(readOnly = true)
    public CartaDTO getCartaById(Long id) {
        Carta carta = cartaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carta no encontrada"));
        return modelMapper.map(carta, CartaDTO.class);
    }
}